// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProofOfOwnershipResponse {

    private Proof proofOfOwnership;
    private long timestamp;

    @Data
    @Builder
    public static class Proof {
        private String message;
        private String signature;
    }
}
