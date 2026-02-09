package com.healthcare.pms.mapper;

import com.healthcare.pms.dto.PatientDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class PatientMapper {

    public Patient toFhirResource(PatientDTO dto) {
        Patient patient = new Patient();

        if (dto.getId() != null && !dto.getId().isEmpty()) {
            patient.setId(dto.getId());
        }

        // Name
        HumanName name = new HumanName();
        name.setFamily(dto.getLastName());
        name.addGiven(dto.getFirstName());
        name.setText(dto.getFullName());
        patient.addName(name);

        // Gender
        if (dto.getGender() != null) {
            switch (dto.getGender().toLowerCase()) {
                case "male":
                    patient.setGender(Enumerations.AdministrativeGender.MALE);
                    break;
                case "female":
                    patient.setGender(Enumerations.AdministrativeGender.FEMALE);
                    break;
                case "other":
                    patient.setGender(Enumerations.AdministrativeGender.OTHER);
                    break;
                default:
                    patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
            }
        }

        // Birth Date
        if (dto.getDateOfBirth() != null) {
            patient.setBirthDate(Date.from(dto.getDateOfBirth()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        // Phone
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(dto.getPhone());
            phone.setUse(ContactPoint.ContactPointUse.MOBILE);
            patient.addTelecom(phone);
        }

        // Email
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            ContactPoint email = new ContactPoint();
            email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
            email.setValue(dto.getEmail());
            patient.addTelecom(email);
        }

        // Address
        if (dto.getAddress() != null) {
            Address address = new Address();
            address.addLine(dto.getAddress());
            address.setCity(dto.getCity());
            address.setState(dto.getState());
            address.setPostalCode(dto.getPostalCode());
            address.setCountry("IN");
            patient.addAddress(address);
        }

        // Marital Status
        if (dto.getMaritalStatus() != null) {
            CodeableConcept maritalStatus = new CodeableConcept();
            maritalStatus.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                    .setCode(dto.getMaritalStatus().toUpperCase())
                    .setDisplay(dto.getMaritalStatus());
            patient.setMaritalStatus(maritalStatus);
        }

        // Active
        patient.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Extensions for Indian-specific fields
        
        // Blood Group
        if (dto.getBloodGroup() != null) {
            Extension bloodGroupExt = new Extension();
            bloodGroupExt.setUrl("http://healthcare.com/fhir/StructureDefinition/blood-group");
            bloodGroupExt.setValue(new StringType(dto.getBloodGroup()));
            patient.addExtension(bloodGroupExt);
        }

        // PAN Card
        if (dto.getPanCard() != null) {
            Extension panExt = new Extension();
            panExt.setUrl("http://healthcare.com/fhir/StructureDefinition/pan-card");
            panExt.setValue(new StringType(dto.getPanCard()));
            patient.addExtension(panExt);
        }

        // Aadhaar
        if (dto.getAadhaarNumber() != null) {
            Extension aadhaarExt = new Extension();
            aadhaarExt.setUrl("http://healthcare.com/fhir/StructureDefinition/aadhaar");
            aadhaarExt.setValue(new StringType(dto.getAadhaarNumber()));
            patient.addExtension(aadhaarExt);
        }

        // Emergency Contact
        if (dto.getEmergencyContactName() != null) {
            Patient.ContactComponent contact = new Patient.ContactComponent();
            
            HumanName contactName = new HumanName();
            contactName.setText(dto.getEmergencyContactName());
            contact.setName(contactName);

            if (dto.getEmergencyContactPhone() != null) {
                ContactPoint contactPhone = new ContactPoint();
                contactPhone.setSystem(ContactPoint.ContactPointSystem.PHONE);
                contactPhone.setValue(dto.getEmergencyContactPhone());
                contact.addTelecom(contactPhone);
            }

            if (dto.getEmergencyContactRelation() != null) {
                CodeableConcept relationship = new CodeableConcept();
                relationship.setText(dto.getEmergencyContactRelation());
                contact.addRelationship(relationship);
            }

            patient.addContact(contact);
        }

        return patient;
    }

    public PatientDTO toDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();

        dto.setId(patient.getIdElement().getIdPart());

        // Name
        if (patient.hasName()) {
            HumanName name = patient.getNameFirstRep();
            dto.setLastName(name.getFamily());
            if (name.hasGiven()) {
                dto.setFirstName(name.getGivenAsSingleString());
            }
        }

        // Gender
        if (patient.hasGender()) {
            dto.setGender(patient.getGender().toCode());
        }

        // Birth Date
        if (patient.hasBirthDate()) {
            dto.setDateOfBirth(patient.getBirthDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        }

        // Telecom
        for (ContactPoint telecom : patient.getTelecom()) {
            if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhone(telecom.getValue());
            } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(telecom.getValue());
            }
        }

        // Address
        if (patient.hasAddress()) {
            Address address = patient.getAddressFirstRep();
            if (address.hasLine()) {
                dto.setAddress(address.getLine().get(0).getValue());
            }
            dto.setCity(address.getCity());
            dto.setState(address.getState());
            dto.setPostalCode(address.getPostalCode());
        }

        // Marital Status
        if (patient.hasMaritalStatus()) {
            dto.setMaritalStatus(patient.getMaritalStatus().getCodingFirstRep().getCode());
        }

        dto.setActive(patient.getActive());

        // Extensions
        for (Extension ext : patient.getExtension()) {
            if (ext.getUrl().contains("blood-group")) {
                dto.setBloodGroup(((StringType) ext.getValue()).getValue());
            } else if (ext.getUrl().contains("pan-card")) {
                dto.setPanCard(((StringType) ext.getValue()).getValue());
            } else if (ext.getUrl().contains("aadhaar")) {
                dto.setAadhaarNumber(((StringType) ext.getValue()).getValue());
            }
        }

        // Emergency Contact
        if (patient.hasContact()) {
            Patient.ContactComponent contact = patient.getContactFirstRep();
            if (contact.hasName()) {
                dto.setEmergencyContactName(contact.getName().getText());
            }
            for (ContactPoint telecom : contact.getTelecom()) {
                if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                    dto.setEmergencyContactPhone(telecom.getValue());
                }
            }
            if (contact.hasRelationship()) {
                dto.setEmergencyContactRelation(contact.getRelationshipFirstRep().getText());
            }
        }

        return dto;
    }
}
