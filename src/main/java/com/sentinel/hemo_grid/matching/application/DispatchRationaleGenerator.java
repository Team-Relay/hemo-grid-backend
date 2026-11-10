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
 * Explains the recorded inventory rules and transport assumptions behind a proposal.
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
			String policyRationale,
			boolean corridorEstimate
	) {}

	public String generateRationaleString(RationaleContext ctx) {
		List<String> bullets = generateRationaleBullets(ctx);
		return String.join(" | ", bullets);
	}

	public List<String> generateRationaleBullets(RationaleContext ctx) {
		List<String> bullets = new ArrayList<>();

		// 1. Clinical Compatibility & Conservation
		if (ctx.request().isUnknownPatientBloodGroup()) {
			bullets.add(String.format(
					"Trauma request with unknown group: configured policy proposes %s inventory. Qualified staff must confirm suitability and authorize release.",
					formatBloodGroup(ctx.offeredBloodGroup())
			));
		} else if (ctx.matchType() == MatchCompatibilityType.EXACT) {
			bullets.add(String.format(
					"Exact inventory group (%s) under the configured policy. Qualified staff must confirm suitability and authorize release.",
					formatBloodGroup(ctx.offeredBloodGroup())
			));
		} else {
			if (ctx.conservationRuleApplied()) {
				bullets.add(String.format(
						"Reserve-conservation rule applies to this proposal for a %s request. Qualified staff approval is required.",
						formatBloodGroup(ctx.request().getBloodGroup())
				));
			} else {
				bullets.add(String.format(
						"Proposed inventory substitute (%s) for %s under configured rules. Qualified staff must confirm suitability.",
						formatBloodGroup(ctx.offeredBloodGroup()),
						formatBloodGroup(ctx.request().getBloodGroup())
				));
			}
		}

		// 2. Cold Chain Safety
		if (ctx.coldChainVerified()) {
			bullets.add(String.format(
					Locale.US,
					"Recorded fridge temperature %.1f°C satisfies the prototype's configured 2–6°C rule. Staff must confirm current telemetry and unit condition.",
					ctx.fridgeTempCelsius()
			));
		} else {
			bullets.add(String.format(
					Locale.US,
					"Cold-chain hold: recorded temperature %.1f°C; %d units recorded as quarantined. This inventory requires staff review.",
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
					"Road Transit ETA: %.2fh%s from %s.",
					ctx.estimatedTransitHours(),
					distanceText,
					ctx.corridorEstimate() ? "a seeded corridor estimate" : "straight-line distance at an assumed 45 km/h"
			));
			bullets.add("ETA uncertainty is uncalibrated. Confirm transport with the supplying bank. Preparation, approval and traffic delays are additional.");
		} else {
			bullets.add("Transit ETA is unavailable. Staff must confirm the transport plan with the supplying bank.");
		}

		if (ctx.provider().getSavedTransitHours() != null && ctx.provider().getSavedTransitHours() > 0) {
			bullets.add(String.format(
					Locale.US,
					"Seeded route metadata estimates %.2fh savings relative to the assigned supply centre. This figure requires operational validation.",
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
