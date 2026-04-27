package com.daella.hospital_management_system.graphql;

import com.daella.hospital_management_system.dto.request.PatientRequest;
import com.daella.hospital_management_system.dto.response.PatientResponse;
import com.daella.hospital_management_system.enums.BloodType;
import com.daella.hospital_management_system.enums.Gender;
import com.daella.hospital_management_system.service.PatientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.Map;

@Controller
public class PatientResolver {

    private final PatientService patientService;

    public PatientResolver(PatientService patientService) {
        this.patientService = patientService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public PatientResponse getPatient(@Argument Long id) {
        return patientService.getPatientById(id);
    }

    @QueryMapping
    public Page<PatientResponse> getAllPatients(
            @Argument Integer page, @Argument Integer size) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 10;
        return patientService.getAllPatients(PageRequest.of(p, s));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    public PatientResponse createPatient(@Argument Map<String, Object> input) {
        return patientService.createPatient(mapToRequest(input));
    }

    @MutationMapping
    public PatientResponse updatePatient(@Argument Long id, @Argument Map<String, Object> input) {
        return patientService.updatePatient(id, mapToRequest(input));
    }

    @MutationMapping
    public Boolean deletePatient(@Argument Long id) {
        patientService.deletePatient(id);
        return true;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private PatientRequest mapToRequest(Map<String, Object> input) {
        PatientRequest req = new PatientRequest();
        req.setFirstName((String) input.get("firstName"));
        req.setLastName((String) input.get("lastName"));
        req.setEmail((String) input.get("email"));
        req.setPhone((String) input.get("phone"));
        req.setAddress((String) input.get("address"));
        req.setMedicalHistory((String) input.get("medicalHistory"));
        req.setEmergencyContactName((String) input.get("emergencyContactName"));
        req.setEmergencyContactPhone((String) input.get("emergencyContactPhone"));
        if (input.get("dateOfBirth") != null) {
            req.setDateOfBirth(LocalDate.parse((String) input.get("dateOfBirth")));
        }
        if (input.get("gender") != null) {
            req.setGender(Gender.valueOf((String) input.get("gender")));
        }
        if (input.get("bloodType") != null) {
            req.setBloodType(BloodType.valueOf((String) input.get("bloodType")));
        }
        return req;
    }
}
