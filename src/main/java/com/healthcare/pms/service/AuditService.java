package com.healthcare.pms.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.healthcare.pms.dto.AuditEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final IGenericClient fhirClient;

    public void createAuditEvent(String action, String resourceType, String resourceId, String description) {
        log.info("Creating audit event: {} on {} {}", action, resourceType, resourceId);
        
        try {
            AuditEvent auditEvent = new AuditEvent();
            
            // Type - what was done
            auditEvent.setType(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/audit-event-type")
                    .setCode("rest")
                    .setDisplay("RESTful Operation"));
            
            // Subtype - specific action
            auditEvent.addSubtype(new Coding()
                    .setSystem("http://hl7.org/fhir/restful-interaction")
                    .setCode(action)
                    .setDisplay(capitalize(action)));
            
            // Action
            auditEvent.setAction(getAuditEventAction(action));
            
            // Recorded time
            auditEvent.setRecorded(new Date());
            
            // Outcome
            auditEvent.setOutcome(AuditEvent.AuditEventOutcome._0); // Success
            auditEvent.setOutcomeDesc(description);
            
            // Agent (who performed the action)
            AuditEvent.AuditEventAgentComponent agent = new AuditEvent.AuditEventAgentComponent();
            agent.setRequestor(true);
            
            CodeableConcept agentType = new CodeableConcept();
            agentType.addCoding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/extra-security-role-type")
                    .setCode("humanuser")
                    .setDisplay("Human User");
            agent.setType(agentType);
            
            // Who (can be enhanced with actual user from security context)
            Reference whoRef = new Reference();
            whoRef.setDisplay("System User");
            agent.setWho(whoRef);
            
            auditEvent.addAgent(agent);
            
            // Source (system that recorded the event)
            AuditEvent.AuditEventSourceComponent source = new AuditEvent.AuditEventSourceComponent();
            Reference observerRef = new Reference();
            observerRef.setDisplay("Patient Management System");
            source.setObserver(observerRef);
            
            
            Coding sourceType = new Coding();
            sourceType.setSystem("http://terminology.hl7.org/CodeSystem/security-source-type")
                    .setCode("4")
                    .setDisplay("Application Server");
            source.addType(sourceType);
            
            auditEvent.setSource(source);
            
            // Entity (what was affected)
            AuditEvent.AuditEventEntityComponent entity = new AuditEvent.AuditEventEntityComponent();
            
            Reference whatRef = new Reference();
            whatRef.setReference(resourceType + "/" + resourceId);
            entity.setWhat(whatRef);
            
            Coding entityType = new Coding();
            entityType.setSystem("http://hl7.org/fhir/resource-types");
            entityType.setCode(resourceType);
            entityType.setDisplay(resourceType);
            entity.setType(entityType);
            
            auditEvent.addEntity(entity);
            
            // Save to FHIR server
            fhirClient.create()
                    .resource(auditEvent)
                    .execute();
            
            log.info("Audit event created successfully");
            
        } catch (Exception e) {
            log.error("Failed to create audit event", e);
            // Don't throw exception - audit failure shouldn't break business logic
        }
    }

    private AuditEvent.AuditEventAction getAuditEventAction(String action) {
        return switch (action.toLowerCase()) {
            case "create" -> AuditEvent.AuditEventAction.C;
            case "update" -> AuditEvent.AuditEventAction.U;
            case "delete" -> AuditEvent.AuditEventAction.D;
            case "read", "search" -> AuditEvent.AuditEventAction.R;
            default -> AuditEvent.AuditEventAction.E; // Execute
        };
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    public List<AuditEventDTO> getAllAuditEvents() {
        log.info("Fetching all audit events");
        
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(AuditEvent.class)
                    .sort().descending("date")
                    .count(100)
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractAuditEventsFromBundle(bundle);
        } catch (Exception e) {
            log.error("Error fetching audit events", e);
            return new ArrayList<>();
        }
    }
    
    public AuditEventDTO getAuditEventById(String id) {
        log.info("Fetching audit event ID: {}", id);
        
        try {
            AuditEvent auditEvent = fhirClient.read()
                    .resource(AuditEvent.class)
                    .withId(id)
                    .execute();
            
            return convertToDTO(auditEvent);
        } catch (Exception e) {
            log.error("Error fetching audit event", e);
            throw new RuntimeException("Audit event not found");
        }
    }
    
    public List<AuditEventDTO> searchByResourceType(String resourceType) {
        log.info("Searching audit events by resource type: {}", resourceType);
        
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(AuditEvent.class)
                    .sort().descending("date")
                    .count(100)
                    .returnBundle(Bundle.class)
                    .execute();
            
            List<AuditEventDTO> allEvents = extractAuditEventsFromBundle(bundle);
            
            return allEvents.stream()
                    .filter(e -> e.getResourceType() != null && 
                               e.getResourceType().equalsIgnoreCase(resourceType))
                    .toList();
        } catch (Exception e) {
            log.error("Error searching audit events", e);
            return new ArrayList<>();
        }
    }
    
    public List<AuditEventDTO> searchByAction(String action) {
        log.info("Searching audit events by action: {}", action);
        
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(AuditEvent.class)
                    .where(AuditEvent.SUBTYPE.exactly().code(action.toLowerCase()))
                    .sort().descending("date")
                    .count(100)
                    .returnBundle(Bundle.class)
                    .execute();
            
            return extractAuditEventsFromBundle(bundle);
        } catch (Exception e) {
            log.error("Error searching audit events", e);
            return new ArrayList<>();
        }
    }
    
    private List<AuditEventDTO> extractAuditEventsFromBundle(Bundle bundle) {
        List<AuditEventDTO> events = new ArrayList<>();
        
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof AuditEvent) {
                    AuditEvent auditEvent = (AuditEvent) entry.getResource();
                    events.add(convertToDTO(auditEvent));
                }
            }
        }
        
        log.info("Found {} audit events", events.size());
        return events;
    }
    
    private AuditEventDTO convertToDTO(AuditEvent auditEvent) {
        AuditEventDTO dto = new AuditEventDTO();
        
        dto.setId(auditEvent.getIdElement().getIdPart());
        
        if (auditEvent.hasAction()) {
            dto.setAction(auditEvent.getAction().toCode());
        }
        
        if (auditEvent.hasRecorded()) {
            dto.setRecorded(auditEvent.getRecorded().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        if (auditEvent.hasOutcome()) {
            dto.setOutcome(auditEvent.getOutcome().toCode());
        }
        
        if (auditEvent.hasOutcomeDesc()) {
            dto.setDescription(auditEvent.getOutcomeDesc());
        }
        
        if (auditEvent.hasAgent()) {
            AuditEvent.AuditEventAgentComponent agent = auditEvent.getAgentFirstRep();
            if (agent.hasWho() && agent.getWho().hasDisplay()) {
                dto.setAgentName(agent.getWho().getDisplay());
            }
        }
        
        if (auditEvent.hasSource() && auditEvent.getSource().hasObserver()) {
            dto.setSystemName(auditEvent.getSource().getObserver().getDisplay());
        }
        
        if (auditEvent.hasEntity()) {
            AuditEvent.AuditEventEntityComponent entity = auditEvent.getEntityFirstRep();
            if (entity.hasWhat() && entity.getWhat().hasReference()) {
                String reference = entity.getWhat().getReference();
                if (reference.contains("/")) {
                    String[] parts = reference.split("/");
                    dto.setResourceType(parts[0]);
                    dto.setResourceId(parts[1]);
                }
            }
        }
        
        return dto;
    }
}
