// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.util;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.Objects;
import java.util.Set;

public class ValidationErrorUtil {

	public static BusinessException createException(BindingResult bindingResult) {
		// We only consider the first failure (even if there are multiple)
		FieldError fieldError = bindingResult.getFieldError();
		String message;
		if(fieldError == null) {
			//if there is no field error it is most likely an object error
			ObjectError objectError = bindingResult.getGlobalError();
			message = Objects.requireNonNull(objectError).getDefaultMessage();
		}
		else {
			message = String.format("'%s' %s", fieldError.getField(), fieldError.getDefaultMessage());
		}

		return new BusinessException(message, BusinessReason.ERROR_INPUT_VALIDATION_FAILED);
	}

	public static <T> BusinessException createException(Set<ConstraintViolation<T>> constraintViolations) {
		if(constraintViolations.iterator().hasNext()) {
			// We only consider the first failure (even if there are multiple)
			ConstraintViolation<T> constraintViolation = constraintViolations.iterator().next();
			String lastNodeName = findNameOfLastNode(constraintViolation.getPropertyPath());
			String message;
			if("".equals(lastNodeName)) {
				message = constraintViolation.getMessage();
			}
			else {
				message = String.format("'%s' %s", lastNodeName, constraintViolation.getMessage());
			}
			return new BusinessException(message, BusinessReason.ERROR_INPUT_VALIDATION_FAILED);
		}
		else {
			throw new IllegalStateException("Can not create exception if there are no constraint violations");
		}
	}

	/**
	 * Returns the name of the last node or an empty string if the path is empty.
	 */
	private static String findNameOfLastNode(Path path) {
		String lastNodeName = "";
		for(Path.Node node : path) {
			lastNodeName = node.toString();
		}
		return lastNodeName;
	}
}
