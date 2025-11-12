// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.controller;

import com.securosys.fireblocks.IntTestBase;
import com.securosys.fireblocks.TestBusinessApp;
import com.securosys.fireblocks.TestDataFactory;
import com.securosys.fireblocks.business.dto.customServer.MessageStatus;
import com.securosys.fireblocks.business.dto.customServer.MessagesRequest;
import com.securosys.fireblocks.business.dto.customServer.MessagesStatusRequest;
import com.securosys.fireblocks.business.dto.customServer.MessagesStatusResponse;
import com.securosys.fireblocks.datamodel.configuration.DataSourceConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = DataSourceConfiguration.class)
class MessageToSignIntTest extends IntTestBase {

    private static final String VALID_SERVICE_NAME = "SIGNING_SERVICE";
    private static final String INVALID_SERVICE_NAME = "wrong-service";
    private static final String VALID_SIGNATURE = "VALID_SIGNATURE";
    private static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";
    private static final String ALGORITHM_EC = "ECDSA_SECP256K1";
    private static final String ALGORITHM_ED = "EDDSA_ED25519";

    private static final String EC_KEY_ID = "fireblocksTestEc";
    private static final String ED_KEY_ID = "fireblocksTestEd";
    private static final String RSA_KEY_ID = "fireblocksTestRsa";

    @BeforeEach
    void resetBefore() {
        ReflectionTestUtils.setField(properties, "verifySignatures", false);
    }

    // ======================================================
    // SUCCESS CASES
    // ======================================================

    @Test
    @DisplayName("2.4.1.1 Execute EC transaction → should succeed")
    void executeEcTransaction_shouldSucceed() {
        MessagesRequest request = TestDataFactory.buildMessagesRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("SIGNED");
    }

    @Test
    @DisplayName("2.4.1.2 Execute ED transaction → should succeed")
    void executeEdTransaction_shouldSucceed() {
        MessagesRequest request = TestDataFactory.buildMessagesRequest(ED_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_ED);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("SIGNED");
    }

    @Test
    @DisplayName("2.4.1.3 Execute EC multiple transaction → should succeed")
    void executeEcMultipleTransaction_shouldSucceed() {
        MessagesRequest request = TestDataFactory.buildMultipleMessagesRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC, true);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).hasSize(2);
        assertThat(response.getStatuses())
                .allMatch(s -> s.getStatus().equals("SIGNED"));
    }

//    @Test
//    @DisplayName("2.4.1.4 Execute EC transaction with wrong service name → should fail")
//    void executeEcTransaction_wrongServiceName_shouldFail() {
//
//        ReflectionTestUtils.setField(properties, "verifySignatures", true);
//
//        MessagesRequest request = TestDataFactory.buildMessagesRequest(EC_KEY_ID, INVALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC);
//
//        MessagesStatusResponse response =
//                TestBusinessApp.sendValidMessagesToSignRequest(request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatuses()).isNotEmpty();
//        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("FAILED");
//    }

    @Test
    @DisplayName("2.4.1.6 Execute EC multiple messages in one transaction → should succeed")
    void executeEcMultipleMessages_shouldSucceed() {
        MessagesRequest request = TestDataFactory.buildMessagesInOneEnvelopeRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC, true);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("SIGNED");
    }

    @Test
    @DisplayName("2.4.1.7 Execute message status → should succeed")
    void executeMessageStatus_shouldSucceed() {
        MessagesRequest signRequest = TestDataFactory.buildMessagesRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC);

        MessagesStatusResponse signResponse = TestBusinessApp.sendValidMessagesToSignRequest(signRequest);


        MessagesStatusRequest statusRequest = new MessagesStatusRequest();
        List<UUID> requestsIds = signResponse.getStatuses()
                .stream()
                .map(MessageStatus::getRequestId)
                .collect(Collectors.toList());
        statusRequest.setRequestsIds(requestsIds);

        MessagesStatusResponse statusResponse = TestBusinessApp.sendValidMessagesStatusRequest(statusRequest);

        assertThat(statusResponse).isNotNull();
        assertThat(statusResponse.getStatuses()).isNotEmpty();
        assertThat(statusResponse.getStatuses().get(0).getStatus()).isEqualTo("SIGNED");
    }

    @Test
    @DisplayName("2.4.1.8 Execute message status for multiple transaction → should succeed")
    void executeMessageStatusForMultipleTransaction_shouldSucceed() {
        MessagesRequest signRequest = TestDataFactory.buildMultipleMessagesRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC, true);

        MessagesStatusResponse signResponse = TestBusinessApp.sendValidMessagesToSignRequest(signRequest);

        MessagesStatusRequest statusRequest = new MessagesStatusRequest();
        List<UUID> requestsIds = signResponse.getStatuses()
                .stream()
                .map(MessageStatus::getRequestId)
                .collect(Collectors.toList());
        statusRequest.setRequestsIds(requestsIds);

        MessagesStatusResponse statusResponse = TestBusinessApp.sendValidMessagesStatusRequest(statusRequest);

        assertThat(statusResponse).isNotNull();
        assertThat(statusResponse.getStatuses()).hasSize(2);
        assertThat(statusResponse.getStatuses())
                .allMatch(s -> s.getStatus().equals("SIGNED"));
    }

    @Test
    @DisplayName("2.4.1.9 Execute message status for multiple messages in one transaction → should succeed")
    void executeMessageStatusForMultipleMessages_shouldSucceed() {
        MessagesRequest signRequest = TestDataFactory.buildMessagesInOneEnvelopeRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC, true);

        MessagesStatusResponse signResponse = TestBusinessApp.sendValidMessagesToSignRequest(signRequest);

        MessagesStatusRequest statusRequest = new MessagesStatusRequest();
        List<UUID> requestsIds = signResponse.getStatuses()
                .stream()
                .map(MessageStatus::getRequestId)
                .collect(Collectors.toList());
        statusRequest.setRequestsIds(requestsIds);

        MessagesStatusResponse statusResponse = TestBusinessApp.sendValidMessagesStatusRequest(statusRequest);

        assertThat(statusResponse).isNotNull();
        assertThat(statusResponse.getStatuses()).isNotEmpty();
        assertThat(statusResponse.getStatuses().get(0).getStatus()).isEqualTo("SIGNED");
    }

    // ======================================================
    // FAILURE CASES
    // ======================================================


    @Test
    @DisplayName("2.4.1.10 Execute EC transaction, key does not exist → should fail")
    void executeEcTransaction_shouldFailKeyNotExist() {
        MessagesRequest request = TestDataFactory.buildMessagesRequest("wrongFireblocksEcKey", VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("2.4.1.11 Execute EC transaction, wrong algorithm → should fail")
    void executeEcTransaction_shouldFailWrongAlgorithm() {
        MessagesRequest request = TestDataFactory.buildMessagesRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, "WRONG_ALGORITHM");

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("2.4.1.12 Execute EC transaction, key with different algorithm → should fail")
    void executeEcTransaction_shouldFailWrongKeyAlgorithm() {
        MessagesRequest request = TestDataFactory.buildMessagesRequest(RSA_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("2.4.1.13 Execute EC multiple transaction, one of keys are wrong → should fail")
    void executeEcTransaction_shouldFailWrongKeys() {
        MessagesRequest request = TestDataFactory.buildMultipleMessagesRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC, false);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses())
                .hasSize(2)
                .satisfiesExactly(
                        s1 -> assertThat(s1.getStatus()).isEqualTo("SIGNED"),
                        s2 -> assertThat(s2.getStatus()).isEqualTo("FAILED")
                );
    }

    @Test
    @DisplayName("2.4.1.14 Execute EC multiple messages in one transaction, one message wrong → should fail")
    void executeEcMultipleMessages_shouldFail() {
        MessagesRequest request = TestDataFactory.buildMessagesInOneEnvelopeRequest(EC_KEY_ID, VALID_SERVICE_NAME, VALID_SIGNATURE, ALGORITHM_EC, false);

        MessagesStatusResponse response = TestBusinessApp.sendValidMessagesToSignRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatuses()).isNotEmpty();
        assertThat(response.getStatuses().get(0).getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("2.4.1.15 Execute message status with wrong id → should fail")
    void executeMessageStatus_shouldFail() {

        MessagesStatusRequest statusRequest = new MessagesStatusRequest();
        List<UUID> requestsIds = List.of(UUID.randomUUID());
        statusRequest.setRequestsIds(requestsIds);

        MessagesStatusResponse statusResponse = TestBusinessApp.sendValidMessagesStatusRequest(statusRequest);

        assertThat(statusResponse).isNotNull();
        assertThat(statusResponse.getStatuses()).isEmpty();
    }


//
//    @Test
//    @DisplayName("2.4.1.5 Execute EC transaction with wrong payload signature → should fail")
//    void executeEcTransaction_wrongSignature_shouldFail() {
//        MessagesRequest request = TestDataFactory.buildMessagesRequest(EC_KEY_ID, VALID_SERVICE_NAME, INVALID_SIGNATURE);
//
//        ReasonBasedExceptionDto response =
//                TestBusinessApp.sendInvalidMessagesToSignRequest(request, HttpStatus.BAD_REQUEST);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getReason()).contains("Signature");
//    }


}
