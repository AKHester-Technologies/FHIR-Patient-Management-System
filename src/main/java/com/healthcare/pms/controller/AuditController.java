package com.healthcare.pms.controller;

import com.healthcare.pms.dto.AuditEventDTO;
import com.healthcare.pms.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public String listAuditEvents(@RequestParam(required = false) String resourceType,
                                  @RequestParam(required = false) String action,
                                  Model model) {
        List<AuditEventDTO> auditEvents;
        
        if (resourceType != null && !resourceType.isEmpty()) {
            auditEvents = auditService.searchByResourceType(resourceType);
            model.addAttribute("resourceType", resourceType);
        } else if (action != null && !action.isEmpty()) {
            auditEvents = auditService.searchByAction(action);
            model.addAttribute("action", action);
        } else {
            auditEvents = auditService.getAllAuditEvents();
        }
        
        model.addAttribute("auditEvents", auditEvents);
        return "audit/list";
    }

    @GetMapping("/{id}")
    public String viewAuditEvent(@PathVariable String id, Model model) {
        try {
            AuditEventDTO auditEvent = auditService.getAuditEventById(id);
            model.addAttribute("auditEvent", auditEvent);
            return "audit/view";
        } catch (Exception e) {
            log.error("Error fetching audit event", e);
            model.addAttribute("errorMessage", "Audit event not found");
            return "redirect:/audit";
        }
    }
}
