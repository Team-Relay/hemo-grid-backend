package com.sentinel.hemo_grid.matching.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import com.sentinel.hemo_grid.inventory.domain.ColdChainStatus;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.persistence.OrganizationRepository;
import com.sentinel.hemo_grid.request.api.CandidateResponse;
import com.sentinel.hemo_grid.request.api.CreateBloodRequestRequest;
import com.sentinel.hemo_grid.request.api.SelectProviderRequest;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
import com.sentinel.hemo_grid.request.application.BloodRequestService;
import com.sentinel.hemo_grid.request.domain.MatchCompatibilityType;
import com.sentinel.hemo_grid.request.domain.RequestUrgency;
import org.junit.jupiter.api.DisplayName;
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
class SmartDispatchRankingIntegrationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private BloodRequestService bloodRequestService;

	@Autowired
	private BloodInventoryRepository inventoryRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Test
	@DisplayName("Trauma unknown patient group protocol requests O-Negative and generates clinical rationale")
	void traumaUnknownPatientGroupAllocatesONegativeWithRationale() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");

		CreateBloodRequestRequest traumaRequest = new CreateBloodRequestRequest(
				null,
				BloodComponent.RED_CELLS,
				2,
				RequestUrgency.CRITICAL,
				true,
				"TRAUMA-STAT-001",
				"[EMERGENCY UNKNOWN BLOOD GROUP TRAUMA PROTOCOL]"
		);

		BloodRequestResponse created = bloodRequestService.createRequest(hospitalJwt, traumaRequest);
		assertThat(created.unknownPatientBloodGroup()).isTrue();
		assertThat(created.bloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);

		List<CandidateResponse> candidates = bloodRequestService.getCandidates(hospitalJwt, created.id()).candidates();
		assertThat(candidates).isNotEmpty();

		CandidateResponse topCandidate = candidates.get(0);
		assertThat(topCandidate.offeredBloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
		assertThat(topCandidate.recommendationReasons()).anyMatch(r -> r.contains("Trauma"));
	}

	@Test
	@DisplayName("Cold-chain temperature excursion (>6.0C) drops candidate below verified facilities")
	void coldChainExcursionDropsCandidateRank() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");

		// Set Maitama (org 2222...) O_NEGATIVE inventory to a temperature excursion (8.5C)
		UUID maitamaId = UUID.fromString("22222222-2222-2222-2222-222222222222");
		BloodInventory maitamaInventory = inventoryRepository.findByOrganizationIdAndBloodGroupAndComponent(
				maitamaId,
				BloodGroup.O_NEGATIVE,
				BloodComponent.RED_CELLS
		).orElseThrow();
		maitamaInventory.updateColdChainTelemetry(8.5, maitamaInventory.getUnitsAvailable());
		inventoryRepository.saveAndFlush(maitamaInventory);

		CreateBloodRequestRequest request = new CreateBloodRequestRequest(
				BloodGroup.O_NEGATIVE,
				BloodComponent.RED_CELLS,
				3,
				RequestUrgency.CRITICAL,
				"COLD-TEST-001",
				"Cold chain test"
		);

		BloodRequestResponse created = bloodRequestService.createRequest(hospitalJwt, request);
		List<CandidateResponse> candidates = bloodRequestService.getCandidates(hospitalJwt, created.id()).candidates();

		// Wuse (4444...) has 7 verified units free at 3.8C, so Wuse must now be rank 1!
		assertThat(candidates.get(0).organizationName()).isEqualTo("Wuse Regional Blood Bank");
		assertThat(candidates.get(0).coldChainVerified()).isTrue();
		assertThat(candidates.get(0).canFullyFulfil()).isTrue();

		// Maitama has excursion, so its dispatchable free units are 0
		CandidateResponse maitamaCandidate = candidates.stream()
				.filter(c -> c.organizationId().equals(maitamaId))
				.findFirst()
				.orElseThrow();
		assertThat(maitamaCandidate.coldChainVerified()).isFalse();
		assertThat(maitamaCandidate.unitsFree()).isEqualTo(0);
	}

	@Test
	@DisplayName("Road corridor route supplies accurate transit hours and distance on candidates")
	void roadCorridorSuppliesTransitHoursAndKm() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");

		CreateBloodRequestRequest request = new CreateBloodRequestRequest(
				BloodGroup.O_NEGATIVE,
				BloodComponent.RED_CELLS,
				2,
				RequestUrgency.CRITICAL,
				"CORRIDOR-001",
				"Corridor ETA test"
		);

		BloodRequestResponse created = bloodRequestService.createRequest(hospitalJwt, request);
		List<CandidateResponse> candidates = bloodRequestService.getCandidates(hospitalJwt, created.id()).candidates();

		// Central Care (1111... / HSP-001) to Maitama (2222... / RBTC-NBO) in routes.csv has corridor route: 0.1km, 0.30h
		CandidateResponse maitama = candidates.stream()
				.filter(c -> c.organizationName().equals("Maitama Blood Centre"))
				.findFirst()
				.orElseThrow();

		assertThat(maitama.distanceKm()).isEqualTo(0.1);
		assertThat(maitama.estimatedTransitHours()).isEqualTo(0.30);
		assertThat(maitama.recommendationReasons()).anyMatch(r -> r.contains("Road Transit ETA"));
		assertThat(maitama.recommendationReasons()).anyMatch(r -> r.contains("seeded corridor estimate"));
		assertThat(maitama.recommendationReasons()).anyMatch(r -> r.contains("uncertainty is uncalibrated"));
	}

	@Test
	@DisplayName("Selecting a candidate updates allocated blood group and preserves cold chain status on request")
	void hospitalSelectsCandidateWithOfferedGroupAndTelemetry() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");

		CreateBloodRequestRequest request = new CreateBloodRequestRequest(
				BloodGroup.O_NEGATIVE,
				BloodComponent.RED_CELLS,
				2,
				RequestUrgency.CRITICAL,
				"SELECT-TEST-001",
				"Selection test"
		);

		BloodRequestResponse created = bloodRequestService.createRequest(hospitalJwt, request);
		CandidateResponse top = bloodRequestService.getCandidates(hospitalJwt, created.id()).candidates().get(0);

		BloodRequestResponse selected = bloodRequestService.selectProvider(
				hospitalJwt,
				created.id(),
				new SelectProviderRequest(top.organizationId())
		);

		assertThat(selected.provider()).isNotNull();
		assertThat(selected.provider().id()).isEqualTo(top.organizationId());
		assertThat(selected.allocatedBloodGroup()).isEqualTo(top.offeredBloodGroup());
		assertThat(selected.matchType()).isEqualTo(top.matchType());
		assertThat(selected.coldChainVerified()).isEqualTo(top.coldChainVerified());
		assertThat(selected.estimatedTransitHours()).isEqualTo(top.estimatedTransitHours());
	}

	private Jwt login(String email, String password) {
		String token = authService.login(new LoginRequest(email, password)).accessToken();
		return jwtDecoder.decode(token);
	}
}
