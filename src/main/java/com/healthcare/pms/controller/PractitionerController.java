package com.healthcare.pms.controller;

import com.healthcare.pms.dto.PractitionerDTO;
import com.healthcare.pms.service.OrganizationService;
import com.healthcare.pms.service.PractitionerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/practitioners")
@RequiredArgsConstructor
@Slf4j
public class PractitionerController {

    private final PractitionerService practitionerService;
    private final OrganizationService organizationService;

    @GetMapping
    public String listPractitioners(@RequestParam(required = false) String search,
                                    @RequestParam(required = false) String specialization,
                                    Model model) {
        List<PractitionerDTO> practitioners;
        
        if (specialization != null && !specialization.isEmpty()) {
            practitioners = practitionerService.searchBySpecialization(specialization);
            model.addAttribute("specialization", specialization);
        } else if (search != null && !search.isEmpty()) {
            practitioners = practitionerService.searchPractitionersByName(search);
            model.addAttribute("search", search);
        } else {
            practitioners = practitionerService.getAllPractitioners();
        }
        
        model.addAttribute("practitioners", practitioners);
        return "practitioners/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("practitioner", new PractitionerDTO());
        model.addAttribute("mode", "create");
        
        // Load organizations/departments for selection
        try {
            model.addAttribute("organizations", organizationService.getAllOrganizations());
        } catch (Exception e) {
            log.warn("Could not load organizations: {}", e.getMessage());
        }
        
        return "practitioners/form";
    }

    @PostMapping
    public String createPractitioner(@Valid @ModelAttribute("practitioner") PractitionerDTO practitionerDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            try {
                model.addAttribute("organizations", organizationService.getAllOrganizations());
            } catch (Exception e) {
                log.warn("Could not load organizations");
            }
            return "practitioners/form";
        }

        try {
            PractitionerDTO created = practitionerService.createPractitioner(practitionerDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Practitioner created successfully with ID: " + created.getId());
            return "redirect:/practitioners/" + created.getId();
        } catch (Exception e) {
            log.error("Error creating practitioner", e);
            model.addAttribute("errorMessage", "Error creating practitioner: " + e.getMessage());
            model.addAttribute("mode", "create");
            try {
                model.addAttribute("organizations", organizationService.getAllOrganizations());
            } catch (Exception ex) {
                log.warn("Could not load organizations");
            }
            return "practitioners/form";
        }
    }

    @GetMapping("/{id}")
    public String viewPractitioner(@PathVariable String id, Model model) {
        try {
            PractitionerDTO practitioner = practitionerService.getPractitionerById(id);
            model.addAttribute("practitioner", practitioner);
            return "practitioners/view";
        } catch (Exception e) {
            log.error("Error fetching practitioner", e);
            model.addAttribute("errorMessage", "Practitioner not found");
            return "redirect:/practitioners";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model) {
        try {
            PractitionerDTO practitioner = practitionerService.getPractitionerById(id);
            model.addAttribute("practitioner", practitioner);
            model.addAttribute("mode", "edit");
            
            try {
                model.addAttribute("organizations", organizationService.getAllOrganizations());
            } catch (Exception e) {
                log.warn("Could not load organizations");
            }
            
            return "practitioners/form";
        } catch (Exception e) {
            log.error("Error fetching practitioner for edit", e);
            return "redirect:/practitioners";
        }
    }

    @PostMapping("/{id}")
    public String updatePractitioner(@PathVariable String id,
                                    @Valid @ModelAttribute("practitioner") PractitionerDTO practitionerDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            try {
                model.addAttribute("organizations", organizationService.getAllOrganizations());
            } catch (Exception e) {
                log.warn("Could not load organizations");
            }
            return "practitioners/form";
        }

        try {
            practitionerService.updatePractitioner(id, practitionerDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Practitioner updated successfully");
            return "redirect:/practitioners/" + id;
        } catch (Exception e) {
            log.error("Error updating practitioner", e);
            model.addAttribute("errorMessage", "Error updating practitioner: " + e.getMessage());
            model.addAttribute("mode", "edit");
            try {
                model.addAttribute("organizations", organizationService.getAllOrganizations());
            } catch (Exception ex) {
                log.warn("Could not load organizations");
            }
            return "practitioners/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deletePractitioner(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            practitionerService.deletePractitioner(id);
            redirectAttributes.addFlashAttribute("successMessage", "Practitioner deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting practitioner", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting practitioner");
        }
        return "redirect:/practitioners";
    }
}
