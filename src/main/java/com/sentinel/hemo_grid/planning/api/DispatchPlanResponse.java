package com.sentinel.hemo_grid.planning.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Plan;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Status;

public record DispatchPlanResponse(
		Instant generatedAt,
		Status status,
		Double totalEstimatedTransitHours,
		long visitedNodes,
		Plan nearestEligibleBaseline,
		List<Proposal> proposals,
		List<String> notes
) {
	public record Proposal(
			UUID requestId,
			UUID requesterOrganizationId,
			UUID providerOrganizationId,
			String providerName,
			BloodGroup offeredBloodGroup,
			UUID inventoryId,
			long inventoryVersion,
			int unitsRequired,
			int dispatchableUnitsAtPlanning,
			int unitsRemainingAfterWholePlan,
			double estimatedTransitHours,
			double recordedFridgeTempCelsius,
			Instant inventoryUpdatedAt,
			String rationale
	) {}
}
