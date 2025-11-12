// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.scheduled;

import com.securosys.fireblocks.business.service.StatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "tsb", name = "intervalFetchResults")
public class SigningScheduler {

    private final StatusService statusService;

    @Scheduled(fixedDelayString = "#{${tsb.intervalFetchResults} * 1000}", initialDelay = 10000)
    public void fetchPendingMessages() {
        log.info("Scheduler triggered: Get status of all pending messages...");
        statusService.syncPendingStatuses();
    }
}