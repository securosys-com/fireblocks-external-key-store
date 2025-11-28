// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.securosys.fireblocks.business.dto.request.CreateValidationKeyRequest;
import com.securosys.fireblocks.business.dto.request.CreateValidationsRequest;
import com.securosys.fireblocks.business.dto.request.ProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.request.ValidationProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.response.KeyAttributesDto;
import com.securosys.fireblocks.business.dto.response.ProofOfOwnershipResponse;
import com.securosys.fireblocks.business.dto.response.ValidationProofOfOwnershipResponse;
import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.facade.HsmFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelperService {

    private final HsmFacade hsmFacade;
    private final TsbService tsbService;
    private final String KEY_LABEL = "FIREBLOCKS_VALIDATION_KEY";

    public String createValidationKey(CreateValidationKeyRequest request) {

        String algorithm = request.getAlgorithm();
        if (algorithm == null || algorithm.isBlank()) {
            algorithm = "RSA";
        } else {
            algorithm = algorithm.toUpperCase();
            if (!List.of("RSA", "EC", "ED").contains(algorithm)) {
                throw new BusinessException("Unsupported algorithm: " + algorithm, BusinessReason.ERROR_INVALID_ALGORITHM);
            }
        }

        Integer keySize = request.getKeySize();
        if ("RSA".equals(algorithm)) {
            if (keySize == null || keySize <= 0) {
                keySize = 2048;
            }
        } else {
            if (keySize != null) {
                throw new BusinessException("keySize must not be provided for EC or ED algorithms",
                        BusinessReason.ERROR_INVALID_JSON);
            }
            keySize = 0;
        }

        String curveOid = request.getCurveOid();
        if ("EC".equals(algorithm) || "ED".equals(algorithm)) {

            if (curveOid == null || curveOid.isBlank()) {
                throw new BusinessException("curveOid is mandatory for EC or ED algorithms",
                        BusinessReason.ERROR_INVALID_JSON);
            }

        } else if ("RSA".equals(algorithm) && curveOid != null) {
                throw new BusinessException("curveOid must not be provided for RSA algorithm",
                        BusinessReason.ERROR_INVALID_JSON);
            }

        String publicKey = hsmFacade.generateKeyPair(KEY_LABEL, null, algorithm, curveOid, keySize);
        String signatureAlgorithm = selectSignatureAlgorithm(algorithm, keySize, curveOid);
        tsbService.syncSelfSign(KEY_LABEL, null, signatureAlgorithm);
        return toPemFormat(publicKey);
    }

    public static String toPemFormat(String base64Key) {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");

        for (int i = 0; i < base64Key.length(); i += 64) {
            int endIndex = Math.min(i + 64, base64Key.length());
            pem.append(base64Key, i, endIndex).append("\n");
        }

        pem.append("-----END PUBLIC KEY-----\n");
        return pem.toString();
    }

    public String generateCsr(CreateValidationsRequest request) {

        String signatureAlgorithm = mapAlgorithmForCsr(request.getAssetKeyAlgorithm());
        return hsmFacade.generateCertificateRequest(request.getAssetKeyName(), null, signatureAlgorithm, request.getIsSkaKey());
    }

    public String signCsr(String csr) {
        KeyAttributesDto keyAttributes = tsbService.getPublicKey(KEY_LABEL, null);
        String signatureAlgorithm = selectSignatureAlgorithm(keyAttributes.getAlgorithm(), keyAttributes.getKeySize(), keyAttributes.getCurveOid());
        return tsbService.signCertificate(KEY_LABEL, null, signatureAlgorithm, csr);
    }

    private String mapAlgorithmForCsr(String assetKeyAlgorithm) {
        if (assetKeyAlgorithm == null) {
            throw new BusinessException("Asset key algorithm cannot be null", BusinessReason.ERROR_INVALID_ALGORITHM);
        }

        return switch (assetKeyAlgorithm.toUpperCase(Locale.ROOT)) {
            case "RSA" -> "SHA256_WITH_RSA";
            case "EC" -> "SHA256_WITH_ECDSA";
            case "ED" -> "EDDSA";
            default -> throw new BusinessException("Unsupported key algorithm: " + assetKeyAlgorithm, BusinessReason.ERROR_INVALID_ALGORITHM);
        };
    }

    public ProofOfOwnershipResponse generateProofOfOwnership(ProofOfOwnershipRequest request) {

        long unixTimeInSeconds = System.currentTimeMillis() / 1000L;

        String message = String.join("|",
                "Fireblocks",
                "Proof of Ownership Message",
                request.getWorkspaceDisplayName(),
                request.getSdkApiKey(),
                request.getAssetKeyName(),
                String.valueOf(unixTimeInSeconds)
        );

        log.info("Message to be signed for proof of ownership: {}", message);

        String signatureAlgorithm = mapAlgorithmForOwnership(request.getAssetKeyAlgorithm());

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        String payloadBase64;

        if ("EDDSA".equalsIgnoreCase(signatureAlgorithm)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashedMessage = digest.digest(messageBytes);
                payloadBase64 = Base64.getEncoder().encodeToString(hashedMessage);
                log.info("Message HEX pre-hashed with SHA-256 for EDDSA");
            } catch (NoSuchAlgorithmException e) {
                throw new BusinessException("Failed to prehash HEX message for EDDSA: " + e, BusinessReason.ERROR_INVALID_ALGORITHM);
            }
        } else {
            payloadBase64 = Base64.getEncoder().encodeToString(messageBytes);
        }

        String messageHex = HexFormat.of().formatHex(messageBytes);

        String signatureBase64 = hsmFacade.signMessageForOwnership(request.getAssetKeyName(), null, payloadBase64, signatureAlgorithm, null, null);

        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        String signatureHex = HexFormat.of().formatHex(signatureBytes);
        log.info("Decoded signature (HEX): {}", signatureHex);

        return ProofOfOwnershipResponse.builder()
                .timestamp(unixTimeInSeconds)
                .proofOfOwnership(
                        ProofOfOwnershipResponse.Proof.builder()
                                .message(messageHex)
                                .signature(signatureHex)
                                .build()
                )
                .build();
    }

    private String mapAlgorithmForOwnership(String assetKeyAlgorithm) {
        if (assetKeyAlgorithm == null) {
            throw new BusinessException("Asset key algorithm cannot be null", BusinessReason.ERROR_INVALID_ALGORITHM);
        }

        return switch (assetKeyAlgorithm.toUpperCase(Locale.ROOT)) {
            case "ED" -> "EDDSA";
            case "EC", "ECDSA" -> "SHA256_WITH_ECDSA";
            default -> throw new BusinessException("Unsupported key algorithm: " + assetKeyAlgorithm, BusinessReason.ERROR_INVALID_ALGORITHM);
        };
    }

    public ValidationProofOfOwnershipResponse generateValidationProofOfOwnership(ValidationProofOfOwnershipRequest request) {

        String signatureAlgorithm = mapAlgorithmForCsr(request.getAssetKeyAlgorithm());
        String csr = hsmFacade.generateCertificateRequest(request.getAssetKeyName(), null, signatureAlgorithm, request.getIsSkaKey());

        KeyAttributesDto keyAttributes = tsbService.getPublicKey(KEY_LABEL, null);
        String signAlgorithm = selectSignatureAlgorithm(keyAttributes.getAlgorithm(), keyAttributes.getKeySize(), keyAttributes.getCurveOid());
        String signedCertPem = tsbService.signCertificate(KEY_LABEL, null, signAlgorithm, csr);

        ProofOfOwnershipResponse proof = generateProofOfOwnership(
                ProofOfOwnershipRequest.builder()
                        .assetKeyName(request.getAssetKeyName())
                        .assetKeyAlgorithm(request.getAssetKeyAlgorithm())
                        .workspaceDisplayName(request.getWorkspaceDisplayName())
                        .sdkApiKey(request.getSdkApiKey())
                        .build()
        );

        return ValidationProofOfOwnershipResponse.builder()
                .signingDeviceKeyId(request.getAssetKeyName())
                .signedCertPem(signedCertPem)
                .agentUserId(request.getAgentUserId())
                .proofOfOwnership(proof.getProofOfOwnership())
                .build();
    }

    private String selectSignatureAlgorithm(String algorithm, Integer keySize, String curveOid) {

        switch (algorithm.toUpperCase()) {

            case "RSA":
                if (keySize == null || keySize <= 2048) {
                    return "SHA256_WITH_RSA";
                } else if (keySize <= 3072) {
                    return "SHA384_WITH_RSA";
                } else if (keySize <= 4096) {
                    return "SHA512_WITH_RSA";
                } else {
                    return "SHA512_WITH_RSA";
                }

            case "EC":
                if (curveOid == null) {
                    throw new BusinessException("Missing curveOid for EC key", BusinessReason.ERROR_INVALID_ALGORITHM);
                }
                    return "SHA256_WITH_ECDSA";

            case "ED":
                // Ed25519
                if ("1.3.101.112".equals(curveOid)) {
                    return "EDDSA";
                }
                throw new BusinessException("Unsupported ED curve OID: " + curveOid,
                        BusinessReason.ERROR_INVALID_ALGORITHM);

            default:
                throw new BusinessException("Unsupported algorithm: " + algorithm,
                        BusinessReason.ERROR_INVALID_ALGORITHM);
        }
    }

}
