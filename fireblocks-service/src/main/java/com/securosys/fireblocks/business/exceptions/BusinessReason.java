// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.exceptions;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public enum BusinessReason implements Reason {

	// General Exception when you can not find a better one -> blame the code
	ERROR_IMPLEMENTATION("res.error.impl", 100),
	ERROR_OPERATION_UNREACHABLE("res.error.operation.unreachable", 102),
	ERROR_GENERAL("res.error.general", 103),


	// problems with the configuration
	ERROR_CONFIG_NOT_VALID("res.error.config.not.valid", 301),
	ERROR_INVALID_CONFIG_INPUT("res.error.invalid.config.input", 302),
    ERROR_INVALID_ALGORITHM("res.error.invalid.algorithm", 303),


	// special cases for missing permissions
	ERROR_INVALID_CRYPTO_FILE("res.error.invalid.crypto.file", 401),
	ERROR_INVALID_SIGNATURE	("res.error.input.invalid.signature", 403),
	ERROR_TIMESTAMP_DIFFERENCE("res.error.input.timestamp.off", 404),
	ERROR_PARSING_KEY("res.error.parsing.key", 407),
	ERROR_READING_PEM("res.error.reading.pem", 408),
	ERROR_PARSING_CERTIFICATE("res.error.parsing.certificate", 409),
	ERROR_MISSING_ACCESS_TOKEN("res.error.missing.access.token", 406),
	ERROR_DECRYPT_FAILED("res.error.decrypt.failed", 410),

	ERROR_CLIENT_SUBSCRIPTION("res.error.client.subscription", 450),

	ERROR_INVALID_VALUE_FOR_ENUM("res.error.invalid.value.for.enum", 501),

	// use this reason if you got into troubles because of bad input from anywhere.
	// If you need you can add new reasons and codes for specific problems. Like "lastname can not be null")
	ERROR_INPUT_VALIDATION_FAILED("res.error.input.validation.failed", 600),
    ERROR_INVALID_KEY_NAME("res.error.invalid.key.name", 610),
	ERROR_INVALID_UUID("res.error.invalid.uuid", 609),
    ERROR_DATA_OBJECT_ALREADY_EXISTING("res.error.data.object.already.existing", 615),

	ERROR_OPERATION_FORBIDDEN("res.error.operation.forbidden", 614),
	ERROR_INVALID_ACCESS_TOKEN("res.error.invalid.access.token", 617),
	ERROR_INVALID_JSON("res.error.invalid.json", 621),
	ERROR_PARTITION_NOT_FOUND("res.error.partition.not.found", 622),

	// this is the reason if a subsystem failed. For example one of the webservices returned an error
	ERROR_IN_SUBSYSTEM("res.error.in.subystem", 700),
	ERROR_IN_HSM("res.error.in.hsm", 701),
	ERROR_IO("res.error.io", 703),
	ERROR_FILE_NOT_FOUND("res.error.file.not.found", 704);

	private final String reason;

	private final int errorCode;

	BusinessReason(String reason, int errorCode) {
		this.reason = reason;
		this.errorCode = errorCode;
	}

	@Override
	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
		tsb.append("errorCode", errorCode);
		tsb.append("reason", reason);
		return tsb.toString();
	}

	@Override
	public int getErrorCode() {
		return errorCode;
	}

}

