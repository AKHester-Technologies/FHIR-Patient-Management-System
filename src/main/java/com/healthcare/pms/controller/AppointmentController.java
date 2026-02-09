package com.healthcare.pms.controller;

import com.healthcare.pms.dto.AppointmentDTO;
import com.healthcare.pms.service.AppointmentService;
import com.healthcare.pms.service.PatientService;
import com.healthcare.pms.service.PractitionerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final PractitionerService practitionerService;

    @GetMapping
    public String listAppointments(@RequestParam(required = false) String patientId,
                                   @RequestParam(required = false) String practitionerId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   Model model) {
        List<AppointmentDTO> appointments;
        
        if (patientId != null && !patientId.isEmpty()) {
            appointments = appointmentService.getAppointmentsByPatient(patientId);
            model.addAttribute("filterType", "patient");
            model.addAttribute("patientId", patientId);
        } else if (practitionerId != null && !practitionerId.isEmpty()) {
            appointments = appointmentService.getAppointmentsByPractitioner(practitionerId);
            model.addAttribute("filterType", "practitioner");
            model.addAttribute("practitionerId", practitionerId);
        } else if (date != null) {
            appointments = appointmentService.getAppointmentsByDate(date);
            model.addAttribute("filterType", "date");
            model.addAttribute("date", date);
        } else {
            appointments = appointmentService.getAllAppointments();
        }
        
        model.addAttribute("appointments", appointments);
        return "appointments/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) String patientId,
                                 @RequestParam(required = false) String practitionerId,
                                 Model model) {
        AppointmentDTO appointment = new AppointmentDTO();
        
        // Pre-populate if patientId or practitionerId provided
        if (patientId != null) {
            appointment.setPatientId(patientId);
            try {
                var patient = patientService.getPatientById(patientId);
                appointment.setPatientName(patient.getFullName());
            } catch (Exception e) {
                log.warn("Could not fetch patient");
            }
        }
        
        if (practitionerId != null) {
            appointment.setPractitionerId(practitionerId);
            try {
                var practitioner = practitionerService.getPractitionerById(practitionerId);
                appointment.setPractitionerName(practitioner.getFullName());
            } catch (Exception e) {
                log.warn("Could not fetch practitioner");
            }
        }
        
        model.addAttribute("appointment", appointment);
        model.addAttribute("mode", "create");
        
        // Load patients and practitioners for dropdowns
        try {
            model.addAttribute("patients", patientService.getAllPatients());
            model.addAttribute("practitioners", practitionerService.getAllPractitioners());
        } catch (Exception e) {
            log.warn("Could not load patients or practitioners: {}", e.getMessage());
        }
        
        return "appointments/form";
    }

    @PostMapping
    public String createAppointment(@Valid @ModelAttribute("appointment") AppointmentDTO appointmentDTO,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            try {
                model.addAttribute("patients", patientService.getAllPatients());
                model.addAttribute("practitioners", practitionerService.getAllPractitioners());
            } catch (Exception e) {
                log.warn("Could not load patients or practitioners");
            }
            return "appointments/form";
        }

        try {
            AppointmentDTO created = appointmentService.createAppointment(appointmentDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Appointment created successfully with ID: " + created.getId());
            return "redirect:/appointments/" + created.getId();
        } catch (Exception e) {
            log.error("Error creating appointment", e);
            model.addAttribute("errorMessage", "Error creating appointment: " + e.getMessage());
            model.addAttribute("mode", "create");
            try {
                model.addAttribute("patients", patientService.getAllPatients());
                model.addAttribute("practitioners", practitionerService.getAllPractitioners());
            } catch (Exception ex) {
                log.warn("Could not load patients or practitioners");
            }
            return "appointments/form";
        }
    }

    @GetMapping("/{id}")
    public String viewAppointment(@PathVariable String id, Model model) {
        try {
            AppointmentDTO appointment = appointmentService.getAppointmentById(id);
            model.addAttribute("appointment", appointment);
            return "appointments/view";
        } catch (Exception e) {
            log.error("Error fetching appointment", e);
            model.addAttribute("errorMessage", "Appointment not found");
            return "redirect:/appointments";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model) {
        try {
            AppointmentDTO appointment = appointmentService.getAppointmentById(id);
            model.addAttribute("appointment", appointment);
            model.addAttribute("mode", "edit");
            
            try {
                model.addAttribute("patients", patientService.getAllPatients());
                model.addAttribute("practitioners", practitionerService.getAllPractitioners());
            } catch (Exception e) {
                log.warn("Could not load patients or practitioners");
            }
            
            return "appointments/form";
        } catch (Exception e) {
            log.error("Error fetching appointment for edit", e);
            return "redirect:/appointments";
        }
    }

    @PostMapping("/{id}")
    public String updateAppointment(@PathVariable String id,
                                   @Valid @ModelAttribute("appointment") AppointmentDTO appointmentDTO,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            try {
                model.addAttribute("patients", patientService.getAllPatients());
                model.addAttribute("practitioners", practitionerService.getAllPractitioners());
            } catch (Exception e) {
                log.warn("Could not load patients or practitioners");
            }
            return "appointments/form";
        }

        try {
            appointmentService.updateAppointment(id, appointmentDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment updated successfully");
            return "redirect:/appointments/" + id;
        } catch (Exception e) {
            log.error("Error updating appointment", e);
            model.addAttribute("errorMessage", "Error updating appointment: " + e.getMessage());
            model.addAttribute("mode", "edit");
            try {
                model.addAttribute("patients", patientService.getAllPatients());
                model.addAttribute("practitioners", practitionerService.getAllPractitioners());
            } catch (Exception ex) {
                log.warn("Could not load patients or practitioners");
            }
            return "appointments/form";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelAppointment(@PathVariable String id,
                                   @RequestParam(required = false) String reason,
                                   RedirectAttributes redirectAttributes) {
        try {
            appointmentService.cancelAppointment(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled successfully");
        } catch (Exception e) {
            log.error("Error cancelling appointment", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling appointment");
        }
        return "redirect:/appointments/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteAppointment(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.deleteAppointment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting appointment", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting appointment");
        }
        return "redirect:/appointments";
    }
}
