package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.request.PatientFeedbackRequest;
import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.dto.response.PatientFeedbackResponse;
import com.daella.hospital_management_system.service.PatientFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedbacks")
@Tag(name = "Patient Feedback", description = "Patient feedback and rating management")
public class PatientFeedbackController {

    private final PatientFeedbackService feedbackService;

    public PatientFeedbackController(PatientFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @Operation(summary = "Submit patient feedback")
    public ResponseEntity<ApiResponse<PatientFeedbackResponse>> create(
            @Valid @RequestBody PatientFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feedback submitted", feedbackService.createFeedback(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get feedback by ID")
    public ResponseEntity<ApiResponse<PatientFeedbackResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getFeedbackById(id)));
    }

    @GetMapping
    @Operation(summary = "List all feedback with pagination")
    public ResponseEntity<ApiResponse<Page<PatientFeedbackResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(ApiResponse.success(
                feedbackService.getAllFeedbacks(PageRequest.of(page, size, sort))));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all feedback submitted by a patient")
    public ResponseEntity<ApiResponse<Page<PatientFeedbackResponse>>> byPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackService.getFeedbackByPatient(patientId, PageRequest.of(page, size))));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get all feedback for a specific doctor")
    public ResponseEntity<ApiResponse<Page<PatientFeedbackResponse>>> byDoctor(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackService.getFeedbackByDoctor(doctorId, PageRequest.of(page, size))));
    }

    @GetMapping("/rating")
    @Operation(summary = "Filter feedback by minimum rating (1–5)")
    public ResponseEntity<ApiResponse<Page<PatientFeedbackResponse>>> byMinRating(
            @RequestParam int min,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackService.getFeedbackByMinRating(min, PageRequest.of(page, size))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a feedback entry")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.ok(ApiResponse.success("Feedback deleted", null));
    }
}
