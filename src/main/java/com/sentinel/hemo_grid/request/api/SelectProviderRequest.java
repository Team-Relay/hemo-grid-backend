/* SelectProviderRequest is a validated inbound API contract for the request module. */

package com.sentinel.hemo_grid.request.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SelectProviderRequest(
		@NotNull
		@Schema(example = "22222222-2222-2222-2222-222222222222")
		UUID providerOrganizationId
) {
}
