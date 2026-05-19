package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.request.DoctorRequest;
import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.dto.response.DoctorResponse;
import com.daella.hospital_management_system.dto.response.PagedResponse;
import com.daella.hospital_management_system.service.DoctorService;
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
 * Doctor REST controller.
 *
 * <p>Paginated list endpoints return {@link PagedResponse}.
 *
 * <p><b>Caching note:</b> {@code GET /{id}} is cached in "doctors".
 * {@code PUT /{id}} refreshes the entry; {@code DELETE /{id}} evicts it.
 */
@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Doctors", description = "Doctor management endpoints")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new doctor", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DoctorResponse>> create(@Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor registered", doctorService.createDoctor(request)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get doctor by ID",
            description = "Result is cached in the 'doctors' cache. Subsequent calls with the same ID are served from memory."
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorById(id)));
    }

    @GetMapping
    @Operation(
            summary = "List all doctors with pagination and sorting",
            description = "Example: GET /api/v1/doctors?page=0&size=10&sortBy=lastName&direction=asc"
    )
    public ResponseEntity<ApiResponse<PagedResponse<DoctorResponse>>> getAll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")               @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by")        @RequestParam(defaultValue = "lastName") String sortBy,
            @Parameter(description = "asc or desc")             @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(doctorService.getAllDoctors(PageRequest.of(page, size, sort)))));
    }

    @GetMapping("/search")
    @Operation(summary = "Search doctors by name")
    public ResponseEntity<ApiResponse<PagedResponse<DoctorResponse>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(doctorService.searchDoctors(query, PageRequest.of(page, size)))));
    }

    @GetMapping("/specialization")
    @Operation(summary = "Filter doctors by specialization")
    public ResponseEntity<ApiResponse<PagedResponse<DoctorResponse>>> bySpecialization(
            @RequestParam String value,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedResponse.of(doctorService.getDoctorsBySpecialization(value, PageRequest.of(page, size)))));
    }

    @GetMapping("/department/{departmentId}")
    @Operation(summary = "Get all doctors belonging to a department")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> byDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorsByDepartment(departmentId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a doctor's information",
            description = "Updates the doctor and refreshes the 'doctors' cache entry for this ID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> update(
            @PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Doctor updated", doctorService.updateDoctor(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a doctor",
            description = "Deletes the doctor and evicts the 'doctors' cache entry for this ID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor deleted", null));
    }
}
