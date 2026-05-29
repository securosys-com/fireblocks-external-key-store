// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.controller;

import com.securosys.fireblocks.business.dto.customServer.*;
import com.securosys.fireblocks.business.service.SigningService;
import com.securosys.fireblocks.business.service.StatusService;
import com.securosys.fireblocks.business.util.JsonUtil;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Securosys Custom Server", description = "The primary endpoints, required by the Fireblocks Key Link API specification. Used for submitting signing requests, polling their status, and re-trying them.")
@SecurityRequirement(name = "api_key")
@OpenAPIDefinition(
        info = @Info(
                title = "Fireblocks - Securosys Custom Server",
                version = "1.0.3",
                description = """
                        The Securosys Custom Server enables your Fireblocks workspace to use keys that
                        are securely stored in a Primus HSM.
                        The Securosys Custom Server sits between the Fireblocks Key Link Agent and the Securosys Transaction Security Broker,
                        and translates requests between them.

                        For details, view the [online documentation](https://docs.securosys.com/fireblocks/overview/).

                        [Securosys End-User License Agreement (EULA)](https://www.securosys.com/eula)\s
                        """,
                license = @License(name = "Securosys License",
                        url = "https://www.securosys.com/en/about/securosys-general-terms-and-conditions"),
                contact = @Contact(url = "https://www.securosys.com/en/contactus", name = "Securosys SA",
                        email = "info@securosys.ch")))
public class MessageController extends BaseController{

    private final SigningService signingService;
    private final StatusService statusService;
    private final JsonUtil jsonUtil;

    @PostMapping(value = "/messagesToSign", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Sign message",
            description = "Sends new messages to the HSM for signing. The Fireblocks Agent calls this when it requires something to be signed. "
                    + "The request is persisted to the database, and forwarded to the TSB. The result is stored in the database.",
            responses = { @ApiResponse(responseCode = "200", description = SUCCESSFUL_OPERATION) },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
                    @Content(schema = @Schema(implementation = MessagesRequest.class))}))
    public ResponseEntity<?> messagesToSign(HttpServletRequest servletRequest) {

        String originalBody = getBodyString(servletRequest);
        MessagesStatusResponse response = signingService.signMessages(originalBody);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/messagesStatus", produces = {MediaType.APPLICATION_JSON_VALUE })
    @Operation(summary = "Get message status",
            description = "Query the status of the messages that were previously submitted to be signed. "
                    + "These signatures may already be ready or may still be pending. The Fireblocks Agent polls this endpoint.",
            responses = { @ApiResponse(responseCode = "200", description = SUCCESSFUL_OPERATION) })
    public ResponseEntity<MessagesStatusResponse> messagesStatus(@Valid @RequestBody MessagesStatusRequest request) {
        return ResponseEntity.ok(statusService.getStatuses(request));
    }

    @PostMapping(value = "/signAllPendingMessages", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Retry all pending messages",
            description = "Sends all pending messages (that are cached in the database) once again to the TSB for signing.",
            responses = { @ApiResponse(responseCode = "200", description = SUCCESSFUL_OPERATION) })
    public ResponseEntity<Void> signAllPendingMessages() {
        signingService.signAllPending();
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/signRequest/{requestId}", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Retry single pending message",
            description = "Sends a pending message by request ID (that is cached in the database) once again to the TSB for signing.",
            responses = { @ApiResponse(responseCode = "200", description = SUCCESSFUL_OPERATION) })
    public ResponseEntity<MessagesStatusResponse> signRequest(@PathVariable UUID requestId) {
        signingService.signMessage(requestId);
        return ResponseEntity.ok().build();
    }

}
