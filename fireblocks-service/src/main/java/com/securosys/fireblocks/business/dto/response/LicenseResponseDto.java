// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto.response;


import lombok.Data;

import java.util.Set;

@Data
public class LicenseResponseDto {

    private Set<String> clientFlags;
}
