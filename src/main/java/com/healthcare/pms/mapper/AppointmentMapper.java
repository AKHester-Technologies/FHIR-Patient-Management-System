package com.healthcare.pms.mapper;

import com.healthcare.pms.dto.AppointmentDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class AppointmentMapper {

    public Appointment toFhirResource(AppointmentDTO dto) {
        Appointment appointment = new Appointment();

        if (dto.getId() != null && !dto.getId().isEmpty()) {
            appointment.setId(dto.getId());
        }

        // Status
        if (dto.getStatus() != null) {
            appointment.setStatus(Appointment.AppointmentStatus.fromCode(dto.getStatus().toLowerCase()));
        }

        // Patient participant
        if (dto.getPatientId() != null) {
            Appointment.AppointmentParticipantComponent patientParticipant = 
                new Appointment.AppointmentParticipantComponent();
            Reference patientRef = new Reference("Patient/" + dto.getPatientId());
            patientRef.setDisplay(dto.getPatientName());
            patientParticipant.setActor(patientRef);
            patientParticipant.setRequired(Appointment.ParticipantRequired.REQUIRED);
            patientParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
            appointment.addParticipant(patientParticipant);
        }

        // Practitioner participant
        if (dto.getPractitionerId() != null) {
            Appointment.AppointmentParticipantComponent practitionerParticipant = 
                new Appointment.AppointmentParticipantComponent();
            Reference practitionerRef = new Reference("Practitioner/" + dto.getPractitionerId());
            practitionerRef.setDisplay(dto.getPractitionerName());
            practitionerParticipant.setActor(practitionerRef);
            practitionerParticipant.setRequired(Appointment.ParticipantRequired.REQUIRED);
            practitionerParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);
            appointment.addParticipant(practitionerParticipant);
        }

        // Start time
        if (dto.getAppointmentDate() != null && dto.getAppointmentTime() != null) {
            LocalDateTime startDateTime = LocalDateTime.of(dto.getAppointmentDate(), dto.getAppointmentTime());
            appointment.setStart(Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));

            // End time
            if (dto.getDurationMinutes() != null) {
                LocalDateTime endDateTime = startDateTime.plusMinutes(dto.getDurationMinutes());
                appointment.setEnd(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            }
        }

        // Duration
        if (dto.getDurationMinutes() != null) {
            appointment.setMinutesDuration(dto.getDurationMinutes());
        }

        // Appointment Type
        if (dto.getAppointmentType() != null) {
            CodeableConcept appointmentType = new CodeableConcept();
            appointmentType.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v2-0276")
                    .setCode(dto.getAppointmentType().toUpperCase())
                    .setDisplay(dto.getAppointmentType());
            appointment.setAppointmentType(appointmentType);
        }

        // Reason
        if (dto.getReasonCode() != null) {
            CodeableConcept reason = new CodeableConcept();
            reason.setText(dto.getReasonCode());
            appointment.addReasonCode(reason);
        }

        if (dto.getReasonDescription() != null) {
            appointment.setDescription(dto.getReasonDescription());
        }

        // Specialty
        if (dto.getSpecialty() != null) {
            CodeableConcept specialty = new CodeableConcept();
            specialty.setText(dto.getSpecialty());
            appointment.addSpecialty(specialty);
        }

        // Comment
        if (dto.getComment() != null) {
            appointment.setComment(dto.getComment());
        }

        // Cancellation reason
        if (dto.getCancellationReason() != null) {
            CodeableConcept cancelReason = new CodeableConcept();
            cancelReason.setText(dto.getCancellationReason());
            appointment.setCancelationReason(cancelReason);
        }

        return appointment;
    }

    public AppointmentDTO toDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();

        dto.setId(appointment.getIdElement().getIdPart());

        if (appointment.hasStatus()) {
            dto.setStatus(appointment.getStatus().toCode());
        }

        // Extract patient and practitioner from participants
        for (Appointment.AppointmentParticipantComponent participant : appointment.getParticipant()) {
            if (participant.hasActor()) {
                Reference actor = participant.getActor();
                String reference = actor.getReference();
                
                if (reference.startsWith("Patient/")) {
                    dto.setPatientId(reference.replace("Patient/", ""));
                    dto.setPatientName(actor.getDisplay());
                } else if (reference.startsWith("Practitioner/")) {
                    dto.setPractitionerId(reference.replace("Practitioner/", ""));
                    dto.setPractitionerName(actor.getDisplay());
                }
            }
        }

        // Start date/time
        if (appointment.hasStart()) {
            LocalDateTime startDateTime = appointment.getStart().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            dto.setAppointmentDate(startDateTime.toLocalDate());
            dto.setAppointmentTime(startDateTime.toLocalTime());
        }

        // Duration
        if (appointment.hasMinutesDuration()) {
            dto.setDurationMinutes(appointment.getMinutesDuration());
        }

        // Appointment Type
        if (appointment.hasAppointmentType()) {
            dto.setAppointmentType(appointment.getAppointmentType().getCodingFirstRep().getDisplay());
        }

        // Reason
        if (appointment.hasReasonCode()) {
            dto.setReasonCode(appointment.getReasonCodeFirstRep().getText());
        }

        if (appointment.hasDescription()) {
            dto.setReasonDescription(appointment.getDescription());
        }

        // Specialty
        if (appointment.hasSpecialty()) {
            dto.setSpecialty(appointment.getSpecialtyFirstRep().getText());
        }

        // Comment
        if (appointment.hasComment()) {
            dto.setComment(appointment.getComment());
        }

        // Cancellation reason
        if (appointment.hasCancelationReason()) {
            dto.setCancellationReason(appointment.getCancelationReason().getText());
        }

        return dto;
    }
}
