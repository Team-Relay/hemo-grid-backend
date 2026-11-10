/* OrganizationResponse is a safe outbound API projection for the organization module. */

package com.sentinel.hemo_grid.organization.api;

import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.FacilityTier;
import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrganizationResponse(
		@Schema(example = "11111111-1111-1111-1111-111111111111")
		UUID id,

		@Schema(example = "Central Care Hospital")
		String name,

		@Schema(example = "HOSPITAL")
		OrganizationType type,

		@Schema(example = "HSP-001")
		String facilityCode,

		@Schema(example = "HOSPITAL")
		FacilityTier facilityTier,

		String email,
		String phone,
		String address,
		String city,
		String state,
		String county,
		String assignedSupplyCentre,
		String recommendedSupplyCentre,
		Double savedTransitHours,
		boolean active
) {

	public static OrganizationResponse from(Organization organization) {
		return new OrganizationResponse(
				organization.getId(),
				organization.getName(),
				organization.getOrganizationType(),
				organization.getFacilityCode(),
				organization.getFacilityTier(),
				organization.getEmail(),
				organization.getPhone(),
				organization.getAddress(),
				organization.getCity(),
				organization.getState(),
				organization.getCounty(),
				organization.getAssignedSupplyCentre(),
				organization.getRecommendedSupplyCentre(),
				organization.getSavedTransitHours(),
				organization.isActive()
		);
	}
}
