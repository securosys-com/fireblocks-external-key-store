// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix="tsb")
public class TsbProperties {

    private String tsbRestApi;
    private String tsbAccessToken;
    private String tsbMTlsCertificate;
    private String tsbMTlsKey;
    private ApiKeyTypes apiAuthentication;
    private boolean airGapped = false;

}


