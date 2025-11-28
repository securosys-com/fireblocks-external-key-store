// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks;

import com.securosys.fireblocks.business.dto.ReasonBasedExceptionDto;
import com.securosys.fireblocks.business.dto.customServer.MessagesRequest;
import com.securosys.fireblocks.business.dto.customServer.MessagesStatusRequest;
import com.securosys.fireblocks.business.dto.customServer.MessagesStatusResponse;
import com.securosys.fireblocks.business.dto.request.CreateValidationKeyRequest;
import com.securosys.fireblocks.business.dto.request.CreateValidationsRequest;
import com.securosys.fireblocks.business.dto.request.ProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.request.ValidationProofOfOwnershipRequest;
import com.securosys.fireblocks.business.dto.response.CreateValidationKeyResponse;
import com.securosys.fireblocks.business.dto.response.CreateValidationResponse;
import com.securosys.fireblocks.business.dto.response.ProofOfOwnershipResponse;
import com.securosys.fireblocks.business.dto.response.ValidationProofOfOwnershipResponse;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class TestBusinessApp {

	private TestBusinessApp() {}


    // ======================================================
    // SERVICE CONTROLLER
    // ======================================================


    public static List<String> sendGetLogsRequest() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .log().all()
                .get("/logs")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(new TypeRef<>() {
                });
    }

    public static Map<String, String> sendGetVersionRequest() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .log().all()
                .get("/versionInfo")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(new TypeRef<>() {
                });
    }

    // ======================================================
    // HELPERS CONTROLLER
    // ======================================================

    private static ValidatableResponse sendCreateValidationKeyRequest(CreateValidationKeyRequest request) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .log().all()
                .post("/createValidationKey")
                .then()
                .log().all();
    }

    public static ReasonBasedExceptionDto sendInvalidCreateValidationKeyRequest(CreateValidationKeyRequest request, HttpStatus expectedStatus) {
        return sendCreateValidationKeyRequest(request)
                .statusCode(expectedStatus.value())
                .extract()
                .as(ReasonBasedExceptionDto.class);
    }

    public static CreateValidationKeyResponse sendValidCreateValidationKeyRequest(CreateValidationKeyRequest request) {
        return sendCreateValidationKeyRequest(request)
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(CreateValidationKeyResponse.class);
    }

    public static CreateValidationResponse sendValidCreateValidationsRequest(CreateValidationsRequest request) {
        return sendCreateValidationsRequest(request)
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(CreateValidationResponse.class);
    }

    public static ReasonBasedExceptionDto sendInvalidCreateValidationsRequest(CreateValidationsRequest request, HttpStatus expectedStatus) {
        return sendCreateValidationsRequest(request)
                .statusCode(expectedStatus.value())
                .extract()
                .as(ReasonBasedExceptionDto.class);
    }

    private static ValidatableResponse sendCreateValidationsRequest(CreateValidationsRequest request) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .log().all()
                .post("/createValidations")
                .then()
                .log().all();
    }

    public static ProofOfOwnershipResponse sendValidProofOfOwnershipRequest(ProofOfOwnershipRequest request) {
        return sendProofOfOwnershipRequest(request)
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(ProofOfOwnershipResponse.class);
    }

    public static ReasonBasedExceptionDto sendInvalidProofOfOwnershipRequest(ProofOfOwnershipRequest request, HttpStatus expectedStatus) {
        return sendProofOfOwnershipRequest(request)
                .statusCode(expectedStatus.value())
                .extract()
                .as(ReasonBasedExceptionDto.class);
    }

    private static ValidatableResponse sendProofOfOwnershipRequest(ProofOfOwnershipRequest request) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .log().all()
                .post("/proofOfOwnership")
                .then()
                .log().all();
    }

    public static ValidationProofOfOwnershipResponse sendValidValidationAndProofOfOwnershipRequest(ValidationProofOfOwnershipRequest request) {
        return sendValidationAndProofOfOwnershipRequest(request)
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(ValidationProofOfOwnershipResponse.class);
    }

    public static ReasonBasedExceptionDto sendInvalidValidationAndProofOfOwnershipRequest(ValidationProofOfOwnershipRequest request, HttpStatus expectedStatus) {
        return sendValidationAndProofOfOwnershipRequest(request)
                .statusCode(expectedStatus.value())
                .extract()
                .as(ReasonBasedExceptionDto.class);
    }

    private static ValidatableResponse sendValidationAndProofOfOwnershipRequest(ValidationProofOfOwnershipRequest request) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .log().all()
                .post("/validationAndProofOfOwnership")
                .then()
                .log().all();
    }

    // ======================================================
    // MESSAGES CONTROLLER
    // ======================================================

    public static MessagesStatusResponse sendValidMessagesToSignRequest(MessagesRequest request) {
        return sendMessagesToSignRequest(request)
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(MessagesStatusResponse.class);
    }

    public static ReasonBasedExceptionDto sendInvalidMessagesToSignRequest(MessagesRequest request, HttpStatus expectedStatus) {
        return sendMessagesToSignRequest(request)
                .statusCode(expectedStatus.value())
                .extract()
                .as(ReasonBasedExceptionDto.class);
    }

    private static ValidatableResponse sendMessagesToSignRequest(MessagesRequest request) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .log().all()
                .post("/messagesToSign")
                .then()
                .log().all();
    }

    public static MessagesStatusResponse sendValidMessagesStatusRequest(MessagesStatusRequest request) {
        return sendMessagesStatusRequest(request)
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(MessagesStatusResponse.class);
    }

    public static ReasonBasedExceptionDto sendInvalidMessagesStatusRequest(MessagesStatusRequest request, HttpStatus expectedStatus) {
        return sendMessagesStatusRequest(request)
                .statusCode(expectedStatus.value())
                .extract()
                .as(ReasonBasedExceptionDto.class);
    }

    private static ValidatableResponse sendMessagesStatusRequest(MessagesStatusRequest request) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(request)
                .log().all()
                .post("/messagesStatus")
                .then()
                .log().all();
    }

}
