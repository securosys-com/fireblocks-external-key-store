// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.stereotype.Component;


import java.util.Set;

@Component
public class JsonUtil {

	private final ObjectMapper objectMapper;

	public JsonUtil(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public <T> T getObjectFromJsonString(String json, Class<T> returnType) {
		try {
			T object = objectMapper.readValue(json, returnType);
			validate(object);
			return object;
		}
		catch (UnrecognizedPropertyException e) {
			String msg = String.format("Cannot parse json string. The field '%s' is unknown", e.getPropertyName());
			throw new BusinessException(msg, BusinessReason.ERROR_INPUT_VALIDATION_FAILED, e);
		}
		catch (JsonProcessingException e) {
			if(e.getCause() instanceof BusinessException) {
				throw (BusinessException) e.getCause();
			}
			throw new BusinessException("Cannot parse json string", BusinessReason.ERROR_INPUT_VALIDATION_FAILED, e);
		}
	}

	private static <T> void validate(T request) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<T>> constraintViolations = validator.validate(request);
		if(!constraintViolations.isEmpty()) {
			throw ValidationErrorUtil.createException(constraintViolations);
		}
	}
}
