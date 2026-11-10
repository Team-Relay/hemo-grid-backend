package com.sentinel.hemo_grid.request.api;

import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.request.domain.RequestUrgency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBloodRequestRequest(
		@NotNull
		@Schema(example = "O_NEGATIVE")
		BloodGroup bloodGroup,

		@NotNull
		@Schema(example = "RED_CELLS")
		BloodComponent component,

		@Min(1)
		@Max(20)
		@Schema(example = "3")
		int unitsRequired,

		@NotNull
		@Schema(example = "CRITICAL")
		RequestUrgency urgency,

		@Size(max = 120)
		@Schema(example = "ER-2026-0820-001")
		String clinicalReference,

		@Size(max = 2000)
		@Schema(example = "Emergency request")
		String notes
) {
}
