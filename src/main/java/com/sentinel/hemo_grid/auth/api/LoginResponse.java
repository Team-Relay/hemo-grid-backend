package com.sentinel.hemo_grid.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
		@Schema(description = "JWT bearer access token")
		String accessToken,

		@Schema(example = "Bearer")
		String tokenType,

		@Schema(description = "Token lifetime in seconds", example = "28800")
		long expiresIn,

		UserResponse user
) {
}
