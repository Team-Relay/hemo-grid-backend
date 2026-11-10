package com.sentinel.hemo_grid.request.api;

import com.sentinel.hemo_grid.request.domain.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateRequestStatusRequest(
		@NotNull
		@Schema(example = "PREPARING", allowableValues = {"PREPARING", "IN_TRANSIT", "DELIVERED"})
		RequestStatus status
) {
}
