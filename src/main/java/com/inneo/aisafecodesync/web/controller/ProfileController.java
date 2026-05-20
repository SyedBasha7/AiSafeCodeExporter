package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.core.config.ProfileType;
import com.inneo.aisafecodesync.exception.ConfigValidationException;
import com.inneo.aisafecodesync.web.dto.ProfileForm;
import com.inneo.aisafecodesync.web.service.ProfileService;
import com.inneo.aisafecodesync.web.validation.ProfileFormValidator;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileFormValidator profileFormValidator;

    public ProfileController(ProfileService profileService, ProfileFormValidator profileFormValidator) {
        this.profileService = profileService;
        this.profileFormValidator = profileFormValidator;
    }

    @InitBinder("profileForm")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(profileFormValidator);
    }

    @ModelAttribute("profileTypes")
    public ProfileType[] profileTypes() {
        return ProfileType.values();
    }

    @GetMapping("/profiles")
    public String list(Model model) {
        model.addAttribute("profiles", profileService.listProfiles());
        return "profiles/list";
    }

    @GetMapping("/profiles/new")
    public String newProfile(Model model) {
        model.addAttribute("profileForm", profileService.newForm());
        model.addAttribute("profileId", null);
        return "profiles/form";
    }

    @PostMapping("/profiles")
    public String create(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profileId", null);
            return "profiles/form";
        }
        try {
            long id = profileService.create(form).getId();
            redirectAttributes.addFlashAttribute("message", "Profile created.");
            return "redirect:/profiles/" + id + "/edit";
        } catch (ConfigValidationException ex) {
            ex.getErrors().forEach(error -> bindingResult.reject("profile.invalid", error));
            model.addAttribute("profileId", null);
            return "profiles/form";
        }
    }

    @GetMapping("/profiles/{id}/edit")
    public String edit(@PathVariable long id, Model model) {
        model.addAttribute("profileForm", profileService.getForm(id));
        model.addAttribute("profileId", id);
        return "profiles/form";
    }

    @PostMapping("/profiles/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profileId", id);
            return "profiles/form";
        }
        try {
            profileService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "Profile saved. Run dry-run again before execution.");
            return "redirect:/profiles/" + id + "/edit";
        } catch (ConfigValidationException ex) {
            ex.getErrors().forEach(error -> bindingResult.reject("profile.invalid", error));
            model.addAttribute("profileId", id);
            return "profiles/form";
        }
    }

    @PostMapping("/profiles/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        profileService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Profile deleted.");
        return "redirect:/profiles";
    }
}
