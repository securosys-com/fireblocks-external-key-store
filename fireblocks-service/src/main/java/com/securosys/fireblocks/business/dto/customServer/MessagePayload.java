// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.customServer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.securosys.fireblocks.datamodel.entities.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagePayload {

    private String tenantId;

    private RequestType type;

    private Algorithm algorithm;

    private String userAccessToken;

    private String signingDeviceKeyId;

    private String keyId;

    private List<MessageToSign> messagesToSign;

    private String txId;
    private Map<String, Object> metadata;
}
