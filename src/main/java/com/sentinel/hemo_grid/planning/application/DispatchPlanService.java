package com.sentinel.hemo_grid.planning.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.inventory.domain.AboRhCompatibilityPolicy;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.matching.application.HaversineDistanceCalculator;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import com.sentinel.hemo_grid.planning.api.CreateDispatchPlanRequest;
import com.sentinel.hemo_grid.planning.api.DispatchPlanResponse;
import com.sentinel.hemo_grid.planning.api.DispatchPlanResponse.Proposal;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Option;
import com.sentinel.hemo_grid.planning.domain.BatchDispatchPlanner.Order;
import com.sentinel.hemo_grid.request.api.BloodRequestResponse;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.RequestCandidate;
import com.sentinel.hemo_grid.request.domain.RequestStatus;
import com.sentinel.hemo_grid.request.domain.RequestUrgency;
import com.sentinel.hemo_grid.request.persistence.BloodRequestRepository;
import com.sentinel.hemo_grid.request.persistence.RequestCandidateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Advisory network planning uses existing candidate options and current stock eligibility. */
@Service
public class DispatchPlanService {
	private final AuthService authService;
	private final BloodRequestRepository requestRepository;
	private final RequestCandidateRepository candidateRepository;
	private final BloodInventoryRepository inventoryRepository;
	private final HaversineDistanceCalculator distanceCalculator;
	private final BatchDispatchPlanner planner = new BatchDispatchPlanner();

	public DispatchPlanService(AuthService authService, BloodRequestRepository requestRepository,
			RequestCandidateRepository candidateRepository, BloodInventoryRepository inventoryRepository,
			HaversineDistanceCalculator distanceCalculator) {
		this.authService = authService;
		this.requestRepository = requestRepository;
		this.candidateRepository = candidateRepository;
		this.inventoryRepository = inventoryRepository;
		this.distanceCalculator = distanceCalculator;
	}

	@Transactional(readOnly = true)
	public List<BloodRequestResponse> pendingRequests(Jwt jwt) {
		authService.requirePlatformAdmin(jwt);
		return requestRepository.findTop100ByStatusAndUrgencyAndComponentOrderByRequestedAtAsc(
				RequestStatus.REQUESTED, RequestUrgency.CRITICAL, BloodComponent.RED_CELLS).stream()
				.filter(request -> request.getRequesterOrganization().isActive())
				.map(BloodRequestResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public DispatchPlanResponse preview(Jwt jwt, CreateDispatchPlanRequest input) {
		authService.requirePlatformAdmin(jwt);
		List<UUID> ids = input.requestIds();
		if (!input.staffApproved() || ids == null || ids.size() < 2 || ids.size() > BatchDispatchPlanner.MAX_ORDERS
				|| ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
					"Select two to five distinct requests and confirm staff approval of their inclusion.");
		}
		Map<UUID, BloodRequest> requests = new HashMap<>();
		for (BloodRequest request : requestRepository.findAllById(ids)) {
			requests.put(request.getId(), request);
		}
		if (requests.size() != ids.size()) {
			throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "A selected request was not found.");
		}
		Map<String, Integer> stock = new HashMap<>();
		Map<String, BloodInventory> inventory = new HashMap<>();
		Map<String, RequestCandidate> candidates = new HashMap<>();
		List<Order> orders = new ArrayList<>();
		for (UUID id : ids) {
			BloodRequest request = requests.get(id);
			if (request.getStatus() != RequestStatus.REQUESTED || request.getUrgency() != RequestUrgency.CRITICAL
					|| request.getComponent() != BloodComponent.RED_CELLS || !request.getRequesterOrganization().isActive()
					|| request.getRequesterOrganization().getOrganizationType() != OrganizationType.HOSPITAL) {
				throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_REQUEST_STATUS,
						"Planning requires pending critical red-cell requests from active hospitals.");
			}
			List<Option> options = new ArrayList<>();
			for (RequestCandidate candidate : candidateRepository.findByBloodRequestIdOrderByRankPositionAsc(id)) {
				var provider = candidate.getProviderOrganization();
				BloodGroup offered = candidate.getOfferedBloodGroup() != null ? candidate.getOfferedBloodGroup() : request.getBloodGroup();
				if (!provider.isActive() || provider.getOrganizationType() != OrganizationType.BLOOD_BANK
						|| !AboRhCompatibilityPolicy.isCompatible(request.getBloodGroup(), offered, request.getComponent(), request.isUnknownPatientBloodGroup())) {
					continue;
				}
				BloodInventory current = inventoryRepository.findByOrganizationIdAndBloodGroupAndComponent(
						provider.getId(), offered, request.getComponent()).orElse(null);
				if (current == null || !current.isColdChainVerified() || current.dispatchableUnitsFree() < request.getUnitsRequired()) {
					continue;
				}
				Double distance = distanceCalculator.distanceKm(request.getRequesterOrganization().getLatitude(),
						request.getRequesterOrganization().getLongitude(), provider.getLatitude(), provider.getLongitude());
				if (!validEstimate(distance)) {
					distance = candidate.getDistanceKm();
				}
				if (!validEstimate(distance) || !validEstimate(candidate.getEstimatedTransitHours())) {
					continue;
				}
				String stockId = current.getId().toString();
				stock.put(stockId, current.dispatchableUnitsFree());
				inventory.put(stockId, current);
				candidates.put(id + "/" + stockId, candidate);
				options.add(new Option(stockId, candidate.getEstimatedTransitHours(), distance));
			}
			orders.add(new Order(id.toString(), request.getUnitsRequired(), options));
		}
		var plan = planner.solve(orders, stock);
		var baseline = planner.nearestEligible(orders, stock);
		Map<String, Integer> remaining = new HashMap<>(stock);
		for (var assignment : plan.assignments()) {
			int units = requests.get(UUID.fromString(assignment.orderId())).getUnitsRequired();
			remaining.compute(assignment.stockId(), (key, free) -> free - units);
		}
		List<Proposal> proposals = new ArrayList<>();
		for (var assignment : plan.assignments()) {
			UUID requestId = UUID.fromString(assignment.orderId());
			BloodRequest request = requests.get(requestId);
			BloodInventory current = inventory.get(assignment.stockId());
			RequestCandidate candidate = candidates.get(requestId + "/" + assignment.stockId());
			proposals.add(new Proposal(requestId, request.getRequesterOrganization().getId(), candidate.getProviderOrganization().getId(),
					candidate.getProviderOrganization().getName(), current.getBloodGroup(), current.getId(), current.getVersion(),
					request.getUnitsRequired(), stock.get(assignment.stockId()), remaining.get(assignment.stockId()),
					assignment.transitHours(), current.getFridgeTempCelsius(), current.getUpdatedAt(),
					"Proposes " + request.getUnitsRequired() + " units from " + stock.get(assignment.stockId())
							+ " recorded dispatchable units. " + remaining.get(assignment.stockId())
							+ " remain after the whole batch. Staff confirmation is required."));
		}
		List<String> notes = new ArrayList<>(List.of(
						"Hospital officers select the proposed providers through the existing request workflow. Banks confirm and reserve stock on acceptance.",
						"Preview leaves stock and requests unchanged. Refresh after stock, quarantine or request status changes.",
						"Every included request is mandatory. OPTIMAL applies to recorded options and costs. FEASIBLE means optimality is unproven. INFEASIBLE and LIMIT_REACHED require staff review.",
						"ETA uncertainty is uncalibrated. Total hours sum separate estimated journeys and exclude approval, preparation and traffic delays.",
						"The baseline uses geographic distance, falling back to recorded distance when coordinates are unavailable. Inventory timestamps record database updates."
				));
		if (!proposals.isEmpty() && baseline.assignments().isEmpty()) {
			notes.add("Sequential nearest eligible cannot fill this whole batch under the recorded snapshot. The joint proposal fits every included request.");
		}
		return new DispatchPlanResponse(Instant.now(), plan.status(), plan.totalTransitHours(), plan.visitedNodes(),
				baseline, List.copyOf(proposals), List.copyOf(notes));
	}

	private boolean validEstimate(Double value) {
		return value != null && Double.isFinite(value) && value >= 0;
	}
}
