package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.core.config.ConfigHasher;
import com.inneo.aisafecodesync.exception.AiSafeCodeSyncException;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.web.mapper.ProfileMapper;
import com.inneo.aisafecodesync.web.service.ExecutionEligibilityService;
import com.inneo.aisafecodesync.web.service.ProfileService;
import com.inneo.aisafecodesync.web.service.SyncRunCoordinator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ExecutionController {

    private final ProfileService profileService;
    private final ProfileMapper profileMapper;
    private final ConfigHasher configHasher;
    private final ExecutionEligibilityService eligibilityService;
    private final SyncRunCoordinator syncRunCoordinator;

    public ExecutionController(
            ProfileService profileService,
            ProfileMapper profileMapper,
            ConfigHasher configHasher,
            ExecutionEligibilityService eligibilityService,
            SyncRunCoordinator syncRunCoordinator
    ) {
        this.profileService = profileService;
        this.profileMapper = profileMapper;
        this.configHasher = configHasher;
        this.eligibilityService = eligibilityService;
        this.syncRunCoordinator = syncRunCoordinator;
    }

    @GetMapping("/profiles/{id}/execute")
    public String confirm(@PathVariable long id, Model model) {
        SyncProfileEntity profile = profileService.getProfile(id);
        String hash = configHasher.hash(profileService.toConfig(id));
        model.addAttribute("profile", profile);
        model.addAttribute("configHash", hash);
        model.addAttribute("blocks", eligibilityService.explainExecutionBlocks(profile, hash));
        return "profiles/execute";
    }

    @PostMapping("/profiles/{id}/execute")
    public String execute(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            long runId = syncRunCoordinator.execute(id);
            return "redirect:/reports/" + runId;
        } catch (AiSafeCodeSyncException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profiles/" + id + "/execute";
        }
    }
}
