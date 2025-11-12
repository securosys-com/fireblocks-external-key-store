// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Response for a specific error when an operation can not be completed successfully.")
@Getter
@Setter
public class ReasonBasedExceptionDto {

	@Schema(description = "The code of the error.")
	private Integer errorCode;

	@Schema(description = "The reason for the error.")
	private String reason;

	@Schema(description = "The message containing why the error was returned by the application.")
	private String message;

}
