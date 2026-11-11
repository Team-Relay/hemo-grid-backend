package com.sentinel.hemo_grid.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.Organization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BloodInventoryColdChainTest {

	@Test
	@DisplayName("Verified cold chain (3.8C) provides full dispatchable free units")
	void verifiedColdChainCalculatesFreeUnits() throws Exception {
		BloodInventory inventory = createInventory(10, 2, 0, 3.8, ColdChainStatus.VERIFIED_2_6C);

		assertThat(inventory.isColdChainVerified()).isTrue();
		assertThat(inventory.unitsFree()).isEqualTo(8);
		assertThat(inventory.dispatchableUnitsFree()).isEqualTo(8);
	}

	@Test
	@DisplayName("Temperature excursion (> 6.0C) quarantines entire inventory from dispatch")
	void tempExcursionAbove6DegreesQuarantinesUnits() throws Exception {
		BloodInventory inventory = createInventory(10, 0, 0, 8.4, ColdChainStatus.EXCURSION_QUARANTINED);

		assertThat(inventory.isColdChainVerified()).isFalse();
		// unitsFree reports raw non-reserved count, but dispatchableUnitsFree is 0 for patient safety
		assertThat(inventory.unitsFree()).isEqualTo(10);
		assertThat(inventory.dispatchableUnitsFree()).isEqualTo(0);
		assertThatThrownBy(() -> inventory.reserve(1)).isInstanceOf(IllegalArgumentException.class);
		assertThat(inventory.getUnitsReserved()).isZero();
	}

	@Test
	@DisplayName("Sub-2.0C temperature excursion (hemolysis risk) invalidates cold chain verification")
	void subTwoDegreesInvalidatesColdChain() throws Exception {
		BloodInventory inventory = createInventory(10, 0, 0, 1.2, ColdChainStatus.VERIFIED_2_6C);

		assertThat(inventory.isColdChainVerified()).isFalse();
		assertThat(inventory.dispatchableUnitsFree()).isEqualTo(0);
	}

	@Test
	@DisplayName("Partial excursion quarantines only compromised units while preserving verified stock")
	void partialExcursionQuarantinesCompromisedUnits() throws Exception {
		BloodInventory inventory = createInventory(10, 1, 3, 4.0, ColdChainStatus.VERIFIED_2_6C);

		assertThat(inventory.isColdChainVerified()).isTrue();
		assertThat(inventory.unitsFree()).isEqualTo(9); // 10 available - 1 reserved
		assertThat(inventory.dispatchableUnitsFree()).isEqualTo(6); // 9 - 3 quarantined
		assertThatThrownBy(() -> inventory.reserve(7)).isInstanceOf(IllegalArgumentException.class);
		inventory.reserve(6);
		assertThat(inventory.dispatchableUnitsFree()).isZero();
	}

	@Test
	@DisplayName("Telemetry update automatically adjusts cold chain status")
	void telemetryUpdateTogglesStatus() throws Exception {
		BloodInventory inventory = createInventory(10, 0, 0, 4.0, ColdChainStatus.VERIFIED_2_6C);

		inventory.updateColdChainTelemetry(7.2, 5);
		assertThat(inventory.getColdChainStatus()).isEqualTo(ColdChainStatus.EXCURSION_QUARANTINED);
		assertThat(inventory.isColdChainVerified()).isFalse();
		assertThat(inventory.dispatchableUnitsFree()).isEqualTo(0);

		inventory.updateColdChainTelemetry(4.1, 0);
		assertThat(inventory.getColdChainStatus()).isEqualTo(ColdChainStatus.VERIFIED_2_6C);
		assertThat(inventory.isColdChainVerified()).isTrue();
		assertThat(inventory.dispatchableUnitsFree()).isEqualTo(10);
	}

	private BloodInventory createInventory(
			int available,
			int reserved,
			int quarantined,
			double tempCelsius,
			ColdChainStatus status
	) throws Exception {
		BloodInventory inventory = new BloodInventory();

		setField(inventory, "id", UUID.randomUUID());
		setField(inventory, "bloodGroup", BloodGroup.O_NEGATIVE);
		setField(inventory, "component", BloodComponent.RED_CELLS);
		setField(inventory, "unitsAvailable", available);
		setField(inventory, "unitsReserved", reserved);
		setField(inventory, "quarantinedTempExcursionUnits", quarantined);
		setField(inventory, "fridgeTempCelsius", tempCelsius);
		setField(inventory, "coldChainStatus", status);

		return inventory;
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
