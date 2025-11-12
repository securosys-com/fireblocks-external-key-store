// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.customServer;

import com.securosys.fireblocks.datamodel.entities.ResponseType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatus {

    public static final String PENDING_SIGN = "PENDING_SIGN";
    public static final String SIGNED = "SIGNED";
    public static final String FAILED = "FAILED";

    @NotNull
    private ResponseType type;

    @NotNull
    private String status; // PENDING_SIGN, SIGNED, FAILED

    @NotNull
    private UUID requestId;

    @NotNull
    private MessageResponse response;

    /**
     * Map status of signing request from TSB into MessageStatus format
     * @param tsbStatus status of signing request from TSB
     */
    public static String mapTsbToLocalStatus(String tsbStatus) {
        if (tsbStatus == null) return PENDING_SIGN;
        return switch (tsbStatus.toUpperCase()) {
            case "PENDING", "APPROVED" -> PENDING_SIGN;
            case "EXECUTED" -> SIGNED;
            case "FAILED", "REJECTED", "CANCELLED", "EXPIRED" -> FAILED;
            default -> tsbStatus;
        };
    }
}
