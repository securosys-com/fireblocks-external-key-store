// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.controller;

import com.securosys.fireblocks.business.dto.request.CreateValidationsRequest;
import com.securosys.fireblocks.business.dto.request.ProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.request.ValidationProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.response.CreateValidationKeyResponse;
import com.securosys.fireblocks.business.dto.response.CreateValidationResponse;
import com.securosys.fireblocks.business.dto.response.ProofOfOwnershipResponse;
import com.securosys.fireblocks.business.dto.response.ValidationProofOfOwnershipResponse;
import com.securosys.fireblocks.business.service.HelperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Helper Functions", description = "Helper functions for validation keys and asset keys (signing keys).")
@SecurityRequirement(name = "api_key")
@Slf4j
@ConditionalOnProperty(
        value = "tsb.airGapped",
        havingValue = "false",
        matchIfMissing = true
)
public class HelpersController extends BaseController{

    private final HelperService helperService;

    @PostMapping(value = "/createValidationKey", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Create a validation key",
            description = "Creates an RSA key that can be used as the validation key."
                + " The key label is the fixed value \"FIREBLOCKS_VALIDATION_KEY\"."
                + " Returns the public key in PEM format. Onboard this public key to your Fireblocks workspace.",
            responses = { @ApiResponse(responseCode = "201", description = SUCCESSFUL_OPERATION) })
    public ResponseEntity<CreateValidationKeyResponse> createValidationKey() {

        CreateValidationKeyResponse response = new CreateValidationKeyResponse();
        response.setPublicKeyPem(helperService.createValidationKey());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/createValidations", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Create a validation certificate",
            description = "Creates a validation certificate. The certificate is signed by the validation key."
                + " The public key that is certified is the provided asset key (in Fireblocks' terms: signing key)."
                + " Prerequisite: the validation key with label \"FIREBLOCKS_VALIDATION_KEY\" must exist.",
            responses = { @ApiResponse(responseCode = "201", description = SUCCESSFUL_OPERATION) })
    public ResponseEntity<CreateValidationResponse> createValidations(@Valid @RequestBody CreateValidationsRequest request) {

        String csr = helperService.generateCsr(request);
        CreateValidationResponse response = new CreateValidationResponse();
        response.setCertificate(helperService.signCsr(csr));

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/proofOfOwnership", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a Proof of Ownership",
            description = "Generates a signed Proof of Ownership message, thus confirming control over the private asset key (in Fireblocks' terms: signing key).",
            responses = { @ApiResponse(responseCode = "201", description = SUCCESSFUL_OPERATION) }
    )
    public ResponseEntity<ProofOfOwnershipResponse> proofOfOwnership(@Valid @RequestBody ProofOfOwnershipRequest request){

        ProofOfOwnershipResponse response = helperService.generateProofOfOwnership(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/validationAndProofOfOwnership", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create validation and Proof of Ownership in one go",
            description = "Creates a validation certificate and a signed Proof of Ownership in a single request. See also the separate endpoints.",
            responses = { @ApiResponse(responseCode = "201", description = SUCCESSFUL_OPERATION) }
    )
    public ResponseEntity<ValidationProofOfOwnershipResponse> validationAndProofOfOwnership(@Valid @RequestBody ValidationProofOfOwnershipRequest request){

        ValidationProofOfOwnershipResponse response = helperService.generateValidationProofOfOwnership(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}