// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix="custom-server")
public class CustomServerProperties {

    private String serviceName;
    private String fireblocksSignatureCertificate;
    private String fireblocksSignaturePublicKey;

    /**
     * Whether to verify Fireblocks signatures. Defaults to true.
     * Can be disabled in tests or local dev environments.
     */
    private boolean verifySignatures = true;

    @NotNull
    private FireblocksAgentConfiguration fireblocksAgentConfiguration = new FireblocksAgentConfiguration();

    @Data
    public static class FireblocksAgentConfiguration {
        @NotNull
        private String apiAuthorization;
    }

}
