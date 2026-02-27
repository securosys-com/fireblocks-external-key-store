// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.exceptions;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public enum BusinessReason implements Reason {

	// General Exception when you can not find a better one -> blame the code
	ERROR_IMPLEMENTATION("res.error.impl", 100),
	ERROR_NOT_IMPLEMENTED("res.error.not.implemented", 101),
	ERROR_OPERATION_UNREACHABLE("res.error.operation.unreachable", 102),
	ERROR_GENERAL("res.error.general", 103),

	// Problem during initialization of something
	ERROR_INIT("res.error.init", 200),

	// problems with the configuration
	ERROR_CONFIG_NOT_EXISTENT("res.error.config.not.existent", 300),
	ERROR_CONFIG_NOT_VALID("res.error.config.not.valid", 301),
	ERROR_INVALID_CONFIG_INPUT("res.error.invalid.config.input", 302),
    ERROR_INVALID_ALGORITHM("res.error.invalid.algorithm", 303),

	// security problems detected
	ERROR_SECURITY_NOT_ALLOWED("res.error.security.not.allowed", 400),
	ERROR_INVALID_CRYPTO_FILE("res.error.invalid.crypto.file", 401),
	ERROR_INVALID_CERTIFICATE("res.error.invalid.certificate", 402),
	ERROR_INVALID_SIGNATURE("res.error.input.invalid.signature", 403),
	ERROR_TIMESTAMP_DIFFERENCE("res.error.input.timestamp.off", 404),
	ERROR_TIMESTAMP_SIGNER_NOT_ALLOWED("res.error.timestamp.signer.not.allowed", 405),
	ERROR_MISSING_ACCESS_TOKEN("res.error.missing.access.token", 406),
	ERROR_PARSING_KEY("res.error.parsing.key", 407),
	ERROR_READING_PEM("res.error.reading.pem", 408),
	ERROR_PARSING_CERTIFICATE("res.error.parsing.certificate", 409),
	ERROR_DECRYPT_FAILED("res.error.decrypt.failed", 410),
	ERROR_INVALID_CERTIFICATE_REQUEST("res.error.invalid.certificate.request", 411),
	ERROR_INVALID_ONE_TIME_PASSWORD("res.error.invalid.onetimepassword", 412),
	ERROR_APPROVER_DOES_NOT_EXIST("res.error.approver.not.existent", 413),
	ERROR_APPROVER_NOT_ONBOARDED("res.error.approver.not.onboarded", 414),
	ERROR_REQUEST_SIGNER_NOT_ALLOWED("res.error.request.signer.not.allowed", 415),
	ERROR_MISSING_API_KEY("res.error.missing.api.key", 416),


	// special cases for missing permissions
	ERROR_CLIENT_SUBSCRIPTION("res.error.client.subscription", 450),
	ERROR_CLIENT_ENDPOINT_NOT_LICENSED("res.error.client.endpoint.not.licensed", 451),


	// use this reason if you get an unexpected data constellation. Like loading something from the DB and
	// then some fields are empty. Or you get 2 results instead of 3.
	// these are what you would use assertions in a unit test
	ERROR_DATA_INVALID_CONSTELLATION("res.error.data.invalid.constellation", 500),
	ERROR_INVALID_VALUE_FOR_ENUM("res.error.invalid.value.for.enum", 501),

	// use this reason if you got into troubles because of bad input from anywhere.
	// If you need you can add new reasons and codes for specific problems. Like "lastname can not be null")
	ERROR_INPUT_VALIDATION_FAILED("res.error.input.validation.failed", 600),
	ERROR_UNSUPPORTED_APPROVER_KEY_TYPE("res.error.unsupported.approver.key.type", 601),
	ERROR_UNSUPPORTED_COMBINATION_OF_APPROVAL_TYPES("res.error.unsupported.combination.of.approval.types", 602),
	ERROR_UNSUPPORTED_KEYSIZE("res.error.input.keysize", 603),
	ERROR_NO_NAME_FOR_CERTIFICATE_APPROVAL("res.error.input.no.name.for.certificate.approver", 604),
	ERROR_UNSUPPORTED_CRYPTOCURRENCY("res.error.unsupported.cryptocurrency", 605),
	ERROR_KEY_ATTRIBUTES_INVALID("res.error.key.attributes.invalid", 606),
	ERROR_ALGORITHM_NOT_SUPPORTED_BY_KEY("res.error.algorithm.not.supported.by.key", 607),
	ERROR_KEY_ALREADY_EXISTING("res.error.key.already.existing", 608),
	ERROR_INVALID_UUID("res.error.invalid.uuid", 609),
	ERROR_INVALID_KEY_NAME("res.error.invalid.key.name", 610),
	ERROR_UNSUPPORTED_OPERATION("res.error.unsupported.operation", 611),
	ERROR_INVALID_PAYLOAD("res.error.invalid.payload", 612),
	ERROR_INVALID_KEY_TYPE("res.error.invalid.key.type", 613),
	ERROR_OPERATION_FORBIDDEN_FOR_KEY("res.error.operation.forbidden.for.key", 614),
	ERROR_DATA_OBJECT_ALREADY_EXISTING("res.error.data.object.already.existing", 615),
	ERROR_DATA_OBJECT_INVALID_VALUE("res.error.data.object.invalid.value", 616),
	ERROR_INVALID_ACCESS_TOKEN("res.error.invalid.access.token", 617),
	ERROR_INSUFFICIENT_LOGIN_INFORMATION("res.error.insufficient.login.information", 618),
	ERROR_INVALID_NUMBER_RANGE("res.error.invalid.number.range", 619),
	ERROR_KEY_PASSWORD_MISMATCH("res.error.key.password.mismatch", 620),
	ERROR_INVALID_JSON("res.error.invalid.json", 621),
	ERROR_RESOURCE_NOT_FOUND("res.error.resource.not.found", 622),
	ERROR_INVALID_FILE_HEADER("res.error.invalid.file.header", 623),
	ERROR_INVALID_TAGLENGTH("res.error.key.invalid.tagLength", 624),
	ERROR_INVALID_KEY_FORMAT("res.error.key.invalid.encoding", 625),
	ERROR_INVALID_CERTIFICATE_FORMAT("res.error.certificate.invalid.encoding", 626),
	ERROR_INVALID_KEY_ID("res.error.invalid.key.id", 627),
	ERROR_INVALID_CSR_FORMAT("res.error.csr.invalid.format", 628),
	ERROR_UNSUPPORTED_CIPHER("res.error.unsupported.cipher", 629),
	ERROR_INVALID_ADDRESS_ON_NON_SKA_KEY("res.error.invalid.address.on.non.ska.key", 630),
	ERROR_INVALID_API_KEY("res.error.invalid.api.key", 631),
	ERROR_APPROVER_ALREADY_EXISTING("res.error.approver.already.existing", 632),
	ERROR_NO_VALUE_FOR_APPROVER_MGMT("res.error.input.no.value.for.approver.mgmt", 633),
	ERROR_DERIVATION_NOT_SUPPORTED_BY_ALGORITHM("res.error.derivation.not.supported.by.algorithm", 634),
	ERROR_ENCODING_EXCEPTION("res.error.encoding.exception", 635),

	// These reasons are special cases where the bad input is an identifier for a resource which can not be found by the system.
	ERROR_KEY_NOT_EXISTENT("res.error.key.not.existent", 650),
	ERROR_REQUEST_NOT_EXISTENT("res.error.request.not.existent", 651),
	ERROR_TASK_NOT_EXISTENT("res.error.task.not.existent", 652),
	ERROR_DATA_OBJECT_NOT_EXISTENT("res.error.data.object.not.existent", 653),


	// this is the reason if a subsystem failed. For example one of the webservices returned an error
	ERROR_IN_SUBSYSTEM("res.error.in.subystem", 700),
	ERROR_IN_HSM("res.error.in.hsm", 701),
	ERROR_CREATION_INTEGRITY_KEY( "res.error.creation.integrity.key", 702),
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

