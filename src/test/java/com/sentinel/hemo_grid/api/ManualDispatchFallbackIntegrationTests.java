package com.sentinel.hemo_grid.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.planning.application.DispatchPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The coordinator's optional decision engine may fail while the authenticated
 * hospital -> bank transaction remains operational. This runs against real
 * PostgreSQL test inventory and real authorization/acceptance HTTP endpoints.
 *
 * No live demo stock is used. @Sql resets seeded test records before execution.
 */
@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class ManualDispatchFallbackIntegrationTests {

  private static final String BANK_ID = "22222222-2222-2222-2222-222222222222";
  private static final String INVENTORY_ID = "30000000-0000-0000-0000-000000000008";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManager entityManager;

  // Deliberate failure affects only the coordinator solver; it does NOT replace
  // real hospital provider selection, bank acceptance or stock validation.
  @MockitoBean private DispatchPlanService planner;

  @Test
  void plannerServiceUnavailableStillAllowsManualHospitalAndBankLifecycle() throws Exception {
    String hospital = login("hospital.demo@hemogrid.local", "HospitalDemo123!");
    String bank = login("bank.demo@hemogrid.local", "BankDemo123!");
    String admin = login("admin.demo@hemogrid.local", "AdminDemo123!");

    String first = create(hospital, "FALLBACK-TEST-001", 3);
    String second = create(hospital, "FALLBACK-TEST-002", 1);

    int unitsBefore = inventoryValue("units_available");
    int reservedBefore = inventoryValue("units_reserved");
    assertThat(unitsBefore).isEqualTo(5);
    assertThat(reservedBefore).isZero();

    when(planner.preview(any(), any())).thenThrow(new BusinessException(
        HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_ERROR,
        "Joint planning is temporarily unavailable; use an authorized individual workflow."));

    // A real coordinator preview fails. Nothing was approved or reserved.
    mvc.perform(post("/api/v1/dispatch-plans/preview")
            .header(HttpHeaders.AUTHORIZATION, bearer(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("requestIds", List.of(first, second), "staffApproved", true))))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value(
            "Joint planning is temporarily unavailable; use an authorized individual workflow."));
    assertThat(inventoryValue("units_available")).isEqualTo(unitsBefore);
    assertThat(inventoryValue("units_reserved")).isZero();

    // Hospital can still examine server-generated candidates and select its
    // own provider, without approval from the failed coordinator batch planner.
    mvc.perform(get("/api/v1/blood-requests/{id}/candidates", first)
            .header(HttpHeaders.AUTHORIZATION, bearer(hospital)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.candidates[0].canFullyFulfil").value(true));

    mvc.perform(post("/api/v1/blood-requests/{id}/select-provider", first)
            .header(HttpHeaders.AUTHORIZATION, bearer(hospital))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("providerOrganizationId", BANK_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REQUESTED"))
        .andExpect(jsonPath("$.provider.id").value(BANK_ID));
    assertThat(inventoryValue("units_reserved")).isZero();

    // Only the supplying blood bank can approve and reserve its stock.
    mvc.perform(post("/api/v1/provider/requests/{id}/accept", first)
            .header(HttpHeaders.AUTHORIZATION, bearer(bank)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.acceptedAt").isNotEmpty());
    assertThat(inventoryValue("units_available")).isEqualTo(5);
    assertThat(inventoryValue("units_reserved")).isEqualTo(3);

    advance(bank, first, "PREPARING", "preparingAt");
    advance(bank, first, "IN_TRANSIT", "dispatchedAt");
    advance(bank, first, "DELIVERED", "deliveredAt");

    // Inventory and timestamped request milestones form a checkable digital
    // state trail. A separate per-actor audit log is NOT claimed here.
    assertThat(inventoryValue("units_available")).isEqualTo(2);
    assertThat(inventoryValue("units_reserved")).isZero();
    mvc.perform(get("/api/v1/blood-requests/{id}", first)
            .header(HttpHeaders.AUTHORIZATION, bearer(hospital)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DELIVERED"))
        .andExpect(jsonPath("$.acceptedAt").isNotEmpty())
        .andExpect(jsonPath("$.preparingAt").isNotEmpty())
        .andExpect(jsonPath("$.dispatchedAt").isNotEmpty())
        .andExpect(jsonPath("$.deliveredAt").isNotEmpty());
    mvc.perform(get("/api/v1/blood-requests/{id}", second)
            .header(HttpHeaders.AUTHORIZATION, bearer(hospital)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REQUESTED"));
  }

  private String create(String token, String ref, int units) throws Exception {
    var response = mvc.perform(post("/api/v1/blood-requests")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of(
                "bloodGroup", "O_NEGATIVE",
                "component", "RED_CELLS",
                "unitsRequired", units,
                "urgency", "CRITICAL",
                "clinicalReference", ref,
                "notes", "Synthetic fallback integration test only"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("REQUESTED"))
        .andReturn();
    return json.readTree(response.getResponse().getContentAsString()).get("id").asText();
  }

  private void advance(String bank, String id, String next, String timestamp) throws Exception {
    mvc.perform(post("/api/v1/provider/requests/{id}/status", id)
            .header(HttpHeaders.AUTHORIZATION, bearer(bank))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("status", next))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(next))
        .andExpect(jsonPath("$." + timestamp).isNotEmpty());
  }

  private int inventoryValue(String column) {
    // Persist JPA reservation changes before inspecting the same test transaction via JDBC.
    entityManager.flush();
    if (!column.equals("units_available") && !column.equals("units_reserved")) {
      throw new IllegalArgumentException("Unexpected inventory column");
    }
    return jdbc.queryForObject(
        "select " + column + " from blood_inventory where id = ?",
        Integer.class, java.util.UUID.fromString(INVENTORY_ID));
  }

  private String login(String email, String password) throws Exception {
    var response = mvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("email", email, "password", password))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andReturn();
    JsonNode parsed = json.readTree(response.getResponse().getContentAsString());
    return parsed.get("accessToken").asText();
  }

  private String body(Object value) throws Exception { return json.writeValueAsString(value); }
  private String bearer(String token) { return "Bearer " + token; }
}
