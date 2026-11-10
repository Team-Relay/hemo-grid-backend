package com.sentinel.hemo_grid.common.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message
) {

	public static ApiErrorResponse of(HttpStatus status, String message) {
		return new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.name(),
				message
		);
	}

	public static ApiErrorResponse validation(String message) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.name(),
				message
		);
	}
}
