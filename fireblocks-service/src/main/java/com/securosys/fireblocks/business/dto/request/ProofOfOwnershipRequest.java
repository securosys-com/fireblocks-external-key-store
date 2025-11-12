// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProofOfOwnershipRequest {

    @NotBlank
    private String assetKeyName;

    @NotEmpty
    @Schema(description = "The algorithm of the asset key.", example="EC", allowableValues = {"EC", "ED"})
    private String assetKeyAlgorithm;

    @NotBlank
    private String workspaceDisplayName;

    @NotBlank
    private String sdkApiKey;
}
