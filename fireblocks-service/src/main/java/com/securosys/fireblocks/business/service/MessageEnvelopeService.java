// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.securosys.fireblocks.business.dto.customServer.*;
import com.securosys.fireblocks.business.repository.MessageEnvelopeRepository;
import com.securosys.fireblocks.datamodel.entities.MessageEntity;
import com.securosys.fireblocks.datamodel.entities.MessageEnvelopeEntity;
import com.securosys.fireblocks.datamodel.entities.PayloadSignatureDataEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MessageEnvelopeService {

    private final MessageEnvelopeRepository jpaRepository;

    public void save(MessageEnvelope envelope) {

        PayloadSignatureDataEntity sigEntity = PayloadSignatureDataEntity.builder()
                .signature(envelope.getMessage().getPayloadSignatureData().getSignature())
                .service(envelope.getMessage().getPayloadSignatureData().getService())
                .build();

        MessageEntity messageEntity = MessageEntity.builder()
                .payload(envelope.getMessage().getPayload())
                .payloadSignatureData(sigEntity)
                .build();

        sigEntity.setMessage(messageEntity);

        MessageEnvelopeEntity entity = MessageEnvelopeEntity.builder()
                .requestId(envelope.getTransportMetadata().getRequestId())
                .metadataType(envelope.getTransportMetadata().getType())
                .message(messageEntity)
                .build();

        messageEntity.setEnvelope(entity);
        jpaRepository.save(entity);
    }

    public Optional<MessageEnvelope> findByRequestId(UUID requestId) {
        return jpaRepository.findByRequestId(requestId)
                .map(this::toDomain);
    }

    public List<MessageEnvelope> findAllByRequestIds(List<UUID> requestIds) {
        return jpaRepository.findByRequestIdIn(requestIds).stream()
                .map(this::toDomain)
                .toList();
    }

    private MessageEnvelope toDomain(MessageEnvelopeEntity entity) {
        PayloadSignatureData sig = new PayloadSignatureData(
                entity.getMessage().getPayloadSignatureData().getSignature(),
                entity.getMessage().getPayloadSignatureData().getService()
        );
        Message message = new Message(sig, entity.getMessage().getPayload());

        TransportMetadata metadata = new TransportMetadata(
                entity.getRequestId(),
                entity.getMetadataType()
        );

        return new MessageEnvelope(message, metadata);
    }
}
