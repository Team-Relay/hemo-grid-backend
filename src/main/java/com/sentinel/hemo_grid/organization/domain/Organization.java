/* Organization belongs to the authoritative organization domain model. */

package com.sentinel.hemo_grid.organization.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "organization_type", nullable = false)
	private OrganizationType organizationType;

	@Column(name = "facility_code")
	private String facilityCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "facility_tier")
	private FacilityTier facilityTier = FacilityTier.HOSPITAL;

	private String email;

	private String phone;

	@Column(nullable = false)
	private String address;

	private String city;

	private String state;

	private String county;

	private Double latitude;

	private Double longitude;

	@Column(name = "assigned_supply_centre")
	private String assignedSupplyCentre;

	@Column(name = "recommended_supply_centre")
	private String recommendedSupplyCentre;

	@Column(name = "saved_transit_hours")
	private Double savedTransitHours;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Organization() {
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public OrganizationType getOrganizationType() {
		return organizationType;
	}

	public String getFacilityCode() {
		return facilityCode;
	}

	public FacilityTier getFacilityTier() {
		return facilityTier;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getAddress() {
		return address;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public String getCounty() {
		return county;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public String getAssignedSupplyCentre() {
		return assignedSupplyCentre;
	}

	public String getRecommendedSupplyCentre() {
		return recommendedSupplyCentre;
	}

	public Double getSavedTransitHours() {
		return savedTransitHours;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
