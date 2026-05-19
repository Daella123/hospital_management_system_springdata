package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.request.MedicalInventoryRequest;
import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.dto.response.MedicalInventoryResponse;
import com.daella.hospital_management_system.dto.response.PagedResponse;
import com.daella.hospital_management_system.enums.InventoryCategory;
import com.daella.hospital_management_system.service.MedicalInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Medical Inventory REST controller.
 *
 * <p>Paginated list endpoints return {@link PagedResponse}.
 *
 * <p><b>Caching note:</b> {@code GET /{id}} is cached in "inventory".
 * {@code PUT /{id}} refreshes the cache entry; {@code PATCH /{id}/stock} and
 * {@code DELETE /{id}} both evict the entry to prevent stale stock values being served.
 *
 * <p><b>Transaction note:</b> {@code PATCH /{id}/stock} runs under
 * {@code REPEATABLE_READ} isolation — prevents concurrent transactions from both
 * reading, then both writing the same stock level simultaneously.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Medical Inventory", description = "Medical supply and stock management")
public class MedicalInventoryController {

    private final MedicalInventoryService inventoryService;

    public MedicalInventoryController(MedicalInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','NURSE')")
    @Operation(summary = "Add a new inventory item", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<MedicalInventoryResponse>> create(
            @Valid @RequestBody MedicalInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added", inventoryService.createItem(request)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get inventory item by ID",
            description = "Result is cached in the 'inventory' cache. Subsequent calls with the same ID are served from memory."
    )
    public ResponseEntity<ApiResponse<MedicalInventoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getItemById(id)));
    }

    @GetMapping
    @Operation(
            summary = "List all inventory items with pagination",
            description = "Example: GET /api/v1/inventory?page=0&size=10&sortBy=name&direction=asc"
    )
    public ResponseEntity<ApiResponse<PagedResponse<MedicalInventoryResponse>>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")               @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by")        @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "asc or desc")             @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(inventoryService.getAllItems(PageRequest.of(page, size, sort)))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search inventory items by name")
    public ResponseEntity<ApiResponse<PagedResponse<MedicalInventoryResponse>>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(inventoryService.searchItems(name, PageRequest.of(page, size)))));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Filter inventory by category")
    public ResponseEntity<ApiResponse<PagedResponse<MedicalInventoryResponse>>> byCategory(
            @PathVariable InventoryCategory category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(inventoryService.getItemsByCategory(category, PageRequest.of(page, size)))));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get items at or below minimum stock level (JPQL query)")
    public ResponseEntity<ApiResponse<List<MedicalInventoryResponse>>> lowStock() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockItems()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NURSE')")
    @Operation(
            summary = "Update an inventory item",
            description = "Updates the item and refreshes the 'inventory' cache entry.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<MedicalInventoryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody MedicalInventoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Item updated", inventoryService.updateItem(id, request)));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','NURSE')")
    @Operation(
            summary = "Adjust stock (positive = add, negative = remove)",
            description = "Runs under REPEATABLE_READ isolation. Evicts 'inventory' cache entry. " +
                          "Rolls back if the resulting quantity would drop below zero.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<MedicalInventoryResponse>> adjustStock(
            @PathVariable Long id, @RequestParam int delta) {
        return ResponseEntity.ok(
                ApiResponse.success("Stock adjusted", inventoryService.adjustStock(id, delta)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove an inventory item",
            description = "Deletes the item and evicts the 'inventory' cache entry.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        inventoryService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success("Item deleted", null));
    }
}
