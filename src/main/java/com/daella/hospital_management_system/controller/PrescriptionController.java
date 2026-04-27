package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.request.PrescriptionRequest;
import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.dto.response.PrescriptionResponse;
import com.daella.hospital_management_system.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prescriptions")
@Tag(name = "Prescriptions", description = "Prescription management endpoints")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    @Operation(summary = "Create a prescription for an appointment")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> create(@Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prescription created", prescriptionService.createPrescription(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prescription by ID")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getPrescriptionById(id)));
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Get prescription for a specific appointment")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> byAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                prescriptionService.getPrescriptionByAppointment(appointmentId)));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all prescriptions for a patient")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getPrescriptionsByPatient(patientId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a prescription")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Prescription updated", prescriptionService.updatePrescription(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a prescription")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.ok(ApiResponse.success("Prescription deleted", null));
    }
}
