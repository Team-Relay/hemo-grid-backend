/* MatchingService coordinates matching use cases and enforces their business invariants. */

package com.sentinel.hemo_grid.matching.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sentinel.hemo_grid.inventory.domain.AboRhCompatibilityPolicy;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.organization.domain.FacilityRoute;
import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import com.sentinel.hemo_grid.organization.persistence.FacilityRouteRepository;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.MatchCompatibilityType;
import com.sentinel.hemo_grid.request.domain.RequestCandidate;
import com.sentinel.hemo_grid.request.persistence.RequestCandidateRepository;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {

	private final BloodInventoryRepository inventoryRepository;
	private final RequestCandidateRepository candidateRepository;
	private final FacilityRouteRepository facilityRouteRepository;
	private final HaversineDistanceCalculator distanceCalculator;
	private final DispatchRationaleGenerator rationaleGenerator;

	public MatchingService(
			BloodInventoryRepository inventoryRepository,
			RequestCandidateRepository candidateRepository,
			FacilityRouteRepository facilityRouteRepository,
			HaversineDistanceCalculator distanceCalculator,
			DispatchRationaleGenerator rationaleGenerator
	) {
		this.inventoryRepository = inventoryRepository;
		this.candidateRepository = candidateRepository;
		this.facilityRouteRepository = facilityRouteRepository;
		this.distanceCalculator = distanceCalculator;
		this.rationaleGenerator = rationaleGenerator;
	}

	public List<RequestCandidate> createCandidates(BloodRequest request) {
		List<AboRhCompatibilityPolicy.CompatibilityOption> compatibilityOptions =
				AboRhCompatibilityPolicy.getCompatibleDonors(
						request.getBloodGroup(),
						request.getComponent(),
						request.isUnknownPatientBloodGroup()
				);

		if (compatibilityOptions.isEmpty()) {
			return Collections.emptyList();
		}

		Map<BloodGroup, AboRhCompatibilityPolicy.CompatibilityOption> optionMap = compatibilityOptions.stream()
				.collect(Collectors.toMap(
						AboRhCompatibilityPolicy.CompatibilityOption::bloodGroup,
						option -> option,
						(existing, replacement) -> existing
				));

		List<BloodGroup> compatibleGroups = compatibilityOptions.stream()
				.map(AboRhCompatibilityPolicy.CompatibilityOption::bloodGroup)
				.toList();

		List<BloodInventory> rows = inventoryRepository.findMatchingInventoryRowsForGroups(
				compatibleGroups,
				request.getComponent(),
				OrganizationType.BLOOD_BANK
		);

		List<SmartMatch> sortedMatches = rows.stream()
				.map(inventory -> toSmartMatch(request, inventory, optionMap.get(inventory.getBloodGroup())))
				.sorted(matchComparator())
				.toList();

		// Deduplicate: Each candidate blood bank should appear exactly once offering its optimal match option
		Map<UUID, SmartMatch> bestMatchPerProvider = new LinkedHashMap<>();
		for (SmartMatch match : sortedMatches) {
			bestMatchPerProvider.putIfAbsent(match.provider().getId(), match);
		}

		List<SmartMatch> matches = new ArrayList<>(bestMatchPerProvider.values());

		List<RequestCandidate> candidates = IntStream.range(0, matches.size())
				.mapToObj(index -> toCandidate(request, matches.get(index), index + 1))
				.toList();

		return candidateRepository.saveAll(candidates);
	}

	private SmartMatch toSmartMatch(
			BloodRequest request,
			BloodInventory inventory,
			AboRhCompatibilityPolicy.CompatibilityOption option
	) {
		Organization requester = request.getRequesterOrganization();
		Organization provider = inventory.getOrganization();

		// Check pre-computed road corridor route first (origin -> destination or destination -> origin)
		Optional<FacilityRoute> route = facilityRouteRepository.findByOriginIdAndDestinationId(
				requester.getId(),
				provider.getId()
		).or(() -> facilityRouteRepository.findByOriginIdAndDestinationId(
				provider.getId(),
				requester.getId()
		));

		Double distanceKm;
		Double estimatedTransitHours;

		if (route.isPresent()) {
			distanceKm = route.get().getRoadDistanceKm();
			estimatedTransitHours = route.get().getEstimatedTransitHours();
		} else {
			distanceKm = distanceCalculator.distanceKm(
					requester.getLatitude(),
					requester.getLongitude(),
					provider.getLatitude(),
					provider.getLongitude()
			);
			estimatedTransitHours = distanceKm != null
					? Math.round((distanceKm / 45.0) * 100.0) / 100.0
					: null;
		}

		boolean coldChainVerified = inventory.isColdChainVerified();
		int dispatchableUnits = inventory.dispatchableUnitsFree();
		boolean canFullyFulfil = dispatchableUnits >= request.getUnitsRequired();

		MatchCompatibilityType matchType = option != null ? option.matchType() : MatchCompatibilityType.EXACT;
		int preferenceRank = option != null ? option.preferenceRank() : 1;
		boolean conservationRuleApplied = option != null && option.conservationRuleApplied();
		String policyRationale = option != null ? option.clinicalRationale() : null;

		DispatchRationaleGenerator.RationaleContext ctx = new DispatchRationaleGenerator.RationaleContext(
				request,
				provider,
				inventory.getBloodGroup(),
				matchType,
				dispatchableUnits,
				request.getUnitsRequired(),
				distanceKm,
				estimatedTransitHours,
				coldChainVerified,
				inventory.getFridgeTempCelsius(),
				inventory.getQuarantinedTempExcursionUnits(),
				conservationRuleApplied,
				policyRationale,
				route.isPresent()
		);

		String rationaleString = rationaleGenerator.generateRationaleString(ctx);

		return new SmartMatch(
				provider,
				inventory.getBloodGroup(),
				matchType,
				dispatchableUnits,
				distanceKm,
				estimatedTransitHours,
				canFullyFulfil,
				coldChainVerified,
				inventory.getFridgeTempCelsius(),
				inventory.getQuarantinedTempExcursionUnits(),
				preferenceRank,
				conservationRuleApplied,
				rationaleString
		);
	}

	private RequestCandidate toCandidate(BloodRequest request, SmartMatch match, int rank) {
		return new RequestCandidate(
				request,
				match.provider(),
				match.offeredBloodGroup(),
				match.matchType(),
				match.dispatchableUnits(),
				match.distanceKm(),
				match.estimatedTransitHours(),
				match.coldChainVerified(),
				match.fridgeTempCelsius(),
				match.quarantinedUnitsExcluded(),
				match.conservationRuleApplied(),
				match.recommendationReasons(),
				rank,
				matchScore(match)
		);
	}

	private Double matchScore(SmartMatch match) {
		double fulfilmentScore = match.canFullyFulfil() ? 1_000_000.0 : 0.0;
		double coldChainScore = match.coldChainVerified() ? 200_000.0 : -500_000.0;
		double preferenceScore = Math.max(0, (4 - match.preferenceRank())) * 50_000.0;
		double transitScore;
		if (match.estimatedTransitHours() != null) {
			transitScore = Math.max(0, 10_000.0 - (match.estimatedTransitHours() * 100.0));
		} else if (match.distanceKm() != null) {
			transitScore = Math.max(0, 10_000.0 - match.distanceKm());
		} else {
			transitScore = 0.0;
		}
		return fulfilmentScore + coldChainScore + preferenceScore + transitScore + match.dispatchableUnits();
	}

	private Comparator<SmartMatch> matchComparator() {
		return Comparator.comparing(match -> new DispatchPriority(
				match.canFullyFulfil(),
				match.coldChainVerified(),
				match.preferenceRank(),
				match.estimatedTransitHours(),
				match.dispatchableUnits(),
				match.provider().getName()
		));
	}

	private record SmartMatch(
			Organization provider,
			BloodGroup offeredBloodGroup,
			MatchCompatibilityType matchType,
			int dispatchableUnits,
			Double distanceKm,
			Double estimatedTransitHours,
			boolean canFullyFulfil,
			boolean coldChainVerified,
			double fridgeTempCelsius,
			int quarantinedUnitsExcluded,
			int preferenceRank,
			boolean conservationRuleApplied,
			String recommendationReasons
	) {
	}
}
