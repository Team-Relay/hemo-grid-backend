package com.sentinel.hemo_grid.inventory.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sentinel.hemo_grid.request.domain.MatchCompatibilityType;

/**
 * Clinical ABO / Rh D component-aware compatibility engine.
 * Incorporates universal red-cell conservation rules (preserving O-Negative inventory
 * strictly for O- recipients or unknown trauma resuscitation).
 */
public final class AboRhCompatibilityPolicy {

	private AboRhCompatibilityPolicy() {
	}

	public record CompatibilityOption(
			BloodGroup bloodGroup,
			MatchCompatibilityType matchType,
			int preferenceRank, // 1 = exact, 2 = compatible substitute, 3 = O- emergency substitute
			boolean conservationRuleApplied,
			String clinicalRationale
	) {}

	/**
	 * Returns compatible donor blood groups ranked in clinical order of preference.
	 */
	public static List<CompatibilityOption> getCompatibleDonors(
			BloodGroup recipientGroup,
			BloodComponent component,
			boolean unknownPatientBloodGroup
	) {
		if (unknownPatientBloodGroup) {
			return getTraumaEmergencyDonors(component);
		}

		if (recipientGroup == null) {
			return Collections.emptyList();
		}

		return switch (component) {
			case RED_CELLS, WHOLE_BLOOD, PLATELETS -> getCellularCompatibleDonors(recipientGroup);
			case PLASMA -> getPlasmaCompatibleDonors(recipientGroup);
		};
	}

	/**
	 * Evaluates whether donorGroup is compatible for recipientGroup and returns its compatibility profile.
	 */
	public static Optional<CompatibilityOption> evaluateCompatibility(
			BloodGroup recipientGroup,
			BloodGroup donorGroup,
			BloodComponent component,
			boolean unknownPatientBloodGroup
	) {
		return getCompatibleDonors(recipientGroup, component, unknownPatientBloodGroup)
				.stream()
				.filter(option -> option.bloodGroup() == donorGroup)
				.findFirst();
	}

	public static boolean isCompatible(
			BloodGroup recipientGroup,
			BloodGroup donorGroup,
			BloodComponent component,
			boolean unknownPatientBloodGroup
	) {
		return evaluateCompatibility(recipientGroup, donorGroup, component, unknownPatientBloodGroup).isPresent();
	}

	private static List<CompatibilityOption> getTraumaEmergencyDonors(BloodComponent component) {
		List<CompatibilityOption> options = new ArrayList<>();
		if (component == BloodComponent.PLASMA) {
			options.add(new CompatibilityOption(
					BloodGroup.AB_POSITIVE,
					MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
					1,
					false,
					"Universal plasma donor: AB plasma lacks anti-A and anti-B antibodies for unknown blood groups"
			));
			options.add(new CompatibilityOption(
					BloodGroup.AB_NEGATIVE,
					MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
					2,
					false,
					"Universal plasma donor substitute: AB plasma safely transfused for uncrossmatched recipients"
			));
		} else {
			options.add(new CompatibilityOption(
					BloodGroup.O_NEGATIVE,
					MatchCompatibilityType.EXACT,
					1,
					false,
					"Trauma Protocol: Universal uncrossmatched red blood cells for unknown patient blood group"
			));
		}
		return Collections.unmodifiableList(options);
	}

	private static List<CompatibilityOption> getCellularCompatibleDonors(BloodGroup recipient) {
		List<CompatibilityOption> options = new ArrayList<>();

		// 1. Exact match is always tier 1
		options.add(new CompatibilityOption(
				recipient,
				MatchCompatibilityType.EXACT,
				1,
				false,
				"Exact ABO/Rh D match for optimal transfusion safety and preservation of donor inventory"
		));

		// 2. Compatible substitutes based on recipient ABO & Rh
		switch (recipient) {
			case O_NEGATIVE -> {
				// O- can only receive O-
			}
			case O_POSITIVE -> {
				// Can receive O- as substitute (with conservation rule)
				options.add(new CompatibilityOption(
						BloodGroup.O_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						3,
						true,
						"O-Negative conservation: O+ recipients accept O- only when exact O+ units are depleted"
				));
			}
			case A_NEGATIVE -> {
				options.add(new CompatibilityOption(
						BloodGroup.O_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						3,
						true,
						"O-Negative conservation: A- recipients accept O- only when exact A- units are depleted"
				));
			}
			case A_POSITIVE -> {
				options.add(new CompatibilityOption(
						BloodGroup.A_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Rh-negative identical ABO substitute: A- red cells are fully safe for A+ recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.O_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO alternative: O+ red cells safely compatible without depleting scarce O- stocks"
				));
				options.add(new CompatibilityOption(
						BloodGroup.O_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						3,
						true,
						"O-Negative conservation: O- units reserved as last resort emergency substitute"
				));
			}
			case B_NEGATIVE -> {
				options.add(new CompatibilityOption(
						BloodGroup.O_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						3,
						true,
						"O-Negative conservation: B- recipients accept O- only when exact B- units are depleted"
				));
			}
			case B_POSITIVE -> {
				options.add(new CompatibilityOption(
						BloodGroup.B_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Rh-negative identical ABO substitute: B- red cells are safe for B+ recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.O_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO alternative: O+ red cells safely compatible without depleting scarce O- stocks"
				));
				options.add(new CompatibilityOption(
						BloodGroup.O_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						3,
						true,
						"O-Negative conservation: O- units reserved as last resort emergency substitute"
				));
			}
			case AB_NEGATIVE -> {
				options.add(new CompatibilityOption(
						BloodGroup.A_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO Rh- substitute: A- red cells safe for AB- recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.B_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO Rh- substitute: B- red cells safe for AB- recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.O_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						3,
						true,
						"O-Negative conservation: O- units reserved as last resort emergency substitute"
				));
			}
			case AB_POSITIVE -> {
				// Universal cellular recipient: accept non-O- substitutes first
				options.add(new CompatibilityOption(
						BloodGroup.AB_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible Rh- substitute: AB- red cells safe for AB+ recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.A_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO alternative: A+ red cells safe for AB+ recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.B_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO alternative: B+ red cells safe for AB+ recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.O_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO alternative: O+ red cells safely transfusable"
				));
				options.add(new CompatibilityOption(
						BloodGroup.A_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO Rh- alternative: A- red cells safely transfusable"
				));
				options.add(new CompatibilityOption(
						BloodGroup.B_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible ABO Rh- alternative: B- red cells safely transfusable"
				));
				options.add(new CompatibilityOption(
						BloodGroup.O_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						3,
						true,
						"O-Negative conservation: O- units reserved as last resort emergency substitute"
				));
			}
		}

		return Collections.unmodifiableList(options);
	}

	private static List<CompatibilityOption> getPlasmaCompatibleDonors(BloodGroup recipient) {
		List<CompatibilityOption> options = new ArrayList<>();

		// In plasma, exact is preferred first
		options.add(new CompatibilityOption(
				recipient,
				MatchCompatibilityType.EXACT,
				1,
				false,
				"Exact ABO/Rh match for plasma transfusion"
		));

		// Plasma universal donor is AB (lacks anti-A and anti-B antibodies)
		switch (recipient) {
			case AB_POSITIVE, AB_NEGATIVE -> {
				// AB can receive AB
				if (recipient == BloodGroup.AB_POSITIVE) {
					options.add(new CompatibilityOption(
							BloodGroup.AB_NEGATIVE,
							MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
							2,
							false,
							"Compatible plasma: AB- lacks ABO antibodies"
					));
				} else {
					options.add(new CompatibilityOption(
							BloodGroup.AB_POSITIVE,
							MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
							2,
							false,
							"Compatible plasma: AB+ lacks ABO antibodies"
					));
				}
			}
			case A_POSITIVE, A_NEGATIVE -> {
				options.add(new CompatibilityOption(
						BloodGroup.AB_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Universal plasma substitute: AB plasma safe for A recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.AB_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Universal plasma substitute: AB- plasma safe for A recipients"
				));
			}
			case B_POSITIVE, B_NEGATIVE -> {
				options.add(new CompatibilityOption(
						BloodGroup.AB_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Universal plasma substitute: AB plasma safe for B recipients"
				));
				options.add(new CompatibilityOption(
						BloodGroup.AB_NEGATIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Universal plasma substitute: AB- plasma safe for B recipients"
				));
			}
			case O_POSITIVE, O_NEGATIVE -> {
				// O is universal plasma recipient
				options.add(new CompatibilityOption(
						BloodGroup.A_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible plasma: A+ plasma transfusable to O recipient"
				));
				options.add(new CompatibilityOption(
						BloodGroup.B_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Compatible plasma: B+ plasma transfusable to O recipient"
				));
				options.add(new CompatibilityOption(
						BloodGroup.AB_POSITIVE,
						MatchCompatibilityType.COMPATIBLE_SUBSTITUTE,
						2,
						false,
						"Universal plasma: AB+ plasma safe for all recipients"
				));
			}
		}

		return Collections.unmodifiableList(options);
	}
}
