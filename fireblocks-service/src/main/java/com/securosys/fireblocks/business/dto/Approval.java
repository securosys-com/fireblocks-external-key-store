// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an approval in a workflow or process, including the type of approval, the name of the
 * approver, and the value associated with the approval. Approvals are used to track decisions in
 * processes that require validation or consent.
 */
@Data
class Approval {
    @JsonProperty("type")
    private String type;
    @JsonProperty("name")
    private String name;
    @JsonProperty("value")
    private String value;

}
