// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.facade;

import com.securosys.fireblocks.business.dto.ServiceName;
import com.securosys.fireblocks.business.dto.response.KeyAttributesDto;
import com.securosys.fireblocks.business.dto.response.LicenseResponseDto;
import com.securosys.fireblocks.business.dto.response.RequestStatusResponseDto;
import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.service.TsbService;
import com.securosys.fireblocks.business.service.TsbService.PayloadType;
import com.securosys.fireblocks.business.service.TsbService.SignatureAlgorithm;
import com.securosys.fireblocks.business.service.TsbService.SignatureType;
import com.securosys.fireblocks.business.util.CryptoUtil;
import com.securosys.fireblocks.configuration.CustomServerProperties;
import com.securosys.fireblocks.configuration.TsbProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HsmFacade {

    private enum FireblocksSignatureAlgorithm {
        ECDSA_SECP256K1(SignatureAlgorithm.NONE_WITH_ECDSA),
        EDDSA_ED25519(SignatureAlgorithm.EDDSA);

        private final SignatureAlgorithm tsbAlgorithm;

        FireblocksSignatureAlgorithm(SignatureAlgorithm tsbAlgorithm) {
            this.tsbAlgorithm = tsbAlgorithm;
        }

        private SignatureAlgorithm getTsbAlgorithm() {
            return tsbAlgorithm;
        }

        private static FireblocksSignatureAlgorithm fromString(String algorithm) {
            for (FireblocksSignatureAlgorithm value : values()) {
                if (value.name().equals(algorithm)) {
                    return value;
                }
            }
            throw new BusinessException("Unsupported algorithm: " + algorithm, BusinessReason.ERROR_INVALID_ALGORITHM);
        }
    }

    private static final long APPROVAL_TIMEOUT_MILLIS = 120_000;
    private static final long APPROVAL_POLL_INTERVAL_SECONDS = 5;

    private final TsbService tsbService;
    private final CustomServerProperties properties;
    private final TsbProperties tsbProperties;
    private final CryptoUtil cryptoUtil;

    public String generateKeyPair(String label, String password, String algorithm, String curveOid, int size) {
        tsbService.createOrUpdateKey(label, password, algorithm, curveOid, size);
        KeyAttributesDto keyAttributes = tsbService.getPublicKey(label, password);
        return keyAttributes.getPublicKey();
    }

    public RequestStatusResponseDto sign(String label, String password, String payload, String algorithm, String metadata, String metadataSignature) {
        SignatureAlgorithm tsbSigningAlgorithm = FireblocksSignatureAlgorithm.fromString(algorithm).getTsbAlgorithm();

        String signatureId = tsbService.sign(label, password, payload, PayloadType.HEX, SignatureType.RAW, tsbSigningAlgorithm, metadata, metadataSignature);
        RequestStatusResponseDto requestStatusResponseDto = tsbService.getRequest(signatureId);

        requestStatusResponseDto = waitForApproval(requestStatusResponseDto, signatureId);

        return requestStatusResponseDto;
    }

    public boolean verify(byte[] payloadSignature, ServiceName serviceName, String payload) {

        try {
            List<PublicKey> publicKeys = getPublicKeys(serviceName);
            for (PublicKey publicKey : publicKeys) {
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(publicKey);
                sig.update(payload.getBytes(StandardCharsets.UTF_8));

                if (sig.verify(payloadSignature)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("Signature verification failed for service={}, reason={}", serviceName, safeErrorMessage(e));
            return false;
        }
    }

    private String safeErrorMessage(Exception e) {
        if (e instanceof BusinessException businessException) {
            return businessException.getReason().name();
        }
        return e.getClass().getSimpleName();
    }

    private List<PublicKey> getPublicKeys(ServiceName serviceName) {
        List<PublicKey> publicKeys = new ArrayList<>();
        boolean matchedSignatureService = false;

        if (serviceName != null && properties.getSignatureServices() != null) {
            for (Map.Entry<String, CustomServerProperties.FireblocksSignatureService> entry : properties.getSignatureServices().entrySet()) {
                String key = entry.getKey();
                CustomServerProperties.FireblocksSignatureService serviceConfig = entry.getValue();
                if (serviceConfig == null) {
                    continue;
                }

                String configuredServiceName = serviceConfig.getServiceName();
                if (configuredServiceName == null || configuredServiceName.isBlank()) {
                    configuredServiceName = key;
                }

                if (serviceName.name().equals(configuredServiceName)) {
                    matchedSignatureService = true;
                    if (!serviceConfig.isEnabled()) {
                        log.warn("Signature verification is disabled for service: {}", serviceName);
                        return publicKeys;
                    }
                    publicKeys.addAll(loadPublicKeys(
                            serviceConfig.getFireblocksSignatureCertificates(),
                            serviceConfig.getFireblocksSignaturePublicKeys()));
                }
            }
        }

        if (!matchedSignatureService && publicKeys.isEmpty() && properties.getServiceName() != null && properties.getServiceName().equals(serviceName.name())) {
            List<String> certificateLocations = new ArrayList<>();
            certificateLocations.add(properties.getFireblocksSignatureCertificate());
            certificateLocations.addAll(properties.getFireblocksSignatureCertificates());

            List<String> publicKeyLocations = new ArrayList<>();
            publicKeyLocations.add(properties.getFireblocksSignaturePublicKey());
            publicKeyLocations.addAll(properties.getFireblocksSignaturePublicKeys());

            publicKeys.addAll(loadPublicKeys(certificateLocations, publicKeyLocations));
        }

        if (publicKeys.isEmpty()) {
            throw new BusinessException("No Fireblocks verification key or certificate configured for service: " + serviceName,
                    BusinessReason.ERROR_INVALID_CONFIG_INPUT);
        }
        return publicKeys;
    }

    private List<PublicKey> loadPublicKeys(List<String> certificateLocations, List<String> publicKeyLocations) {
        List<PublicKey> publicKeys = new ArrayList<>();

        if (certificateLocations != null) {
            for (String certificateLocation : certificateLocations) {
                if (certificateLocation != null && !certificateLocation.isBlank()) {
                    X509Certificate fireblocksCertificate = cryptoUtil.loadCertificate(certificateLocation);
                    publicKeys.add(fireblocksCertificate.getPublicKey());
                }
            }
        }

        if (publicKeyLocations != null) {
            for (String publicKeyLocation : publicKeyLocations) {
                if (publicKeyLocation != null && !publicKeyLocation.isBlank()) {
                    publicKeys.add(cryptoUtil.getPublicKeyFromBase64(publicKeyLocation));
                }
            }
        }

        return publicKeys;
    }

    private RequestStatusResponseDto waitForApproval(RequestStatusResponseDto requestStatusResponseDto, String requestId) {
        long startTime = System.currentTimeMillis();
        while ("PENDING".equals(requestStatusResponseDto.getStatus())) {

            if (System.currentTimeMillis() - startTime > APPROVAL_TIMEOUT_MILLIS) {
                log.warn("Approval timeout reached for request: {}", requestId);
                return requestStatusResponseDto;
            }

            try {
                TimeUnit.SECONDS.sleep(APPROVAL_POLL_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new BusinessException("Interrupted while waiting for response", BusinessReason.ERROR_IN_SUBSYSTEM);
            }
            requestStatusResponseDto = tsbService.getRequest(requestId);
        }

        return requestStatusResponseDto;
    }


    public boolean isLicensed(){

        if (tsbProperties.isAirGapped()){
            return false;
        }
        LicenseResponseDto license = tsbService.getLicense();
        return license.getClientFlags() == null
                || !license.getClientFlags().contains("FIREBLOCKS_AGENT");

    }

    public String generateCertificateRequest(String assetKeyName, String password, SignatureAlgorithm signatureAlgorithm, boolean skaKey) {

        if (skaKey){
            String signatureId = tsbService.generateCertificateRequest(assetKeyName, password, signatureAlgorithm);
            RequestStatusResponseDto requestStatusResponseDto = tsbService.getRequest(signatureId);

            requestStatusResponseDto = waitForApproval(requestStatusResponseDto, signatureId);

            return requestStatusResponseDto.getResult();
        } else {
            return tsbService.generateSynchronousCertificateRequest(assetKeyName, password, signatureAlgorithm);
        }
    }

    public String signMessageForOwnership(String label, String password, String payload, SignatureAlgorithm algorithm, String metadata, String metadataSignature) {

        return signMessageForOwnershipRequest(label, password, payload, algorithm, metadata, metadataSignature).getResult();
    }

    public RequestStatusResponseDto signMessageForOwnershipRequest(String label, String password, String payload, SignatureAlgorithm algorithm, String metadata, String metadataSignature) {

        String signatureId = tsbService.sign(label, password, payload, PayloadType.UNSPECIFIED, SignatureType.RAW, algorithm, metadata, metadataSignature);
        RequestStatusResponseDto requestStatusResponseDto = tsbService.getRequest(signatureId);

        requestStatusResponseDto = waitForApproval(requestStatusResponseDto, signatureId);

        return requestStatusResponseDto;
    }

}
