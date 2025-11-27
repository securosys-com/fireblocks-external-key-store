// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.securosys.fireblocks.business.dto.customServer.*;
import com.securosys.fireblocks.business.dto.response.RequestStatusResponseDto;
import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.facade.HsmFacade;
import com.securosys.fireblocks.business.util.JsonUtil;
import com.securosys.fireblocks.configuration.CustomServerProperties;
import com.securosys.fireblocks.datamodel.entities.RequestType;
import com.securosys.fireblocks.datamodel.entities.ResponseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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
     * Method for /messagesToSign. Sign messages with Securosys TSB
     * @param originalBody Contains the MessagesRequest object with information about messages
     */
    public MessagesStatusResponse signMessages(String originalBody) {

        if (hsmFacade.isLicensed()){
            throw new BusinessException("Your current HSM subscription does not support this operation as the required flag: FIREBLOCKS_AGENT is not set", BusinessReason.ERROR_CLIENT_SUBSCRIPTION);
        }

        log.info("Got request from fireblocks: {}", originalBody);
        List<MessageStatus> statuses = new ArrayList<>();

        JsonNode root;
        try {
            root = objectMapper.readTree(originalBody);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Error reading json request.", BusinessReason.ERROR_INVALID_JSON);
        }
        ArrayNode messagesArray = (ArrayNode) root.get("messages");
        MessagesRequest request = jsonUtil.getObjectFromJsonString(originalBody, MessagesRequest.class);

        for (int i = 0; i < request.getMessages().size(); i++) {
            MessageEnvelope envelope = request.getMessages().get(i);
            log.info("Message envelope: {}", envelope);

            JsonNode rawMessageJson = messagesArray.get(i).get("message");
            String rawPayload = rawMessageJson.get("payload").asText();

            UUID requestId = envelope.getTransportMetadata().getRequestId();
            log.info("Saving message with ID: {}", requestId);
            envelopeService.save(envelope);

            try {
                String service = envelope.getMessage().getPayloadSignatureData().getService();
                String payloadSignature = envelope.getMessage().getPayloadSignatureData().getSignature();
                byte[] signatureBytes = HexFormat.of().parseHex(payloadSignature);
                log.info("Raw payload: {}", rawPayload);

                if (properties.isVerifySignatures() && !verifySignature(signatureBytes, service, rawPayload)) {
                    log.error("Invalid signature for requestId {}", requestId);
                    statuses.add(buildFailedStatus(envelope, requestId));
                    continue;
                }

                JsonNode payloadNode = objectMapper.readTree(rawPayload);

                if (!payloadNode.isObject()) {
                    throw new BusinessException("Expected JSON object as rawPayload", BusinessReason.ERROR_INVALID_JSON);
                }

                if (properties.isVerifySignatures()){
                    String publicKeyPemValue = hsmFacade.getPublicKeyString();
                    ((ObjectNode) payloadNode).put("publicKeyPem", publicKeyPemValue);
                    ((ObjectNode) payloadNode).put("rawPayload", rawPayload);
                    ((ObjectNode) payloadNode).put("serviceName", service);
                    ((ObjectNode) payloadNode).put("expectedServiceName", properties.getServiceName());
                }

                String updatedRawPayload = objectMapper.writeValueAsString(payloadNode);

                MessagePayload payload = objectMapper.readValue(
                        envelope.getMessage().getPayload(),
                        MessagePayload.class);

                log.info("Message payload: {}", payload);

                List<SignedMessage> signedMessages = new ArrayList<>();
                String status = MessageStatus.FAILED;
                String tsbRequestId = "";
                String metadataBase64 = Base64.getEncoder().encodeToString(updatedRawPayload.getBytes(StandardCharsets.UTF_8));
                String metadataSignatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

                for (MessageToSign msg : payload.getMessagesToSign()) {
                    log.info("Message to sign: {}", msg);

                    RequestStatusResponseDto response = hsmFacade.sign(
                            payload.getSigningDeviceKeyId(),
                            null,
                            msg.getMessage(),
                            payload.getAlgorithm().name(),
                            metadataBase64,
                            metadataSignatureBase64
                    );
                    log.info("TSB response: {}", response);

                    if (response.getResult() != null){
                        byte[] signatureResponseBytes = Base64.getDecoder().decode(response.getResult());
                        String signatureHex = HexFormat.of().formatHex(signatureResponseBytes);
                        log.info("Decoded signature (HEX): {}", signatureHex);
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), signatureHex));
                    } else {
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), ""));
                    }

                    tsbRequestId = response.getId();
                    status = MessageStatus.mapTsbToLocalStatus(response.getStatus());
                }

                MessageResponse messageResponse = new MessageResponse(signedMessages);

                MessageStatus messageStatus = MessageStatus.builder()
                        .type(toResponseType(envelope.getTransportMetadata().getType()))
                        .status(MessageStatus.mapTsbToLocalStatus(status))
                        .requestId(requestId)
                        .response(messageResponse)
                        .build();

                log.info("Saving signed message with request ID: {}, transaction ID: {} and tenant ID: {}", requestId, payload.getTxId(), payload.getTenantId());
                statusService.save(messageStatus, tsbRequestId);
                statuses.add(messageStatus);

            } catch (Exception ex) {
                log.error("Failed signing for request ID: {}, reason: {}", requestId, ex.getMessage());
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

    private boolean verifySignature(byte[] signature, String service, String rawPayload) {
        return hsmFacade.verify(signature, service, rawPayload);
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
                String service = envelope.getMessage().getPayloadSignatureData().getService();

                if (properties.isVerifySignatures() && !verifySignature(signatureBytes, service, rawPayload)) {
                    log.error("Invalid signature for requestId {}", requestId);
                    buildFailedStatus(envelope, requestId);
                    continue;
                }

                JsonNode payloadNode = objectMapper.readTree(rawPayload);

                if (!payloadNode.isObject()) {
                    throw new BusinessException("Expected JSON object as rawPayload", BusinessReason.ERROR_INVALID_JSON);
                }

                if (properties.isVerifySignatures()){
                    String publicKeyPemValue = hsmFacade.getPublicKeyString();
                    ((ObjectNode) payloadNode).put("publicKeyPem", publicKeyPemValue);
                    ((ObjectNode) payloadNode).put("rawPayload", rawPayload);
                    ((ObjectNode) payloadNode).put("serviceName", service);
                    ((ObjectNode) payloadNode).put("expectedServiceName", properties.getServiceName());
                }

                String updatedRawPayload = objectMapper.writeValueAsString(payloadNode);

                MessagePayload payload = objectMapper.readValue(envelope.getMessage().getPayload(), MessagePayload.class);

                List<SignedMessage> signedMessages = new ArrayList<>();
                String status = MessageStatus.FAILED;
                String tsbRequestId = "";
                String metadataBase64 = Base64.getEncoder().encodeToString(updatedRawPayload.getBytes(StandardCharsets.UTF_8));
                String metadataSignatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

                for (MessageToSign msg : payload.getMessagesToSign()) {

                    RequestStatusResponseDto response = hsmFacade.sign(
                            payload.getSigningDeviceKeyId(),
                            null,
                            msg.getMessage(),
                            payload.getAlgorithm().name(),
                            metadataBase64,
                            metadataSignatureBase64
                    );
                    if (response.getResult() != null){
                        byte[] signatureResponseBytes = Base64.getDecoder().decode(response.getResult());
                        String signatureHex = HexFormat.of().formatHex(signatureResponseBytes);
                        log.info("Decoded signature (HEX): {}", signatureHex);
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), signatureHex));
                    } else {
                        signedMessages.add(new SignedMessage(msg.getMessage(), msg.getIndex(), ""));
                    }

                    tsbRequestId = response.getId();
                    status = MessageStatus.mapTsbToLocalStatus(response.getStatus());
                }

                messageStatus.setStatus(status);
                messageStatus.setResponse(new MessageResponse(signedMessages));
                log.info("Saving signed message with request ID: {}, transaction ID: {} and tenant ID: {}", requestId, payload.getTxId(), payload.getTenantId());
                statusService.save(messageStatus, tsbRequestId);

            } catch (Exception ex) {

                if (ex instanceof BusinessException) {
                    log.error("Failed signing for request ID: {}", ex.getMessage());
                } else {
                    log.error("Failed signing for request ID: {}", requestId);
                }
                messageStatus.setStatus(MessageStatus.FAILED);
                messageStatus.setResponse(new MessageResponse(null));
                statusService.save(messageStatus, "unknown");
                log.error("Signing failed for requestId {}", requestId);
            }
        }
    }

    private ResponseType toResponseType(RequestType type) {
        return switch (type) {
            case KEY_LINK_PROOF_OF_OWNERSHIP_REQUEST -> ResponseType.KEY_LINK_PROOF_OF_OWNERSHIP_RESPONSE;
            case KEY_LINK_TX_SIGN_REQUEST -> ResponseType.KEY_LINK_TX_SIGN_RESPONSE;
        };
    }
}
