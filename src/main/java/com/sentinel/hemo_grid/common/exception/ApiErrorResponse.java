/* Stable error envelope returned consistently by controllers and security handlers. */

package com.sentinel.hemo_grid.common.exception;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/** Stable error envelope consumed by the Next.js API error adapter. */
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		ErrorCode code,
		String message,
		String path,
		List<FieldErrorResponse> fieldErrors
) {

	public ApiErrorResponse {
		fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
	}

	public static ApiErrorResponse of(HttpStatus status, ErrorCode code, String message, String path) {
		return new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.name(),
				code,
				message,
				path,
				List.of()
		);
	}

	public static ApiErrorResponse validation(
			String message,
			String path,
			List<FieldErrorResponse> fieldErrors
	) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		return new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.name(),
				ErrorCode.VALIDATION_FAILED,
				message,
				path,
				fieldErrors
		);
	}
}
