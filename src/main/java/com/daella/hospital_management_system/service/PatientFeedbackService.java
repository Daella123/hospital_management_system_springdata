package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.request.PatientFeedbackRequest;
import com.daella.hospital_management_system.dto.response.PatientFeedbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientFeedbackService {

    PatientFeedbackResponse createFeedback(PatientFeedbackRequest request);

    PatientFeedbackResponse getFeedbackById(Long id);

    Page<PatientFeedbackResponse> getAllFeedbacks(Pageable pageable);

    Page<PatientFeedbackResponse> getFeedbackByPatient(Long patientId, Pageable pageable);

    Page<PatientFeedbackResponse> getFeedbackByDoctor(Long doctorId, Pageable pageable);

    Page<PatientFeedbackResponse> getFeedbackByMinRating(Integer minRating, Pageable pageable);

    void deleteFeedback(Long id);
}
