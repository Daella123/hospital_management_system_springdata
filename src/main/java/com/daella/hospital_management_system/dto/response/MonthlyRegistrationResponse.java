package com.daella.hospital_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Report DTO: patient registration count per month.
 * Populated from the native SQL query in {@code PatientRepository}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyRegistrationResponse {
    private int  month;
    private long registrationCount;
}
