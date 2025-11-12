// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum EnabledDisabledType {

	@JsonProperty("enabled")
	ENABLED,
	@JsonProperty("disabled")
	DISABLED

}
