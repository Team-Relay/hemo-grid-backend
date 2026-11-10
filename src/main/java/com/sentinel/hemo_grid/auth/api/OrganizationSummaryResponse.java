package com.sentinel.hemo_grid.auth.api;

import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrganizationSummaryResponse(
		@Schema(example = "11111111-1111-1111-1111-111111111111")
		UUID id,

		@Schema(example = "Central Care Hospital")
		String name,

		@Schema(example = "HOSPITAL")
		OrganizationType type
) {

	public static OrganizationSummaryResponse from(Organization organization) {
		if (organization == null) {
			return null;
		}
		return new OrganizationSummaryResponse(
				organization.getId(),
				organization.getName(),
				organization.getOrganizationType()
		);
	}
}
