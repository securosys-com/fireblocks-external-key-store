// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.repository;

import com.securosys.fireblocks.datamodel.entities.MessageStatusEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Transactional
public interface MessageStatusRepository extends CrudRepository<MessageStatusEntity, UUID> {

    List<MessageStatusEntity> findByRequestIdIn(List<UUID> requestIds);

    List<MessageStatusEntity> findByStatus(String status);

    Optional<MessageStatusEntity> findByRequestId(UUID requestId);
}
