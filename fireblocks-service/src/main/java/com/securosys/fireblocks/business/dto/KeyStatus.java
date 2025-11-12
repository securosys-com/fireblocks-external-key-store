// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the status of a cryptographic key, indicating whether the key is currently blocked
 * or active. This class is used to track and manage the availability of the key for operations.
 */
@Data
public class KeyStatus {
    @JsonProperty("blocked")
    private boolean blocked;

}