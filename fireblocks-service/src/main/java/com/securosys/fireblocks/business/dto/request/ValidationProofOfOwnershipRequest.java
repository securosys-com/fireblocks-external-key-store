// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationProofOfOwnershipRequest {

    @NotNull
    private String assetKeyName;

    @NotNull
    @Schema(description = "The algorithm of the asset key.", example="EC", allowableValues = {"EC", "ED"})
    private String assetKeyAlgorithm;

    @NotNull
    private String workspaceDisplayName;

    @NotNull
    private String sdkApiKey;

    @NotNull
    private String agentUserId;

    @Schema(description = "If true the asset key is an SKA key with policy.")
    private Boolean isSkaKey;
}
