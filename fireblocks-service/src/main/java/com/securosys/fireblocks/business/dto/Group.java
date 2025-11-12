// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents a group within the context of a token, including the group's name, quorum requirement,
 * and the list of approvals associated with the group. The quorum defines the minimum number of
 * approvals required for the group's decision to be considered valid.
 */
@Data
public class Group {
    @JsonProperty("name")
    private String name;
    @JsonProperty("quorum")
    private int quorum;
    @JsonProperty("approvals")
    private List<Approval> approvals;

}