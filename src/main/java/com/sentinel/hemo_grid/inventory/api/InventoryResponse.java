package com.sentinel.hemo_grid.inventory.api;

import java.util.UUID;

import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import io.swagger.v3.oas.annotations.media.Schema;

public record InventoryResponse(
		@Schema(example = "30000000-0000-0000-0000-000000000008")
		UUID id,

		@Schema(example = "O_NEGATIVE")
		BloodGroup bloodGroup,

		@Schema(example = "RED_CELLS")
		BloodComponent component,

		@Schema(example = "5")
		int unitsAvailable,

		@Schema(example = "0")
		int unitsReserved,

		@Schema(example = "5")
		int unitsFree
) {

	public static InventoryResponse from(BloodInventory inventory) {
		return new InventoryResponse(
				inventory.getId(),
				inventory.getBloodGroup(),
				inventory.getComponent(),
				inventory.getUnitsAvailable(),
				inventory.getUnitsReserved(),
				inventory.unitsFree()
		);
	}
}
