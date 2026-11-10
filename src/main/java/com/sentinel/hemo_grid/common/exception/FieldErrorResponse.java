package com.sentinel.hemo_grid.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/** One validation problem tied to a request-body field. */
public record FieldErrorResponse(
		@Schema(example = "unitsRequired") String field,
		@Schema(example = "must be greater than or equal to 1") String message
) {
}
