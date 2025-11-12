// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.service;

import com.securosys.fireblocks.business.dto.response.RequestStatusResponseDto;
import com.securosys.fireblocks.business.facade.HsmFacade;
import com.securosys.fireblocks.business.service.TsbService;
import com.securosys.fireblocks.business.util.CryptoUtil;
import com.securosys.fireblocks.configuration.CustomServerProperties;
import com.securosys.fireblocks.configuration.TsbProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DeploymentsMockTest {

    @Mock
    private TsbService tsbService;
    @Mock
    private CustomServerProperties properties;
    @Mock
    private TsbProperties tsbProperties;
    @Mock
    private CryptoUtil cryptoUtil;

    @InjectMocks
    private HsmFacade hsmFacade;

    @Test
    @DisplayName("2.6.1.2 SKA key → should succeed")
    void executeTransactionWithSkaKey_shouldSucceed() {

        // given
        String label = "test-key";
        String password = "secret";
        String payload = "deadbeef";
        String algorithm = "ECDSA_SECP256K1";
        String metadata = "meta";
        String metadataSignature = "metaSig";

        String fakeSignatureId = "req-123";

        when(tsbService.sign(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(fakeSignatureId);

        RequestStatusResponseDto pendingResponse = new RequestStatusResponseDto();
        pendingResponse.setId(fakeSignatureId);
        pendingResponse.setStatus("PENDING");

        RequestStatusResponseDto executedResponse = new RequestStatusResponseDto();
        executedResponse.setId(fakeSignatureId);
        executedResponse.setStatus("EXECUTED");
        executedResponse.setResult(Base64.getEncoder().encodeToString("signature".getBytes()));

        when(tsbService.getRequest(fakeSignatureId))
                .thenReturn(pendingResponse)
                .thenReturn(executedResponse);

        // when
        RequestStatusResponseDto result = hsmFacade.sign(
                label,
                password,
                payload,
                algorithm,
                metadata,
                metadataSignature
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).isNotBlank();

        verify(tsbService, times(2)).getRequest(fakeSignatureId);
        verify(tsbService).sign(label, password, payload, "HEX", "RAW", "NONE_WITH_ECDSA", metadata, metadataSignature);
    }


    @Test
    @DisplayName("2.6.1.3 SKA key – automated approval → should succeed")
    void executeTransactionWithSkaKeyAndAutomatedApproval_shouldSucceed() {

        // given
        String label = "test-key";
        String password = "secret";
        String payload = "deadbeef";
        String algorithm = "ECDSA_SECP256K1";
        String metadata = "meta";
        String metadataSignature = "metaSig";

        String fakeSignatureId = "req-123";

        when(tsbService.sign(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(fakeSignatureId);

        RequestStatusResponseDto pendingResponse = new RequestStatusResponseDto();
        pendingResponse.setId(fakeSignatureId);
        pendingResponse.setStatus("PENDING");

        RequestStatusResponseDto executedResponse = new RequestStatusResponseDto();
        executedResponse.setId(fakeSignatureId);
        executedResponse.setStatus("EXECUTED");
        executedResponse.setResult(Base64.getEncoder().encodeToString("signature".getBytes()));

        when(tsbService.getRequest(fakeSignatureId))
                .thenReturn(pendingResponse)
                .thenReturn(executedResponse);

        // when
        RequestStatusResponseDto result = hsmFacade.sign(
                label,
                password,
                payload,
                algorithm,
                metadata,
                metadataSignature
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).isNotBlank();

        verify(tsbService, times(2)).getRequest(fakeSignatureId);
    }
}
