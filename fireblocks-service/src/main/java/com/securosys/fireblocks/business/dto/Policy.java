// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a set of rules and status associated with a cryptographic key. This class defines
 * various operations that can be performed on the key, such as usage, blocking, unblocking, and
 * modification, as well as the current status of the key.
 */
@Data
public class Policy {
    @JsonProperty("ruleUse")
    private Rule ruleUse;
    @JsonProperty("ruleBlock")
    private Rule ruleBlock;
    @JsonProperty("ruleUnblock")
    private Rule ruleUnblock;
    @JsonProperty("ruleModify")
    private Rule ruleModify;
    @JsonProperty("keyStatus")
    private KeyStatus keyStatus;

}
