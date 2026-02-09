package com.healthcare.pms.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDTO {

    private String id; // FHIR resource ID

    @NotBlank(message = "Patient is required")
    private String patientId; // Reference to Patient FHIR resource

    @NotBlank(message = "Practitioner is required")
    private String practitionerId; // Reference to Practitioner FHIR resource

    @NotNull(message = "Appointment date is required")
    @Future(message = "Appointment must be in future")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;

    @NotBlank(message = "Appointment type is required")
    private String appointmentType; // Consultation, Follow-up, Emergency

    @NotBlank(message = "Status is required")
    private String status; // proposed, pending, booked, arrived, fulfilled, cancelled, noshow

    private String reasonCode; // General consultation, Follow-up, etc.

    private String reasonDescription;

    private String specialty; // Cardiology, General Medicine, etc.

    private Integer durationMinutes = 30;

    private String comment;

    // For display purposes
    private String patientName;
    private String practitionerName;

    // Cancellation info
    private String cancellationReason;

    public String getAppointmentDateTime() {
        if (appointmentDate != null && appointmentTime != null) {
            return appointmentDate + " " + appointmentTime;
        }
        return "";
    }
}
