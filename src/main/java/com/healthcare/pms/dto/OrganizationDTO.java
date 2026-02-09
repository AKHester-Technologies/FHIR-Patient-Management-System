package com.healthcare.pms.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {

    private String id; // FHIR resource ID

    @NotBlank(message = "Organization name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "Type is required")
    private String type; // Hospital, Clinic, Pharmacy, Laboratory

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Email
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be 6 digits")
    private String postalCode;

    private String registrationNumber; // Hospital/Clinic registration

    private String website;

    private Boolean active = true;

    private String description;
}
