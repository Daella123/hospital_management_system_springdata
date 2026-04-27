package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.request.MedicalInventoryRequest;
import com.daella.hospital_management_system.dto.response.MedicalInventoryResponse;
import com.daella.hospital_management_system.enums.InventoryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MedicalInventoryService {

    MedicalInventoryResponse createItem(MedicalInventoryRequest request);

    MedicalInventoryResponse getItemById(Long id);

    MedicalInventoryResponse updateItem(Long id, MedicalInventoryRequest request);

    void deleteItem(Long id);

    Page<MedicalInventoryResponse> getAllItems(Pageable pageable);

    Page<MedicalInventoryResponse> getItemsByCategory(InventoryCategory category, Pageable pageable);

    Page<MedicalInventoryResponse> searchItems(String name, Pageable pageable);

    List<MedicalInventoryResponse> getLowStockItems();

    /** Adds (positive) or removes (negative) stock units. */
    MedicalInventoryResponse adjustStock(Long id, int delta);
}
