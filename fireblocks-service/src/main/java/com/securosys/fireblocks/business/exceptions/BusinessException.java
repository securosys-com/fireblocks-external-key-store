// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.exceptions;

import lombok.Getter;

import java.io.Serial;

@Getter
public class BusinessException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final BusinessReason reason;
	private final Throwable cause;

	public BusinessException(String message, BusinessReason reason, Throwable cause) {
		super(message);
		this.reason = reason;
		this.cause = cause;
	}

	public BusinessException(String message, BusinessReason reason) {
		super(message);
		this.reason = reason;
		this.cause = null;
	}

	public int getErrorCode() {
		assert reason != null;
		return reason.getErrorCode();
	}

}


