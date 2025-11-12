// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.util;

import java.util.Arrays;

public class SecurityUtils {

	private SecurityUtils() {}

	public static void clear(char[] secret) {
		if(secret != null) {
			Arrays.fill(secret, '\0');
		}
	}

}
