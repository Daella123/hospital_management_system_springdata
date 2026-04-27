package com.daella.hospital_management_system.dto.response;

import com.daella.hospital_management_system.enums.InventoryCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalInventoryResponse {

    private Long id;
    private String name;
    private String sku;
    private InventoryCategory category;
    private Integer quantityInStock;
    private Integer minimumStockLevel;
    private BigDecimal unitPrice;
    private String supplierName;
    private LocalDate expiryDate;
    private String description;
    /** True when quantityInStock <= minimumStockLevel. */
    private boolean lowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
