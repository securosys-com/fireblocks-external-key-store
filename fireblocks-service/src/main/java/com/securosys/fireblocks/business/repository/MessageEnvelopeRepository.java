// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.repository;

import com.securosys.fireblocks.datamodel.entities.MessageEnvelopeEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Transactional
public interface MessageEnvelopeRepository extends CrudRepository<MessageEnvelopeEntity, UUID> {

    Optional<MessageEnvelopeEntity> findByRequestId(UUID requestId);

    List<MessageEnvelopeEntity> findByRequestIdIn(List<UUID> requestIds);
}