// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.securosys.fireblocks.business.dto.request.CreateValidationsRequest;
import com.securosys.fireblocks.business.dto.request.ProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.request.ValidationProofOfOwnershipRequest;
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
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelperService {

    private final HsmFacade hsmFacade;
    private final TsbService tsbService;
    private final String KEY_LABEL = "FIREBLOCKS_VALIDATION_KEY";
    private final String SIGNATURE_ALGORITHM = "SHA256_WITH_RSA";

    public String createValidationKey() {

        String publicKey = hsmFacade.generateKeyPair(KEY_LABEL, null, "RSA", 2048);
        tsbService.syncSelfSign(KEY_LABEL, null, SIGNATURE_ALGORITHM);
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
        return tsbService.signCertificate(KEY_LABEL, null, SIGNATURE_ALGORITHM, csr);
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

        String signedCertPem = tsbService.signCertificate(KEY_LABEL, null, SIGNATURE_ALGORITHM, csr);

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
}
