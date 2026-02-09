package com.healthcare.pms.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.healthcare.pms.dto.PatientDTO;
import com.healthcare.pms.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final IGenericClient fhirClient;
    private final PatientMapper patientMapper;
    private final AuditService auditService;

    public PatientDTO createPatient(PatientDTO patientDTO) {
        log.info("Creating patient: {} {}", patientDTO.getFirstName(), patientDTO.getLastName());
        
        Patient patient = patientMapper.toFhirResource(patientDTO);
        
        MethodOutcome outcome = fhirClient.create()
                .resource(patient)
                .execute();
        
        Patient createdPatient = (Patient) outcome.getResource();
        PatientDTO result = patientMapper.toDTO(createdPatient);
        
        // Create audit event
        auditService.createAuditEvent("create", "Patient", result.getId(), "Patient created successfully");
        
        log.info("Patient created with ID: {}", result.getId());
        return result;
    }

    public PatientDTO updatePatient(String id, PatientDTO patientDTO) {
        log.info("Updating patient ID: {}", id);
        
        patientDTO.setId(id);
        Patient patient = patientMapper.toFhirResource(patientDTO);
        
        MethodOutcome outcome = fhirClient.update()
                .resource(patient)
                .execute();
        
        Patient updatedPatient = (Patient) outcome.getResource();
        PatientDTO result = patientMapper.toDTO(updatedPatient);
        
        auditService.createAuditEvent("update", "Patient", id, "Patient updated successfully");
        
        log.info("Patient updated successfully: {}", id);
        return result;
    }

    public PatientDTO getPatientById(String id) {
        log.info("Fetching patient ID: {}", id);
        
        try {
            Patient patient = fhirClient.read()
                    .resource(Patient.class)
                    .withId(id)
                    .execute();
            
            return patientMapper.toDTO(patient);
        } catch (ResourceNotFoundException e) {
            log.error("Patient not found with ID: {}", id);
            throw new RuntimeException("Patient not found with ID: " + id);
        }
    }

    public List<PatientDTO> getAllPatients() {
        log.info("Fetching all patients");
        
        Bundle bundle = fhirClient.search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .execute();
        
        return extractPatientsFromBundle(bundle);
    }

    public List<PatientDTO> searchPatientsByName(String name) {
        log.info("Searching patients by name: {}", name);
        
        Bundle bundle = fhirClient.search()
                .forResource(Patient.class)
                .where(Patient.NAME.matches().value(name))
                .returnBundle(Bundle.class)
                .execute();
        
        return extractPatientsFromBundle(bundle);
    }

    public List<PatientDTO> searchPatientsByPhone(String phone) {
        log.info("Searching patients by phone: {}", phone);
        
        Bundle bundle = fhirClient.search()
                .forResource(Patient.class)
                .where(Patient.TELECOM.exactly().code(phone))
                .returnBundle(Bundle.class)
                .execute();
        
        return extractPatientsFromBundle(bundle);
    }

    public void deletePatient(String id) {
        log.info("Deleting patient ID: {}", id);
        
        fhirClient.delete()
                .resourceById("Patient", id)
                .execute();
        
        auditService.createAuditEvent("delete", "Patient", id, "Patient deleted");
        
        log.info("Patient deleted successfully: {}", id);
    }

    private List<PatientDTO> extractPatientsFromBundle(Bundle bundle) {
        List<PatientDTO> patients = new ArrayList<>();
        
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Patient) {
                    Patient patient = (Patient) entry.getResource();
                    patients.add(patientMapper.toDTO(patient));
                }
            }
        }
        
        log.info("Found {} patients", patients.size());
        return patients;
    }
}
