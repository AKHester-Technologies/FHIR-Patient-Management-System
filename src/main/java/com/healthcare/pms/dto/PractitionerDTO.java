package com.healthcare.pms.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PractitionerDTO {

    private String id; // FHIR resource ID

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Date of birth is required")
    @Past
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Specialization is required")
    private String specialization; // General Practitioner, Cardiologist, etc.

    @NotBlank(message = "Registration number is required")
    private String registrationNumber; // Medical Council Registration

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Email
    private String email;

    private String qualifications; // MBBS, MD, etc.

    private Integer yearsOfExperience;

    @NotBlank(message = "Department is required")
    private String department;

    private String organizationId; // Link to Organization FHIR resource

    private Boolean active = true;

    public String getFullName() {
        return "Dr. " + firstName + " " + lastName;
    }

    public Integer getAge() {
        if (dateOfBirth != null) {
            return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
        }
        return null;
    }
}
