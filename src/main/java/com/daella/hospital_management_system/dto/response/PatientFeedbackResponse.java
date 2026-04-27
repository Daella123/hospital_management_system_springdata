package com.daella.hospital_management_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientFeedbackResponse {

    private Long id;
    private Long patientId;
    private String patientFullName;
    private Long doctorId;
    private String doctorFullName;
    private Integer rating;
    private String comment;
    private LocalDateTime feedbackDate;
    private LocalDateTime createdAt;
}
