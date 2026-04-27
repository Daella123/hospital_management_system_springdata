package com.daella.hospital_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemRequest {

    @NotBlank(message = "Medicine name is required")
    @Size(max = 100, message = "Medicine name must not exceed 100 characters")
    private String medicineName;

    @NotBlank(message = "Dosage is required")
    @Size(max = 50)
    private String dosage;

    @NotBlank(message = "Frequency is required")
    @Size(max = 100)
    private String frequency;

    @NotBlank(message = "Duration is required")
    @Size(max = 50)
    private String duration;

    @Size(max = 500, message = "Instructions must not exceed 500 characters")
    private String instructions;
}
