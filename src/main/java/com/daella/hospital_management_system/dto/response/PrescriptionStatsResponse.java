package com.daella.hospital_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Report DTO: prescription count per doctor.
 * Populated from the native SQL query in {@code PrescriptionRepository}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionStatsResponse {
    private Long   doctorId;
    private String doctorName;
    private long   prescriptionCount;
}
