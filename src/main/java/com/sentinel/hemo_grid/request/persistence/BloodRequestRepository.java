package com.sentinel.hemo_grid.request.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sentinel.hemo_grid.request.domain.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, UUID> {

	@Query("""
			select request from BloodRequest request
			left join fetch request.providerOrganization
			where request.requesterOrganization.id = :organizationId
			order by request.requestedAt desc
			""")
	List<BloodRequest> findByRequesterOrganizationIdOrderByRequestedAtDesc(UUID organizationId);

	@Query("""
			select request from BloodRequest request
			left join fetch request.providerOrganization
			where request.id = :id and request.requesterOrganization.id = :organizationId
			""")
	Optional<BloodRequest> findByIdAndRequesterOrganizationId(UUID id, UUID organizationId);

	@Query("""
			select request from BloodRequest request
			join fetch request.requesterOrganization
			left join fetch request.providerOrganization
			where request.providerOrganization.id = :providerOrganizationId
			order by request.requestedAt desc
			""")
	List<BloodRequest> findByProviderOrganizationIdOrderByRequestedAtDesc(UUID providerOrganizationId);

	@Query("""
			select request from BloodRequest request
			join fetch request.requesterOrganization
			left join fetch request.providerOrganization
			where request.id = :id and request.providerOrganization.id = :providerOrganizationId
			""")
	Optional<BloodRequest> findByIdAndProviderOrganizationId(UUID id, UUID providerOrganizationId);
}
