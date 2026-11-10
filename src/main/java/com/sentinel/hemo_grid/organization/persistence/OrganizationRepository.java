/* OrganizationRepository is the persistence boundary for the organization module. */

package com.sentinel.hemo_grid.organization.persistence;

import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
