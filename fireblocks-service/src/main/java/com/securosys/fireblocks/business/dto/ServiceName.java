// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

public enum ServiceName {
    SIGNING_SERVICE,
    CONFIGURATION_MANAGER;

    public static ServiceName fromString(String serviceName) {
        for (ServiceName value : values()) {
            if (value.name().equals(serviceName)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported service name: " + serviceName);
    }
}
