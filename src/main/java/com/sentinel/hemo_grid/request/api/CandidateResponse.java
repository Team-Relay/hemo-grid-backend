package com.sentinel.hemo_grid.request.api;

import java.util.UUID;

import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.RequestCandidate;
import io.swagger.v3.oas.annotations.media.Schema;

public record CandidateResponse(
		@Schema(example = "22222222-2222-2222-2222-222222222222")
		UUID organizationId,

		@Schema(example = "Maitama Blood Centre")
		String organizationName,

		@Schema(example = "O_NEGATIVE")
		BloodGroup bloodGroup,

		@Schema(example = "RED_CELLS")
		BloodComponent component,

		@Schema(example = "5")
		int unitsFree,

		@Schema(example = "2.83")
		Double distanceKm,

		@Schema(example = "true")
		boolean canFullyFulfil,

		@Schema(example = "1")
		int rank
) {

	public static CandidateResponse from(RequestCandidate candidate) {
		BloodRequest request = candidate.getBloodRequest();
		return new CandidateResponse(
				candidate.getProviderOrganization().getId(),
				candidate.getProviderOrganization().getName(),
				request.getBloodGroup(),
				request.getComponent(),
				candidate.getAvailableUnitsSnapshot(),
				roundDistance(candidate.getDistanceKm()),
				candidate.getAvailableUnitsSnapshot() >= request.getUnitsRequired(),
				candidate.getRankPosition()
		);
	}

	private static Double roundDistance(Double distanceKm) {
		if (distanceKm == null) {
			return null;
		}
		return Math.round(distanceKm * 100.0) / 100.0;
	}
}
