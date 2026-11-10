package com.sentinel.hemo_grid.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class ApiFlowIntegrationTests {

	private static final String MAITAMA_BLOOD_CENTRE_ID = "22222222-2222-2222-2222-222222222222";
	private static final String O_NEGATIVE_MAITAMA_INVENTORY_ID = "30000000-0000-0000-0000-000000000008";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void healthAndAuthBoundariesUseExpectedHttpStatusCodes() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));

		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("Authentication is required to access this resource."));
	}

	@Test
	void frontendDemoFlowWorksThroughHttpEndpoints() throws Exception {
		String hospitalToken = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
		String bankToken = login("bank.demo@hemogrid.local", "BankDemo123!");

		String requestId = createHospitalRequest(hospitalToken);

		mockMvc.perform(get("/api/v1/blood-requests/{requestId}/candidates", requestId)
						.header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.requestId").value(requestId))
				.andExpect(jsonPath("$.candidates", hasSize(3)))
				.andExpect(jsonPath("$.candidates[0].organizationName").value("Maitama Blood Centre"))
				.andExpect(jsonPath("$.candidates[0].canFullyFulfil").value(true));

		mockMvc.perform(post("/api/v1/blood-requests/{requestId}/select-provider", requestId)
						.header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("providerOrganizationId", MAITAMA_BLOOD_CENTRE_ID))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andExpect(jsonPath("$.provider.id").value(MAITAMA_BLOOD_CENTRE_ID));

		mockMvc.perform(get("/api/v1/provider/requests")
						.header(HttpHeaders.AUTHORIZATION, bearer(bankToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(requestId))
				.andExpect(jsonPath("$[0].provider.id").value(MAITAMA_BLOOD_CENTRE_ID));

		mockMvc.perform(post("/api/v1/provider/requests/{requestId}/accept", requestId)
						.header(HttpHeaders.AUTHORIZATION, bearer(bankToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.acceptedAt").isNotEmpty());

		JsonNode reservedInventory = getMaitamaONegativeInventory(bankToken);
		assertThat(reservedInventory.get("unitsAvailable").asInt()).isEqualTo(5);
		assertThat(reservedInventory.get("unitsReserved").asInt()).isEqualTo(3);
		assertThat(reservedInventory.get("unitsFree").asInt()).isEqualTo(2);

		updateProviderStatus(bankToken, requestId, "PREPARING");
		updateProviderStatus(bankToken, requestId, "IN_TRANSIT");

		mockMvc.perform(post("/api/v1/provider/requests/{requestId}/status", requestId)
						.header(HttpHeaders.AUTHORIZATION, bearer(bankToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("status", "DELIVERED"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DELIVERED"))
				.andExpect(jsonPath("$.deliveredAt").isNotEmpty());

		JsonNode deliveredInventory = getMaitamaONegativeInventory(bankToken);
		assertThat(deliveredInventory.get("unitsAvailable").asInt()).isEqualTo(2);
		assertThat(deliveredInventory.get("unitsReserved").asInt()).isZero();
		assertThat(deliveredInventory.get("unitsFree").asInt()).isEqualTo(2);

		mockMvc.perform(get("/api/v1/blood-requests/{requestId}", requestId)
						.header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DELIVERED"));
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("email", email, "password", password))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
	}

	private String createHospitalRequest(String hospitalToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/blood-requests")
						.header(HttpHeaders.AUTHORIZATION, bearer(hospitalToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of(
								"bloodGroup", "O_NEGATIVE",
								"component", "RED_CELLS",
								"unitsRequired", 3,
								"urgency", "CRITICAL",
								"clinicalReference", "ER-2026-0821-HTTP",
								"notes", "Emergency request from HTTP integration test"
						))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andExpect(jsonPath("$.requester.name").value("Central Care Hospital"))
				.andExpect(jsonPath("$.provider").doesNotExist())
				.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
	}

	private void updateProviderStatus(String bankToken, String requestId, String nextStatus) throws Exception {
		mockMvc.perform(post("/api/v1/provider/requests/{requestId}/status", requestId)
						.header(HttpHeaders.AUTHORIZATION, bearer(bankToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("status", nextStatus))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(nextStatus));
	}

	private JsonNode getMaitamaONegativeInventory(String bankToken) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/inventory")
						.header(HttpHeaders.AUTHORIZATION, bearer(bankToken)))
				.andExpect(status().isOk())
				.andReturn();

		for (JsonNode inventory : objectMapper.readTree(result.getResponse().getContentAsString())) {
			if (O_NEGATIVE_MAITAMA_INVENTORY_ID.equals(inventory.get("id").asText())) {
				return inventory;
			}
		}
		throw new AssertionError("Expected Maitama O_NEGATIVE inventory row in response.");
	}

	private String json(Object value) throws Exception {
		return objectMapper.writeValueAsString(value);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
