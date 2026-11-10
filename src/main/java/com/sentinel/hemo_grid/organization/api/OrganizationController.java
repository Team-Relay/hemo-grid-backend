/* OrganizationController is the authenticated HTTP boundary for organization operations. */

package com.sentinel.hemo_grid.organization.api;

import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.organization.domain.Organization;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations")
public class OrganizationController {

	private final AuthService authService;

	public OrganizationController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/me")
	@Operation(summary = "Get the authenticated user's organization")
	@SecurityRequirement(name = "bearerAuth")
	OrganizationResponse currentOrganization(@AuthenticationPrincipal Jwt jwt) {
		Organization organization = authService.requireCurrentUser(jwt).getOrganization();
		if (organization == null) {
			throw new BusinessException(
					HttpStatus.NOT_FOUND,
					ErrorCode.RESOURCE_NOT_FOUND,
					"The current user is not attached to an organization."
			);
		}
		return OrganizationResponse.from(organization);
	}
}
