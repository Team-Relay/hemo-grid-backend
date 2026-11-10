/* OrganizationBriefResponse is a safe outbound API projection for the request module. */

package com.sentinel.hemo_grid.request.api;

import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrganizationBriefResponse(
		@Schema(example = "22222222-2222-2222-2222-222222222222")
		UUID id,

		@Schema(example = "Maitama Blood Centre")
		String name,

		@Schema(example = "BLOOD_BANK")
		OrganizationType type
) {

	public static OrganizationBriefResponse from(Organization organization) {
		if (organization == null) {
			return null;
		}
		return new OrganizationBriefResponse(
				organization.getId(),
				organization.getName(),
				organization.getOrganizationType()
		);
	}
}
