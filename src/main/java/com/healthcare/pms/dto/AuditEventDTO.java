package com.healthcare.pms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDTO {

    private String id;
    private String action; // CREATE, READ, UPDATE, DELETE, EXECUTE
    private String resourceType; // Patient, Practitioner, Organization, Appointment
    private String resourceId;
    private String description;
    private LocalDateTime recorded;
    private String outcome; // Success, Failure
    private String agentName; // Who performed the action
    private String systemName; // Which system
    
    // Helper methods
    public String getActionBadgeClass() {
        return switch (action != null ? action.toUpperCase() : "") {
            case "C", "CREATE" -> "bg-success";
            case "R", "READ" -> "bg-info";
            case "U", "UPDATE" -> "bg-warning";
            case "D", "DELETE" -> "bg-danger";
            case "E", "EXECUTE" -> "bg-primary";
            default -> "bg-secondary";
        };
    }
    
    public String getActionDisplayName() {
        return switch (action != null ? action.toUpperCase() : "") {
            case "C" -> "CREATE";
            case "R" -> "READ";
            case "U" -> "UPDATE";
            case "D" -> "DELETE";
            case "E" -> "EXECUTE";
            default -> action;
        };
    }
    
    public String getOutcomeBadgeClass() {
        if (outcome != null && outcome.equals("0")) {
            return "bg-success";
        }
        return "bg-danger";
    }
    
    public String getOutcomeDisplayName() {
        if (outcome != null && outcome.equals("0")) {
            return "Success";
        }
        return "Failure";
    }
}
