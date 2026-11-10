/* BloodInventory belongs to the authoritative inventory domain model. */

package com.sentinel.hemo_grid.inventory.domain;

import java.time.Instant;
import java.util.UUID;

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
import jakarta.persistence.Version;

@Entity
@Table(name = "blood_inventory")
public class BloodInventory {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;

	@Enumerated(EnumType.STRING)
	@Column(name = "blood_group", nullable = false)
	private BloodGroup bloodGroup;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BloodComponent component;

	@Column(name = "units_available", nullable = false)
	private int unitsAvailable;

	@Column(name = "units_reserved", nullable = false)
	private int unitsReserved;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BloodInventory() {
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

	public Organization getOrganization() {
		return organization;
	}

	public BloodGroup getBloodGroup() {
		return bloodGroup;
	}

	public BloodComponent getComponent() {
		return component;
	}

	public int getUnitsAvailable() {
		return unitsAvailable;
	}

	public int getUnitsReserved() {
		return unitsReserved;
	}

	public long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public int unitsFree() {
		return unitsAvailable - unitsReserved;
	}

	public void updateUnitsAvailable(int unitsAvailable) {
		if (unitsAvailable < unitsReserved) {
			throw new IllegalArgumentException("unitsAvailable cannot be less than unitsReserved.");
		}
		this.unitsAvailable = unitsAvailable;
	}

	public void reserve(int units) {
		if (units < 1) {
			throw new IllegalArgumentException("units must be positive.");
		}
		if (unitsFree() < units) {
			throw new IllegalArgumentException("not enough free units.");
		}
		unitsReserved += units;
	}

	public void releaseReservation(int units) {
		if (units < 1) {
			throw new IllegalArgumentException("units must be positive.");
		}
		if (unitsReserved < units) {
			throw new IllegalArgumentException("cannot release more units than reserved.");
		}
		unitsReserved -= units;
	}

	public void consumeReservation(int units) {
		if (units < 1) {
			throw new IllegalArgumentException("units must be positive.");
		}
		if (unitsReserved < units || unitsAvailable < units) {
			throw new IllegalArgumentException("cannot consume unavailable reserved units.");
		}
		unitsReserved -= units;
		unitsAvailable -= units;
	}
}
