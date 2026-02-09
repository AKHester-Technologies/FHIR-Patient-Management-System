package com.healthcare.pms.controller;

import com.healthcare.pms.dto.OrganizationDTO;
import com.healthcare.pms.service.OrganizationService;
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
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public String listOrganizations(@RequestParam(required = false) String search,
                                   @RequestParam(required = false) String type,
                                   Model model) {
        List<OrganizationDTO> organizations;
        
        if (type != null && !type.isEmpty()) {
            organizations = organizationService.searchByType(type);
            model.addAttribute("type", type);
        } else if (search != null && !search.isEmpty()) {
            organizations = organizationService.searchOrganizationsByName(search);
            model.addAttribute("search", search);
        } else {
            organizations = organizationService.getAllOrganizations();
        }
        
        model.addAttribute("organizations", organizations);
        return "organizations/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("organization", new OrganizationDTO());
        model.addAttribute("mode", "create");
        return "organizations/form";
    }

    @PostMapping
    public String createOrganization(@Valid @ModelAttribute("organization") OrganizationDTO organizationDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "organizations/form";
        }

        try {
            OrganizationDTO created = organizationService.createOrganization(organizationDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Organization/Department created successfully with ID: " + created.getId());
            return "redirect:/organizations/" + created.getId();
        } catch (Exception e) {
            log.error("Error creating organization", e);
            model.addAttribute("errorMessage", "Error creating organization: " + e.getMessage());
            model.addAttribute("mode", "create");
            return "organizations/form";
        }
    }

    @GetMapping("/{id}")
    public String viewOrganization(@PathVariable String id, Model model) {
        try {
            OrganizationDTO organization = organizationService.getOrganizationById(id);
            model.addAttribute("organization", organization);
            return "organizations/view";
        } catch (Exception e) {
            log.error("Error fetching organization", e);
            model.addAttribute("errorMessage", "Organization not found");
            return "redirect:/organizations";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model) {
        try {
            OrganizationDTO organization = organizationService.getOrganizationById(id);
            model.addAttribute("organization", organization);
            model.addAttribute("mode", "edit");
            return "organizations/form";
        } catch (Exception e) {
            log.error("Error fetching organization for edit", e);
            return "redirect:/organizations";
        }
    }

    @PostMapping("/{id}")
    public String updateOrganization(@PathVariable String id,
                                    @Valid @ModelAttribute("organization") OrganizationDTO organizationDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            return "organizations/form";
        }

        try {
            organizationService.updateOrganization(id, organizationDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Organization/Department updated successfully");
            return "redirect:/organizations/" + id;
        } catch (Exception e) {
            log.error("Error updating organization", e);
            model.addAttribute("errorMessage", "Error updating organization: " + e.getMessage());
            model.addAttribute("mode", "edit");
            return "organizations/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteOrganization(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            organizationService.deleteOrganization(id);
            redirectAttributes.addFlashAttribute("successMessage", "Organization/Department deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting organization", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting organization");
        }
        return "redirect:/organizations";
    }
}
