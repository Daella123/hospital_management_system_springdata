package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.request.AppointmentRequest;
import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.dto.response.AppointmentResponse;
import com.daella.hospital_management_system.dto.response.PagedResponse;
import com.daella.hospital_management_system.enums.AppointmentStatus;
import com.daella.hospital_management_system.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Appointment REST controller.
 *
 * <p>All list endpoints return {@link PagedResponse} with full pagination metadata.
 *
 * <p><b>Transaction note:</b> {@code POST /} and {@code PATCH /{id}/cancel} both run under
 * {@code @Transactional(propagation=REQUIRED, rollbackFor=Exception.class)}.
 * Any failure (invalid patient/doctor, slot conflict, cancelling a completed appointment)
 * rolls back the entire operation and returns an appropriate error response.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Appointment scheduling and management")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @Operation(
            summary = "Schedule a new appointment",
            description = "Books an appointment. Rolls back if: doctor/patient not found, or the slot is already taken."
    )
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment scheduled", appointmentService.createAppointment(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentById(id)));
    }

    @GetMapping
    @Operation(
            summary = "List all appointments with pagination and sorting",
            description = "Example: GET /api/v1/appointments?page=0&size=10&sortBy=appointmentDateTime&direction=desc"
    )
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentResponse>>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")               @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by")        @RequestParam(defaultValue = "appointmentDateTime") String sortBy,
            @Parameter(description = "asc or desc")             @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(appointmentService.getAllAppointments(PageRequest.of(page, size, sort)))));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get appointments for a specific patient")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentResponse>>> byPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(appointmentService.getAppointmentsByPatient(patientId, PageRequest.of(page, size)))));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get appointments for a specific doctor")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentResponse>>> byDoctor(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(appointmentService.getAppointmentsByDoctor(doctorId, PageRequest.of(page, size)))));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter appointments by status")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentResponse>>> byStatus(
            @PathVariable AppointmentStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(appointmentService.getAppointmentsByStatus(status, PageRequest.of(page, size)))));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Filter appointments within a date range")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentResponse>>> byDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(appointmentService.getAppointmentsByDateRange(start, end, PageRequest.of(page, size)))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Appointment updated", appointmentService.updateAppointment(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id, @RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(
                ApiResponse.success("Status updated", appointmentService.updateStatus(id, status)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel an appointment",
            description = "Rolls back if the appointment is already COMPLETED."
    )
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an appointment")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(ApiResponse.success("Appointment deleted", null));
    }
}
