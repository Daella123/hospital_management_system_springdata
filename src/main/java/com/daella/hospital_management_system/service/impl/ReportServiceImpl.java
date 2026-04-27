package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.response.AppointmentCountByDeptResponse;
import com.daella.hospital_management_system.dto.response.MedicalInventoryResponse;
import com.daella.hospital_management_system.dto.response.MonthlyRegistrationResponse;
import com.daella.hospital_management_system.dto.response.PrescriptionStatsResponse;
import com.daella.hospital_management_system.repository.AppointmentRepository;
import com.daella.hospital_management_system.repository.MedicalInventoryRepository;
import com.daella.hospital_management_system.repository.PatientRepository;
import com.daella.hospital_management_system.repository.PrescriptionRepository;
import com.daella.hospital_management_system.service.MedicalInventoryService;
import com.daella.hospital_management_system.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Report service implementation.
 *
 * <p>All methods use {@code readOnly = true} — they only aggregate existing data
 * and have no write operations, allowing Hibernate to skip dirty-checking overhead.
 *
 * <p><b>Performance notes:</b>
 * <ul>
 *   <li>Native SQL queries bypass JPQL entity materialisation, returning raw projections
 *       that are mapped to lightweight DTOs — significantly faster for large tables.</li>
 *   <li>Results include GROUP BY aggregates computed entirely inside the database engine,
 *       avoiding any in-application counting loops.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final AppointmentRepository    appointmentRepository;
    private final PatientRepository        patientRepository;
    private final PrescriptionRepository   prescriptionRepository;
    private final MedicalInventoryService  inventoryService;

    public ReportServiceImpl(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              PrescriptionRepository prescriptionRepository,
                              MedicalInventoryService inventoryService) {
        this.appointmentRepository  = appointmentRepository;
        this.patientRepository      = patientRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.inventoryService       = inventoryService;
    }

    // ── Appointment count by department ──────────────────────────────────────

    @Override
    public List<AppointmentCountByDeptResponse> getAppointmentCountByDepartment() {
        return appointmentRepository.countAppointmentsByDepartmentNative()
                .stream()
                .map(row -> AppointmentCountByDeptResponse.builder()
                        .departmentName((String) row[0])
                        .appointmentCount(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    // ── Monthly patient registrations ─────────────────────────────────────────

    @Override
    public List<MonthlyRegistrationResponse> getMonthlyPatientRegistrations(int year) {
        return patientRepository.countRegistrationsByMonthNative(year)
                .stream()
                .map(row -> MonthlyRegistrationResponse.builder()
                        .month(((Number) row[0]).intValue())
                        .registrationCount(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    // ── Prescription stats by doctor ──────────────────────────────────────────

    @Override
    public List<PrescriptionStatsResponse> getPrescriptionStatsByDoctor() {
        return prescriptionRepository.countPrescriptionsByDoctorNative()
                .stream()
                .map(row -> PrescriptionStatsResponse.builder()
                        .doctorId(((Number) row[0]).longValue())
                        .doctorName((String) row[1])
                        .prescriptionCount(((Number) row[2]).longValue())
                        .build())
                .toList();
    }

    // ── Low-stock report ──────────────────────────────────────────────────────

    @Override
    public List<MedicalInventoryResponse> getLowStockReport() {
        return inventoryService.getLowStockItems();
    }
}
