/* Proves concurrent acceptance cannot reserve the same inventory twice. */

package com.sentinel.hemo_grid.request.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.sentinel.hemo_grid.auth.api.LoginRequest;
import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.inventory.api.InventoryResponse;
import com.sentinel.hemo_grid.inventory.application.InventoryService;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
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

/** Proves concurrent HTTP-equivalent service calls cannot double-reserve one request. */
@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
@Sql(
		scripts = "/sql/reset-test-data.sql",
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
		scripts = "/sql/reset-test-data.sql",
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class ProviderRequestConcurrencyIntegrationTests {

	private static final UUID MAITAMA_BANK_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
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
	void simultaneousAcceptsReserveOneRequestExactlyOnce() throws Exception {
		Jwt hospitalJwt = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		Jwt bankJwt = login("bank.demo@hemogrid.local", "BankDemo123!");
		BloodRequestResponse created = bloodRequestService.createRequest(
				hospitalJwt,
				new CreateBloodRequestRequest(
						BloodGroup.O_NEGATIVE,
						BloodComponent.RED_CELLS,
						3,
						RequestUrgency.CRITICAL,
						"CONCURRENCY-ACCEPT",
						"Concurrent acceptance regression test"
				)
		);
		BloodRequestResponse selected = bloodRequestService.selectProvider(
				hospitalJwt,
				created.id(),
				new SelectProviderRequest(MAITAMA_BANK_ID)
		);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<BloodRequestResponse> first = executor.submit(
					() -> acceptTogether(bankJwt, selected.id(), ready, start)
			);
			Future<BloodRequestResponse> second = executor.submit(
					() -> acceptTogether(bankJwt, selected.id(), ready, start)
			);

			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			assertThat(first.get(10, TimeUnit.SECONDS).status()).isEqualTo(RequestStatus.ACCEPTED);
			assertThat(second.get(10, TimeUnit.SECONDS).status()).isEqualTo(RequestStatus.ACCEPTED);
		}
		finally {
			executor.shutdownNow();
		}

		InventoryResponse inventory = inventoryService.listInventory(bankJwt)
				.stream()
				.filter(row -> row.id().equals(O_NEGATIVE_MAITAMA_INVENTORY_ID))
				.findFirst()
				.orElseThrow();
		assertThat(inventory.unitsReserved()).isEqualTo(3);
		assertThat(inventory.unitsFree()).isEqualTo(2);
	}

	private BloodRequestResponse acceptTogether(
			Jwt jwt,
			UUID requestId,
			CountDownLatch ready,
			CountDownLatch start
	) throws InterruptedException {
		ready.countDown();
		assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
		return providerRequestService.accept(jwt, requestId);
	}

	private Jwt login(String email, String password) {
		String token = authService.login(new LoginRequest(email, password)).accessToken();
		return jwtDecoder.decode(token);
	}
}
