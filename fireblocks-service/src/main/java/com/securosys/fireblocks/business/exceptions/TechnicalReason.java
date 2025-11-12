// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.exceptions;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public enum TechnicalReason implements Reason {

	// Exception for "something did not work". Like DB not available or out of memory.
	ERROR_TECHNICAL("res.error.technical", 1),
	ERROR_CONNECTIVITY("res.error.connectivity", 2);

	private final String reason;

	private final int errorCode;

	TechnicalReason(String reason, int errorCode) {
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
