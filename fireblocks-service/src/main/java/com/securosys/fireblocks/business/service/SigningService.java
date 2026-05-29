// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.securosys.fireblocks.business.dto.ServiceName;
import com.securosys.fireblocks.business.dto.customServer.*;
import com.securosys.fireblocks.business.dto.response.RequestStatusResponseDto;
import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.facade.HsmFacade;
import com.securosys.fireblocks.business.service.TsbService.SignatureAlgorithm;
import com.securosys.fireblocks.business.util.JsonUtil;
import com.securosys.fireblocks.configuration.CustomServerProperties;
import com.securosys.fireblocks.datamodel.entities.RequestType;
import com.securosys.fireblocks.datamodel.entities.ResponseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SigningService {

    private final HsmFacade hsmFacade;
    private final CustomServerProperties properties;
    private final MessageStatusService statusService;
    private final MessageEnvelopeService envelopeService;
    private final JsonUtil jsonUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Method for /messagesToSign. Sign transaction and proof-of-ownership messages with Securosys TSB.
     * The request type and signing service are read from each message envelope.
     * @param originalBody Contains the MessagesRequest object with information about messages
     */
    public MessagesStatusResponse signMessages(String originalBody) {
        return signMessagesRequest(originalBody);
    }

    private MessagesStatusResponse signMessagesRequest(String originalBody) {

        if (hsmFacade.isLicensed()){
            throw new BusinessException("Your current HSM subscription does not support this operation as the required flag: FIREBLOCKS_AGENT is not set", BusinessReason.ERROR_CLIENT_SUBSCRIPTION);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(originalBody);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Error reading json request.", BusinessReason.ERROR_INVALID_JSON);
        }
        ArrayNode messagesArray = (ArrayNode) root.get("messages");
        MessagesRequest request = jsonUtil.getObjectFromJsonString(originalBody, MessagesRequest.class);
        List<MessageStatus> statuses = new ArrayList<>();
        log.info("Received Fireblocks signing request with {} message(s)", request.getMessages().size());

        for (int i = 0; i < request.getMessages().size(); i++) {
            MessageEnvelope envelope = request.getMessages().get(i);

            JsonNode rawMessageJson = messagesArray.get(i).get("message");
            String rawPayload = rawMessageJson.get("payload").asText();

            UUID requestId = envelope.getTransportMetadata().getRequestId();
            envelopeService.save(envelope);

            try {
                String requestedService = envelope.getMessage().getPayloadSignatureData().getService();
                RequestType transportRequestType = envelope.getTransportMetadata().getType();
                log.info("Received Fireblocks message requestId={}, type={}, service={}", requestId, transportRequestType, requestedService);

                ServiceName serviceName = ServiceName.fromString(requestedService);
                validateServiceCanProcessRequestType(serviceName, transportRequestType);
                String payloadSignature = envelope.getMessage().getPayloadSignatureData().getSignature();
                byte[] signatureBytes = HexFormat.of().parseHex(payloadSignature);

                if (properties.isVerifySignatures() && !verifySignature(signatureBytes, serviceName, rawPayload)) {
                    log.warn("Invalid Fireblocks payload signature requestId={}, type={}, service={}", requestId, transportRequestType, serviceName);
                    statuses.add(buildFailedStatus(envelope, requestId));
                    continue;
                }

                JsonNode payloadNode = objectMapper.readTree(rawPayload);

                if (!payloadNode.isObject()) {
                    throw new BusinessException("Expected JSON object as rawPayload", BusinessReason.ERROR_INVALID_JSON);
                }

                if (properties.isVerifySignatures()){
                    ((ObjectNode) payloadNode).put("rawPayload", rawPayload);
                    ((ObjectNode) payloadNode).put("serviceName", serviceName.name());
                }

                String updatedRawPayload = objectMapper.writeValueAsString(payloadNode);

                MessagePayload payload = objectMapper.readValue(
                        envelope.getMessage().getPayload(),
                        MessagePayload.class);
                RequestType requestType = resolveRequestType(transportRequestType, payload.getType());

                List<MessageToSign> messagesToSign = resolveMessagesToSign(payload, payloadNode, requestType);
                String signingDeviceKeyId = resolveSigningDeviceKeyId(payload, payloadNode);
                String algorithmName = resolveAlgorithmName(payload, payloadNode);
                log.info("Signing Fireblocks message requestId={}, type={}, service={}, messageCount={}",
                        requestId, requestType, serviceName, messagesToSign.size());

                List<SignedMessage> signedMessages = new ArrayList<>();
                String status = MessageStatus.FAILED;
                String tsbRequestId = "";
                String metadataBase64 = Base64.getEncoder().encodeToString(updatedRawPayload.getBytes(StandardCharsets.UTF_8));
                String metadataSignatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

                for (MessageToSign msg : messagesToSign) {
                    RequestStatusResponseDto response = signWithHsm(
                            requestType,
                            signingDeviceKeyId,
                            msg.getMessage(),
                            algorithmName,
                            metadataBase64,
                            metadataSignatureBase64);
                    log.info("TSB signing response requestId={}, tsbRequestId={}, tsbStatus={}",
                            requestId, response.getId(), response.getStatus());

                    if (response.getResult() != null){
                        byte[] signatureResponseBytes = Base64.getDecoder().decode(response.getResult());
                        String signatureHex = HexFormat.of().formatHex(signatureResponseBytes);
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), signatureHex));
                    } else {
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), ""));
                    }

                    tsbRequestId = response.getId();
                    status = MessageStatus.mapTsbToLocalStatus(response.getStatus());
                }

                MessageResponse messageResponse = new MessageResponse(signedMessages);

                MessageStatus messageStatus = MessageStatus.builder()
                        .type(toResponseType(requestType))
                        .status(MessageStatus.mapTsbToLocalStatus(status))
                        .requestId(requestId)
                        .response(messageResponse)
                        .build();

                statusService.save(messageStatus, tsbRequestId);
                log.info("Saved Fireblocks message status requestId={}, type={}, status={}, tsbRequestId={}",
                        requestId, requestType, messageStatus.getStatus(), tsbRequestId);
                statuses.add(messageStatus);

            } catch (Exception ex) {
                log.error("Failed signing requestId={}, reason={}", requestId, safeErrorMessage(ex));
                statuses.add(buildFailedStatus(envelope, requestId));
            }
        }

        return new MessagesStatusResponse(statuses);
    }

    private MessageStatus buildFailedStatus(MessageEnvelope envelope, UUID requestId) {
        MessageResponse response = new MessageResponse(null);
        MessageStatus failed = MessageStatus.builder()
                .type(toResponseType(envelope.getTransportMetadata().getType()))
                .status(MessageStatus.FAILED)
                .requestId(requestId)
                .response(response)
                .build();

        statusService.save(failed, "unknown");
        return failed;
    }

    private boolean verifySignature(byte[] signature, ServiceName serviceName, String rawPayload) {
        return hsmFacade.verify(signature, serviceName, rawPayload);
    }

    private void validateServiceCanProcessRequestType(ServiceName serviceName, RequestType requestType) {
        if (requestType == null) {
            throw new BusinessException("Request type must be provided", BusinessReason.ERROR_INVALID_JSON);
        }

        if (serviceName == ServiceName.CONFIGURATION_MANAGER && requestType != RequestType.KEY_LINK_PROOF_OF_OWNERSHIP_REQUEST) {
            throw new BusinessException("CONFIGURATION_MANAGER can only send proof-of-ownership requests", BusinessReason.ERROR_INVALID_JSON);
        }

        if (serviceName == ServiceName.SIGNING_SERVICE && requestType != RequestType.KEY_LINK_TX_SIGN_REQUEST) {
            throw new BusinessException("SIGNING_SERVICE can only send transaction signing requests", BusinessReason.ERROR_INVALID_JSON);
        }
    }

    private RequestType resolveRequestType(RequestType transportRequestType, RequestType payloadRequestType) {
        if (transportRequestType == null && payloadRequestType == null) {
            throw new BusinessException("Request type must be provided", BusinessReason.ERROR_INVALID_JSON);
        }

        if (transportRequestType != null && payloadRequestType != null && transportRequestType != payloadRequestType) {
            throw new BusinessException("Transport metadata type does not match payload type", BusinessReason.ERROR_INVALID_JSON);
        }

        return transportRequestType != null ? transportRequestType : payloadRequestType;
    }


    /**
     * Method for option /signAllPendingMessages. Sign all messages with Securosys TSB
     */
    public void signAllPending() {
        if (hsmFacade.isLicensed()){
            throw new BusinessException("Your current HSM subscription does not support this operation as the required flag: FIREBLOCKS_AGENT is not set", BusinessReason.ERROR_CLIENT_SUBSCRIPTION);
        }

        List<MessageStatus> pendingMessages = statusService.findByStatus(MessageStatus.PENDING_SIGN);
        List<UUID> requestIds = pendingMessages.stream().map(MessageStatus::getRequestId).toList();

        if (requestIds.isEmpty()) {
            log.info("No pending messages to sign");
            return;
        }
        signMessages(requestIds);
    }


    /**
     * Method for option /signRequest/{requestId}. Sign message by requestId with Securosys TSB
     * @param requestId requestId of message
     */
    public void signMessage(UUID requestId) {

        if (hsmFacade.isLicensed()){
            throw new BusinessException("Your current HSM subscription does not support this operation as the required flag: FIREBLOCKS_AGENT is not set", BusinessReason.ERROR_CLIENT_SUBSCRIPTION);
        }

        signMessages(Collections.singletonList(requestId));
    }


    /**
     * Method for option sign message by list of requestId with Securosys TSB
     * @param requestIds list of requestId of message
     */
    private void signMessages(List<UUID> requestIds) {
        List<MessageEnvelope> envelopes = envelopeService.findAllByRequestIds(requestIds);

        for (MessageEnvelope envelope : envelopes) {
            UUID requestId = envelope.getTransportMetadata().getRequestId();
            MessageStatus messageStatus = statusService.findByRequestId(requestId).orElseThrow();

            try {
                String rawPayload = envelope.getMessage().getPayload();
                String payloadSignature = envelope.getMessage().getPayloadSignatureData().getSignature();
                byte[] signatureBytes = HexFormat.of().parseHex(payloadSignature);
                String requestedService = envelope.getMessage().getPayloadSignatureData().getService();
                ServiceName serviceName = ServiceName.fromString(requestedService);
                RequestType transportRequestType = envelope.getTransportMetadata().getType();
                log.info("Picked up pending Fireblocks message requestId={}, type={}, service={}", requestId, transportRequestType, requestedService);
                validateServiceCanProcessRequestType(serviceName, transportRequestType);

                if (properties.isVerifySignatures() && !verifySignature(signatureBytes, serviceName, rawPayload)) {
                    log.warn("Invalid Fireblocks payload signature requestId={}, type={}, service={}", requestId, transportRequestType, serviceName);
                    buildFailedStatus(envelope, requestId);
                    continue;
                }

                JsonNode payloadNode = objectMapper.readTree(rawPayload);

                if (!payloadNode.isObject()) {
                    throw new BusinessException("Expected JSON object as rawPayload", BusinessReason.ERROR_INVALID_JSON);
                }

                if (properties.isVerifySignatures()){
                    ((ObjectNode) payloadNode).put("rawPayload", rawPayload);
                    ((ObjectNode) payloadNode).put("serviceName", serviceName.name());
                }

                String updatedRawPayload = objectMapper.writeValueAsString(payloadNode);

                MessagePayload payload = objectMapper.readValue(envelope.getMessage().getPayload(), MessagePayload.class);
                RequestType requestType = resolveRequestType(transportRequestType, payload.getType());

                List<MessageToSign> messagesToSign = resolveMessagesToSign(payload, payloadNode, requestType);
                String signingDeviceKeyId = resolveSigningDeviceKeyId(payload, payloadNode);
                String algorithmName = resolveAlgorithmName(payload, payloadNode);
                List<SignedMessage> signedMessages = new ArrayList<>();
                String status = MessageStatus.FAILED;
                String tsbRequestId = "";
                String metadataBase64 = Base64.getEncoder().encodeToString(updatedRawPayload.getBytes(StandardCharsets.UTF_8));
                String metadataSignatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

                for (MessageToSign msg : messagesToSign) {

                    RequestStatusResponseDto response = signWithHsm(
                            requestType,
                            signingDeviceKeyId,
                            msg.getMessage(),
                            algorithmName,
                            metadataBase64,
                            metadataSignatureBase64);
                    log.info("TSB signing response requestId={}, tsbRequestId={}, tsbStatus={}",
                            requestId, response.getId(), response.getStatus());
                    if (response.getResult() != null){
                        byte[] signatureResponseBytes = Base64.getDecoder().decode(response.getResult());
                        String signatureHex = HexFormat.of().formatHex(signatureResponseBytes);
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), signatureHex));
                    } else {
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), ""));
                    }

                    tsbRequestId = response.getId();
                    status = MessageStatus.mapTsbToLocalStatus(response.getStatus());
                }

                messageStatus.setStatus(status);
                messageStatus.setResponse(new MessageResponse(signedMessages));
                statusService.save(messageStatus, tsbRequestId);
                log.info("Saved pending Fireblocks message status requestId={}, type={}, status={}, tsbRequestId={}",
                        requestId, requestType, messageStatus.getStatus(), tsbRequestId);

            } catch (Exception ex) {
                log.error("Failed signing pending requestId={}, reason={}", requestId, safeErrorMessage(ex));
                messageStatus.setStatus(MessageStatus.FAILED);
                messageStatus.setResponse(new MessageResponse(null));
                statusService.save(messageStatus, "unknown");
            }
        }
    }

    private String safeErrorMessage(Exception ex) {
        if (ex instanceof BusinessException) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    private RequestStatusResponseDto signWithHsm(RequestType requestType,
                                                 String signingDeviceKeyId,
                                                 String message,
                                                 String algorithmName,
                                                 String metadataBase64,
                                                 String metadataSignatureBase64) {
        if (requestType == RequestType.KEY_LINK_PROOF_OF_OWNERSHIP_REQUEST) {
            SignatureAlgorithm signatureAlgorithm = mapAlgorithmForProofOfOwnership(algorithmName);
            String payloadBase64 = encodeProofOfOwnershipPayload(message, signatureAlgorithm);

            return hsmFacade.signMessageForOwnershipRequest(
                    signingDeviceKeyId,
                    null,
                    payloadBase64,
                    signatureAlgorithm,
                    metadataBase64,
                    metadataSignatureBase64);
        }

        return hsmFacade.sign(
                signingDeviceKeyId,
                null,
                message,
                algorithmName,
                metadataBase64,
                metadataSignatureBase64);
    }

    private SignatureAlgorithm mapAlgorithmForProofOfOwnership(String algorithmName) {
        if (!hasText(algorithmName)) {
            throw new BusinessException("algorithm must be provided", BusinessReason.ERROR_INVALID_JSON);
        }

        return switch (algorithmName.toUpperCase(Locale.ROOT)) {
            case "ECDSA_SECP256K1", "SHA256_WITH_ECDSA", "EC", "ECDSA" -> SignatureAlgorithm.SHA256_WITH_ECDSA;
            case "EDDSA_ED25519", "EDDSA", "ED" -> SignatureAlgorithm.EDDSA;
            default -> throw new BusinessException("Unsupported algorithm: " + algorithmName, BusinessReason.ERROR_INVALID_ALGORITHM);
        };
    }

    private String encodeProofOfOwnershipPayload(String message, SignatureAlgorithm signatureAlgorithm) {
        byte[] messageBytes = resolveProofOfOwnershipMessageBytes(message);

        if (signatureAlgorithm == SignatureAlgorithm.EDDSA) {
            try {
                messageBytes = MessageDigest.getInstance("SHA-256").digest(messageBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new BusinessException("Failed to prehash message for EDDSA: " + e, BusinessReason.ERROR_INVALID_ALGORITHM);
            }
        }

        return Base64.getEncoder().encodeToString(messageBytes);
    }

    private byte[] resolveProofOfOwnershipMessageBytes(String message) {
        if (!hasText(message)) {
            throw new BusinessException("Proof of ownership message must be provided", BusinessReason.ERROR_INVALID_JSON);
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.length() % 2 == 0 && trimmedMessage.matches("(?i)[0-9a-f]+")) {
            return HexFormat.of().parseHex(trimmedMessage);
        }

        return trimmedMessage.getBytes(StandardCharsets.UTF_8);
    }

    private List<MessageToSign> resolveMessagesToSign(MessagePayload payload, JsonNode payloadNode, RequestType requestType) {
        if (requestType == RequestType.KEY_LINK_PROOF_OF_OWNERSHIP_REQUEST) {
            return resolveProofOfOwnershipMessagesToSign(payload, payloadNode);
        }

        if (payload.getMessagesToSign() == null || payload.getMessagesToSign().isEmpty()) {
            throw new BusinessException("messagesToSign must not be empty", BusinessReason.ERROR_INVALID_JSON);
        }

        return payload.getMessagesToSign();
    }

    private List<MessageToSign> resolveProofOfOwnershipMessagesToSign(MessagePayload payload, JsonNode payloadNode) {
        if (payload.getMessagesToSign() != null && !payload.getMessagesToSign().isEmpty()) {
            return payload.getMessagesToSign();
        }

        String message = firstNonBlank(
                getText(payloadNode, "proofOfOwnershipMessage"),
                getText(payloadNode.path("proofOfOwnership"), "message"),
                getText(payloadNode, "message")
        );

        if (!hasText(message)) {
            throw new BusinessException("Proof of ownership message must be provided", BusinessReason.ERROR_INVALID_JSON);
        }

        Integer index = payloadNode.hasNonNull("index") ? payloadNode.get("index").asInt() : 0;
        return List.of(new MessageToSign(message, index));
    }

    private String resolveSigningDeviceKeyId(MessagePayload payload, JsonNode payloadNode) {
        String signingDeviceKeyId = firstNonBlank(
                payload.getSigningDeviceKeyId(),
                getText(payloadNode, "signingDeviceKeyId"),
                getText(payloadNode, "assetKeyName"),
                getText(payloadNode, "keyId")
        );

        if (!hasText(signingDeviceKeyId)) {
            throw new BusinessException("signingDeviceKeyId must be provided", BusinessReason.ERROR_INVALID_JSON);
        }

        return signingDeviceKeyId;
    }

    private String resolveAlgorithmName(MessagePayload payload, JsonNode payloadNode) {
        if (payload.getAlgorithm() != null) {
            return payload.getAlgorithm().name();
        }

        String algorithm = firstNonBlank(
                getText(payloadNode, "algorithm"),
                mapAssetKeyAlgorithm(getText(payloadNode, "assetKeyAlgorithm"))
        );

        if (!hasText(algorithm)) {
            throw new BusinessException("algorithm must be provided", BusinessReason.ERROR_INVALID_JSON);
        }

        return Algorithm.valueOf(algorithm.toUpperCase(Locale.ROOT)).name();
    }

    private String mapAssetKeyAlgorithm(String assetKeyAlgorithm) {
        if (!hasText(assetKeyAlgorithm)) {
            return null;
        }

        return switch (assetKeyAlgorithm.toUpperCase(Locale.ROOT)) {
            case "EC", "ECDSA" -> Algorithm.ECDSA_SECP256K1.name();
            case "ED", "EDDSA" -> Algorithm.EDDSA_ED25519.name();
            default -> assetKeyAlgorithm;
        };
    }

    private String getText(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return null;
        }
        JsonNode field = node.get(fieldName);
        return field.isTextual() ? field.asText() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ResponseType toResponseType(RequestType type) {
        return switch (type) {
            case KEY_LINK_PROOF_OF_OWNERSHIP_REQUEST -> ResponseType.KEY_LINK_PROOF_OF_OWNERSHIP_RESPONSE;
            case KEY_LINK_TX_SIGN_REQUEST -> ResponseType.KEY_LINK_TX_SIGN_RESPONSE;
        };
    }
}
