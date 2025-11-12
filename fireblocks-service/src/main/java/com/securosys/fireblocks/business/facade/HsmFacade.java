// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HsmFacade {

    private final TsbService tsbService;
    private final CustomServerProperties properties;
    private final TsbProperties tsbProperties;
    private final CryptoUtil cryptoUtil;

    public String generateKeyPair(String label, String password, String algorithm, int size) {
        tsbService.createOrUpdateKey(label, password, algorithm, null, size);
        return tsbService.getPublicKey(label, password);
    }

    public RequestStatusResponseDto sign(String label, String password, String payload, String algorithm, String metadata, String metadataSignature) {

        String tsbSigningAlgorithm;
        switch (algorithm) {
            case "ECDSA_SECP256K1" -> tsbSigningAlgorithm = "NONE_WITH_ECDSA";
            case "EDDSA_ED25519" -> tsbSigningAlgorithm = "EDDSA";
            default -> throw new BusinessException("Unsupported algorithm: " + algorithm, BusinessReason.ERROR_INVALID_ALGORITHM);
        }

//        if ("EDDSA".equalsIgnoreCase(tsbSigningAlgorithm)) {
//            try {
//                byte[] rawBytes = hexStringToByteArray(payload);
//
//                MessageDigest digest = MessageDigest.getInstance("SHA-256");
//                byte[] hashed = digest.digest(rawBytes);
//                log.info("Message pre-hashed with SHA-256 for EDDSA");
//
//                payloadForTsb = Base64.getEncoder().encodeToString(hashed);
//                payloadType = "UNSPECIFIED";
//            } catch (NoSuchAlgorithmException e) {
//                throw new RuntimeException("Failed to prehash HEX message for EDDSA", e);
//            }
//        }

        //String payloadBase64 = Base64.getEncoder().encodeToString(messageToSignBytes);

        String signatureId = tsbService.sign(label, password, payload, "HEX", "RAW", tsbSigningAlgorithm, metadata, metadataSignature);
        RequestStatusResponseDto requestStatusResponseDto = tsbService.getRequest(signatureId);

        requestStatusResponseDto = waitForApproval(requestStatusResponseDto, signatureId);

        return requestStatusResponseDto;
    }

    public String getPublicKeyString(){
        PublicKey publicKey = getPublicKey();
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public boolean verify(String payloadSignature, String serviceName, String payload) {

        try {
            if (!properties.getServiceName().equals(serviceName)){
                return false;
            }

            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            PublicKey publicKey = getPublicKey();
            String algorithm = getSignatureAlgorithm(publicKey);

            byte[] signatureBytes = HexFormat.of().parseHex(payloadSignature);

            if (algorithm.equals("SHA256withECDSA") && signatureBytes.length == 64) {
                signatureBytes = convertRawEcdsaToDer(signatureBytes);
            }

            Signature sig = Signature.getInstance(algorithm, "BC");
            sig.initVerify(publicKey);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));

            return sig.verify(signatureBytes);

        } catch (Exception e) {
            return false;
        }
    }

//    public boolean verifyEd25519(String payloadSignatureHex, String payload, PublicKey publicKey) {
//        try {
//            byte[] signatureBytes = Hex.decode(payloadSignatureHex);
//            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
//
//            Signature sig = Signature.getInstance("Ed25519", "BC");
//            sig.initVerify(publicKey);
//            sig.update(payloadBytes);
//            return sig.verify(signatureBytes);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    private byte[] convertRawEcdsaToDer(byte[] rawSignature) throws IOException {
        if (rawSignature.length != 64) {
            return rawSignature; // already DER or invalid length
        }

        int half = rawSignature.length / 2;
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(rawSignature, 0, half));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(rawSignature, half, rawSignature.length));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DERSequenceGenerator seq = new DERSequenceGenerator(bos);
        seq.addObject(new ASN1Integer(r));
        seq.addObject(new ASN1Integer(s));
        seq.close();
        return bos.toByteArray();
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

    private static String getSignatureAlgorithm(PublicKey publicKey) {
        String alg = publicKey.getAlgorithm();
        return switch (alg) {
            case "RSA" -> "SHA256withRSA";
            case "EC" -> "SHA256withECDSA";
            case "Ed25519" -> "Ed25519"; // bez SHA
            default -> throw new BusinessException("Unsupported key algorithm: " + alg,
                    BusinessReason.ERROR_INVALID_ALGORITHM);
        };
    }
}
