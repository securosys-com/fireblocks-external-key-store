// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.facade;

import com.securosys.fireblocks.business.dto.response.KeyAttributesDto;
import com.securosys.fireblocks.business.dto.response.LicenseResponseDto;
import com.securosys.fireblocks.business.dto.response.RequestStatusResponseDto;
import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.service.TsbService;
import com.securosys.fireblocks.business.util.CryptoUtil;
import com.securosys.fireblocks.configuration.CustomServerProperties;
import com.securosys.fireblocks.configuration.TsbProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HsmFacade {

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

        String tsbSigningAlgorithm;
        switch (algorithm) {
            case "ECDSA_SECP256K1" -> tsbSigningAlgorithm = "NONE_WITH_ECDSA";
            case "EDDSA_ED25519" -> tsbSigningAlgorithm = "EDDSA";
            default -> throw new BusinessException("Unsupported algorithm: " + algorithm, BusinessReason.ERROR_INVALID_ALGORITHM);
        }


        String signatureId = tsbService.sign(label, password, payload, "HEX", "RAW", tsbSigningAlgorithm, metadata, metadataSignature);
        RequestStatusResponseDto requestStatusResponseDto = tsbService.getRequest(signatureId);

        requestStatusResponseDto = waitForApproval(requestStatusResponseDto, signatureId);

        return requestStatusResponseDto;
    }

    public boolean verify(byte[] payloadSignature, String serviceName, String payload) {

        try {
            if (!properties.getServiceName().equals(serviceName)){
                return false;
            }

            PublicKey publicKey = getPublicKey();

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));

            return sig.verify(payloadSignature);

        } catch (Exception e) {
            return false;
        }
    }

    private PublicKey getPublicKey() {
        PublicKey publicKey;
        if (properties.getFireblocksSignatureCertificate() != null && !properties.getFireblocksSignatureCertificate().isBlank()){
            X509Certificate fireblocksCertificate = cryptoUtil.loadCertificate(properties.getFireblocksSignatureCertificate());
            publicKey = fireblocksCertificate.getPublicKey();
        } else if (properties.getFireblocksSignaturePublicKey() != null && !properties.getFireblocksSignaturePublicKey().isBlank()){
            publicKey = cryptoUtil.getPublicKeyFromBase64(properties.getFireblocksSignaturePublicKey());
        } else {
            throw new BusinessException("No Fireblocks verification key or certificate configured",
                    BusinessReason.ERROR_INVALID_CONFIG_INPUT);
        }
        return publicKey;
    }

    private RequestStatusResponseDto waitForApproval(RequestStatusResponseDto requestStatusResponseDto, String decryptRequestId) {
        long timeoutMillis = 120000;
        long startTime = System.currentTimeMillis();
        while ("PENDING".equals(requestStatusResponseDto.getStatus())) {

            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                return requestStatusResponseDto;
            }

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                throw new BusinessException("Interrupted while waiting for response", BusinessReason.ERROR_IN_SUBSYSTEM);
            }
            requestStatusResponseDto = tsbService.getRequest(decryptRequestId);
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

    public String generateCertificateRequest(String assetKeyName, String password, String signatureAlgorithm, boolean skaKey) {

        if (skaKey){
            String signatureId = tsbService.generateCertificateRequest(assetKeyName, password, signatureAlgorithm);
            RequestStatusResponseDto requestStatusResponseDto = tsbService.getRequest(signatureId);

            requestStatusResponseDto = waitForApproval(requestStatusResponseDto, signatureId);

            return requestStatusResponseDto.getResult();
        } else {
            return tsbService.generateSynchronousCertificateRequest(assetKeyName, password, signatureAlgorithm);
        }
    }

    public String signMessageForOwnership(String label, String password, String payload, String algorithm, String metadata, String metadataSignature) {

        String signatureId = tsbService.sign(label, password, payload, "UNSPECIFIED", "RAW", algorithm, metadata, metadataSignature);
        RequestStatusResponseDto requestStatusResponseDto = tsbService.getRequest(signatureId);

        requestStatusResponseDto = waitForApproval(requestStatusResponseDto, signatureId);

        return requestStatusResponseDto.getResult();
    }

}
