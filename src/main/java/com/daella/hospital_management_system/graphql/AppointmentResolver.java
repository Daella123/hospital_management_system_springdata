package com.daella.hospital_management_system.graphql;

import com.daella.hospital_management_system.dto.request.AppointmentRequest;
import com.daella.hospital_management_system.dto.response.AppointmentResponse;
import com.daella.hospital_management_system.enums.AppointmentStatus;
import com.daella.hospital_management_system.service.AppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class AppointmentResolver {

    private final AppointmentService appointmentService;

    public AppointmentResolver(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public AppointmentResponse getAppointment(@Argument Long id) {
        return appointmentService.getAppointmentById(id);
    }

    @QueryMapping
    public Page<AppointmentResponse> getAllAppointments(@Argument Integer page, @Argument Integer size) {
        return appointmentService.getAllAppointments(PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10));
    }

    @QueryMapping
    public Page<AppointmentResponse> getAppointmentsByPatient(
            @Argument Long patientId, @Argument Integer page, @Argument Integer size) {
        return appointmentService.getAppointmentsByPatient(patientId, PageRequest.of(
                page != null ? page : 0, size != null ? size : 10));
    }

    @QueryMapping
    public Page<AppointmentResponse> getAppointmentsByDoctor(
            @Argument Long doctorId, @Argument Integer page, @Argument Integer size) {
        return appointmentService.getAppointmentsByDoctor(doctorId, PageRequest.of(
                page != null ? page : 0, size != null ? size : 10));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    public AppointmentResponse createAppointment(@Argument Map<String, Object> input) {
        return appointmentService.createAppointment(mapToRequest(input));
    }

    @MutationMapping
    public AppointmentResponse updateAppointmentStatus(@Argument Long id, @Argument String status) {
        return appointmentService.updateStatus(id, AppointmentStatus.valueOf(status));
    }

    @MutationMapping
    public Boolean cancelAppointment(@Argument Long id) {
        appointmentService.cancelAppointment(id);
        return true;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AppointmentRequest mapToRequest(Map<String, Object> input) {
        AppointmentRequest req = new AppointmentRequest();
        req.setPatientId(Long.valueOf(input.get("patientId").toString()));
        req.setDoctorId(Long.valueOf(input.get("doctorId").toString()));
        req.setAppointmentDateTime(LocalDateTime.parse((String) input.get("appointmentDateTime")));
        req.setReason((String) input.get("reason"));
        req.setNotes((String) input.get("notes"));
        return req;
    }
}
