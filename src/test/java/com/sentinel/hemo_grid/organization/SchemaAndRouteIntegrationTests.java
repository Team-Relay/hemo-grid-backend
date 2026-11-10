package com.sentinel.hemo_grid.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.FacilityRoute;
import com.sentinel.hemo_grid.organization.domain.FacilityTier;
import com.sentinel.hemo_grid.organization.domain.Organization;
import com.sentinel.hemo_grid.organization.persistence.FacilityRouteRepository;
import com.sentinel.hemo_grid.organization.persistence.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-for-jwt-signing-32-bytes")
@Sql(scripts = "/sql/reset-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
class SchemaAndRouteIntegrationTests {

	private static final UUID KNH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID NAIROBI_RBTC_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID GARISSA_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	private static final UUID EMBU_RBTC_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private FacilityRouteRepository facilityRouteRepository;

	@Test
	void organizationsHaveFacilityTiersAndCodesFromMigration() {
		Organization knh = organizationRepository.findById(KNH_ID).orElseThrow();
		assertThat(knh.getFacilityCode()).isEqualTo("HSP-001");
		assertThat(knh.getFacilityTier()).isEqualTo(FacilityTier.HOSPITAL);
		assertThat(knh.getCounty()).isEqualTo("Nairobi");

		Organization nairobiRbtc = organizationRepository.findById(NAIROBI_RBTC_ID).orElseThrow();
		assertThat(nairobiRbtc.getFacilityCode()).isEqualTo("RBTC-NBO");
		assertThat(nairobiRbtc.getFacilityTier()).isEqualTo(FacilityTier.RBTC);
	}

	@Test
	void garissaCorridorOptimizationMetadataIsLoaded() {
		Organization garissa = organizationRepository.findById(GARISSA_ID).orElseThrow();
		assertThat(garissa.getFacilityCode()).isEqualTo("HSP-032");
		assertThat(garissa.getAssignedSupplyCentre()).isEqualTo("RBTC-NBO");
		assertThat(garissa.getRecommendedSupplyCentre()).isEqualTo("RBTC-EMB");
		assertThat(garissa.getSavedTransitHours()).isEqualTo(2.96);
	}

	@Test
	void facilityRoutesCanBeLookedUpDirectly() {
		Optional<FacilityRoute> route = facilityRouteRepository.findByOriginIdAndDestinationId(GARISSA_ID, EMBU_RBTC_ID);
		assertThat(route).isPresent();
		assertThat(route.get().getEstimatedTransitHours()).isEqualTo(8.76);
		assertThat(route.get().getRoadDistanceKm()).isEqualTo(321.6);
	}
}
