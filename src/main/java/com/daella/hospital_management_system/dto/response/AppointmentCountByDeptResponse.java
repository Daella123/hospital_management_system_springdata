package com.daella.hospital_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Report DTO: appointment count per department.
 * Populated from the native SQL query in {@code AppointmentRepository}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentCountByDeptResponse {
    private String departmentName;
    private long   appointmentCount;
}
