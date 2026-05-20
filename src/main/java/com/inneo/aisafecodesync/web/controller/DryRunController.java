package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.exception.AiSafeCodeSyncException;
import com.inneo.aisafecodesync.web.service.SyncRunCoordinator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DryRunController {

    private final SyncRunCoordinator syncRunCoordinator;

    public DryRunController(SyncRunCoordinator syncRunCoordinator) {
        this.syncRunCoordinator = syncRunCoordinator;
    }

    @PostMapping("/profiles/{id}/dry-run")
    public String dryRun(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            long runId = syncRunCoordinator.runDryRun(id);
            return "redirect:/reports/" + runId;
        } catch (AiSafeCodeSyncException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profiles/" + id + "/edit";
        }
    }
}
