// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.securosys.fireblocks.business.dto.customServer.MessageResponse;
import com.securosys.fireblocks.business.dto.customServer.MessageStatus;
import com.securosys.fireblocks.business.dto.customServer.SignedMessage;
import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.repository.MessageStatusRepository;
import com.securosys.fireblocks.datamodel.entities.MessageResponseEntity;
import com.securosys.fireblocks.datamodel.entities.MessageStatusEntity;
import com.securosys.fireblocks.datamodel.entities.SignedMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MessageStatusService {

    private final MessageStatusRepository jpaRepository;

    public void save(MessageStatus status, String tsbRequestId) {

        MessageResponseEntity responseEntity = MessageResponseEntity.builder()
                .build();

        if (status.getResponse().getSignedMessages() != null) {
            List<SignedMessageEntity> signedEntities = status.getResponse().getSignedMessages().stream()
                    .map(sm -> SignedMessageEntity.builder()
                            .message(sm.getMessage())
                            .index(sm.getIndex())
                            .signature(sm.getSignature())
                            .response(responseEntity)
                            .build())
                    .toList();

            responseEntity.setSignedMessages(signedEntities);
        }

        MessageStatusEntity entity = MessageStatusEntity.builder()
                .requestId(status.getRequestId())
                .tsbRequestId(tsbRequestId)
                .type(status.getType())
                .status(status.getStatus())
                .response(responseEntity)
                .build();

        responseEntity.setStatus(entity);

        jpaRepository.save(entity);
    }

    public List<MessageStatus> findByStatus(String status) {
        return jpaRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<MessageStatus> findByRequestId(UUID requestId) {
        return jpaRepository.findByRequestId(requestId)
                .map(this::toDomain);
    }

    private MessageStatus toDomain(MessageStatusEntity entity) {
        try {
            return MessageStatus.builder()
                    .requestId(entity.getRequestId())
                    .type(entity.getType())
                    .status(entity.getStatus())
                    .response(toDomain(entity.getResponse()))
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Deserialization error: " + e, BusinessReason.ERROR_IN_SUBSYSTEM);
        }
    }

    private MessageResponse toDomain(MessageResponseEntity entity) {
        if (entity == null) {
            return null;
        }
        List<SignedMessage> signedDtos = entity.getSignedMessages().stream()
                .map(sm -> new SignedMessage(sm.getMessage(), sm.getIndex(), sm.getSignature()))
                .toList();

        return new MessageResponse(signedDtos);
    }
}
