/* GlobalExceptionHandler owns the exception boundary of the common module. */

package com.sentinel.hemo_grid.common.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception, HttpServletRequest request) {
		return ResponseEntity
				.status(exception.status())
				.body(ApiErrorResponse.of(
						exception.status(),
						exception.code(),
						exception.getMessage(),
						request.getRequestURI()
				));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity
				.badRequest()
				.body(ApiErrorResponse.validation(
						"Request validation failed.",
						request.getRequestURI(),
						fieldErrors
				));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(
			HandlerMethodValidationException exception,
			HttpServletRequest request
	) {
		return ResponseEntity
				.badRequest()
				.body(ApiErrorResponse.validation(
						"Request validation failed.",
						request.getRequestURI(),
						List.of()
				));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		log.error("Unexpected application error", exception);
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		return ResponseEntity
				.status(status)
				.body(ApiErrorResponse.of(
						status,
						ErrorCode.INTERNAL_ERROR,
						"An unexpected error occurred.",
						request.getRequestURI()
				));
	}
}
