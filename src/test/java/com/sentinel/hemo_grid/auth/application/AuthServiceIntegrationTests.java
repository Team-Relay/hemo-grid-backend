package com.sentinel.hemo_grid.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.auth.api.LoginResponse;
import com.sentinel.hemo_grid.auth.domain.UserRole;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthServiceIntegrationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void loginReturnsJwtWithUserAndOrganizationContext() {
		LoginResponse response = authService.login(new LoginRequest(
				"hospital.demo@hemogrid.local",
				"HospitalDemo123!"
		));

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.expiresIn()).isPositive();
		assertThat(response.user().role()).isEqualTo(UserRole.HOSPITAL_STAFF);
		assertThat(response.user().organization().type()).isEqualTo(OrganizationType.HOSPITAL);

		Jwt jwt = jwtDecoder.decode(response.accessToken());
		assertThat(jwt.getSubject()).isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
		assertThat(jwt.getClaimAsString("email")).isEqualTo("hospital.demo@hemogrid.local");
		assertThat(jwt.getClaimAsString("role")).isEqualTo("HOSPITAL_STAFF");
		assertThat(jwt.getClaimAsString("organizationType")).isEqualTo("HOSPITAL");
	}

	@Test
	void loginRejectsInvalidPassword() {
		LoginRequest request = new LoginRequest("hospital.demo@hemogrid.local", "wrong-password");

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Invalid email or password.");
	}
}
