// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.service;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AuthState {

    private int keyManagementTokenIndex = 0;
    private int keyOperationTokenIndex = 0;
    private int keyServiceTokenIndex = 0;

    public void incrementKeyManagementIndex() {
        keyManagementTokenIndex++;
    }

    public void incrementKeyOperationIndex() {
        keyOperationTokenIndex++;
    }

    public void incrementKeyServiceIndex() {
        keyServiceTokenIndex++;
    }
}