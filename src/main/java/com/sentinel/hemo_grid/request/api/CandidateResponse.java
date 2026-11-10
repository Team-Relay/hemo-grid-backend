/* CandidateResponse is a safe outbound API projection for the request module. */

package com.sentinel.hemo_grid.request.api;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.MatchCompatibilityType;
import com.sentinel.hemo_grid.request.domain.RequestCandidate;
import io.swagger.v3.oas.annotations.media.Schema;

public record CandidateResponse(
		@Schema(example = "22222222-2222-2222-2222-222222222222")
		UUID organizationId,

		@Schema(example = "Maitama Blood Centre")
		String organizationName,

		@Schema(example = "O_NEGATIVE")
		BloodGroup bloodGroup,

		@Schema(example = "O_NEGATIVE")
		BloodGroup offeredBloodGroup,

		@Schema(example = "RED_CELLS")
		BloodComponent component,

		@Schema(example = "EXACT")
		MatchCompatibilityType matchType,

		@Schema(example = "5")
		int unitsFree,

		@Schema(example = "2.83")
		Double distanceKm,

		@Schema(example = "0.15")
		Double estimatedTransitHours,

		@Schema(example = "true")
		boolean coldChainVerified,

		@Schema(example = "3.8")
		double fridgeTempCelsius,

		@Schema(example = "0")
		int quarantinedUnitsExcluded,

		@Schema(example = "false")
		boolean conservationRuleApplied,

		List<String> recommendationReasons,

		@Schema(example = "true")
		boolean canFullyFulfil,

		@Schema(example = "1")
		int rank
) {

	public static CandidateResponse from(RequestCandidate candidate) {
		BloodRequest request = candidate.getBloodRequest();
		List<String> reasons = candidate.getRecommendationReasons() != null && !candidate.getRecommendationReasons().isBlank()
				? List.of(candidate.getRecommendationReasons().split(" \\| "))
				: List.of();

		return new CandidateResponse(
				candidate.getProviderOrganization().getId(),
				candidate.getProviderOrganization().getName(),
				request.getBloodGroup(),
				candidate.getOfferedBloodGroup() != null ? candidate.getOfferedBloodGroup() : request.getBloodGroup(),
				request.getComponent(),
				candidate.getMatchType() != null ? candidate.getMatchType() : MatchCompatibilityType.EXACT,
				candidate.getAvailableUnitsSnapshot(),
				roundValue(candidate.getDistanceKm()),
				roundValue(candidate.getEstimatedTransitHours()),
				candidate.isColdChainVerified(),
				candidate.getFridgeTempCelsius(),
				candidate.getQuarantinedUnitsExcluded(),
				candidate.isConservationRuleApplied(),
				reasons,
				candidate.getAvailableUnitsSnapshot() >= request.getUnitsRequired(),
				candidate.getRankPosition()
		);
	}

	private static Double roundValue(Double value) {
		if (value == null) {
			return null;
		}
		return Math.round(value * 100.0) / 100.0;
	}
}
