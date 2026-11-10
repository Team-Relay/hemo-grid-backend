package com.sentinel.hemo_grid.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.inventory.api.InventoryResponse;
import com.sentinel.hemo_grid.inventory.api.UpdateInventoryRequest;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
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
class InventoryServiceIntegrationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private InventoryService inventoryService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void bloodBankUserCanReadOwnInventory() {
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");

		List<InventoryResponse> inventory = inventoryService.listInventory(bankJwt);

		assertThat(inventory).hasSize(8);
		assertThat(inventory)
				.anySatisfy(row -> {
					assertThat(row.bloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
					assertThat(row.component()).isEqualTo(BloodComponent.RED_CELLS);
					assertThat(row.unitsAvailable()).isEqualTo(5);
					assertThat(row.unitsReserved()).isZero();
					assertThat(row.unitsFree()).isEqualTo(5);
				});
	}

	@Test
	void bloodBankUserCanUpdateAvailableUnits() {
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		UUID inventoryId = UUID.fromString("30000000-0000-0000-0000-000000000008");

		InventoryResponse response = inventoryService.updateUnitsAvailable(bankJwt, inventoryId, new UpdateInventoryRequest(9));

		assertThat(response.unitsAvailable()).isEqualTo(9);
		assertThat(response.unitsReserved()).isZero();
		assertThat(response.unitsFree()).isEqualTo(9);
	}

	@Test
	void negativeAvailableUnitsAreRejected() {
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		UUID inventoryId = UUID.fromString("30000000-0000-0000-0000-000000000008");

		assertThatThrownBy(() -> inventoryService.updateUnitsAvailable(bankJwt, inventoryId, new UpdateInventoryRequest(-1)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Units available cannot be negative.");
	}

	@Test
	void hospitalUserCannotAccessInventory() {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");

		assertThatThrownBy(() -> inventoryService.listInventory(hospitalJwt))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Only blood-bank users can access inventory.");
	}

	private Jwt login(String email, String password) {
		String token = authService.login(new LoginRequest(email, password)).accessToken();
		return jwtDecoder.decode(token);
	}
}
