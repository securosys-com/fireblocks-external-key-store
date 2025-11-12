// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Represents the body of a request, typically used in workflows that involve approval, rejection,
 * and execution tracking. This class stores details about the request's status, execution time,
 * and the participants involved in the approval process.
 */
@Data
public class RequestStatusResponseDto {

    /**
     * The unique identifier for the request. This ID is used to track and reference the request
     * throughout its lifecycle.
     */
    private String id;

    /**
     * The current status of the request. This might include values such as "Pending", "Approved",
     * "Rejected", or "Completed", indicating the request's state in the workflow.
     */
    private String status;

    /**
     * The time at which the request was executed, typically in a format such as ISO 8601. This
     * field records when the request was processed.
     */
    private String executionTime;

    /**
     * A list of individuals or entities who have approved the request. Each entry in this list
     * represents someone who has given their approval as part of the workflow.
     */
    private List<String> approvedBy;

    /**
     * A list of individuals or entities who have not yet approved the request. This list identifies
     * those who are still required to take action for the request to proceed.
     */
    private List<String> notYetApprovedBy;

    /**
     * A list of individuals or entities who have rejected the request. Each entry in this list
     * represents someone who has explicitly rejected the request, affecting its outcome.
     */
    private List<String> rejectedBy;

    /**
     * The result of the request, which could include details about the outcome of the execution,
     * such as "Success", "Failure", or any relevant message or data produced as a result.
     */
    private String result;
    
}
