package com.healthcare.pms.mapper;

import com.healthcare.pms.dto.PractitionerDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class PractitionerMapper {

    public Practitioner toFhirResource(PractitionerDTO dto) {
        Practitioner practitioner = new Practitioner();

        if (dto.getId() != null && !dto.getId().isEmpty()) {
            practitioner.setId(dto.getId());
        }

        // Name
        HumanName name = new HumanName();
        name.setFamily(dto.getLastName());
        name.addGiven(dto.getFirstName());
        name.addPrefix("Dr.");
        name.setText(dto.getFullName());
        practitioner.addName(name);

        // Gender
        if (dto.getGender() != null) {
            practitioner.setGender(Enumerations.AdministrativeGender.fromCode(dto.getGender().toLowerCase()));
        }

        // Birth Date
        if (dto.getDateOfBirth() != null) {
            practitioner.setBirthDate(Date.from(dto.getDateOfBirth()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        // Telecom
        if (dto.getPhone() != null) {
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(dto.getPhone());
            practitioner.addTelecom(phone);
        }

        if (dto.getEmail() != null) {
            ContactPoint email = new ContactPoint();
            email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
            email.setValue(dto.getEmail());
            practitioner.addTelecom(email);
        }

        // Qualification
        if (dto.getQualifications() != null) {
            Practitioner.PractitionerQualificationComponent qualification = 
                new Practitioner.PractitionerQualificationComponent();
            CodeableConcept code = new CodeableConcept();
            code.setText(dto.getQualifications());
            qualification.setCode(code);
            
            if (dto.getRegistrationNumber() != null) {
                Identifier identifier = new Identifier();
                identifier.setSystem("http://healthcare.com/fhir/medical-council");
                identifier.setValue(dto.getRegistrationNumber());
                qualification.addIdentifier(identifier);
            }
            
            practitioner.addQualification(qualification);
        }

        practitioner.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Extensions for additional fields
        if (dto.getSpecialization() != null) {
            Extension specExt = new Extension();
            specExt.setUrl("http://healthcare.com/fhir/StructureDefinition/specialization");
            specExt.setValue(new StringType(dto.getSpecialization()));
            practitioner.addExtension(specExt);
        }

        if (dto.getDepartment() != null) {
            Extension deptExt = new Extension();
            deptExt.setUrl("http://healthcare.com/fhir/StructureDefinition/department");
            deptExt.setValue(new StringType(dto.getDepartment()));
            practitioner.addExtension(deptExt);
        }

        if (dto.getYearsOfExperience() != null) {
            Extension expExt = new Extension();
            expExt.setUrl("http://healthcare.com/fhir/StructureDefinition/years-experience");
            expExt.setValue(new IntegerType(dto.getYearsOfExperience()));
            practitioner.addExtension(expExt);
        }

        return practitioner;
    }

    public PractitionerDTO toDTO(Practitioner practitioner) {
        PractitionerDTO dto = new PractitionerDTO();

        dto.setId(practitioner.getIdElement().getIdPart());

        if (practitioner.hasName()) {
            HumanName name = practitioner.getNameFirstRep();
            dto.setLastName(name.getFamily());
            if (name.hasGiven()) {
                dto.setFirstName(name.getGivenAsSingleString());
            }
        }

        if (practitioner.hasGender()) {
            dto.setGender(practitioner.getGender().toCode());
        }

        if (practitioner.hasBirthDate()) {
            dto.setDateOfBirth(practitioner.getBirthDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        }

        for (ContactPoint telecom : practitioner.getTelecom()) {
            if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhone(telecom.getValue());
            } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(telecom.getValue());
            }
        }

        if (practitioner.hasQualification()) {
            Practitioner.PractitionerQualificationComponent qual = practitioner.getQualificationFirstRep();
            if (qual.hasCode()) {
                dto.setQualifications(qual.getCode().getText());
            }
            if (qual.hasIdentifier()) {
                dto.setRegistrationNumber(qual.getIdentifierFirstRep().getValue());
            }
        }

        dto.setActive(practitioner.getActive());

        for (Extension ext : practitioner.getExtension()) {
            if (ext.getUrl().contains("specialization")) {
                dto.setSpecialization(((StringType) ext.getValue()).getValue());
            } else if (ext.getUrl().contains("department")) {
                dto.setDepartment(((StringType) ext.getValue()).getValue());
            } else if (ext.getUrl().contains("years-experience")) {
                dto.setYearsOfExperience(((IntegerType) ext.getValue()).getValue());
            }
        }

        return dto;
    }
}
