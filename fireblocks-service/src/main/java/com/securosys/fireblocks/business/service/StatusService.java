// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import com.securosys.fireblocks.business.dto.customServer.*;
import com.securosys.fireblocks.business.dto.response.RequestStatusResponseDto;
import com.securosys.fireblocks.business.repository.MessageStatusRepository;
import com.securosys.fireblocks.datamodel.entities.MessageResponseEntity;
import com.securosys.fireblocks.datamodel.entities.MessageStatusEntity;
import com.securosys.fireblocks.datamodel.entities.SignedMessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusService {

    private final MessageStatusRepository repository;
    private final TsbService tsbService;

    public MessagesStatusResponse getStatuses(MessagesStatusRequest request) {
        List<MessageStatusEntity> entities = repository.findByRequestIdIn(request.getRequestsIds());
        return new MessagesStatusResponse(toDtoList(entities));
    }

    public MessageStatus toDto(MessageStatusEntity entity) {
        return MessageStatus.builder()
                .requestId(entity.getRequestId())
                .type(entity.getType())
                .status(entity.getStatus())
                .response(toDto(entity.getResponse()))
                .build();
    }

    private MessageResponse toDto(MessageResponseEntity entity) {
        if (entity == null) return null;

        return new MessageResponse(
                entity.getSignedMessages().stream().map(this::toDto).toList()
        );
    }

    private SignedMessage toDto(SignedMessageEntity entity) {
        return new SignedMessage(entity.getMessage(), entity.getIndex(), entity.getSignature());
    }

    public List<MessageStatus> toDtoList(List<MessageStatusEntity> entities) {
        return entities.stream().map(this::toDto).toList();
    }

    public void syncPendingStatuses() {
        List<MessageStatusEntity> pendingEntities = repository.findByStatus(MessageStatus.PENDING_SIGN);

        if (pendingEntities.isEmpty()) {
            log.debug("No pending messages to sync");
            return;
        }

        for (MessageStatusEntity entity : pendingEntities) {
            String requestId = String.valueOf(entity.getRequestId());
            String tsbRequestId = String.valueOf(entity.getTsbRequestId());

            if (tsbRequestId == null || tsbRequestId.isBlank()){
                continue;
            }

            try {
                RequestStatusResponseDto tsbResp = tsbService.getRequest(tsbRequestId);
                String tsbStatus = tsbResp.getStatus();
                String mapped = MessageStatus.mapTsbToLocalStatus(tsbStatus);

                if (!Objects.equals(entity.getStatus(), mapped)) {
                    log.debug("Request {}: status changed from {} -> {} (TSB: {})",
                            requestId, entity.getStatus(), mapped, tsbStatus);

                    entity.setStatus(mapped);

                    if (MessageStatus.SIGNED.equals(mapped)) {
                        MessageResponseEntity response = entity.getResponse();
                        if (response != null) {
                            for (SignedMessageEntity signed : response.getSignedMessages()) {
                                signed.setSignature(tsbResp.getResult());
                            }
                        }
                    }

                    repository.save(entity);
                }
            } catch (Exception ex) {
                log.warn("Failed to sync TSB status for {}: {}", requestId, ex.getMessage(), ex);
            }
        }
    }
}
