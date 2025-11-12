// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.util;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;

import java.util.UUID;

public final class InputConverter {

    public static UUID uuidFromString(String input) {
        try {
            return UUID.fromString(input);
        }
        catch (IllegalArgumentException e) {
            String msg = String.format("Could not convert '%s' to an UUID", input);
            throw new BusinessException(msg, BusinessReason.ERROR_INVALID_UUID, e);
        }
    }

}
