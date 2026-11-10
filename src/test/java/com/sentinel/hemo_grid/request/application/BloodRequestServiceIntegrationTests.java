package com.sentinel.hemo_grid.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
import com.sentinel.hemo_grid.request.api.CandidateResponse;
import com.sentinel.hemo_grid.request.api.CreateBloodRequestRequest;
import com.sentinel.hemo_grid.request.api.SelectProviderRequest;
import com.sentinel.hemo_grid.request.domain.RequestStatus;
import com.sentinel.hemo_grid.request.domain.RequestUrgency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class BloodRequestServiceIntegrationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private BloodRequestService bloodRequestService;

	@Test
	void hospitalCanCreateRequestAndGetRankedCandidates() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");

		BloodRequestResponse created = bloodRequestService.createRequest(hospitalJwt, createONegativeRequest());

		assertThat(created.status()).isEqualTo(RequestStatus.REQUESTED);
		assertThat(created.requester().name()).isEqualTo("Central Care Hospital");
		assertThat(created.provider()).isNull();

		List<CandidateResponse> candidates = bloodRequestService.getCandidates(hospitalJwt, created.id()).candidates();

		assertThat(candidates).hasSize(3);
		assertThat(candidates.get(0).organizationName()).isEqualTo("Maitama Blood Centre");
		assertThat(candidates.get(0).canFullyFulfil()).isTrue();
		assertThat(candidates.get(1).organizationName()).isEqualTo("Wuse Regional Blood Bank");
		assertThat(candidates.get(1).canFullyFulfil()).isTrue();
		assertThat(candidates.get(2).organizationName()).isEqualTo("Garki Emergency Blood Bank");
		assertThat(candidates.get(2).canFullyFulfil()).isFalse();
	}

	@Test
	void hospitalCanSelectCandidateProvider() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		BloodRequestResponse created = bloodRequestService.createRequest(hospitalJwt, createONegativeRequest());

		BloodRequestResponse updated = bloodRequestService.selectProvider(
				hospitalJwt,
				created.id(),
				new SelectProviderRequest(candidatesProviderId(created, "Maitama Blood Centre"))
		);

		assertThat(updated.status()).isEqualTo(RequestStatus.REQUESTED);
		assertThat(updated.provider()).isNotNull();
		assertThat(updated.provider().name()).isEqualTo("Maitama Blood Centre");
	}

	@Test
	void bloodBankUserCannotCreateHospitalRequest() {
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");

		assertThatThrownBy(() -> bloodRequestService.createRequest(bankJwt, createONegativeRequest()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Only hospital users can access blood requests.");
	}

	private CreateBloodRequestRequest createONegativeRequest() {
		return new CreateBloodRequestRequest(
				BloodGroup.O_NEGATIVE,
				BloodComponent.RED_CELLS,
				3,
				RequestUrgency.CRITICAL,
				"ER-2026-0820-001",
				"Emergency request"
		);
	}

	private java.util.UUID candidatesProviderId(BloodRequestResponse request, String organizationName) {
		return bloodRequestService.getCandidates(login("hospital.demo@hemogrid.local", "HospitalDemo123!"), request.id())
				.candidates()
				.stream()
				.filter(candidate -> candidate.organizationName().equals(organizationName))
				.findFirst()
				.orElseThrow()
				.organizationId();
	}

	private Jwt login(String email, String password) {
		String token = authService.login(new LoginRequest(email, password)).accessToken();
		return jwtDecoder.decode(token);
	}
}
