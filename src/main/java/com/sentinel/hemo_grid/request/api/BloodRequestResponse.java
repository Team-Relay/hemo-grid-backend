/* BloodRequestResponse is a safe outbound API projection for the request module. */

package com.sentinel.hemo_grid.request.api;

import java.time.Instant;
import java.util.UUID;

import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.RequestStatus;
import com.sentinel.hemo_grid.request.domain.RequestUrgency;
import io.swagger.v3.oas.annotations.media.Schema;

public record BloodRequestResponse(
		@Schema(example = "90000000-0000-0000-0000-000000000001")
		UUID id,

		OrganizationBriefResponse requester,
		OrganizationBriefResponse provider,

		@Schema(example = "O_NEGATIVE")
		BloodGroup bloodGroup,

		@Schema(example = "RED_CELLS")
		BloodComponent component,

		@Schema(example = "3")
		int unitsRequired,

		@Schema(example = "CRITICAL")
		RequestUrgency urgency,

		@Schema(example = "REQUESTED")
		RequestStatus status,

		String clinicalReference,
		String notes,
		Instant requestedAt,
		Instant acceptedAt,
		Instant preparingAt,
		Instant dispatchedAt,
		Instant deliveredAt,
		Instant cancelledAt,
		Instant updatedAt
) {

	public static BloodRequestResponse from(BloodRequest request) {
		return new BloodRequestResponse(
				request.getId(),
				OrganizationBriefResponse.from(request.getRequesterOrganization()),
				OrganizationBriefResponse.from(request.getProviderOrganization()),
				request.getBloodGroup(),
				request.getComponent(),
				request.getUnitsRequired(),
				request.getUrgency(),
				request.getStatus(),
				request.getClinicalReference(),
				request.getNotes(),
				request.getRequestedAt(),
				request.getAcceptedAt(),
				request.getPreparingAt(),
				request.getDispatchedAt(),
				request.getDeliveredAt(),
				request.getCancelledAt(),
				request.getUpdatedAt()
		);
	}
}
