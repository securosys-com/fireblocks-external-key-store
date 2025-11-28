// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.controller;

import com.securosys.fireblocks.IntTestBase;
import com.securosys.fireblocks.TestBusinessApp;
import com.securosys.fireblocks.business.dto.ReasonBasedExceptionDto;
import com.securosys.fireblocks.business.dto.request.CreateValidationKeyRequest;
import com.securosys.fireblocks.business.dto.request.CreateValidationsRequest;
import com.securosys.fireblocks.business.dto.request.ProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.request.ValidationProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.response.CreateValidationKeyResponse;
import com.securosys.fireblocks.business.dto.response.CreateValidationResponse;
import com.securosys.fireblocks.business.dto.response.ProofOfOwnershipResponse;
import com.securosys.fireblocks.business.dto.response.ValidationProofOfOwnershipResponse;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.service.TsbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HelpersControllerIntTest extends IntTestBase {

    @Autowired
    private TsbService tsbService;

    private static final String VALIDATION_KEY = "FIREBLOCKS_VALIDATION_KEY";
    private static final String TEST_KEY_EC = "fireblocksTestEc";
    private static final String TEST_KEY_ED = "fireblocksTestEd";
    private static final String ALGORITHM_EC = "EC";
    private static final String ALGORITHM_ED = "ED";
    private static final String WORKSPACE_NAME = "Test Workspace";
    private static final String SDK_API_KEY = "test-sdk-api-key";
    private static final String AGENT_ID = "test-agent-id";

    @Test
    @DisplayName("2.5.1.1 Execute get logs → should succeed")
    void executeGetLogs_shouldSucceed() {
        List<String> response = TestBusinessApp.sendGetLogsRequest();
        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
    }

    @Test
    @DisplayName("2.5.1.2 Execute get version → should succeed")
    void executeGetVersion_shouldSucceed() {
        Map<String, String> response = TestBusinessApp.sendGetVersionRequest();
        assertThat(response).isNotNull();
        assertThat(response).containsKeys("Version");
    }

    @Test
    @DisplayName("2.5.1.3 Execute create validation key → should succeed")
    void executeCreateValidationKey_shouldSucceed() {
        tsbService.deleteKey(VALIDATION_KEY);
        CreateValidationKeyRequest request  = new CreateValidationKeyRequest();
        request.setAlgorithm("RSA");
        CreateValidationKeyResponse response = TestBusinessApp.sendValidCreateValidationKeyRequest(request);
        assertThat(response).isNotNull();
        assertThat(response.getPublicKeyPem()).contains("-----BEGIN PUBLIC KEY-----");
    }

    @Test
    @DisplayName("2.5.1.4 Execute create validation key when it already exists → should fail")
    void executeCreateValidationKey_shouldFailAlreadyExists() {
        tsbService.deleteKey(VALIDATION_KEY);
        CreateValidationKeyRequest request  = new CreateValidationKeyRequest();
        request.setAlgorithm("RSA");
        TestBusinessApp.sendValidCreateValidationKeyRequest(request);

        ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidCreateValidationKeyRequest(request,HttpStatus.BAD_REQUEST);

        assertThat(response).isNotNull();
        assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_DATA_OBJECT_ALREADY_EXISTING.getReason());
        assertThat(response.getMessage()).contains("Key already exist");
    }

    @Test
    @DisplayName("2.5.1.5 Execute create validation with EC key → should succeed")
    void executeCreateValidationEC_shouldSucceed() {
        tsbService.deleteKey(VALIDATION_KEY);
        CreateValidationKeyRequest keyRequest = new CreateValidationKeyRequest();
        keyRequest.setAlgorithm("EC");
        keyRequest.setCurveOid("1.3.132.0.10");
        TestBusinessApp.sendValidCreateValidationKeyRequest(keyRequest);

        CreateValidationsRequest request = new CreateValidationsRequest();
        request.setAssetKeyName(TEST_KEY_EC);
        request.setAssetKeyAlgorithm(ALGORITHM_EC);
        request.setIsSkaKey(false);

        CreateValidationResponse response = TestBusinessApp.sendValidCreateValidationsRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getCertificate()).contains("-----BEGIN CERTIFICATE-----");
    }

    @Test
    @DisplayName("2.5.1.6 Execute create proof of ownership with EC key → should succeed")
    void executeProofOfOwnershipEC_shouldSucceed() {

        ProofOfOwnershipRequest request = ProofOfOwnershipRequest.builder()
                .assetKeyName(TEST_KEY_EC)
                .assetKeyAlgorithm(ALGORITHM_EC)
                .workspaceDisplayName(WORKSPACE_NAME)
                .sdkApiKey(SDK_API_KEY)
                .build();

        ProofOfOwnershipResponse response = TestBusinessApp.sendValidProofOfOwnershipRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getProofOfOwnership().getMessage()).isNotEmpty();
        assertThat(response.getProofOfOwnership().getSignature()).isNotEmpty();
    }

    @Test
    @DisplayName("2.5.1.7 Execute create proof of ownership + validation → should succeed")
    void executeProofOfOwnershipAndValidationEC_shouldSucceed() {

        ValidationProofOfOwnershipRequest request = ValidationProofOfOwnershipRequest.builder()
                .assetKeyName(TEST_KEY_EC)
                .assetKeyAlgorithm(ALGORITHM_EC)
                .workspaceDisplayName(WORKSPACE_NAME)
                .agentUserId(AGENT_ID)
                .sdkApiKey(SDK_API_KEY)
                .isSkaKey(false)
                .build();

        ValidationProofOfOwnershipResponse response = TestBusinessApp.sendValidValidationAndProofOfOwnershipRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getSignedCertPem()).isNotEmpty();
        assertThat(response.getSigningDeviceKeyId()).isEqualTo(TEST_KEY_EC);
        assertThat(response.getAgentUserId()).isEqualTo(AGENT_ID);
        assertThat(response.getProofOfOwnership().getMessage()).isNotEmpty();
        assertThat(response.getProofOfOwnership().getSignature()).isNotEmpty();
    }


    @Test
    @DisplayName("2.5.1.8 Execute create validation with invalid key → should fail")
    void executeCreateValidationWithInvalidKey_shouldFail() {
        tsbService.deleteKey(VALIDATION_KEY);
        CreateValidationKeyRequest keyRequest = new CreateValidationKeyRequest();
        keyRequest.setAlgorithm("RSA");
        TestBusinessApp.sendValidCreateValidationKeyRequest(keyRequest);

        CreateValidationsRequest request = new CreateValidationsRequest();
        request.setAssetKeyName("FireblocksInvalidKey");
        request.setAssetKeyAlgorithm(ALGORITHM_EC);
        request.setIsSkaKey(false);

        ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidCreateValidationsRequest(request, HttpStatus.NOT_IMPLEMENTED);

        assertThat(response).isNotNull();
        assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_GENERAL.getReason());
        assertThat(response.getMessage()).contains("res.error.key.not.existent");
    }

    @Test
    @DisplayName("2.5.1.9 Execute create proof of ownership with invalid key → should fail")
    void executeProofOfOwnershipWithInvalidKey_shouldFail() {

        ProofOfOwnershipRequest request = ProofOfOwnershipRequest.builder()
                .assetKeyName("FireblocksInvalidKey")
                .assetKeyAlgorithm(ALGORITHM_EC)
                .workspaceDisplayName(WORKSPACE_NAME)
                .sdkApiKey(SDK_API_KEY)
                .build();

        ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidProofOfOwnershipRequest(request, HttpStatus.NOT_IMPLEMENTED);

        assertThat(response).isNotNull();
        assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_GENERAL.getReason());
        assertThat(response.getMessage()).contains("res.error.key.not.existent");
    }

    @Test
    @DisplayName("2.5.1.10 Execute create proof of ownership + validation with invalid key → should fail")
    void executeProofOfOwnershipAndValidationWithInvalidKey_shouldFail() {

        ValidationProofOfOwnershipRequest request = ValidationProofOfOwnershipRequest.builder()
                .assetKeyName("FireblocksInvalidKey")
                .assetKeyAlgorithm(ALGORITHM_EC)
                .workspaceDisplayName(WORKSPACE_NAME)
                .agentUserId(AGENT_ID)
                .sdkApiKey(SDK_API_KEY)
                .isSkaKey(false)
                .build();

        ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidValidationAndProofOfOwnershipRequest(request, HttpStatus.NOT_IMPLEMENTED);

        assertThat(response).isNotNull();
        assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_GENERAL.getReason());
        assertThat(response.getMessage()).contains("res.error.key.not.existent");
    }

    @Test
    @DisplayName("2.5.1.11 Execute create validation with ED key → should succeed")
    void executeCreateValidationED_shouldSucceed() {
        tsbService.deleteKey(VALIDATION_KEY);
        CreateValidationKeyRequest keyRequest = new CreateValidationKeyRequest();
        keyRequest.setAlgorithm("ED");
        keyRequest.setCurveOid("1.3.101.112");
        TestBusinessApp.sendValidCreateValidationKeyRequest(keyRequest);

        CreateValidationsRequest request = new CreateValidationsRequest();
        request.setAssetKeyName(TEST_KEY_ED);
        request.setAssetKeyAlgorithm(ALGORITHM_ED);
        request.setIsSkaKey(false);

        CreateValidationResponse response = TestBusinessApp.sendValidCreateValidationsRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getCertificate()).contains("-----BEGIN CERTIFICATE-----");
    }

    @Test
    @DisplayName("2.5.1.12 Execute create validation with invalid algorithm → should fail")
    void executeCreateValidationWithInvalidAlgorithm_shouldFail() {
        tsbService.deleteKey(VALIDATION_KEY);
        CreateValidationKeyRequest keyRequest = new CreateValidationKeyRequest();
        keyRequest.setAlgorithm("RSA");
        TestBusinessApp.sendValidCreateValidationKeyRequest(keyRequest);

        CreateValidationsRequest request = new CreateValidationsRequest();
        request.setAssetKeyName(TEST_KEY_ED);
        request.setAssetKeyAlgorithm("WRONG_ALGORITHM");
        request.setIsSkaKey(false);

        ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidCreateValidationsRequest(request, HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(response).isNotNull();
        assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_INVALID_ALGORITHM.getReason());
        assertThat(response.getMessage()).contains("Unsupported key algorithm");
    }

    @Test
    @DisplayName("2.5.1.13 Execute create proof of ownership with ED key → should succeed")
    void executeProofOfOwnershipED_shouldSucceed() {

        ProofOfOwnershipRequest request = ProofOfOwnershipRequest.builder()
                .assetKeyName(TEST_KEY_ED)
                .assetKeyAlgorithm(ALGORITHM_ED)
                .workspaceDisplayName(WORKSPACE_NAME)
                .sdkApiKey(SDK_API_KEY)
                .build();

        ProofOfOwnershipResponse response = TestBusinessApp.sendValidProofOfOwnershipRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getProofOfOwnership().getMessage()).isNotEmpty();
        assertThat(response.getProofOfOwnership().getSignature()).isNotEmpty();
    }

    @Test
    @DisplayName("2.5.1.14 Execute create proof of ownership with invalid algorithm → should fail")
    void executeProofOfOwnershipWithInvalidAlgorithm_shouldFail() {

        ProofOfOwnershipRequest request = ProofOfOwnershipRequest.builder()
                .assetKeyName(TEST_KEY_EC)
                .assetKeyAlgorithm("WRONG_ALGORITHM")
                .workspaceDisplayName(WORKSPACE_NAME)
                .sdkApiKey(SDK_API_KEY)
                .build();

        ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidProofOfOwnershipRequest(request, HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(response).isNotNull();
        assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_INVALID_ALGORITHM.getReason());
        assertThat(response.getMessage()).contains("Unsupported key algorithm");
    }

    @Test
    @DisplayName("2.5.1.15 Execute create proof of ownership + validation with ED key → should succeed")
    void executeProofOfOwnershipAndValidationED_shouldSucceed() {

        ValidationProofOfOwnershipRequest request = ValidationProofOfOwnershipRequest.builder()
                .assetKeyName(TEST_KEY_ED)
                .assetKeyAlgorithm(ALGORITHM_ED)
                .workspaceDisplayName(WORKSPACE_NAME)
                .agentUserId(AGENT_ID)
                .sdkApiKey(SDK_API_KEY)
                .isSkaKey(false)
                .build();

        ValidationProofOfOwnershipResponse response = TestBusinessApp.sendValidValidationAndProofOfOwnershipRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getSignedCertPem()).isNotEmpty();
        assertThat(response.getSigningDeviceKeyId()).isEqualTo(TEST_KEY_ED);
        assertThat(response.getAgentUserId()).isEqualTo(AGENT_ID);
        assertThat(response.getProofOfOwnership().getMessage()).isNotEmpty();
        assertThat(response.getProofOfOwnership().getSignature()).isNotEmpty();
    }

    @Test
    @DisplayName("2.5.1.16 Execute create proof of ownership + validation with invalid algorithm → should fail")
    void executeProofOfOwnershipAndValidationWithInvalidAlgorithm_shouldFail() {

        ValidationProofOfOwnershipRequest request = ValidationProofOfOwnershipRequest.builder()
                .assetKeyName(TEST_KEY_EC)
                .assetKeyAlgorithm("WRONG_ALGORITHM")
                .workspaceDisplayName(WORKSPACE_NAME)
                .agentUserId(AGENT_ID)
                .sdkApiKey(SDK_API_KEY)
                .isSkaKey(false)
                .build();

        ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidValidationAndProofOfOwnershipRequest(request, HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(response).isNotNull();
        assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_INVALID_ALGORITHM.getReason());
        assertThat(response.getMessage()).contains("Unsupported key algorithm");
    }

}
