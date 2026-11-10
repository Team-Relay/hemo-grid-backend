/* UpdateInventoryRequest is a validated inbound API contract for the inventory module. */

package com.sentinel.hemo_grid.inventory.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record UpdateInventoryRequest(
		@Min(0)
		@Schema(example = "5")
		int unitsAvailable
) {
}
