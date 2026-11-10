/* OrganizationResponse is a safe outbound API projection for the organization module. */

package com.sentinel.hemo_grid.organization.api;

import java.util.UUID;

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

		String email,
		String phone,
		String address,
		String city,
		String state,
		boolean active
) {

	public static OrganizationResponse from(Organization organization) {
		return new OrganizationResponse(
				organization.getId(),
				organization.getName(),
				organization.getOrganizationType(),
				organization.getEmail(),
				organization.getPhone(),
				organization.getAddress(),
				organization.getCity(),
				organization.getState(),
				organization.isActive()
		);
	}
}
