package com.sentinel.hemo_grid.request.domain;

import java.time.Instant;
import java.util.UUID;

import com.sentinel.hemo_grid.organization.domain.Organization;
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
@Table(name = "request_candidates")
public class RequestCandidate {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "blood_request_id", nullable = false)
	private BloodRequest bloodRequest;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "provider_organization_id", nullable = false)
	private Organization providerOrganization;

	@Column(name = "available_units_snapshot", nullable = false)
	private int availableUnitsSnapshot;

	@Column(name = "distance_km")
	private Double distanceKm;

	@Column(name = "rank_position", nullable = false)
	private int rankPosition;

	@Column(name = "match_score")
	private Double matchScore;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RequestCandidate() {
	}

	public RequestCandidate(
			BloodRequest bloodRequest,
			Organization providerOrganization,
			int availableUnitsSnapshot,
			Double distanceKm,
			int rankPosition,
			Double matchScore
	) {
		this.bloodRequest = bloodRequest;
		this.providerOrganization = providerOrganization;
		this.availableUnitsSnapshot = availableUnitsSnapshot;
		this.distanceKm = distanceKm;
		this.rankPosition = rankPosition;
		this.matchScore = matchScore;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public BloodRequest getBloodRequest() {
		return bloodRequest;
	}

	public Organization getProviderOrganization() {
		return providerOrganization;
	}

	public int getAvailableUnitsSnapshot() {
		return availableUnitsSnapshot;
	}

	public Double getDistanceKm() {
		return distanceKm;
	}

	public int getRankPosition() {
		return rankPosition;
	}

	public Double getMatchScore() {
		return matchScore;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
