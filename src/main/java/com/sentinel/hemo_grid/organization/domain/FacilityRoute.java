package com.sentinel.hemo_grid.organization.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "facility_routes")
public class FacilityRoute {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "origin_organization_id", nullable = false)
	private Organization origin;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_organization_id", nullable = false)
	private Organization destination;

	@Column(name = "road_distance_km", nullable = false)
	private double roadDistanceKm;

	@Column(name = "estimated_transit_hours", nullable = false)
	private double estimatedTransitHours;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected FacilityRoute() {
	}

	public FacilityRoute(Organization origin, Organization destination, double roadDistanceKm, double estimatedTransitHours) {
		this.origin = origin;
		this.destination = destination;
		this.roadDistanceKm = roadDistanceKm;
		this.estimatedTransitHours = estimatedTransitHours;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Organization getOrigin() {
		return origin;
	}

	public Organization getDestination() {
		return destination;
	}

	public double getRoadDistanceKm() {
		return roadDistanceKm;
	}

	public double getEstimatedTransitHours() {
		return estimatedTransitHours;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
