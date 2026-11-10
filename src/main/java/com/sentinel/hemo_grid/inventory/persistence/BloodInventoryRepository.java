package com.sentinel.hemo_grid.inventory.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, UUID> {

	List<BloodInventory> findByOrganizationIdOrderByBloodGroupAscComponentAsc(UUID organizationId);

	Optional<BloodInventory> findByIdAndOrganizationId(UUID id, UUID organizationId);

	Optional<BloodInventory> findByOrganizationIdAndBloodGroupAndComponent(UUID organizationId, BloodGroup bloodGroup, BloodComponent component);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select inventory from BloodInventory inventory
			where inventory.organization.id = :organizationId
			  and inventory.bloodGroup = :bloodGroup
			  and inventory.component = :component
			""")
	Optional<BloodInventory> lockByOrganizationIdAndBloodGroupAndComponent(
			UUID organizationId,
			BloodGroup bloodGroup,
			BloodComponent component
	);

	@Query("""
			select inventory from BloodInventory inventory
			join fetch inventory.organization organization
			where inventory.bloodGroup = :bloodGroup
			  and inventory.component = :component
			  and organization.organizationType = :organizationType
			  and organization.active = true
			""")
	List<BloodInventory> findMatchingInventoryRows(
			BloodGroup bloodGroup,
			BloodComponent component,
			OrganizationType organizationType
	);
}
