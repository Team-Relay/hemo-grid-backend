package com.sentinel.hemo_grid.planning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.planning.api.CreateDispatchPlanRequest;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Status;
import com.sentinel.hemo_grid.request.api.CreateBloodRequestRequest;
import com.sentinel.hemo_grid.request.api.SelectProviderRequest;
import com.sentinel.hemo_grid.request.application.BloodRequestService;
import com.sentinel.hemo_grid.request.domain.RequestUrgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class DispatchPlanServiceIntegrationTests {
	private static final UUID NAIROBI = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID EMBU = UUID.fromString("44444444-4444-4444-4444-444444444444");
	@Autowired private AuthService authService;
	@Autowired private JwtDecoder jwtDecoder;
	@Autowired private BloodRequestService requests;
	@Autowired private DispatchPlanService planning;
	@Autowired private BloodInventoryRepository inventory;
	@Autowired private JdbcTemplate jdbc;

	@BeforeEach
	void prepareCompetingInventory() {
		jdbc.update("update blood_inventory set units_reserved = 0, units_available = 0, quarantined_temp_excursion_units = 0, fridge_temp_celsius = 3.8, cold_chain_status = 'VERIFIED_2_6C'");
		seedStock(NAIROBI, "O_NEGATIVE");
		seedStock(EMBU, "A_POSITIVE");
	}

	@Test
	void completePreviewPreservesStockAndCanUseExistingHospitalApproval() {
		Jwt hospital = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		List<UUID> ids = createRequests(hospital);
		var result = planning.preview(admin(), new CreateDispatchPlanRequest(ids, true));

		assertThat(result.status()).isEqualTo(Status.OPTIMAL);
		assertThat(result.nearestEligibleBaseline().status()).isEqualTo(Status.INFEASIBLE);
		assertThat(result.proposals()).hasSize(2);
		assertThat(result.proposals().get(0).providerOrganizationId()).isEqualTo(EMBU);
		assertThat(result.proposals().get(1).providerOrganizationId()).isEqualTo(NAIROBI);
		assertThat(jdbc.queryForObject("select sum(units_reserved) from blood_inventory", Integer.class)).isZero();
		assertThat(requests.getRequest(hospital, ids.get(0)).provider()).isNull();
		for (var proposal : result.proposals()) {
			var selected = requests.selectProvider(hospital, proposal.requestId(), new SelectProviderRequest(proposal.providerOrganizationId()));
			assertThat(selected.allocatedBloodGroup()).isEqualTo(proposal.offeredBloodGroup());
		}
	}

	@Test
	void refreshedQuarantineInvalidatesTheWholeProposal() {
		Jwt hospital = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		List<UUID> ids = createRequests(hospital);
		var input = new CreateDispatchPlanRequest(ids, true);
		assertThat(planning.preview(admin(), input).proposals()).hasSize(2);
		var held = inventory.findByOrganizationIdAndBloodGroupAndComponent(EMBU, BloodGroup.A_POSITIVE, BloodComponent.RED_CELLS).orElseThrow();
		held.updateColdChainTelemetry(8.5, held.getUnitsAvailable());
		inventory.saveAndFlush(held);

		var result = planning.preview(admin(), input);
		assertThat(result.status()).isEqualTo(Status.INFEASIBLE);
		assertThat(result.proposals()).isEmpty();
		assertThatThrownBy(() -> requests.selectProvider(hospital, ids.get(0), new SelectProviderRequest(EMBU)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void coordinatorRoleAndExplicitRequestApprovalAreRequired() {
		Jwt hospital = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		List<UUID> ids = createRequests(hospital);
		assertThatThrownBy(() -> planning.preview(hospital, new CreateDispatchPlanRequest(ids, true)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.code()).isEqualTo(ErrorCode.FORBIDDEN));
		assertThatThrownBy(() -> planning.preview(admin(), new CreateDispatchPlanRequest(ids, false)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
		assertThatThrownBy(() -> planning.preview(admin(), new CreateDispatchPlanRequest(List.of(ids.get(0), ids.get(0)), true)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	void eachHospitalApprovesItsOwnOrderAfterNetworkPreview() {
		UUID secondHospital = jdbc.queryForObject("select id from organizations where facility_code = 'HSP-002'", UUID.class);
		jdbc.update("""
				insert into users (id, organization_id, full_name, email, password_hash, role, active, created_at, updated_at)
				select ?, ?, 'Second planning officer', 'planning.second@hemogrid.local', password_hash,
				       'HOSPITAL_STAFF', true, now(), now() from users where email = 'hospital.demo@hemogrid.local'
				""", UUID.randomUUID(), secondHospital);
		Jwt first = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		Jwt second = login("planning.second@hemogrid.local", "HospitalDemo123!");
		var a = requests.createRequest(first, new CreateBloodRequestRequest(BloodGroup.A_POSITIVE, BloodComponent.RED_CELLS,
				3, RequestUrgency.CRITICAL, "PLAN-HOSPITAL-A", "Synthetic planning fixture"));
		var b = requests.createRequest(second, new CreateBloodRequestRequest(BloodGroup.O_NEGATIVE, BloodComponent.RED_CELLS,
				3, RequestUrgency.CRITICAL, "PLAN-HOSPITAL-B", "Synthetic planning fixture"));
		var result = planning.preview(admin(), new CreateDispatchPlanRequest(List.of(a.id(), b.id()), true));
		assertThat(result.proposals()).hasSize(2);
		assertThatThrownBy(() -> requests.selectProvider(first, b.id(), new SelectProviderRequest(NAIROBI)))
				.isInstanceOf(BusinessException.class);
		assertThat(requests.selectProvider(second, b.id(), new SelectProviderRequest(result.proposals().get(1).providerOrganizationId())).provider()).isNotNull();
	}

	private List<UUID> createRequests(Jwt hospital) {
		var flexible = requests.createRequest(hospital, new CreateBloodRequestRequest(BloodGroup.A_POSITIVE, BloodComponent.RED_CELLS,
				3, RequestUrgency.CRITICAL, "PLAN-A", "Synthetic planning fixture"));
		var constrained = requests.createRequest(hospital, new CreateBloodRequestRequest(BloodGroup.O_NEGATIVE, BloodComponent.RED_CELLS,
				3, RequestUrgency.CRITICAL, "PLAN-B", "Synthetic planning fixture"));
		return List.of(flexible.id(), constrained.id());
	}

	private void seedStock(UUID provider, String group) {
		jdbc.update("""
				insert into blood_inventory (id, organization_id, blood_group, component, units_available, units_reserved, version, created_at, updated_at)
				values (?, ?, ?, 'RED_CELLS', 3, 0, 0, now(), now())
				on conflict (organization_id, blood_group, component) do update set units_available = 3
				""", UUID.randomUUID(), provider, group);
	}

	private Jwt admin() {
		return login("admin.demo@hemogrid.local", "AdminDemo123!");
	}

	private Jwt login(String email, String password) {
		return jwtDecoder.decode(authService.login(new LoginRequest(email, password)).accessToken());
	}
}
