package com.healthcare.pms.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.healthcare.pms.dto.PractitionerDTO;
import com.healthcare.pms.mapper.PractitionerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PractitionerService {

    private final IGenericClient fhirClient;
    private final PractitionerMapper practitionerMapper;
    private final AuditService auditService;

    public PractitionerDTO createPractitioner(PractitionerDTO practitionerDTO) {
        log.info("Creating practitioner: {} {}", practitionerDTO.getFirstName(), practitionerDTO.getLastName());
        
        Practitioner practitioner = practitionerMapper.toFhirResource(practitionerDTO);
        
        MethodOutcome outcome = fhirClient.create()
                .resource(practitioner)
                .execute();
        
        Practitioner createdPractitioner = (Practitioner) outcome.getResource();
        PractitionerDTO result = practitionerMapper.toDTO(createdPractitioner);
        
        auditService.createAuditEvent("create", "Practitioner", result.getId(), 
                "Practitioner created successfully");
        
        log.info("Practitioner created with ID: {}", result.getId());
        return result;
    }

    public PractitionerDTO updatePractitioner(String id, PractitionerDTO practitionerDTO) {
        log.info("Updating practitioner ID: {}", id);
        
        practitionerDTO.setId(id);
        Practitioner practitioner = practitionerMapper.toFhirResource(practitionerDTO);
        
        MethodOutcome outcome = fhirClient.update()
                .resource(practitioner)
                .execute();
        
        Practitioner updatedPractitioner = (Practitioner) outcome.getResource();
        PractitionerDTO result = practitionerMapper.toDTO(updatedPractitioner);
        
        auditService.createAuditEvent("update", "Practitioner", id, 
                "Practitioner updated successfully");
        
        log.info("Practitioner updated successfully: {}", id);
        return result;
    }

    public PractitionerDTO getPractitionerById(String id) {
        log.info("Fetching practitioner ID: {}", id);
        
        try {
            Practitioner practitioner = fhirClient.read()
                    .resource(Practitioner.class)
                    .withId(id)
                    .execute();
            
            return practitionerMapper.toDTO(practitioner);
        } catch (ResourceNotFoundException e) {
            log.error("Practitioner not found with ID: {}", id);
            throw new RuntimeException("Practitioner not found with ID: " + id);
        }
    }

    public List<PractitionerDTO> getAllPractitioners() {
        log.info("Fetching all practitioners");
        
        Bundle bundle = fhirClient.search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .execute();
        
        return extractPractitionersFromBundle(bundle);
    }

    public List<PractitionerDTO> searchPractitionersByName(String name) {
        log.info("Searching practitioners by name: {}", name);
        
        Bundle bundle = fhirClient.search()
                .forResource(Practitioner.class)
                .where(Practitioner.NAME.matches().value(name))
                .returnBundle(Bundle.class)
                .execute();
        
        return extractPractitionersFromBundle(bundle);
    }

    public List<PractitionerDTO> searchBySpecialization(String specialization) {
        log.info("Searching practitioners by specialization: {}", specialization);
        
        // Note: This searches in extensions since specialization is stored there
        Bundle bundle = fhirClient.search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .execute();
        
        List<PractitionerDTO> allPractitioners = extractPractitionersFromBundle(bundle);
        
        // Filter by specialization in Java since it's in extension
        return allPractitioners.stream()
                .filter(p -> p.getSpecialization() != null && 
                           p.getSpecialization().toLowerCase().contains(specialization.toLowerCase()))
                .toList();
    }

    public void deletePractitioner(String id) {
        log.info("Deleting practitioner ID: {}", id);
        
        fhirClient.delete()
                .resourceById("Practitioner", id)
                .execute();
        
        auditService.createAuditEvent("delete", "Practitioner", id, "Practitioner deleted");
        
        log.info("Practitioner deleted successfully: {}", id);
    }

    private List<PractitionerDTO> extractPractitionersFromBundle(Bundle bundle) {
        List<PractitionerDTO> practitioners = new ArrayList<>();
        
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Practitioner) {
                    Practitioner practitioner = (Practitioner) entry.getResource();
                    practitioners.add(practitionerMapper.toDTO(practitioner));
                }
            }
        }
        
        log.info("Found {} practitioners", practitioners.size());
        return practitioners;
    }
}
