package com.sentinel.hemo_grid.planning.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDispatchPlanRequest(
		@NotNull @Size(min = 2, max = 5) List<@NotNull UUID> requestIds,
		@AssertTrue(message = "Staff approval of the included requests is required.") boolean staffApproved
) {}
