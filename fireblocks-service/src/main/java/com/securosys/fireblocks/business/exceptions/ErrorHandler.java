// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.exceptions;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import com.securosys.fireblocks.business.dto.ReasonBasedExceptionDto;
import com.securosys.fireblocks.business.util.ValidationErrorUtil;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@ApiResponses( value = {
		@ApiResponse( responseCode = "400", description = "Client Error",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
						schema = @Schema(implementation = ReasonBasedExceptionDto.class))),
		@ApiResponse( responseCode = "403", description = "Request refused",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
						schema = @Schema(implementation = ReasonBasedExceptionDto.class))),
		@ApiResponse( responseCode = "404", description = "Resource not found",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
						schema = @Schema(implementation = ReasonBasedExceptionDto.class))),
		@ApiResponse( responseCode = "500", description = "Server Error",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
						schema = @Schema(implementation = ReasonBasedExceptionDto.class))),
		@ApiResponse( responseCode = "501", description = "Implementation Error",
				content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
						schema = @Schema(implementation = ReasonBasedExceptionDto.class)))})
public class ErrorHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);

	@ExceptionHandler(value = { TechnicalException.class})
	public ResponseEntity<ReasonBasedExceptionDto> handleTechnicalException(TechnicalException e) {
		ReasonBasedExceptionDto dto = new ReasonBasedExceptionDto();
		dto.setErrorCode(e.getErrorCode());
		dto.setReason(e.getReason().getReason());
		dto.setMessage(e.getMessage());
		LOGGER.warn("TechnicalException: {}", e.getMessage());
		if (LOGGER.isDebugEnabled()){
			LOGGER.debug("Stack trace: ", e);
		}
		return new ResponseEntity<>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(value = { BusinessException.class})
	public ResponseEntity<ReasonBasedExceptionDto> handleBusinessException(BusinessException e) {
		ReasonBasedExceptionDto dto = new ReasonBasedExceptionDto();
		dto.setErrorCode(e.getErrorCode());
		dto.setReason(e.getReason().getReason());
		dto.setMessage(e.getMessage());
		LOGGER.warn("BusinessException: {}", e.getMessage());
		if (LOGGER.isDebugEnabled()){
			LOGGER.debug("Stack trace: ", e);
		}
		return generateResponseForBusinessException(dto);
	}

	private static ResponseEntity<ReasonBasedExceptionDto> generateResponseForBusinessException(ReasonBasedExceptionDto dto) {
		String errorCode = dto.getErrorCode().toString();
		if(errorCode.startsWith("45")) {
			return new ResponseEntity<>(dto, HttpStatus.FORBIDDEN);
		}
		if(errorCode.startsWith("65")) {
			return new ResponseEntity<>(dto, HttpStatus.NOT_FOUND);
		}
		if(errorCode.startsWith("4") || errorCode.startsWith("6")) {
			return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
		}
		if(errorCode.startsWith("2") || errorCode.startsWith("3") || errorCode.startsWith("7") || errorCode.startsWith("5")) {
			return new ResponseEntity<>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if(errorCode.startsWith("1")) {
			return new ResponseEntity<>(dto, HttpStatus.NOT_IMPLEMENTED);
		}
		return new ResponseEntity<>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(value = { MethodArgumentNotValidException.class})
	public ResponseEntity<ReasonBasedExceptionDto> dtoValidationFailed(MethodArgumentNotValidException e) {
		BusinessException businessException = ValidationErrorUtil.createException(e.getBindingResult());
		return handleBusinessException(businessException);
	}

	@ExceptionHandler(value = { HttpMessageNotReadableException.class})
	public ResponseEntity<ReasonBasedExceptionDto> dtoEnumValidationFailed(HttpMessageNotReadableException e) {
		ReasonBasedExceptionDto dto = new ReasonBasedExceptionDto();
		Reason reason = BusinessReason.ERROR_INPUT_VALIDATION_FAILED;
		dto.setErrorCode(reason.getErrorCode());
		dto.setReason(reason.getReason());

		if (e.getCause() instanceof InvalidFormatException formatException){

            dto.setMessage(String.format("Value '%s' is not valid for field %s", formatException.getValue(), formatException.getPath().get(0).getFieldName()));
		} else if(e.getCause() instanceof JsonMappingException || e.getCause() instanceof JsonEOFException){
			String msg = String.format("Received invalid request. Underlying exception is: %s", e.getMessage());
			LOGGER.error(msg, e);
			dto.setMessage("Invalid request. Cannot parse json string");
		}
		else {
			String msg = String.format("Received invalid request. Underlying exception is: %s", e.getMessage());
			LOGGER.error(msg, e);
			dto.setMessage("Invalid request. See log for details.");
		}

		return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(value = { HttpMessageConversionException.class })
	public ResponseEntity<ReasonBasedExceptionDto> dtoJacksonObjectConversionValidationFailed(HttpMessageConversionException e) {
		if (e.getCause().getCause() instanceof BusinessException businessException){
            return handleBusinessException(businessException);
		}
		else {
			String msg = String.format("Received invalid request. Underlying exception is: %s", e.getMessage());
			LOGGER.error(msg, e);
			ReasonBasedExceptionDto dto = new ReasonBasedExceptionDto();
			Reason reason = BusinessReason.ERROR_INPUT_VALIDATION_FAILED;
			dto.setErrorCode(reason.getErrorCode());
			dto.setReason(reason.getReason());
			dto.setMessage("Invalid request. See log for details.");
			return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
		}
	}

	@ExceptionHandler(value = { MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ReasonBasedExceptionDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		ReasonBasedExceptionDto dto = new ReasonBasedExceptionDto();
		Reason reason = BusinessReason.ERROR_INPUT_VALIDATION_FAILED;
		dto.setErrorCode(reason.getErrorCode());
		dto.setReason(reason.getReason());
		String msg = "A provided argument is of the wrong type.";
		dto.setMessage(msg);
		LOGGER.error(msg, e);
		return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
	}

}
