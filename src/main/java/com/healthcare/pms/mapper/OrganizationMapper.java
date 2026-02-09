package com.healthcare.pms.mapper;

import com.healthcare.pms.dto.OrganizationDTO;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public Organization toFhirResource(OrganizationDTO dto) {
        Organization organization = new Organization();

        if (dto.getId() != null && !dto.getId().isEmpty()) {
            organization.setId(dto.getId());
        }

        organization.setName(dto.getName());

        // Type
        if (dto.getType() != null) {
            CodeableConcept type = new CodeableConcept();
            type.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                    .setCode(dto.getType().toLowerCase().replace(" ", "-"))
                    .setDisplay(dto.getType());
            organization.addType(type);
        }

        // Telecom
        if (dto.getPhone() != null) {
            ContactPoint phone = new ContactPoint();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setValue(dto.getPhone());
            organization.addTelecom(phone);
        }

        if (dto.getEmail() != null) {
            ContactPoint email = new ContactPoint();
            email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
            email.setValue(dto.getEmail());
            organization.addTelecom(email);
        }

        // Address
        if (dto.getAddress() != null) {
            Address address = new Address();
            address.addLine(dto.getAddress());
            address.setCity(dto.getCity());
            address.setState(dto.getState());
            address.setPostalCode(dto.getPostalCode());
            address.setCountry("IN");
            organization.addAddress(address);
        }

        organization.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Registration Number as Identifier
        if (dto.getRegistrationNumber() != null) {
            Identifier identifier = new Identifier();
            identifier.setSystem("http://healthcare.com/fhir/organization-registration");
            identifier.setValue(dto.getRegistrationNumber());
            organization.addIdentifier(identifier);
        }

        // Extensions
        if (dto.getWebsite() != null) {
            Extension websiteExt = new Extension();
            websiteExt.setUrl("http://healthcare.com/fhir/StructureDefinition/website");
            websiteExt.setValue(new StringType(dto.getWebsite()));
            organization.addExtension(websiteExt);
        }

        if (dto.getDescription() != null) {
            Extension descExt = new Extension();
            descExt.setUrl("http://healthcare.com/fhir/StructureDefinition/description");
            descExt.setValue(new StringType(dto.getDescription()));
            organization.addExtension(descExt);
        }

        return organization;
    }

    public OrganizationDTO toDTO(Organization organization) {
        OrganizationDTO dto = new OrganizationDTO();

        dto.setId(organization.getIdElement().getIdPart());
        dto.setName(organization.getName());

        if (organization.hasType()) {
            dto.setType(organization.getTypeFirstRep().getCodingFirstRep().getDisplay());
        }

        for (ContactPoint telecom : organization.getTelecom()) {
            if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                dto.setPhone(telecom.getValue());
            } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                dto.setEmail(telecom.getValue());
            }
        }

        if (organization.hasAddress()) {
            Address address = organization.getAddressFirstRep();
            if (address.hasLine()) {
                dto.setAddress(address.getLine().get(0).getValue());
            }
            dto.setCity(address.getCity());
            dto.setState(address.getState());
            dto.setPostalCode(address.getPostalCode());
        }

        dto.setActive(organization.getActive());

        if (organization.hasIdentifier()) {
            dto.setRegistrationNumber(organization.getIdentifierFirstRep().getValue());
        }

        for (Extension ext : organization.getExtension()) {
            if (ext.getUrl().contains("website")) {
                dto.setWebsite(((StringType) ext.getValue()).getValue());
            } else if (ext.getUrl().contains("description")) {
                dto.setDescription(((StringType) ext.getValue()).getValue());
            }
        }

        return dto;
    }
}
