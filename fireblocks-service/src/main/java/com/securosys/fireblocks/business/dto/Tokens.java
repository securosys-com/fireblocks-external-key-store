// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;


/**
 * Represents a token used in an authentication or authorization process. The token is associated
 * with a timelock, timeout, and a set of groups, defining how and when the token can be used.
 */
@Data
public class Tokens {
    @JsonProperty("name")
    private String name;
    @JsonProperty("timelock")
    private int timelock;
    @JsonProperty("timeout")
    private int timeout;
    @JsonProperty("groups")
    private List<Group> groups;

}