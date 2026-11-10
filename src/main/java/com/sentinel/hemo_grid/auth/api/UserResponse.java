package com.sentinel.hemo_grid.auth.api;

import java.util.UUID;

import com.sentinel.hemo_grid.auth.domain.AppUser;
import com.sentinel.hemo_grid.auth.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
		@Schema(example = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
		UUID id,

		@Schema(example = "Demo Hospital User")
		String fullName,

		@Schema(example = "hospital.demo@hemogrid.local")
		String email,

		@Schema(example = "HOSPITAL_STAFF")
		UserRole role,

		OrganizationSummaryResponse organization
) {

	public static UserResponse from(AppUser user) {
		return new UserResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getRole(),
				OrganizationSummaryResponse.from(user.getOrganization())
		);
	}
}
