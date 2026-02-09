package com.healthcare.pms.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.healthcare.pms.dto.AppointmentDTO;
import com.healthcare.pms.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final IGenericClient fhirClient;
    private final AppointmentMapper appointmentMapper;
    private final AuditService auditService;
    private final PatientService patientService;
    private final PractitionerService practitionerService;

    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        log.info("Creating appointment for patient: {}", appointmentDTO.getPatientId());
        
        // Fetch patient and practitioner names for display
        try {
            var patient = patientService.getPatientById(appointmentDTO.getPatientId());
            appointmentDTO.setPatientName(patient.getFullName());
        } catch (Exception e) {
            log.warn("Could not fetch patient name for ID: {}", appointmentDTO.getPatientId());
        }
        
        try {
            var practitioner = practitionerService.getPractitionerById(appointmentDTO.getPractitionerId());
            appointmentDTO.setPractitionerName(practitioner.getFullName());
        } catch (Exception e) {
            log.warn("Could not fetch practitioner name for ID: {}", appointmentDTO.getPractitionerId());
        }
        
        Appointment appointment = appointmentMapper.toFhirResource(appointmentDTO);
        
        MethodOutcome outcome = fhirClient.create()
                .resource(appointment)
                .execute();
        
        Appointment createdAppointment = (Appointment) outcome.getResource();
        AppointmentDTO result = appointmentMapper.toDTO(createdAppointment);
        
        auditService.createAuditEvent("create", "Appointment", result.getId(), 
                "Appointment created successfully");
        
        log.info("Appointment created with ID: {}", result.getId());
        return result;
    }

    public AppointmentDTO updateAppointment(String id, AppointmentDTO appointmentDTO) {
        log.info("Updating appointment ID: {}", id);
        
        appointmentDTO.setId(id);
        
        // Fetch names if not present
        if (appointmentDTO.getPatientName() == null) {
            try {
                var patient = patientService.getPatientById(appointmentDTO.getPatientId());
                appointmentDTO.setPatientName(patient.getFullName());
            } catch (Exception e) {
                log.warn("Could not fetch patient name");
            }
        }
        
        if (appointmentDTO.getPractitionerName() == null) {
            try {
                var practitioner = practitionerService.getPractitionerById(appointmentDTO.getPractitionerId());
                appointmentDTO.setPractitionerName(practitioner.getFullName());
            } catch (Exception e) {
                log.warn("Could not fetch practitioner name");
            }
        }
        
        Appointment appointment = appointmentMapper.toFhirResource(appointmentDTO);
        
        MethodOutcome outcome = fhirClient.update()
                .resource(appointment)
                .execute();
        
        Appointment updatedAppointment = (Appointment) outcome.getResource();
        AppointmentDTO result = appointmentMapper.toDTO(updatedAppointment);
        
        auditService.createAuditEvent("update", "Appointment", id, 
                "Appointment updated successfully");
        
        log.info("Appointment updated successfully: {}", id);
        return result;
    }

    public AppointmentDTO getAppointmentById(String id) {
        log.info("Fetching appointment ID: {}", id);
        
        try {
            Appointment appointment = fhirClient.read()
                    .resource(Appointment.class)
                    .withId(id)
                    .execute();
            
            AppointmentDTO dto = appointmentMapper.toDTO(appointment);
            
            // Enrich with names
            enrichWithNames(dto);
            
            return dto;
        } catch (ResourceNotFoundException e) {
            log.error("Appointment not found with ID: {}", id);
            throw new RuntimeException("Appointment not found with ID: " + id);
        }
    }

    public List<AppointmentDTO> getAllAppointments() {
        log.info("Fetching all appointments");
        
        Bundle bundle = fhirClient.search()
                .forResource(Appointment.class)
                .returnBundle(Bundle.class)
                .execute();
        
        List<AppointmentDTO> appointments = extractAppointmentsFromBundle(bundle);
        
        // Enrich all with names
        appointments.forEach(this::enrichWithNames);
        
        return appointments;
    }

    public List<AppointmentDTO> getAppointmentsByPatient(String patientId) {
        log.info("Fetching appointments for patient ID: {}", patientId);
        
        Bundle bundle = fhirClient.search()
                .forResource(Appointment.class)
                .where(Appointment.PATIENT.hasId(patientId))
                .returnBundle(Bundle.class)
                .execute();
        
        List<AppointmentDTO> appointments = extractAppointmentsFromBundle(bundle);
        appointments.forEach(this::enrichWithNames);
        
        return appointments;
    }

    public List<AppointmentDTO> getAppointmentsByPractitioner(String practitionerId) {
        log.info("Fetching appointments for practitioner ID: {}", practitionerId);
        
        Bundle bundle = fhirClient.search()
                .forResource(Appointment.class)
                .where(Appointment.PRACTITIONER.hasId(practitionerId))
                .returnBundle(Bundle.class)
                .execute();
        
        List<AppointmentDTO> appointments = extractAppointmentsFromBundle(bundle);
        appointments.forEach(this::enrichWithNames);
        
        return appointments;
    }

    public List<AppointmentDTO> getAppointmentsByDate(LocalDate date) {
        log.info("Fetching appointments for date: {}", date);
        
        Date startOfDay = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        Bundle bundle = fhirClient.search()
                .forResource(Appointment.class)
                .where(Appointment.DATE.afterOrEquals().day(startOfDay))
                .where(Appointment.DATE.before().day(endOfDay))
                .returnBundle(Bundle.class)
                .execute();
        
        List<AppointmentDTO> appointments = extractAppointmentsFromBundle(bundle);
        appointments.forEach(this::enrichWithNames);
        
        return appointments;
    }

    public AppointmentDTO cancelAppointment(String id, String reason) {
        log.info("Cancelling appointment ID: {}", id);
        
        Appointment appointment = fhirClient.read()
                .resource(Appointment.class)
                .withId(id)
                .execute();
        
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        
        if (reason != null && !reason.isEmpty()) {
            CodeableConcept cancelReason = new CodeableConcept();
            cancelReason.setText(reason);
            appointment.setCancelationReason(cancelReason);
        }
        
        MethodOutcome outcome = fhirClient.update()
                .resource(appointment)
                .execute();
        
        Appointment updated = (Appointment) outcome.getResource();
        AppointmentDTO result = appointmentMapper.toDTO(updated);
        
        auditService.createAuditEvent("update", "Appointment", id, 
                "Appointment cancelled: " + reason);
        
        log.info("Appointment cancelled successfully: {}", id);
        return result;
    }

    public void deleteAppointment(String id) {
        log.info("Deleting appointment ID: {}", id);
        
        fhirClient.delete()
                .resourceById("Appointment", id)
                .execute();
        
        auditService.createAuditEvent("delete", "Appointment", id, "Appointment deleted");
        
        log.info("Appointment deleted successfully: {}", id);
    }

    private List<AppointmentDTO> extractAppointmentsFromBundle(Bundle bundle) {
        List<AppointmentDTO> appointments = new ArrayList<>();
        
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Appointment) {
                    Appointment appointment = (Appointment) entry.getResource();
                    appointments.add(appointmentMapper.toDTO(appointment));
                }
            }
        }
        
        log.info("Found {} appointments", appointments.size());
        return appointments;
    }

    private void enrichWithNames(AppointmentDTO dto) {
        if (dto.getPatientName() == null && dto.getPatientId() != null) {
            try {
                var patient = patientService.getPatientById(dto.getPatientId());
                dto.setPatientName(patient.getFullName());
            } catch (Exception e) {
                log.debug("Could not fetch patient name for ID: {}", dto.getPatientId());
            }
        }
        
        if (dto.getPractitionerName() == null && dto.getPractitionerId() != null) {
            try {
                var practitioner = practitionerService.getPractitionerById(dto.getPractitionerId());
                dto.setPractitionerName(practitioner.getFullName());
            } catch (Exception e) {
                log.debug("Could not fetch practitioner name for ID: {}", dto.getPractitionerId());
            }
        }
    }
}
