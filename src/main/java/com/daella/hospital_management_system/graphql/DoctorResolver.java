package com.daella.hospital_management_system.graphql;

import com.daella.hospital_management_system.dto.request.DoctorRequest;
import com.daella.hospital_management_system.dto.response.DoctorResponse;
import com.daella.hospital_management_system.enums.Gender;
import com.daella.hospital_management_system.service.DoctorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class DoctorResolver {

    private final DoctorService doctorService;

    public DoctorResolver(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public DoctorResponse getDoctor(@Argument Long id) {
        return doctorService.getDoctorById(id);
    }

    @QueryMapping
    public Page<DoctorResponse> getAllDoctors(@Argument Integer page, @Argument Integer size) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 10;
        return doctorService.getAllDoctors(PageRequest.of(p, s));
    }

    @QueryMapping
    public List<DoctorResponse> getDoctorsByDepartment(@Argument Long departmentId) {
        return doctorService.getDoctorsByDepartment(departmentId);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    public DoctorResponse createDoctor(@Argument Map<String, Object> input) {
        return doctorService.createDoctor(mapToRequest(input));
    }

    @MutationMapping
    public DoctorResponse updateDoctor(@Argument Long id, @Argument Map<String, Object> input) {
        return doctorService.updateDoctor(id, mapToRequest(input));
    }

    @MutationMapping
    public Boolean deleteDoctor(@Argument Long id) {
        doctorService.deleteDoctor(id);
        return true;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private DoctorRequest mapToRequest(Map<String, Object> input) {
        DoctorRequest req = new DoctorRequest();
        req.setFirstName((String) input.get("firstName"));
        req.setLastName((String) input.get("lastName"));
        req.setEmail((String) input.get("email"));
        req.setPhone((String) input.get("phone"));
        req.setSpecialization((String) input.get("specialization"));
        req.setLicenseNumber((String) input.get("licenseNumber"));
        if (input.get("yearsOfExperience") != null) {
            req.setYearsOfExperience((Integer) input.get("yearsOfExperience"));
        }
        if (input.get("departmentId") != null) {
            req.setDepartmentId(Long.valueOf(input.get("departmentId").toString()));
        }
        if (input.get("gender") != null) {
            req.setGender(Gender.valueOf((String) input.get("gender")));
        }
        if (input.get("dateOfBirth") != null) {
            req.setDateOfBirth(LocalDate.parse((String) input.get("dateOfBirth")));
        }
        return req;
    }
}
