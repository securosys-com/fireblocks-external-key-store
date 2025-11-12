// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.configuration;

import lombok.Data;

import java.util.List;

/**
 * Represents collections of different types of API keys used for various operations. This class stores
 * lists of tokens that correspond to different functional areas, such as key management, key operations,
 * approval processes, and service-related tasks.
 */
@Data
public class ApiKeyTypes {

    private boolean enabled;
    /**
     * A list of API keys or tokens used for key management operations. These tokens are used to authenticate
     * and authorize operations related to the management of cryptographic keys.
     */
    private List<String> keyManagementToken;

    /**
     * A list of API keys or tokens used for key operation tasks. This includes operations such as encryption,
     * decryption, signing, and other cryptographic processes.
     */
    private List<String> keyOperationToken;

    private List<String> serviceToken;
    
}
