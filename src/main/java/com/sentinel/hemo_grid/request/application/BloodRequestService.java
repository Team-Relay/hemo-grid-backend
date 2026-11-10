package com.sentinel.hemo_grid.request.application;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.auth.domain.AppUser;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.matching.application.MatchingService;
import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import com.sentinel.hemo_grid.organization.persistence.OrganizationRepository;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
import com.sentinel.hemo_grid.request.api.CandidateListResponse;
import com.sentinel.hemo_grid.request.api.CandidateResponse;
import com.sentinel.hemo_grid.request.api.CreateBloodRequestRequest;
import com.sentinel.hemo_grid.request.api.SelectProviderRequest;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.RequestCandidate;
import com.sentinel.hemo_grid.request.domain.RequestStatus;
import com.sentinel.hemo_grid.request.persistence.BloodRequestRepository;
import com.sentinel.hemo_grid.request.persistence.RequestCandidateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BloodRequestService {

	private final AuthService authService;
	private final BloodRequestRepository requestRepository;
	private final RequestCandidateRepository candidateRepository;
	private final OrganizationRepository organizationRepository;
	private final BloodInventoryRepository inventoryRepository;
	private final MatchingService matchingService;

	public BloodRequestService(
			AuthService authService,
			BloodRequestRepository requestRepository,
			RequestCandidateRepository candidateRepository,
			OrganizationRepository organizationRepository,
			BloodInventoryRepository inventoryRepository,
			MatchingService matchingService
	) {
		this.authService = authService;
		this.requestRepository = requestRepository;
		this.candidateRepository = candidateRepository;
		this.organizationRepository = organizationRepository;
		this.inventoryRepository = inventoryRepository;
		this.matchingService = matchingService;
	}

	@Transactional
	public BloodRequestResponse createRequest(Jwt jwt, CreateBloodRequestRequest request) {
		AppUser user = authService.requireHospitalUser(jwt);
		BloodRequest bloodRequest = BloodRequest.create(
				user,
				request.bloodGroup(),
				request.component(),
				request.unitsRequired(),
				request.urgency(),
				blankToNull(request.clinicalReference()),
				blankToNull(request.notes())
		);

		BloodRequest savedRequest = requestRepository.save(bloodRequest);
		matchingService.createCandidates(savedRequest);
		return BloodRequestResponse.from(savedRequest);
	}

	@Transactional(readOnly = true)
	public List<BloodRequestResponse> listRequests(Jwt jwt) {
		AppUser user = authService.requireHospitalUser(jwt);
		return requestRepository.findByRequesterOrganizationIdOrderByRequestedAtDesc(user.getOrganization().getId())
				.stream()
				.map(BloodRequestResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public BloodRequestResponse getRequest(Jwt jwt, UUID requestId) {
		return BloodRequestResponse.from(requireOwnedRequest(jwt, requestId));
	}

	@Transactional(readOnly = true)
	public CandidateListResponse getCandidates(Jwt jwt, UUID requestId) {
		BloodRequest request = requireOwnedRequest(jwt, requestId);
		List<CandidateResponse> candidates = candidateRepository.findByBloodRequestIdOrderByRankPositionAsc(request.getId())
				.stream()
				.map(CandidateResponse::from)
				.toList();
		return new CandidateListResponse(request.getId(), candidates);
	}

	@Transactional
	public BloodRequestResponse selectProvider(Jwt jwt, UUID requestId, SelectProviderRequest request) {
		BloodRequest bloodRequest = requireOwnedRequest(jwt, requestId);
		if (bloodRequest.getStatus() != RequestStatus.REQUESTED) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					ErrorCode.INVALID_REQUEST_STATUS,
					"Provider can only be selected while the request is REQUESTED."
			);
		}

		Organization provider = organizationRepository.findById(request.providerOrganizationId())
				.filter(Organization::isActive)
				.filter(organization -> organization.getOrganizationType() == OrganizationType.BLOOD_BANK)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Provider organization not found."));

		boolean candidateExists = candidateRepository.existsByBloodRequestIdAndProviderOrganizationId(
				bloodRequest.getId(),
				provider.getId()
		);
		if (!candidateExists) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					ErrorCode.RESOURCE_NOT_FOUND,
					"The selected provider is not a candidate for this request."
			);
		}

		boolean canStillFullyFulfil = inventoryRepository.findByOrganizationIdAndBloodGroupAndComponent(
						provider.getId(),
						bloodRequest.getBloodGroup(),
						bloodRequest.getComponent()
				)
				.map(inventory -> inventory.unitsFree() >= bloodRequest.getUnitsRequired())
				.orElse(false);
		if (!canStillFullyFulfil) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					ErrorCode.INSUFFICIENT_INVENTORY,
					"The selected provider no longer has enough matching free inventory."
			);
		}

		bloodRequest.selectProvider(provider);
		return BloodRequestResponse.from(bloodRequest);
	}

	@Transactional
	public BloodRequestResponse cancelRequest(Jwt jwt, UUID requestId) {
		BloodRequest bloodRequest = requireOwnedRequest(jwt, requestId);
		if (bloodRequest.getStatus() != RequestStatus.REQUESTED
				&& bloodRequest.getStatus() != RequestStatus.ACCEPTED
				&& bloodRequest.getStatus() != RequestStatus.PREPARING) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					ErrorCode.INVALID_REQUEST_STATUS,
					"Request cannot be cancelled in its current status."
			);
		}
		if (bloodRequest.getStatus() == RequestStatus.ACCEPTED || bloodRequest.getStatus() == RequestStatus.PREPARING) {
			inventoryRepository.lockByOrganizationIdAndBloodGroupAndComponent(
							bloodRequest.getProviderOrganization().getId(),
							bloodRequest.getBloodGroup(),
							bloodRequest.getComponent()
					)
					.orElseThrow(() -> new BusinessException(
							HttpStatus.CONFLICT,
							ErrorCode.INSUFFICIENT_INVENTORY,
							"Matching inventory row not found."
					))
					.releaseReservation(bloodRequest.getUnitsRequired());
		}
		bloodRequest.cancel();
		return BloodRequestResponse.from(bloodRequest);
	}

	private BloodRequest requireOwnedRequest(Jwt jwt, UUID requestId) {
		AppUser user = authService.requireHospitalUser(jwt);
		return requestRepository.findByIdAndRequesterOrganizationId(requestId, user.getOrganization().getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Blood request not found."));
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}
}
