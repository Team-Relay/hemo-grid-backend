package com.sentinel.hemo_grid.auth.api;

import com.sentinel.hemo_grid.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Log in and receive a JWT bearer token")
	@ApiResponse(responseCode = "200", description = "Login succeeded")
	@ApiResponse(responseCode = "400", description = "Validation failed")
	@ApiResponse(responseCode = "401", description = "Invalid credentials")
	LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/me")
	@Operation(summary = "Get the authenticated user")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponse(responseCode = "200", description = "Current user returned")
	@ApiResponse(responseCode = "401", description = "Missing or invalid token")
	UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return UserResponse.from(authService.requireCurrentUser(jwt));
	}
}
