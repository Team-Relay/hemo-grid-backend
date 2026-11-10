package com.sentinel.hemo_grid.planning.api;

import java.util.List;

import com.sentinel.hemo_grid.planning.application.DispatchPlanService;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dispatch-plans")
@Tag(name = "Dispatch Planning")
@SecurityRequirement(name = "bearerAuth")
public class DispatchPlanController {
	private final DispatchPlanService service;

	public DispatchPlanController(DispatchPlanService service) {
		this.service = service;
	}

	@GetMapping("/requests")
	@Operation(summary = "List up to 100 pending critical red-cell requests for a platform coordinator")
	public List<BloodRequestResponse> requests(@AuthenticationPrincipal Jwt jwt) {
		return service.pendingRequests(jwt);
	}

	@PostMapping("/preview")
	@Operation(summary = "Propose suppliers for two to five staff-approved requests using current shared stock")
	public DispatchPlanResponse preview(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateDispatchPlanRequest request) {
		return service.preview(jwt, request);
	}
}
