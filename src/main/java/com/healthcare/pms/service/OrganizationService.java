package com.healthcare.pms.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.healthcare.pms.dto.OrganizationDTO;
import com.healthcare.pms.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final IGenericClient fhirClient;
    private final OrganizationMapper organizationMapper;
    private final AuditService auditService;

    public OrganizationDTO createOrganization(OrganizationDTO organizationDTO) {
        log.info("Creating organization/department: {}", organizationDTO.getName());
        
        Organization organization = organizationMapper.toFhirResource(organizationDTO);
        
        MethodOutcome outcome = fhirClient.create()
                .resource(organization)
                .execute();
        
        Organization createdOrganization = (Organization) outcome.getResource();
        OrganizationDTO result = organizationMapper.toDTO(createdOrganization);
        
        auditService.createAuditEvent("create", "Organization", result.getId(), 
                "Organization/Department created successfully");
        
        log.info("Organization created with ID: {}", result.getId());
        return result;
    }

    public OrganizationDTO updateOrganization(String id, OrganizationDTO organizationDTO) {
        log.info("Updating organization ID: {}", id);
        
        organizationDTO.setId(id);
        Organization organization = organizationMapper.toFhirResource(organizationDTO);
        
        MethodOutcome outcome = fhirClient.update()
                .resource(organization)
                .execute();
        
        Organization updatedOrganization = (Organization) outcome.getResource();
        OrganizationDTO result = organizationMapper.toDTO(updatedOrganization);
        
        auditService.createAuditEvent("update", "Organization", id, 
                "Organization/Department updated successfully");
        
        log.info("Organization updated successfully: {}", id);
        return result;
    }

    public OrganizationDTO getOrganizationById(String id) {
        log.info("Fetching organization ID: {}", id);
        
        try {
            Organization organization = fhirClient.read()
                    .resource(Organization.class)
                    .withId(id)
                    .execute();
            
            return organizationMapper.toDTO(organization);
        } catch (ResourceNotFoundException e) {
            log.error("Organization not found with ID: {}", id);
            throw new RuntimeException("Organization not found with ID: " + id);
        }
    }

    public List<OrganizationDTO> getAllOrganizations() {
        log.info("Fetching all organizations/departments");
        
        Bundle bundle = fhirClient.search()
                .forResource(Organization.class)
                .returnBundle(Bundle.class)
                .execute();
        
        return extractOrganizationsFromBundle(bundle);
    }

    public List<OrganizationDTO> searchOrganizationsByName(String name) {
        log.info("Searching organizations by name: {}", name);
        
        Bundle bundle = fhirClient.search()
                .forResource(Organization.class)
                .where(Organization.NAME.matches().value(name))
                .returnBundle(Bundle.class)
                .execute();
        
        return extractOrganizationsFromBundle(bundle);
    }

    public List<OrganizationDTO> searchByType(String type) {
        log.info("Searching organizations by type: {}", type);
        
        // Get all and filter by type in Java since type is in CodeableConcept
        Bundle bundle = fhirClient.search()
                .forResource(Organization.class)
                .returnBundle(Bundle.class)
                .execute();
        
        List<OrganizationDTO> allOrganizations = extractOrganizationsFromBundle(bundle);
        
        return allOrganizations.stream()
                .filter(o -> o.getType() != null && 
                           o.getType().toLowerCase().contains(type.toLowerCase()))
                .toList();
    }

    public void deleteOrganization(String id) {
        log.info("Deleting organization ID: {}", id);
        
        fhirClient.delete()
                .resourceById("Organization", id)
                .execute();
        
        auditService.createAuditEvent("delete", "Organization", id, 
                "Organization/Department deleted");
        
        log.info("Organization deleted successfully: {}", id);
    }

    private List<OrganizationDTO> extractOrganizationsFromBundle(Bundle bundle) {
        List<OrganizationDTO> organizations = new ArrayList<>();
        
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Organization) {
                    Organization organization = (Organization) entry.getResource();
                    organizations.add(organizationMapper.toDTO(organization));
                }
            }
        }
        
        log.info("Found {} organizations", organizations.size());
        return organizations;
    }
}
