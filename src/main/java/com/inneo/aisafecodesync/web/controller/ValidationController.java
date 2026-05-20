package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.web.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ValidationController {

    private final ProfileService profileService;

    public ValidationController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profiles/{id}/validate")
    public String validate(@PathVariable long id, Model model) {
        model.addAttribute("profile", profileService.getProfile(id));
        model.addAttribute("validation", profileService.validate(id));
        return "validation/result";
    }
}
