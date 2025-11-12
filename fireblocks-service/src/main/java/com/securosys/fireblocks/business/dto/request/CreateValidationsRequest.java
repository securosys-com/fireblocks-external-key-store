// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "Create Validations request.")
public class CreateValidationsRequest {

    @NotEmpty
    private String assetKeyName;

    @NotEmpty
    @Schema(description = "The algorithm of the asset key.", example="EC", allowableValues = {"EC", "ED"})
    private String assetKeyAlgorithm;

    @Schema(description = "If true the asset key is an SKA key with policy.", defaultValue="false")
    private Boolean isSkaKey = false;
}
