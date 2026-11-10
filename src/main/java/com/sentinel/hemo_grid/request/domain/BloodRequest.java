/* BloodRequest is a validated inbound API contract for the request module. */

package com.sentinel.hemo_grid.request.domain;

import java.time.Instant;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.domain.AppUser;
import com.sentinel.hemo_grid.inventory.domain.BloodComponent;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_organization_id", nullable = false)
	private Organization requesterOrganization;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "provider_organization_id")
	private Organization providerOrganization;

	@Enumerated(EnumType.STRING)
	@Column(name = "blood_group", nullable = false)
	private BloodGroup bloodGroup;

	@Enumerated(EnumType.STRING)
	@Column(name = "allocated_blood_group")
	private BloodGroup allocatedBloodGroup;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BloodComponent component;

	@Column(name = "units_required", nullable = false)
	private int unitsRequired;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RequestUrgency urgency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RequestStatus status;

	@Column(name = "unknown_patient_blood_group", nullable = false)
	private boolean unknownPatientBloodGroup = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_type", nullable = false)
	private MatchCompatibilityType matchType = MatchCompatibilityType.EXACT;

	@Column(name = "estimated_transit_hours")
	private Double estimatedTransitHours;

	@Column(name = "cold_chain_verified", nullable = false)
	private boolean coldChainVerified = true;

	@Column(name = "clinical_reference")
	private String clinicalReference;

	private String notes;

	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@Column(name = "preparing_at")
	private Instant preparingAt;

	@Column(name = "dispatched_at")
	private Instant dispatchedAt;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private AppUser createdByUser;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BloodRequest() {
	}

	private BloodRequest(
			Organization requesterOrganization,
			BloodGroup bloodGroup,
			BloodComponent component,
			int unitsRequired,
			RequestUrgency urgency,
			boolean unknownPatientBloodGroup,
			String clinicalReference,
			String notes,
			AppUser createdByUser
	) {
		this.requesterOrganization = requesterOrganization;
		this.bloodGroup = bloodGroup;
		this.allocatedBloodGroup = bloodGroup;
		this.component = component;
		this.unitsRequired = unitsRequired;
		this.urgency = urgency;
		this.unknownPatientBloodGroup = unknownPatientBloodGroup;
		this.status = RequestStatus.REQUESTED;
		this.clinicalReference = clinicalReference;
		this.notes = notes;
		this.createdByUser = createdByUser;
	}

	public static BloodRequest create(
			AppUser createdByUser,
			BloodGroup bloodGroup,
			BloodComponent component,
			int unitsRequired,
			RequestUrgency urgency,
			boolean unknownPatientBloodGroup,
			String clinicalReference,
			String notes
	) {
		boolean isUnknown = unknownPatientBloodGroup
				|| (notes != null && notes.contains("[EMERGENCY UNKNOWN BLOOD GROUP"));
		BloodGroup actualGroup = bloodGroup != null ? bloodGroup : BloodGroup.O_NEGATIVE;
		return new BloodRequest(
				createdByUser.getOrganization(),
				actualGroup,
				component,
				unitsRequired,
				urgency,
				isUnknown,
				clinicalReference,
				notes,
				createdByUser
		);
	}

	public static BloodRequest create(
			AppUser createdByUser,
			BloodGroup bloodGroup,
			BloodComponent component,
			int unitsRequired,
			RequestUrgency urgency,
			String clinicalReference,
			String notes
	) {
		return create(createdByUser, bloodGroup, component, unitsRequired, urgency, false, clinicalReference, notes);
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (requestedAt == null) {
			requestedAt = now;
		}
		if (allocatedBloodGroup == null) {
			allocatedBloodGroup = bloodGroup;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Organization getRequesterOrganization() {
		return requesterOrganization;
	}

	public Organization getProviderOrganization() {
		return providerOrganization;
	}

	public BloodGroup getBloodGroup() {
		return bloodGroup;
	}

	public BloodGroup getAllocatedBloodGroup() {
		return allocatedBloodGroup != null ? allocatedBloodGroup : bloodGroup;
	}

	public BloodComponent getComponent() {
		return component;
	}

	public int getUnitsRequired() {
		return unitsRequired;
	}

	public RequestUrgency getUrgency() {
		return urgency;
	}

	public RequestStatus getStatus() {
		return status;
	}

	public boolean isUnknownPatientBloodGroup() {
		return unknownPatientBloodGroup;
	}

	public MatchCompatibilityType getMatchType() {
		return matchType;
	}

	public Double getEstimatedTransitHours() {
		return estimatedTransitHours;
	}

	public boolean isColdChainVerified() {
		return coldChainVerified;
	}

	public String getClinicalReference() {
		return clinicalReference;
	}

	public String getNotes() {
		return notes;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public Instant getAcceptedAt() {
		return acceptedAt;
	}

	public Instant getPreparingAt() {
		return preparingAt;
	}

	public Instant getDispatchedAt() {
		return dispatchedAt;
	}

	public Instant getDeliveredAt() {
		return deliveredAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

	public AppUser getCreatedByUser() {
		return createdByUser;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void selectProvider(
			Organization providerOrganization,
			BloodGroup offeredGroup,
			MatchCompatibilityType matchType,
			Double estimatedTransitHours,
			boolean coldChainVerified
	) {
		if (status != RequestStatus.REQUESTED) {
			throw new IllegalStateException("Provider can only be selected while request is REQUESTED.");
		}
		this.providerOrganization = providerOrganization;
		this.allocatedBloodGroup = offeredGroup != null ? offeredGroup : this.bloodGroup;
		this.matchType = matchType != null ? matchType : MatchCompatibilityType.EXACT;
		this.estimatedTransitHours = estimatedTransitHours;
		this.coldChainVerified = coldChainVerified;
	}

	public void selectProvider(Organization providerOrganization) {
		selectProvider(providerOrganization, this.bloodGroup, MatchCompatibilityType.EXACT, null, true);
	}

	public void accept() {
		if (status != RequestStatus.REQUESTED) {
			throw new IllegalStateException("Only REQUESTED requests can be accepted.");
		}
		if (providerOrganization == null) {
			throw new IllegalStateException("Provider must be selected before acceptance.");
		}
		status = RequestStatus.ACCEPTED;
		acceptedAt = Instant.now();
	}

	public void decline() {
		if (status != RequestStatus.REQUESTED) {
			throw new IllegalStateException("Only REQUESTED requests can be declined.");
		}
		status = RequestStatus.DECLINED;
	}

	public void cancel() {
		if (status != RequestStatus.REQUESTED && status != RequestStatus.ACCEPTED && status != RequestStatus.PREPARING) {
			throw new IllegalStateException("Request cannot be cancelled in its current status.");
		}
		status = RequestStatus.CANCELLED;
		cancelledAt = Instant.now();
	}

	public void progressTo(RequestStatus nextStatus) {
		Instant now = Instant.now();
		if (status == RequestStatus.ACCEPTED && nextStatus == RequestStatus.PREPARING) {
			status = RequestStatus.PREPARING;
			preparingAt = now;
			return;
		}
		if (status == RequestStatus.PREPARING && nextStatus == RequestStatus.IN_TRANSIT) {
			status = RequestStatus.IN_TRANSIT;
			dispatchedAt = now;
			return;
		}
		if (status == RequestStatus.IN_TRANSIT && nextStatus == RequestStatus.DELIVERED) {
			status = RequestStatus.DELIVERED;
			deliveredAt = now;
			return;
		}
		throw new IllegalStateException("Invalid request status transition.");
	}
}
