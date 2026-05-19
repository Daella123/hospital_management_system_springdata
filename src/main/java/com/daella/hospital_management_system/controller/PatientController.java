package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.request.PatientRequest;
import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.dto.response.PagedResponse;
import com.daella.hospital_management_system.dto.response.PatientResponse;
import com.daella.hospital_management_system.service.PatientService;
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

/**
 * Patient REST controller.
 *
 * <p>Paginated endpoints return {@link PagedResponse} — a structured wrapper that exposes
 * {@code content}, {@code pageNumber}, {@code pageSize}, {@code totalElements},
 * {@code totalPages}, and {@code last} instead of the raw Spring {@code Page} JSON.
 *
 * <p><b>Caching note:</b> {@code GET /{id}} is served from the "patients" cache after
 * the first hit. {@code PUT /{id}} refreshes the cache entry; {@code DELETE /{id}} evicts it.
 */
@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Patient management endpoints")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    @Operation(summary = "Register a new patient", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PatientResponse>> create(@Valid @RequestBody PatientRequest request) {
        PatientResponse body = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient registered successfully", body));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get patient by ID",
            description = "Result is cached in the 'patients' cache. Subsequent calls with the same ID are served from memory.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PatientResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(patientService.getPatientById(id)));
    }

    @GetMapping
    @Operation(
            summary = "List all patients with pagination and sorting",
            description = "Returns a PagedResponse with content, pageNumber, pageSize, totalElements, totalPages, last. " +
                          "Example: GET /api/v1/patients?page=0&size=10&sortBy=lastName&direction=asc"
    )
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponse>>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")               @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by")        @RequestParam(defaultValue = "lastName") String sortBy,
            @Parameter(description = "asc or desc")             @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PagedResponse<PatientResponse> paged =
                PagedResponse.of(patientService.getAllPatients(PageRequest.of(page, size, sort)));
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search patients by first or last name",
            description = "Case-insensitive partial-match search. Returns a PagedResponse."
    )
    public ResponseEntity<ApiResponse<PagedResponse<PatientResponse>>> search(
            @Parameter(description = "Name fragment to search") @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PatientResponse> paged =
                PagedResponse.of(patientService.searchPatients(query, PageRequest.of(page, size)));
        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    @Operation(
            summary = "Update a patient's information",
            description = "Updates the patient and refreshes the 'patients' cache entry for this ID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PatientResponse>> update(
            @PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Patient updated successfully", patientService.updatePatient(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a patient",
            description = "Deletes the patient and evicts the 'patients' cache entry for this ID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }
}
