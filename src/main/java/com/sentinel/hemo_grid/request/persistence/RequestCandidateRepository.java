/* RequestCandidateRepository is the persistence boundary for the request module. */

package com.sentinel.hemo_grid.request.persistence;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.request.domain.RequestCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequestCandidateRepository extends JpaRepository<RequestCandidate, UUID> {

	@Query("""
			select candidate from RequestCandidate candidate
			join fetch candidate.providerOrganization
			where candidate.bloodRequest.id = :requestId
			order by candidate.rankPosition asc
			""")
	List<RequestCandidate> findByBloodRequestIdOrderByRankPositionAsc(UUID requestId);

	boolean existsByBloodRequestIdAndProviderOrganizationId(UUID bloodRequestId, UUID providerOrganizationId);
}
