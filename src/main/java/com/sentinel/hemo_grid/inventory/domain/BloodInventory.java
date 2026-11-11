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

	@Column(name = "quarantined_temp_excursion_units", nullable = false)
	private int quarantinedTempExcursionUnits = 0;

	@Column(name = "fridge_temp_celsius", nullable = false)
	private double fridgeTempCelsius = 3.8;

	@Enumerated(EnumType.STRING)
	@Column(name = "cold_chain_status", nullable = false)
	private ColdChainStatus coldChainStatus = ColdChainStatus.VERIFIED_2_6C;

	@Column(name = "expiring_within_7_days_units", nullable = false)
	private int expiringWithin7DaysUnits = 0;

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

	public int getQuarantinedTempExcursionUnits() {
		return quarantinedTempExcursionUnits;
	}

	public double getFridgeTempCelsius() {
		return fridgeTempCelsius;
	}

	public ColdChainStatus getColdChainStatus() {
		return coldChainStatus;
	}

	public int getExpiringWithin7DaysUnits() {
		return expiringWithin7DaysUnits;
	}

	public boolean isColdChainVerified() {
		return coldChainStatus == ColdChainStatus.VERIFIED_2_6C
				&& fridgeTempCelsius >= 2.0
				&& fridgeTempCelsius <= 6.0;
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
		return Math.max(0, unitsAvailable - unitsReserved);
	}

	/**
	 * Units free and safe for dispatch (excluding any units quarantined due to temperature excursions).
	 */
	public int dispatchableUnitsFree() {
		if (!isColdChainVerified()) {
			return 0;
		}
		return Math.max(0, unitsAvailable - unitsReserved - quarantinedTempExcursionUnits);
	}

	public void updateUnitsAvailable(int unitsAvailable) {
		if (unitsAvailable < unitsReserved) {
			throw new IllegalArgumentException("unitsAvailable cannot be less than unitsReserved.");
		}
		this.unitsAvailable = unitsAvailable;
	}

	public void updateColdChainTelemetry(double fridgeTempCelsius, int quarantinedUnits) {
		this.fridgeTempCelsius = fridgeTempCelsius;
		this.quarantinedTempExcursionUnits = Math.max(0, quarantinedUnits);
		if (fridgeTempCelsius > 6.0 || quarantinedUnits > 0) {
			this.coldChainStatus = ColdChainStatus.EXCURSION_QUARANTINED;
		} else {
			this.coldChainStatus = ColdChainStatus.VERIFIED_2_6C;
		}
	}

	public void reserve(int units) {
		if (units < 1) {
			throw new IllegalArgumentException("units must be positive.");
		}
		if (dispatchableUnitsFree() < units) {
			throw new IllegalArgumentException("not enough dispatchable units.");
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
