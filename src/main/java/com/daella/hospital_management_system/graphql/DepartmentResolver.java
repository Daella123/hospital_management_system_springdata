package com.daella.hospital_management_system.graphql;

import com.daella.hospital_management_system.dto.request.DepartmentRequest;
import com.daella.hospital_management_system.dto.response.DepartmentResponse;
import com.daella.hospital_management_system.service.DepartmentService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class DepartmentResolver {

    private final DepartmentService departmentService;

    public DepartmentResolver(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public DepartmentResponse getDepartment(@Argument Long id) {
        return departmentService.getDepartmentById(id);
    }

    @QueryMapping
    public List<DepartmentResponse> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    public DepartmentResponse createDepartment(@Argument Map<String, Object> input) {
        return departmentService.createDepartment(mapToRequest(input));
    }

    @MutationMapping
    public DepartmentResponse updateDepartment(@Argument Long id, @Argument Map<String, Object> input) {
        return departmentService.updateDepartment(id, mapToRequest(input));
    }

    @MutationMapping
    public Boolean deleteDepartment(@Argument Long id) {
        departmentService.deleteDepartment(id);
        return true;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private DepartmentRequest mapToRequest(Map<String, Object> input) {
        DepartmentRequest req = new DepartmentRequest();
        req.setName((String) input.get("name"));
        req.setDescription((String) input.get("description"));
        req.setHeadOfDepartment((String) input.get("headOfDepartment"));
        return req;
    }
}
