/* AuthService coordinates auth use cases and enforces their business invariants. */

package com.sentinel.hemo_grid.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.auth.api.LoginResponse;
import com.sentinel.hemo_grid.auth.api.UserResponse;
import com.sentinel.hemo_grid.auth.domain.AppUser;
import com.sentinel.hemo_grid.auth.domain.UserRole;
import com.sentinel.hemo_grid.auth.persistence.UserRepository;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.config.AppProperties;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final AppProperties appProperties;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtEncoder jwtEncoder,
			AppProperties appProperties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.appProperties = appProperties;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		AppUser user = userRepository.findByEmailIgnoreCaseWithOrganization(request.email())
				.filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
				.orElseThrow(this::invalidCredentials);

		if (!user.isActive()) {
			throw invalidCredentials();
		}
		if (user.getOrganization() != null && !user.getOrganization().isActive()) {
			throw invalidCredentials();
		}

		Instant issuedAt = Instant.now();
		Duration tokenLifetime = Duration.ofMinutes(appProperties.security().accessTokenMinutes());
		Instant expiresAt = issuedAt.plus(tokenLifetime);

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(appProperties.security().jwtIssuer())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name());

		if (user.getOrganization() != null) {
			claims.claim("organizationId", user.getOrganization().getId().toString());
			claims.claim("organizationType", user.getOrganization().getOrganizationType().name());
		}

		String token = jwtEncoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
		return new LoginResponse(token, "Bearer", tokenLifetime.toSeconds(), UserResponse.from(user));
	}

	@Transactional(readOnly = true)
	public AppUser requireCurrentUser(Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			throw new BusinessException(
					HttpStatus.UNAUTHORIZED,
					ErrorCode.UNAUTHORIZED,
					"Authentication is required to access this resource."
			);
		}

		UUID userId;
		try {
			userId = UUID.fromString(jwt.getSubject());
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid authentication token.");
		}

		AppUser user = userRepository.findByIdWithOrganization(userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid authentication token."));

		if (!user.isActive()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid authentication token.");
		}
		if (user.getOrganization() != null && !user.getOrganization().isActive()) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid authentication token.");
		}

		return user;
	}

	@Transactional(readOnly = true)
	public AppUser requireHospitalUser(Jwt jwt) {
		AppUser user = requireCurrentUser(jwt);
		if (user.getOrganization() == null) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only hospital users can access blood requests.");
		}
		if (user.getOrganization().getOrganizationType() != OrganizationType.HOSPITAL) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ORGANIZATION_TYPE_MISMATCH, "Only hospital users can access blood requests.");
		}
		if (user.getRole() != UserRole.HOSPITAL_ADMIN && user.getRole() != UserRole.HOSPITAL_STAFF) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only hospital users can access blood requests.");
		}
		return user;
	}

	@Transactional(readOnly = true)
	public AppUser requireBloodBankUser(Jwt jwt) {
		AppUser user = requireCurrentUser(jwt);
		if (user.getOrganization() == null) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only blood-bank users can access provider requests.");
		}
		if (user.getOrganization().getOrganizationType() != OrganizationType.BLOOD_BANK) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ORGANIZATION_TYPE_MISMATCH, "Only blood-bank users can access provider requests.");
		}
		if (user.getRole() != UserRole.BLOOD_BANK_ADMIN && user.getRole() != UserRole.BLOOD_BANK_STAFF) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only blood-bank users can access provider requests.");
		}
		return user;
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid email or password.");
	}
}
