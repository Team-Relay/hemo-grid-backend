/* BloodRequestController is the authenticated HTTP boundary for request operations. */

package com.sentinel.hemo_grid.request.api;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.request.application.BloodRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blood-requests")
@Tag(name = "Blood Requests")
@SecurityRequirement(name = "bearerAuth")
public class BloodRequestController {

	private final BloodRequestService bloodRequestService;

	public BloodRequestController(BloodRequestService bloodRequestService) {
		this.bloodRequestService = bloodRequestService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a hospital blood request")
	@ApiResponse(responseCode = "201", description = "Blood request created")
	@ApiResponse(responseCode = "400", description = "Validation failed")
	@ApiResponse(responseCode = "403", description = "Only hospital users can create requests")
	public BloodRequestResponse createRequest(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateBloodRequestRequest request
	) {
		return bloodRequestService.createRequest(jwt, request);
	}

	@GetMapping
	@Operation(summary = "List blood requests for the authenticated hospital")
	public List<BloodRequestResponse> listRequests(@AuthenticationPrincipal Jwt jwt) {
		return bloodRequestService.listRequests(jwt);
	}

	@GetMapping("/{requestId}")
	@Operation(summary = "Get one hospital blood request")
	@ApiResponse(responseCode = "404", description = "Request not found")
	public BloodRequestResponse getRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return bloodRequestService.getRequest(jwt, requestId);
	}

	@GetMapping("/{requestId}/candidates")
	@Operation(summary = "List ranked provider candidates for a blood request")
	@ApiResponse(responseCode = "404", description = "Request not found")
	public CandidateListResponse getCandidates(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return bloodRequestService.getCandidates(jwt, requestId);
	}

	@PostMapping("/{requestId}/select-provider")
	@Operation(summary = "Select a blood-bank provider for a request")
	@ApiResponse(responseCode = "200", description = "Provider selected")
	@ApiResponse(responseCode = "404", description = "Request or provider not found")
	@ApiResponse(responseCode = "409", description = "Invalid request status or provider is not a candidate")
	public BloodRequestResponse selectProvider(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID requestId,
			@Valid @RequestBody SelectProviderRequest request
	) {
		return bloodRequestService.selectProvider(jwt, requestId, request);
	}

	@PostMapping("/{requestId}/cancel")
	@Operation(summary = "Cancel a hospital blood request while cancellation is still allowed")
	@ApiResponse(responseCode = "200", description = "Request cancelled")
	@ApiResponse(responseCode = "404", description = "Request not found")
	@ApiResponse(responseCode = "409", description = "Request cannot be cancelled")
	public BloodRequestResponse cancelRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return bloodRequestService.cancelRequest(jwt, requestId);
	}
}
