// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents a rule that defines a set of tokens required for a specific operation or action
 * in a security or workflow context. Each rule is associated with a list of tokens that must
 * be provided or validated as part of enforcing the rule.
 */
@Data
public class Rule {
    @JsonProperty("tokens")
    private List<Tokens> tokens;

}