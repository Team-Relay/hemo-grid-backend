package com.sentinel.hemo_grid.organization.persistence;

import java.util.Optional;
import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.FacilityRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacilityRouteRepository extends JpaRepository<FacilityRoute, UUID> {

	@Query("select fr from FacilityRoute fr where fr.origin.id = :originId and fr.destination.id = :destinationId")
	Optional<FacilityRoute> findByOriginIdAndDestinationId(
			@Param("originId") UUID originId,
			@Param("destinationId") UUID destinationId
	);
}
