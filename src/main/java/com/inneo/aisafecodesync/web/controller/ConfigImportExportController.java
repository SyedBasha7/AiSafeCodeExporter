package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.exception.ConfigValidationException;
import com.inneo.aisafecodesync.web.dto.ImportProfileForm;
import com.inneo.aisafecodesync.web.service.ProfileService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
public class ConfigImportExportController {

    private final ProfileService profileService;

    public ConfigImportExportController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/config/import-export")
    public String page(Model model) {
        model.addAttribute("profiles", profileService.listProfiles());
        model.addAttribute("importProfileForm", new ImportProfileForm());
        return "config/import-export";
    }

    @PostMapping("/config/import")
    public String importYaml(@ModelAttribute ImportProfileForm form, RedirectAttributes redirectAttributes, Model model) {
        try {
            long id = profileService.importYaml(form.getYaml()).getId();
            redirectAttributes.addFlashAttribute("message", "Profile imported.");
            return "redirect:/profiles/" + id + "/edit";
        } catch (ConfigValidationException ex) {
            model.addAttribute("profiles", profileService.listProfiles());
            model.addAttribute("importProfileForm", form);
            model.addAttribute("errors", ex.getErrors());
            return "config/import-export";
        }
    }

    @GetMapping("/profiles/{id}/export.yml")
    public ResponseEntity<String> exportYaml(@PathVariable long id) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.valueOf("application/yaml"), StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("profile-" + id + ".yml").build().toString())
                .body(profileService.exportYaml(id));
    }
}
