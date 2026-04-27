package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.response.AppointmentCountByDeptResponse;
import com.daella.hospital_management_system.dto.response.MedicalInventoryResponse;
import com.daella.hospital_management_system.dto.response.MonthlyRegistrationResponse;
import com.daella.hospital_management_system.dto.response.PrescriptionStatsResponse;

import java.util.List;

/**
 * Reporting service — aggregates data via native SQL queries for operational reports.
 */
public interface ReportService {

    /** Appointment count grouped by department (native SQL). */
    List<AppointmentCountByDeptResponse> getAppointmentCountByDepartment();

    /** Patient registration count grouped by month for a given year (native SQL). */
    List<MonthlyRegistrationResponse> getMonthlyPatientRegistrations(int year);

    /** Prescription count grouped by doctor (native SQL). */
    List<PrescriptionStatsResponse> getPrescriptionStatsByDoctor();

    /** Items currently at or below minimum stock level (JPQL, re-used from inventory service). */
    List<MedicalInventoryResponse> getLowStockReport();
}
