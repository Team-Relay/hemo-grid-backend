/* MatchingService coordinates matching use cases and enforces their business invariants. */

package com.sentinel.hemo_grid.matching.application;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import com.sentinel.hemo_grid.request.domain.BloodRequest;
import com.sentinel.hemo_grid.request.domain.RequestCandidate;
import com.sentinel.hemo_grid.request.persistence.RequestCandidateRepository;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {

	private final BloodInventoryRepository inventoryRepository;
	private final RequestCandidateRepository candidateRepository;
	private final HaversineDistanceCalculator distanceCalculator;

	public MatchingService(
			BloodInventoryRepository inventoryRepository,
			RequestCandidateRepository candidateRepository,
			HaversineDistanceCalculator distanceCalculator
	) {
		this.inventoryRepository = inventoryRepository;
		this.candidateRepository = candidateRepository;
		this.distanceCalculator = distanceCalculator;
	}

	public List<RequestCandidate> createCandidates(BloodRequest request) {
		List<InventoryMatch> matches = inventoryRepository.findMatchingInventoryRows(
						request.getBloodGroup(),
						request.getComponent(),
						OrganizationType.BLOOD_BANK
				)
				.stream()
				.map(inventory -> toMatch(request, inventory))
				.sorted(matchComparator())
				.toList();

		List<RequestCandidate> candidates = IntStream.range(0, matches.size())
				.mapToObj(index -> toCandidate(request, matches.get(index), index + 1))
				.toList();

		return candidateRepository.saveAll(candidates);
	}

	private InventoryMatch toMatch(BloodRequest request, BloodInventory inventory) {
		Organization requester = request.getRequesterOrganization();
		Organization provider = inventory.getOrganization();
		Double distanceKm = distanceCalculator.distanceKm(
				requester.getLatitude(),
				requester.getLongitude(),
				provider.getLatitude(),
				provider.getLongitude()
		);
		return new InventoryMatch(provider, inventory.unitsFree(), distanceKm, inventory.unitsFree() >= request.getUnitsRequired());
	}

	private RequestCandidate toCandidate(BloodRequest request, InventoryMatch match, int rank) {
		return new RequestCandidate(
				request,
				match.provider(),
				match.unitsFree(),
				match.distanceKm(),
				rank,
				matchScore(match)
		);
	}

	private Double matchScore(InventoryMatch match) {
		double fulfilmentScore = match.canFullyFulfil() ? 1_000_000 : 0;
		double distanceScore = match.distanceKm() == null ? 0 : Math.max(0, 10_000 - match.distanceKm());
		return fulfilmentScore + distanceScore + match.unitsFree();
	}

	private Comparator<InventoryMatch> matchComparator() {
		return Comparator
				.comparing(InventoryMatch::canFullyFulfil, Comparator.reverseOrder())
				.thenComparing(match -> match.distanceKm() == null)
				.thenComparing(InventoryMatch::distanceKm, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(InventoryMatch::unitsFree, Comparator.reverseOrder())
				.thenComparing(match -> match.provider().getName().toLowerCase(Locale.ROOT));
	}

	private record InventoryMatch(
			Organization provider,
			int unitsFree,
			Double distanceKm,
			boolean canFullyFulfil
	) {
	}
}
