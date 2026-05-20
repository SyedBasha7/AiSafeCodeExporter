package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.exception.AiSafeCodeSyncException;
import com.inneo.aisafecodesync.persistence.entity.SyncProfileEntity;
import com.inneo.aisafecodesync.web.dto.ProfileStatus;
import com.inneo.aisafecodesync.web.service.ProfileService;
import com.inneo.aisafecodesync.web.service.SyncRunCoordinator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ExecutionController {

    private final ProfileService profileService;
    private final SyncRunCoordinator syncRunCoordinator;

    public ExecutionController(
            ProfileService profileService,
            SyncRunCoordinator syncRunCoordinator
    ) {
        this.profileService = profileService;
        this.syncRunCoordinator = syncRunCoordinator;
    }

    @GetMapping("/profiles/{id}/execute")
    public String confirm(@PathVariable long id, Model model) {
        SyncProfileEntity profile = profileService.getProfile(id);
        ProfileStatus status = profileService.status(id);
        model.addAttribute("profile", profile);
        model.addAttribute("configHash", status.configHash());
        model.addAttribute("profileStatus", status);
        model.addAttribute("blocks", status.executionBlocks());
        return "profiles/execute";
    }

    @PostMapping("/profiles/{id}/execute")
    public String execute(
            @PathVariable long id,
            @RequestParam(name = "executionAcknowledged", defaultValue = "false") boolean executionAcknowledged,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ProfileStatus status = profileService.status(id);
            if (!status.executionReady()) {
                redirectAttributes.addFlashAttribute("error", String.join(" ", status.executionBlocks()));
                return "redirect:/profiles/" + id + "/execute";
            }
            if (!executionAcknowledged) {
                redirectAttributes.addFlashAttribute("error", "Confirm that actual execution may write files to the target folder.");
                return "redirect:/profiles/" + id + "/execute";
            }
            long runId = syncRunCoordinator.execute(id);
            return "redirect:/reports/" + runId;
        } catch (AiSafeCodeSyncException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profiles/" + id + "/execute";
        }
    }
}
