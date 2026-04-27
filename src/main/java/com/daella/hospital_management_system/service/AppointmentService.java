package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.request.AppointmentRequest;
import com.daella.hospital_management_system.dto.response.AppointmentResponse;
import com.daella.hospital_management_system.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentRequest request);

    AppointmentResponse getAppointmentById(Long id);

    AppointmentResponse updateAppointment(Long id, AppointmentRequest request);

    AppointmentResponse updateStatus(Long id, AppointmentStatus status);

    void cancelAppointment(Long id);

    Page<AppointmentResponse> getAllAppointments(Pageable pageable);

    Page<AppointmentResponse> getAppointmentsByPatient(Long patientId, Pageable pageable);

    Page<AppointmentResponse> getAppointmentsByDoctor(Long doctorId, Pageable pageable);

    Page<AppointmentResponse> getAppointmentsByStatus(AppointmentStatus status, Pageable pageable);

    Page<AppointmentResponse> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
