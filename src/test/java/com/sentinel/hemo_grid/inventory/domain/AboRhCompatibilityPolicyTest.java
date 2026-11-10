package com.sentinel.hemo_grid.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import com.sentinel.hemo_grid.request.domain.MatchCompatibilityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AboRhCompatibilityPolicyTest {

	@Test
	@DisplayName("O-Negative recipient can only receive O-Negative red cells")
	void oNegativeReceivesOnlyONegativeRedCells() {
		List<AboRhCompatibilityPolicy.CompatibilityOption> compatible =
				AboRhCompatibilityPolicy.getCompatibleDonors(BloodGroup.O_NEGATIVE, BloodComponent.RED_CELLS, false);

		assertThat(compatible).hasSize(1);
		assertThat(compatible.get(0).bloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
		assertThat(compatible.get(0).matchType()).isEqualTo(MatchCompatibilityType.EXACT);
		assertThat(compatible.get(0).preferenceRank()).isEqualTo(1);
		assertThat(compatible.get(0).conservationRuleApplied()).isFalse();
	}

	@Test
	@DisplayName("O-Positive recipient accepts O+ exact and O- emergency substitute with conservation flag")
	void oPositiveAcceptsOPlusAndONegative() {
		List<AboRhCompatibilityPolicy.CompatibilityOption> compatible =
				AboRhCompatibilityPolicy.getCompatibleDonors(BloodGroup.O_POSITIVE, BloodComponent.RED_CELLS, false);

		assertThat(compatible).hasSize(2);
		assertThat(compatible.get(0).bloodGroup()).isEqualTo(BloodGroup.O_POSITIVE);
		assertThat(compatible.get(0).matchType()).isEqualTo(MatchCompatibilityType.EXACT);
		assertThat(compatible.get(0).preferenceRank()).isEqualTo(1);

		assertThat(compatible.get(1).bloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
		assertThat(compatible.get(1).matchType()).isEqualTo(MatchCompatibilityType.COMPATIBLE_SUBSTITUTE);
		assertThat(compatible.get(1).preferenceRank()).isEqualTo(3);
		assertThat(compatible.get(1).conservationRuleApplied()).isTrue();
	}

	@Test
	@DisplayName("A-Positive recipient prioritizes A+ exact, then A- and O+, holding O- as last resort")
	void aPositiveRedCellsTransfusionOrder() {
		List<AboRhCompatibilityPolicy.CompatibilityOption> compatible =
				AboRhCompatibilityPolicy.getCompatibleDonors(BloodGroup.A_POSITIVE, BloodComponent.RED_CELLS, false);

		assertThat(compatible).hasSize(4);
		assertThat(compatible.get(0).bloodGroup()).isEqualTo(BloodGroup.A_POSITIVE);
		assertThat(compatible.get(0).matchType()).isEqualTo(MatchCompatibilityType.EXACT);

		// Non-O- substitutes have rank 2
		assertThat(compatible.get(1).bloodGroup()).isEqualTo(BloodGroup.A_NEGATIVE);
		assertThat(compatible.get(1).preferenceRank()).isEqualTo(2);
		assertThat(compatible.get(1).conservationRuleApplied()).isFalse();

		assertThat(compatible.get(2).bloodGroup()).isEqualTo(BloodGroup.O_POSITIVE);
		assertThat(compatible.get(2).preferenceRank()).isEqualTo(2);
		assertThat(compatible.get(2).conservationRuleApplied()).isFalse();

		// O- is last resort with conservation rule applied
		assertThat(compatible.get(3).bloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
		assertThat(compatible.get(3).preferenceRank()).isEqualTo(3);
		assertThat(compatible.get(3).conservationRuleApplied()).isTrue();
	}

	@Test
	@DisplayName("AB-Positive is universal RBC recipient and ranks all groups with O- conserved at lowest priority")
	void abPositiveIsUniversalRbcRecipientWithONegativeConserved() {
		List<AboRhCompatibilityPolicy.CompatibilityOption> compatible =
				AboRhCompatibilityPolicy.getCompatibleDonors(BloodGroup.AB_POSITIVE, BloodComponent.RED_CELLS, false);

		assertThat(compatible).hasSize(8);
		assertThat(compatible.get(0).bloodGroup()).isEqualTo(BloodGroup.AB_POSITIVE);
		assertThat(compatible.get(0).matchType()).isEqualTo(MatchCompatibilityType.EXACT);

		AboRhCompatibilityPolicy.CompatibilityOption lastOption = compatible.get(7);
		assertThat(lastOption.bloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
		assertThat(lastOption.preferenceRank()).isEqualTo(3);
		assertThat(lastOption.conservationRuleApplied()).isTrue();
	}

	@Test
	@DisplayName("Plasma transfusion inverts universal rules: AB is universal donor, O is universal recipient")
	void plasmaTransfusionInvertsUniversalRules() {
		// O recipient can receive plasma from O, A, B, AB
		List<AboRhCompatibilityPolicy.CompatibilityOption> oRecipientPlasma =
				AboRhCompatibilityPolicy.getCompatibleDonors(BloodGroup.O_POSITIVE, BloodComponent.PLASMA, false);
		assertThat(oRecipientPlasma).extracting(AboRhCompatibilityPolicy.CompatibilityOption::bloodGroup)
				.contains(BloodGroup.O_POSITIVE, BloodGroup.A_POSITIVE, BloodGroup.B_POSITIVE, BloodGroup.AB_POSITIVE);

		// AB recipient can only receive AB plasma
		List<AboRhCompatibilityPolicy.CompatibilityOption> abRecipientPlasma =
				AboRhCompatibilityPolicy.getCompatibleDonors(BloodGroup.AB_POSITIVE, BloodComponent.PLASMA, false);
		assertThat(abRecipientPlasma).extracting(AboRhCompatibilityPolicy.CompatibilityOption::bloodGroup)
				.containsOnly(BloodGroup.AB_POSITIVE, BloodGroup.AB_NEGATIVE);
	}

	@Test
	@DisplayName("Emergency Trauma Protocol with unknown patient group strictly assigns O-Negative for cellular components")
	void emergencyTraumaUnknownPatientBloodGroup() {
		List<AboRhCompatibilityPolicy.CompatibilityOption> redCells =
				AboRhCompatibilityPolicy.getCompatibleDonors(null, BloodComponent.RED_CELLS, true);

		assertThat(redCells).hasSize(1);
		assertThat(redCells.get(0).bloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
		assertThat(redCells.get(0).matchType()).isEqualTo(MatchCompatibilityType.EXACT);

		List<AboRhCompatibilityPolicy.CompatibilityOption> plasma =
				AboRhCompatibilityPolicy.getCompatibleDonors(null, BloodComponent.PLASMA, true);

		assertThat(plasma).hasSize(2);
		assertThat(plasma.get(0).bloodGroup()).isEqualTo(BloodGroup.AB_POSITIVE);
		assertThat(plasma.get(1).bloodGroup()).isEqualTo(BloodGroup.AB_NEGATIVE);
	}

	@ParameterizedTest
	@EnumSource(BloodGroup.class)
	@DisplayName("Every blood group matches itself as an EXACT match across cellular components")
	void everyBloodGroupMatchesItselfExactly(BloodGroup group) {
		Optional<AboRhCompatibilityPolicy.CompatibilityOption> option =
				AboRhCompatibilityPolicy.evaluateCompatibility(group, group, BloodComponent.RED_CELLS, false);

		assertThat(option).isPresent();
		assertThat(option.get().matchType()).isEqualTo(MatchCompatibilityType.EXACT);
		assertThat(option.get().preferenceRank()).isEqualTo(1);
		assertThat(option.get().conservationRuleApplied()).isFalse();
	}
}
