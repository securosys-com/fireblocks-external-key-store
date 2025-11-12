// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.controller;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseController {

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseController.class);

	protected static final String SUCCESSFUL_OPERATION = "Successful Operation";

	protected static void logRequestBody(Object request) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("got body: {}", request.toString().replaceAll("[\r\n]", ""));
		}
	}

	protected static String getBodyString(HttpServletRequest servletRequest) {
		String body;

		try (
				InputStream input = servletRequest.getInputStream();
				ByteArrayOutputStream result = new ByteArrayOutputStream()
		) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = input.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
			body = result.toString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new BusinessException("Could not read request", BusinessReason.ERROR_INVALID_JSON, e);
		}
		return body;
	}

}
