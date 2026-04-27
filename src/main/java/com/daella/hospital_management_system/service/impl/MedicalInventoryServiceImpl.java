package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.MedicalInventoryRequest;
import com.daella.hospital_management_system.dto.response.MedicalInventoryResponse;
import com.daella.hospital_management_system.entity.MedicalInventory;
import com.daella.hospital_management_system.enums.InventoryCategory;
import com.daella.hospital_management_system.exception.DuplicateResourceException;
import com.daella.hospital_management_system.exception.InvalidOperationException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.MedicalInventoryRepository;
import com.daella.hospital_management_system.service.MedicalInventoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class MedicalInventoryServiceImpl implements MedicalInventoryService {

    private final MedicalInventoryRepository inventoryRepository;

    public MedicalInventoryServiceImpl(MedicalInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public MedicalInventoryResponse createItem(MedicalInventoryRequest request) {
        if (StringUtils.hasText(request.getSku()) && inventoryRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("SKU '" + request.getSku() + "' already exists");
        }
        return toResponse(inventoryRepository.save(toEntity(request)));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "inventory", key = "#id")
    public MedicalInventoryResponse getItemById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalInventoryResponse> getAllItems(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalInventoryResponse> getItemsByCategory(InventoryCategory category, Pageable pageable) {
        return inventoryRepository.findByCategory(category, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalInventoryResponse> searchItems(String name, Pageable pageable) {
        return inventoryRepository.findByNameContainingIgnoreCase(name, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalInventoryResponse> getLowStockItems() {
        return inventoryRepository.findLowStockItems().stream().map(this::toResponse).toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @CachePut(value = "inventory", key = "#id")
    public MedicalInventoryResponse updateItem(Long id, MedicalInventoryRequest request) {
        MedicalInventory item = findOrThrow(id);
        if (StringUtils.hasText(request.getSku())
                && !request.getSku().equalsIgnoreCase(item.getSku())
                && inventoryRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("SKU '" + request.getSku() + "' already exists");
        }
        applyFields(item, request);
        return toResponse(inventoryRepository.save(item));
    }

    /**
     * Adjust stock quantity by a positive or negative delta.
     *
     * <p>Uses {@code REPEATABLE_READ} isolation so that a concurrent read of the stock
     * level cannot change between our check and our write, preventing race-condition
     * over-deductions (e.g. two prescriptions consuming the last unit simultaneously).
     */
    @Override
    @Transactional(
            propagation  = Propagation.REQUIRED,
            isolation    = Isolation.REPEATABLE_READ,
            rollbackFor  = Exception.class
    )
    @CacheEvict(value = "inventory", key = "#id")
    public MedicalInventoryResponse adjustStock(Long id, int delta) {
        MedicalInventory item = findOrThrow(id);
        int newQty = item.getQuantityInStock() + delta;
        if (newQty < 0) {
            throw new InvalidOperationException(
                    "Cannot reduce stock below zero. Current: " + item.getQuantityInStock() + ", delta: " + delta);
        }
        item.setQuantityInStock(newQty);
        return toResponse(inventoryRepository.save(item));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "inventory", key = "#id")
    public void deleteItem(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("InventoryItem", "id", id);
        }
        inventoryRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MedicalInventory findOrThrow(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", id));
    }

    private MedicalInventory toEntity(MedicalInventoryRequest r) {
        return MedicalInventory.builder()
                .name(r.getName())
                .sku(r.getSku())
                .category(r.getCategory())
                .quantityInStock(r.getQuantityInStock())
                .minimumStockLevel(r.getMinimumStockLevel())
                .unitPrice(r.getUnitPrice())
                .supplierName(r.getSupplierName())
                .expiryDate(r.getExpiryDate())
                .description(r.getDescription())
                .build();
    }

    private void applyFields(MedicalInventory m, MedicalInventoryRequest r) {
        m.setName(r.getName());
        m.setSku(r.getSku());
        m.setCategory(r.getCategory());
        m.setQuantityInStock(r.getQuantityInStock());
        m.setMinimumStockLevel(r.getMinimumStockLevel());
        m.setUnitPrice(r.getUnitPrice());
        m.setSupplierName(r.getSupplierName());
        m.setExpiryDate(r.getExpiryDate());
        m.setDescription(r.getDescription());
    }

    public MedicalInventoryResponse toResponse(MedicalInventory m) {
        boolean lowStock = m.getMinimumStockLevel() != null
                && m.getQuantityInStock() <= m.getMinimumStockLevel();
        return MedicalInventoryResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .sku(m.getSku())
                .category(m.getCategory())
                .quantityInStock(m.getQuantityInStock())
                .minimumStockLevel(m.getMinimumStockLevel())
                .unitPrice(m.getUnitPrice())
                .supplierName(m.getSupplierName())
                .expiryDate(m.getExpiryDate())
                .description(m.getDescription())
                .lowStock(lowStock)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
