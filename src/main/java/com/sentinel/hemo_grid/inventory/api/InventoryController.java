/* InventoryController is the authenticated HTTP boundary for inventory operations. */

package com.sentinel.hemo_grid.inventory.api;

import java.util.List;
import java.util.UUID;

import com.sentinel.hemo_grid.inventory.application.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping
	@Operation(summary = "List inventory for the authenticated blood bank")
	@ApiResponse(responseCode = "200", description = "Inventory returned")
	@ApiResponse(responseCode = "403", description = "User is not a blood-bank user")
	public List<InventoryResponse> listInventory(@AuthenticationPrincipal Jwt jwt) {
		return inventoryService.listInventory(jwt);
	}

	@PutMapping("/{inventoryId}")
	@Operation(summary = "Replace available units for one inventory row")
	@ApiResponse(responseCode = "200", description = "Inventory updated")
	@ApiResponse(responseCode = "400", description = "Invalid units")
	@ApiResponse(responseCode = "403", description = "User cannot update this inventory")
	@ApiResponse(responseCode = "404", description = "Inventory row not found")
	public InventoryResponse updateInventory(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID inventoryId,
			@Valid @RequestBody UpdateInventoryRequest request
	) {
		return inventoryService.updateUnitsAvailable(jwt, inventoryId, request);
	}

	@PatchMapping("/{inventoryId}/units")
	@Operation(summary = "Update available units for one inventory row")
	@ApiResponse(responseCode = "200", description = "Inventory updated")
	@ApiResponse(responseCode = "400", description = "Invalid units")
	@ApiResponse(responseCode = "403", description = "User cannot update this inventory")
	@ApiResponse(responseCode = "404", description = "Inventory row not found")
	public InventoryResponse updateInventoryUnits(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID inventoryId,
			@Valid @RequestBody UpdateInventoryRequest request
	) {
		return inventoryService.updateUnitsAvailable(jwt, inventoryId, request);
	}
}
