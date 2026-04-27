package com.daella.hospital_management_system.repository;

import com.daella.hospital_management_system.entity.MedicalInventory;
import com.daella.hospital_management_system.enums.InventoryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalInventoryRepository extends JpaRepository<MedicalInventory, Long> {

    // ── Derived Queries ───────────────────────────────────────────────────────

    Optional<MedicalInventory> findBySku(String sku);

    boolean existsBySku(String sku);

    Page<MedicalInventory> findByCategory(InventoryCategory category, Pageable pageable);

    Page<MedicalInventory> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // ── JPQL Queries ──────────────────────────────────────────────────────────

    /** Items whose stock is at or below the configured minimum (JPQL). */
    @Query("SELECT m FROM MedicalInventory m WHERE m.minimumStockLevel IS NOT NULL AND m.quantityInStock <= m.minimumStockLevel")
    List<MedicalInventory> findLowStockItems();

    // ── Native SQL Queries ────────────────────────────────────────────────────

    /**
     * Native SQL — inventory items whose expiry date is before the given date.
     * Useful for expiry management reports.
     */
    @Query(value = """
            SELECT *
            FROM medical_inventory
            WHERE expiry_date IS NOT NULL
              AND expiry_date < :beforeDate
            ORDER BY expiry_date ASC
            """, nativeQuery = true)
    List<MedicalInventory> findExpiringBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * Native SQL — inventory usage summary: items with quantity below minimum,
     * sorted by the gap (min - current) descending for priority restocking.
     */
    @Query(value = """
            SELECT id, name, sku, category, quantity_in_stock, minimum_stock_level,
                   (minimum_stock_level - quantity_in_stock) AS shortage
            FROM medical_inventory
            WHERE minimum_stock_level IS NOT NULL
              AND quantity_in_stock < minimum_stock_level
            ORDER BY shortage DESC
            """, nativeQuery = true)
    List<Object[]> findLowStockReportNative();
}

