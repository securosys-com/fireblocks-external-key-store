// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.controller;


import com.securosys.fireblocks.business.service.InfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Service Information")
@SecurityRequirement(name = "api_key")
@Slf4j
public class ServiceController extends BaseController{

    private final InfoService infoService;

    @GetMapping(value = "/versionInfo", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get API version",
            description = "Returns information about the currently deployed service.",
            responses = { @ApiResponse(responseCode = "200", description = SUCCESSFUL_OPERATION) })
    public ResponseEntity<Map<String, String>> getVersionInfo() {
        return new ResponseEntity<>(infoService.getVersion(), HttpStatus.OK);
    }

    @GetMapping(value = "/logs", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Get API logs",
            responses = { @ApiResponse(responseCode = "200", description = SUCCESSFUL_OPERATION) })
    public ResponseEntity<List<String>> getLogs() {

        return new ResponseEntity<>(infoService.fetchLogs(), HttpStatus.OK);
    }
}
