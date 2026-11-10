package com.sentinel.hemo_grid.inventory.application;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.auth.application.AuthService;
import com.sentinel.hemo_grid.auth.domain.AppUser;
import com.sentinel.hemo_grid.auth.domain.UserRole;
import com.sentinel.hemo_grid.common.exception.BusinessException;
import com.sentinel.hemo_grid.common.exception.ErrorCode;
import com.sentinel.hemo_grid.inventory.api.InventoryResponse;
import com.sentinel.hemo_grid.inventory.api.UpdateInventoryRequest;
import com.sentinel.hemo_grid.inventory.domain.BloodInventory;
import com.sentinel.hemo_grid.inventory.persistence.BloodInventoryRepository;
import com.sentinel.hemo_grid.organization.domain.OrganizationType;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

	private final AuthService authService;
	private final BloodInventoryRepository inventoryRepository;

	public InventoryService(AuthService authService, BloodInventoryRepository inventoryRepository) {
		this.authService = authService;
		this.inventoryRepository = inventoryRepository;
	}

	@Transactional(readOnly = true)
	public List<InventoryResponse> listInventory(Jwt jwt) {
		AppUser user = requireBloodBankUser(jwt);
		return inventoryRepository.findByOrganizationIdOrderByBloodGroupAscComponentAsc(user.getOrganization().getId())
				.stream()
				.map(InventoryResponse::from)
				.toList();
	}

	@Transactional
	public InventoryResponse updateUnitsAvailable(Jwt jwt, UUID inventoryId, UpdateInventoryRequest request) {
		AppUser user = requireBloodBankUser(jwt);
		if (request.unitsAvailable() < 0) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Units available cannot be negative.");
		}

		BloodInventory inventory = inventoryRepository.findByIdAndOrganizationId(inventoryId, user.getOrganization().getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Inventory row not found."));

		if (request.unitsAvailable() < inventory.getUnitsReserved()) {
			throw new BusinessException(
					HttpStatus.CONFLICT,
					ErrorCode.INSUFFICIENT_INVENTORY,
					"Units available cannot be lower than currently reserved units."
			);
		}

		inventory.updateUnitsAvailable(request.unitsAvailable());
		return InventoryResponse.from(inventory);
	}

	private AppUser requireBloodBankUser(Jwt jwt) {
		AppUser user = authService.requireCurrentUser(jwt);
		if (user.getOrganization() == null) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only blood-bank users can access inventory.");
		}
		if (user.getOrganization().getOrganizationType() != OrganizationType.BLOOD_BANK) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.ORGANIZATION_TYPE_MISMATCH, "Only blood-bank users can access inventory.");
		}
		if (user.getRole() != UserRole.BLOOD_BANK_ADMIN && user.getRole() != UserRole.BLOOD_BANK_STAFF) {
			throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only blood-bank users can access inventory.");
		}
		return user;
	}
}
