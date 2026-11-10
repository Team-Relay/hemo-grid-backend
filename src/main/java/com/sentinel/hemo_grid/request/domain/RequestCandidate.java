/* RequestCandidate belongs to the authoritative request domain model. */

package com.sentinel.hemo_grid.request.domain;

import java.time.Instant;
import java.util.UUID;

import com.sentinel.hemo_grid.inventory.domain.BloodGroup;
import com.sentinel.hemo_grid.organization.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "offered_blood_group", nullable = false)
	private BloodGroup offeredBloodGroup = BloodGroup.O_NEGATIVE;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_type", nullable = false)
	private MatchCompatibilityType matchType = MatchCompatibilityType.EXACT;

	@Column(name = "available_units_snapshot", nullable = false)
	private int availableUnitsSnapshot;

	@Column(name = "distance_km")
	private Double distanceKm;

	@Column(name = "estimated_transit_hours")
	private Double estimatedTransitHours;

	@Column(name = "cold_chain_verified", nullable = false)
	private boolean coldChainVerified = true;

	@Column(name = "fridge_temp_celsius", nullable = false)
	private double fridgeTempCelsius = 3.8;

	@Column(name = "quarantined_units_excluded", nullable = false)
	private int quarantinedUnitsExcluded = 0;

	@Column(name = "conservation_rule_applied", nullable = false)
	private boolean conservationRuleApplied = false;

	@Column(name = "recommendation_reasons")
	private String recommendationReasons;

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
			BloodGroup offeredBloodGroup,
			MatchCompatibilityType matchType,
			int availableUnitsSnapshot,
			Double distanceKm,
			Double estimatedTransitHours,
			boolean coldChainVerified,
			double fridgeTempCelsius,
			int quarantinedUnitsExcluded,
			boolean conservationRuleApplied,
			String recommendationReasons,
			int rankPosition,
			Double matchScore
	) {
		this.bloodRequest = bloodRequest;
		this.providerOrganization = providerOrganization;
		this.offeredBloodGroup = offeredBloodGroup != null ? offeredBloodGroup : bloodRequest.getBloodGroup();
		this.matchType = matchType != null ? matchType : MatchCompatibilityType.EXACT;
		this.availableUnitsSnapshot = availableUnitsSnapshot;
		this.distanceKm = distanceKm;
		this.estimatedTransitHours = estimatedTransitHours;
		this.coldChainVerified = coldChainVerified;
		this.fridgeTempCelsius = fridgeTempCelsius;
		this.quarantinedUnitsExcluded = quarantinedUnitsExcluded;
		this.conservationRuleApplied = conservationRuleApplied;
		this.recommendationReasons = recommendationReasons;
		this.rankPosition = rankPosition;
		this.matchScore = matchScore;
	}

	public RequestCandidate(
			BloodRequest bloodRequest,
			Organization providerOrganization,
			int availableUnitsSnapshot,
			Double distanceKm,
			int rankPosition,
			Double matchScore
	) {
		this(
				bloodRequest,
				providerOrganization,
				bloodRequest.getBloodGroup(),
				MatchCompatibilityType.EXACT,
				availableUnitsSnapshot,
				distanceKm,
				distanceKm != null ? distanceKm / 48.0 : null,
				true,
				3.8,
				0,
				false,
				null,
				rankPosition,
				matchScore
		);
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

	public BloodGroup getOfferedBloodGroup() {
		return offeredBloodGroup;
	}

	public MatchCompatibilityType getMatchType() {
		return matchType;
	}

	public int getAvailableUnitsSnapshot() {
		return availableUnitsSnapshot;
	}

	public Double getDistanceKm() {
		return distanceKm;
	}

	public Double getEstimatedTransitHours() {
		return estimatedTransitHours;
	}

	public boolean isColdChainVerified() {
		return coldChainVerified;
	}

	public double getFridgeTempCelsius() {
		return fridgeTempCelsius;
	}

	public int getQuarantinedUnitsExcluded() {
		return quarantinedUnitsExcluded;
	}

	public boolean isConservationRuleApplied() {
		return conservationRuleApplied;
	}

	public String getRecommendationReasons() {
		return recommendationReasons;
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
