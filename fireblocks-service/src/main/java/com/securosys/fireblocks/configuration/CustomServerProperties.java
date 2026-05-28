// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix="custom-server")
public class CustomServerProperties {

    /**
     * Legacy single-service verification config. Prefer signatureServices for new deployments.
     */
    private String serviceName;
    private String fireblocksSignatureCertificate;
    private String fireblocksSignaturePublicKey;
    private List<String> fireblocksSignatureCertificates = new ArrayList<>();
    private List<String> fireblocksSignaturePublicKeys = new ArrayList<>();

    /**
     * Service-specific Fireblocks payload signature verification config.
     * Each entry can be named freely; serviceName is matched against payloadSignatureData.service.
     */
    private Map<String, FireblocksSignatureService> signatureServices = new LinkedHashMap<>();

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

    @Data
    public static class FireblocksSignatureService {
        private boolean enabled = true;
        private String serviceName;
        private List<String> fireblocksSignatureCertificates = new ArrayList<>();
        private List<String> fireblocksSignaturePublicKeys = new ArrayList<>();
    }

}
