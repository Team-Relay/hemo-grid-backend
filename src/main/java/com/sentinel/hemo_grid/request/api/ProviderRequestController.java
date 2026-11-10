package com.sentinel.hemo_grid.request.api;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.request.application.ProviderRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider/requests")
@Tag(name = "Provider Requests")
@SecurityRequirement(name = "bearerAuth")
public class ProviderRequestController {

	private final ProviderRequestService providerRequestService;

	public ProviderRequestController(ProviderRequestService providerRequestService) {
		this.providerRequestService = providerRequestService;
	}

	@GetMapping
	@Operation(summary = "List requests assigned to the authenticated blood bank")
	public List<BloodRequestResponse> listProviderRequests(@AuthenticationPrincipal Jwt jwt) {
		return providerRequestService.listProviderRequests(jwt);
	}

	@GetMapping("/{requestId}")
	@Operation(summary = "Get one request assigned to the authenticated blood bank")
	@ApiResponse(responseCode = "404", description = "Provider request not found")
	public BloodRequestResponse getProviderRequest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return providerRequestService.getProviderRequest(jwt, requestId);
	}

	@PostMapping("/{requestId}/accept")
	@Operation(summary = "Accept a selected request and reserve inventory")
	@ApiResponse(responseCode = "200", description = "Request accepted")
	@ApiResponse(responseCode = "409", description = "Invalid state or insufficient inventory")
	public BloodRequestResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return providerRequestService.accept(jwt, requestId);
	}

	@PostMapping("/{requestId}/decline")
	@Operation(summary = "Decline a selected request")
	@ApiResponse(responseCode = "200", description = "Request declined")
	@ApiResponse(responseCode = "409", description = "Invalid state")
	public BloodRequestResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
		return providerRequestService.decline(jwt, requestId);
	}

	@PostMapping("/{requestId}/status")
	@Operation(summary = "Progress an accepted request through fulfilment statuses")
	@ApiResponse(responseCode = "200", description = "Request status updated")
	@ApiResponse(responseCode = "409", description = "Invalid transition")
	public BloodRequestResponse updateStatus(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID requestId,
			@Valid @RequestBody UpdateRequestStatusRequest request
	) {
		return providerRequestService.updateStatus(jwt, requestId, request);
	}
}
