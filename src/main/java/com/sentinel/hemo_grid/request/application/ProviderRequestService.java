package com.sentinel.hemo_grid.request.application;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.auth.domain.AppUser;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
import com.sentinel.hemo_grid.request.api.UpdateRequestStatusRequest;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.RequestStatus;
import com.sentinel.hemo_grid.request.persistence.BloodRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderRequestService {

	private final AuthService authService;
	private final BloodRequestRepository requestRepository;
	private final BloodInventoryRepository inventoryRepository;

	public ProviderRequestService(
			AuthService authService,
			BloodRequestRepository requestRepository,
			BloodInventoryRepository inventoryRepository
	) {
		this.authService = authService;
		this.requestRepository = requestRepository;
		this.inventoryRepository = inventoryRepository;
	}

	@Transactional(readOnly = true)
	public List<BloodRequestResponse> listProviderRequests(Jwt jwt) {
		AppUser user = authService.requireBloodBankUser(jwt);
		return requestRepository.findByProviderOrganizationIdOrderByRequestedAtDesc(user.getOrganization().getId())
				.stream()
				.map(BloodRequestResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public BloodRequestResponse getProviderRequest(Jwt jwt, UUID requestId) {
		return BloodRequestResponse.from(requireProviderRequest(jwt, requestId));
	}

	@Transactional
	public BloodRequestResponse accept(Jwt jwt, UUID requestId) {
		BloodRequest request = requireProviderRequest(jwt, requestId);
		if (request.getStatus() == RequestStatus.ACCEPTED || request.getStatus() == RequestStatus.PREPARING
				|| request.getStatus() == RequestStatus.IN_TRANSIT || request.getStatus() == RequestStatus.DELIVERED) {
			return BloodRequestResponse.from(request);
		}
		if (request.getStatus() != RequestStatus.REQUESTED) {
			throw invalidStatus("Only REQUESTED requests can be accepted.");
		}

		BloodInventory inventory = lockMatchingInventory(request);
		if (inventory.unitsFree() < request.getUnitsRequired()) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					ErrorCode.INSUFFICIENT_INVENTORY,
					"The selected blood bank no longer has enough free units to accept this request."
			);
		}

		inventory.reserve(request.getUnitsRequired());
		request.accept();
		return BloodRequestResponse.from(request);
	}

	@Transactional
	public BloodRequestResponse decline(Jwt jwt, UUID requestId) {
		BloodRequest request = requireProviderRequest(jwt, requestId);
		if (request.getStatus() == RequestStatus.DECLINED) {
			return BloodRequestResponse.from(request);
		}
		if (request.getStatus() != RequestStatus.REQUESTED) {
			throw invalidStatus("Only REQUESTED requests can be declined.");
		}

		request.decline();
		return BloodRequestResponse.from(request);
	}

	@Transactional
	public BloodRequestResponse updateStatus(Jwt jwt, UUID requestId, UpdateRequestStatusRequest statusRequest) {
		BloodRequest request = requireProviderRequest(jwt, requestId);
		RequestStatus nextStatus = statusRequest.status();
		if (nextStatus == request.getStatus()) {
			return BloodRequestResponse.from(request);
		}
		if (nextStatus != RequestStatus.PREPARING && nextStatus != RequestStatus.IN_TRANSIT && nextStatus != RequestStatus.DELIVERED) {
			throw invalidStatus("Provider status updates can only move to PREPARING, IN_TRANSIT, or DELIVERED.");
		}

		if (nextStatus == RequestStatus.DELIVERED) {
			try {
				request.progressTo(nextStatus);
				lockMatchingInventory(request).consumeReservation(request.getUnitsRequired());
				return BloodRequestResponse.from(request);
			}
			catch (IllegalStateException exception) {
				throw invalidTransition();
			}
		}

		try {
			request.progressTo(nextStatus);
			return BloodRequestResponse.from(request);
		}
		catch (IllegalStateException exception) {
			throw invalidTransition();
		}
	}

	private BloodRequest requireProviderRequest(Jwt jwt, UUID requestId) {
		AppUser user = authService.requireBloodBankUser(jwt);
		return requestRepository.findByIdAndProviderOrganizationId(requestId, user.getOrganization().getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Provider request not found."));
	}

	private BloodInventory lockMatchingInventory(BloodRequest request) {
		return inventoryRepository.lockByOrganizationIdAndBloodGroupAndComponent(
						request.getProviderOrganization().getId(),
						request.getBloodGroup(),
						request.getComponent()
				)
				.orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, ErrorCode.INSUFFICIENT_INVENTORY, "Matching inventory row not found."));
	}

	private BusinessException invalidStatus(String message) {
		return new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_REQUEST_STATUS, message);
	}

	private BusinessException invalidTransition() {
		return new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATUS_TRANSITION, "Invalid request status transition.");
	}
}
