package com.sentinel.hemo_grid.matching.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.MatchCompatibilityType;
import org.springframework.stereotype.Component;

/**
 * Generates clinical explainability rationales for smart dispatch decisions.
 * Edge-native and self-contained; guarantees zero network dependencies or API token failures.
 */
@Component
public class DispatchRationaleGenerator {

	public record RationaleContext(
			BloodRequest request,
			Organization provider,
			BloodGroup offeredBloodGroup,
			MatchCompatibilityType matchType,
			int dispatchableUnits,
			int unitsRequired,
			Double distanceKm,
			Double estimatedTransitHours,
			boolean coldChainVerified,
			double fridgeTempCelsius,
			int quarantinedUnitsExcluded,
			boolean conservationRuleApplied,
			String policyRationale
	) {}

	public String generateRationaleString(RationaleContext ctx) {
		List<String> bullets = generateRationaleBullets(ctx);
		return String.join(" | ", bullets);
	}

	public List<String> generateRationaleBullets(RationaleContext ctx) {
		List<String> bullets = new ArrayList<>();

		// 1. Clinical Compatibility & Conservation
		if (ctx.request().isUnknownPatientBloodGroup()) {
			bullets.add("Emergency Trauma Protocol: Universal uncrossmatched O- donor blood allocated for unknown patient blood group.");
		} else if (ctx.matchType() == MatchCompatibilityType.EXACT) {
			bullets.add(String.format(
					"Exact Match (%s): Optimal compatibility with zero hemolytic risk.",
					formatBloodGroup(ctx.offeredBloodGroup())
			));
		} else {
			if (ctx.conservationRuleApplied()) {
				bullets.add(String.format(
						"O-Negative Conservation: Conserved reserve dispensed as emergency fallback; %s recipient held pending standard stocks.",
						formatBloodGroup(ctx.request().getBloodGroup())
				));
			} else {
				bullets.add(String.format(
						"Clinical Substitute (%s): Clinically compatible donor for %s recipient preserving scarce O- reserves.",
						formatBloodGroup(ctx.offeredBloodGroup()),
						formatBloodGroup(ctx.request().getBloodGroup())
				));
			}
		}

		// 2. Cold Chain Safety
		if (ctx.coldChainVerified()) {
			bullets.add(String.format(
					Locale.US,
					"Cold-Chain Verified: Telemetry confirms fridge at %.1f°C (within WHO 2.0–6.0°C safety envelope).",
					ctx.fridgeTempCelsius()
			));
		} else {
			bullets.add(String.format(
					Locale.US,
					"Cold-Chain Excursion: Fridge telemetry at %.1f°C; %d compromised units excluded from dispatch.",
					ctx.fridgeTempCelsius(),
					ctx.quarantinedUnitsExcluded()
			));
		}

		// 3. Corridor Logistics & Route Efficiency
		if (ctx.estimatedTransitHours() != null && ctx.estimatedTransitHours() > 0) {
			String distanceText = ctx.distanceKm() != null
					? String.format(Locale.US, " (%.1f km)", ctx.distanceKm())
					: "";
			bullets.add(String.format(
					Locale.US,
					"Road Transit ETA: %.2fh estimated corridor transit%s.",
					ctx.estimatedTransitHours(),
					distanceText
			));
		}

		if (ctx.provider().getSavedTransitHours() != null && ctx.provider().getSavedTransitHours() > 0) {
			bullets.add(String.format(
					Locale.US,
					"Corridor Optimization: Route bypass saves %.2fh compared to legacy supply centre.",
					ctx.provider().getSavedTransitHours()
			));
		}

		// 4. Fulfillment Capacity
		if (ctx.dispatchableUnits() >= ctx.unitsRequired()) {
			bullets.add(String.format(
					"Full Capacity: %d verified units available (fulfils order of %d).",
					ctx.dispatchableUnits(),
					ctx.unitsRequired()
			));
		} else {
			bullets.add(String.format(
					"Partial Capacity: %d verified units available (request requires %d).",
					ctx.dispatchableUnits(),
					ctx.unitsRequired()
			));
		}

		return bullets;
	}

	private String formatBloodGroup(BloodGroup group) {
		if (group == null) {
			return "Unknown";
		}
		return group.name().replace("_POSITIVE", "+").replace("_NEGATIVE", "-");
	}
}
