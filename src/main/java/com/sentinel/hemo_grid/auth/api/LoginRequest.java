/* LoginRequest is a validated inbound API contract for the auth module. */

package com.sentinel.hemo_grid.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank
		@Email
		@Schema(example = "hospital.demo@hemogrid.local")
		String email,

		@NotBlank
		@Schema(example = "HospitalDemo123!")
		String password
) {
}
