package com.sentinel.hemo_grid.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.inventory.api.InventoryResponse;
import com.sentinel.hemo_grid.inventory.api.UpdateInventoryRequest;
import com.sentinel.hemo_grid.inventory.application.InventoryService;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
import com.sentinel.hemo_grid.request.api.CreateBloodRequestRequest;
import com.sentinel.hemo_grid.request.api.SelectProviderRequest;
import com.sentinel.hemo_grid.request.api.UpdateRequestStatusRequest;
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
class ProviderRequestServiceIntegrationTests {

	private static final UUID MAITAMA_BANK_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID WUSE_BANK_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final UUID O_NEGATIVE_MAITAMA_INVENTORY_ID = UUID.fromString("30000000-0000-0000-0000-000000000008");

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private BloodRequestService bloodRequestService;

	@Autowired
	private ProviderRequestService providerRequestService;

	@Autowired
	private InventoryService inventoryService;

	@Test
	void acceptingSelectedRequestReservesInventory() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		BloodRequestResponse selected = createAndSelect(hospitalJwt, MAITAMA_BANK_ID);

		BloodRequestResponse accepted = providerRequestService.accept(bankJwt, selected.id());

		assertThat(accepted.status()).isEqualTo(RequestStatus.ACCEPTED);
		assertThat(accepted.acceptedAt()).isNotNull();
		InventoryResponse inventory = getMaitamaONegativeInventory(bankJwt);
		assertThat(inventory.unitsAvailable()).isEqualTo(5);
		assertThat(inventory.unitsReserved()).isEqualTo(3);
		assertThat(inventory.unitsFree()).isEqualTo(2);
	}

	@Test
	void deliveryConsumesAvailableAndReservedInventory() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		BloodRequestResponse selected = createAndSelect(hospitalJwt, MAITAMA_BANK_ID);
		providerRequestService.accept(bankJwt, selected.id());

		providerRequestService.updateStatus(bankJwt, selected.id(), new UpdateRequestStatusRequest(RequestStatus.PREPARING));
		providerRequestService.updateStatus(bankJwt, selected.id(), new UpdateRequestStatusRequest(RequestStatus.IN_TRANSIT));
		BloodRequestResponse delivered = providerRequestService.updateStatus(
				bankJwt,
				selected.id(),
				new UpdateRequestStatusRequest(RequestStatus.DELIVERED)
		);

		assertThat(delivered.status()).isEqualTo(RequestStatus.DELIVERED);
		assertThat(delivered.deliveredAt()).isNotNull();
		InventoryResponse inventory = getMaitamaONegativeInventory(bankJwt);
		assertThat(inventory.unitsAvailable()).isEqualTo(2);
		assertThat(inventory.unitsReserved()).isZero();
		assertThat(inventory.unitsFree()).isEqualTo(2);
	}

	@Test
	void hospitalCancellationAfterAcceptanceReleasesReservation() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		BloodRequestResponse selected = createAndSelect(hospitalJwt, MAITAMA_BANK_ID);
		providerRequestService.accept(bankJwt, selected.id());

		BloodRequestResponse cancelled = bloodRequestService.cancelRequest(hospitalJwt, selected.id());

		assertThat(cancelled.status()).isEqualTo(RequestStatus.CANCELLED);
		assertThat(cancelled.cancelledAt()).isNotNull();
		InventoryResponse inventory = getMaitamaONegativeInventory(bankJwt);
		assertThat(inventory.unitsAvailable()).isEqualTo(5);
		assertThat(inventory.unitsReserved()).isZero();
		assertThat(inventory.unitsFree()).isEqualTo(5);
	}

	@Test
	void anotherBloodBankCannotAcceptRequestAssignedElsewhere() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		BloodRequestResponse selected = createAndSelect(hospitalJwt, WUSE_BANK_ID);

		assertThatThrownBy(() -> providerRequestService.accept(bankJwt, selected.id()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Provider request not found.");
	}

	@Test
	void acceptRejectsInsufficientInventory() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		inventoryService.updateUnitsAvailable(bankJwt, O_NEGATIVE_MAITAMA_INVENTORY_ID, new UpdateInventoryRequest(2));
		BloodRequestResponse selected = createAndSelect(hospitalJwt, MAITAMA_BANK_ID);

		assertThatThrownBy(() -> providerRequestService.accept(bankJwt, selected.id()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("The selected blood bank no longer has enough free units to accept this request.");
	}

	private BloodRequestResponse createAndSelect(Jwt hospitalJwt, UUID providerId) {
		BloodRequestResponse created = bloodRequestService.createRequest(hospitalJwt, new CreateBloodRequestRequest(
				BloodGroup.O_NEGATIVE,
				BloodComponent.RED_CELLS,
				3,
				RequestUrgency.CRITICAL,
				"ER-2026-0821-001",
				"Emergency request"
		));
		return bloodRequestService.selectProvider(hospitalJwt, created.id(), new SelectProviderRequest(providerId));
	}

	private InventoryResponse getMaitamaONegativeInventory(Jwt bankJwt) {
		return inventoryService.listInventory(bankJwt)
				.stream()
				.filter(inventory -> inventory.id().equals(O_NEGATIVE_MAITAMA_INVENTORY_ID))
				.findFirst()
				.orElseThrow();
	}

	private Jwt login(String email, String password) {
		String token = authService.login(new LoginRequest(email, password)).accessToken();
		return jwtDecoder.decode(token);
	}
}
