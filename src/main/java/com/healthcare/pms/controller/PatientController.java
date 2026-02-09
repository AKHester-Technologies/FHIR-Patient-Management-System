package com.healthcare.pms.controller;

import com.healthcare.pms.dto.PatientDTO;
import com.healthcare.pms.service.PatientService;
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
@RequestMapping("/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public String listPatients(@RequestParam(required = false) String search, Model model) {
        List<PatientDTO> patients;
        
        if (search != null && !search.isEmpty()) {
            patients = patientService.searchPatientsByName(search);
            model.addAttribute("search", search);
        } else {
            patients = patientService.getAllPatients();
        }
        
        model.addAttribute("patients", patients);
        return "patients/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("patient", new PatientDTO());
        model.addAttribute("mode", "create");
        return "patients/form";
    }

    @PostMapping
    public String createPatient(@Valid @ModelAttribute("patient") PatientDTO patientDTO,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "patients/form";
        }

        try {
            PatientDTO created = patientService.createPatient(patientDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Patient created successfully with ID: " + created.getId());
            return "redirect:/patients/" + created.getId();
        } catch (Exception e) {
            log.error("Error creating patient", e);
            model.addAttribute("errorMessage", "Error creating patient: " + e.getMessage());
            model.addAttribute("mode", "create");
            return "patients/form";
        }
    }

    @GetMapping("/{id}")
    public String viewPatient(@PathVariable String id, Model model) {
        try {
            PatientDTO patient = patientService.getPatientById(id);
            model.addAttribute("patient", patient);
            return "patients/view";
        } catch (Exception e) {
            log.error("Error fetching patient", e);
            model.addAttribute("errorMessage", "Patient not found");
            return "redirect:/patients";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model) {
        try {
            PatientDTO patient = patientService.getPatientById(id);
            model.addAttribute("patient", patient);
            model.addAttribute("mode", "edit");
            return "patients/form";
        } catch (Exception e) {
            log.error("Error fetching patient for edit", e);
            return "redirect:/patients";
        }
    }

    @PostMapping("/{id}")
    public String updatePatient(@PathVariable String id,
                               @Valid @ModelAttribute("patient") PatientDTO patientDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            return "patients/form";
        }

        try {
            patientService.updatePatient(id, patientDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Patient updated successfully");
            return "redirect:/patients/" + id;
        } catch (Exception e) {
            log.error("Error updating patient", e);
            model.addAttribute("errorMessage", "Error updating patient: " + e.getMessage());
            model.addAttribute("mode", "edit");
            return "patients/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deletePatient(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            patientService.deletePatient(id);
            redirectAttributes.addFlashAttribute("successMessage", "Patient deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting patient", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting patient");
        }
        return "redirect:/patients";
    }
}
