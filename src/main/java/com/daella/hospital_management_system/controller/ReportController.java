package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.dto.response.AppointmentCountByDeptResponse;
import com.daella.hospital_management_system.dto.response.MedicalInventoryResponse;
import com.daella.hospital_management_system.dto.response.MonthlyRegistrationResponse;
import com.daella.hospital_management_system.dto.response.PrescriptionStatsResponse;
import com.daella.hospital_management_system.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporting endpoints — all read-only aggregations backed by native SQL queries.
 *
 * <p>Base path: {@code /api/v1/reports}
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /appointments-by-department} — appointment count per department</li>
 *   <li>{@code GET /monthly-registrations?year=YYYY} — monthly patient registration count</li>
 *   <li>{@code GET /prescription-stats} — prescription count per doctor</li>
 *   <li>{@code GET /low-stock} — items at or below minimum stock level</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
@Tag(name = "Reports", description = "Operational reports backed by native SQL aggregations — ADMIN & DOCTOR only")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/appointments-by-department")
    @Operation(
            summary = "Appointment count by department",
            description = "Native SQL: total appointments grouped by department, ordered by count descending."
    )
    public ResponseEntity<ApiResponse<List<AppointmentCountByDeptResponse>>> appointmentsByDepartment() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getAppointmentCountByDepartment()));
    }

    @GetMapping("/monthly-registrations")
    @Operation(
            summary = "Monthly patient registrations",
            description = "Native SQL: patient registration count grouped by month for the given year."
    )
    public ResponseEntity<ApiResponse<List<MonthlyRegistrationResponse>>> monthlyRegistrations(
            @Parameter(description = "Calendar year, e.g. 2025")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getMonthlyPatientRegistrations(year)));
    }

    @GetMapping("/prescription-stats")
    @Operation(
            summary = "Prescription count by doctor",
            description = "Native SQL: prescription count per doctor, ordered by count descending."
    )
    public ResponseEntity<ApiResponse<List<PrescriptionStatsResponse>>> prescriptionStats() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getPrescriptionStatsByDoctor()));
    }

    @GetMapping("/low-stock")
    @Operation(
            summary = "Low-stock inventory report",
            description = "JPQL: all inventory items at or below their configured minimum stock level."
    )
    public ResponseEntity<ApiResponse<List<MedicalInventoryResponse>>> lowStock() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getLowStockReport()));
    }
}
