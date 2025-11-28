// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.service;

import com.securosys.fireblocks.IntTestBase;
import com.securosys.fireblocks.TestBusinessApp;
import com.securosys.fireblocks.Util;
import com.securosys.fireblocks.business.dto.ReasonBasedExceptionDto;
import com.securosys.fireblocks.business.dto.request.CreateValidationKeyRequest;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import com.securosys.fireblocks.business.facade.HsmFacade;
import com.securosys.fireblocks.business.service.MtlsClientFactory;
import com.securosys.fireblocks.business.service.TsbService;
import com.securosys.fireblocks.configuration.ApiKeyTypes;
import com.securosys.fireblocks.configuration.TsbProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConnectionIntTest extends IntTestBase {

    private static final String VALIDATION_KEY = "FIREBLOCKS_VALIDATION_KEY";
    private static final String REST_API = "https://integration-test.cloudshsm.com";

    private static final String TSB_REST_API = "TSB_REST_API";
    private static final String TSB_KEY_MANAGEMENT_TOKEN = "TSB_KEY_MANAGEMENT_TOKEN";
    private static final String TSB_KEY_OPERATION_TOKEN = "TSB_KEY_OPERATION_TOKEN";
    private static final String TSB_MTLS_CERT = "TSB_MTLS_CERT";
    private static final String TSB_MTLS_KEY = "TSB_MTLS_KEY";

    @Autowired
    private TsbService tsbService;

    @Autowired
    private TsbProperties tsbProperties;

    @Autowired
    private MtlsClientFactory mtlsClientFactory;

    @Autowired
    HsmFacade hsmFacade;

    @BeforeEach
    void setConnection(){
        ReflectionTestUtils.setField(tsbProperties, "tsbRestApi", Util.getEnv(TSB_REST_API, REST_API));
        ReflectionTestUtils.setField(tsbProperties, "tsbAccessToken", "");
    }

    @Test
    @DisplayName("2.3.1.1 Whitelisted API key → should succeed")
    void executeTestApiKeyConnection_shouldSucceed() {

        ApiKeyTypes originalApiAuth = tsbProperties.getApiAuthentication();

        ApiKeyTypes validKeys = new ApiKeyTypes();
        validKeys.setEnabled(true);
        validKeys.setKeyManagementToken(List.of(Util.getEnv(TSB_KEY_MANAGEMENT_TOKEN)));
        validKeys.setKeyOperationToken(List.of(Util.getEnv(TSB_KEY_OPERATION_TOKEN)));

        ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", validKeys);

        try {
            String response = hsmFacade.generateKeyPair(VALIDATION_KEY, null, "RSA", null,2048 );

            assertThat(response).isNotNull();
        } finally {
            tsbService.deleteKey(VALIDATION_KEY);
            ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", originalApiAuth);
        }
    }

    @Test
    @DisplayName("2.3.1.2 Wrong API key → should fail")
    void executeTestWrongApiKeyConnection_shouldFail() {

        ApiKeyTypes originalApiAuth = tsbProperties.getApiAuthentication();

        ApiKeyTypes validKeys = new ApiKeyTypes();
        validKeys.setEnabled(true);
        validKeys.setKeyManagementToken(List.of("tsb-x-token_123456789"));
        validKeys.setKeyOperationToken(List.of("tsb-x-token_987654321"));

        ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", validKeys);

        try {
            CreateValidationKeyRequest keyRequest = new CreateValidationKeyRequest();
            keyRequest.setAlgorithm("RSA");
            ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidCreateValidationKeyRequest(keyRequest, HttpStatus.BAD_REQUEST);

            assertThat(response).isNotNull();
            assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_OPERATION_FORBIDDEN.getReason());
            assertThat(response.getMessage()).contains("Unauthorized to TSB");

            assertThat(response).isNotNull();

        } finally {
            ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", originalApiAuth);
        }
    }

    @Test
    @DisplayName("2.3.1.3 Whitelisted mTLS client cert → should succeed")
    void executeMtlsConnection_shouldSucceed() {
        String originalCert = tsbProperties.getTsbMTlsCertificate();
        String originalKey = tsbProperties.getTsbMTlsKey();
        ApiKeyTypes originalApiAuth = tsbProperties.getApiAuthentication();

        ApiKeyTypes validKeys = new ApiKeyTypes();
        validKeys.setEnabled(true);
        validKeys.setKeyManagementToken(List.of(Util.getEnv(TSB_KEY_MANAGEMENT_TOKEN)));
        validKeys.setKeyOperationToken(List.of(Util.getEnv(TSB_KEY_OPERATION_TOKEN)));
        ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", validKeys);

        try {
            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsCertificate", "file:" + Util.getEnv(TSB_MTLS_CERT));
            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsKey", "file:"+ Util.getEnv(TSB_MTLS_KEY));

            mtlsClientFactory.init();

            String response = hsmFacade.generateKeyPair(VALIDATION_KEY, null, "RSA",null,2048 );

            assertThat(response).isNotNull();
        } finally {
            tsbService.deleteKey(VALIDATION_KEY);
            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsCertificate", originalCert);
            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsKey", originalKey);
            ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", originalApiAuth);
        }
    }
    
//    @Test
//    @DisplayName("2.3.1.4 Wrong mTLS  → should fail")
//    void executeWrongMtlsConnection_shouldFail() {
//        String originalCert = tsbProperties.getTsbMTlsCertificate();
//        String originalKey = tsbProperties.getTsbMTlsKey();
//        ApiKeyTypes originalApiAuth = tsbProperties.getApiAuthentication();
//        ApiKeyTypes validKeys = new ApiKeyTypes();
//        validKeys.setEnabled(true);
//        validKeys.setKeyManagementToken(List.of(Util.getEnv(TSB_KEY_MANAGEMENT_TOKEN)));
//        validKeys.setKeyOperationToken(List.of(Util.getEnv(TSB_KEY_OPERATION_TOKEN)));
//
//        try {
//            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsCertificate", "classpath:mtls/invalid-cert.pem");
//            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsKey", "classpath:mtls/invalid-key.pem");
//            ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", validKeys);
//
//            mtlsClientFactory.init();
//
//            ReasonBasedExceptionDto response = TestBusinessApp.sendInvalidCreateValidationKeyRequest(HttpStatus.NOT_IMPLEMENTED);
//
//            assertThat(response).isNotNull();
//            assertThat(response.getReason()).isEqualTo(BusinessReason.ERROR_GENERAL.getReason());
//            assertThat(response.getMessage()).contains("Unauthorized to TSB");
//        } finally {
//            tsbService.deleteKey(VALIDATION_KEY);
//            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsCertificate", originalCert);
//            ReflectionTestUtils.setField(tsbProperties, "tsbMTlsKey", originalKey);
//            ReflectionTestUtils.setField(tsbProperties, "apiAuthentication", originalApiAuth);
//        }
//    }

    @Test
    @DisplayName("2.6.1.1 Connect to standalone TSB → should succeed")
    void executeTransactionWithAirGappedMode_shouldSucceed() {

        try {
            ReflectionTestUtils.setField(tsbProperties, "airGapped", true);
            ReflectionTestUtils.setField(tsbProperties, "tsbRestApi", "");
            ReflectionTestUtils.setField(tsbProperties, "tsbAccessToken", "");

            // when
            boolean result = hsmFacade.isLicensed();

            // then
            assertThat(result).isFalse();
        } finally {
            ReflectionTestUtils.setField(tsbProperties, "airGapped", false);
        }
    }
}
