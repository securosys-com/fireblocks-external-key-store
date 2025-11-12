// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationProofOfOwnershipResponse {

    private String signingDeviceKeyId;
    private String signedCertPem;
    private String agentUserId;
    private ProofOfOwnershipResponse.Proof proofOfOwnership;
}
