package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.request.PrescriptionRequest;
import com.daella.hospital_management_system.dto.response.PrescriptionResponse;

import java.util.List;

public interface PrescriptionService {

    PrescriptionResponse createPrescription(PrescriptionRequest request);

    PrescriptionResponse getPrescriptionById(Long id);

    PrescriptionResponse getPrescriptionByAppointment(Long appointmentId);

    List<PrescriptionResponse> getPrescriptionsByPatient(Long patientId);

    PrescriptionResponse updatePrescription(Long id, PrescriptionRequest request);

    void deletePrescription(Long id);
}
