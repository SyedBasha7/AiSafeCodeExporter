package com.inneo.aisafecodesync.web.controller;

import com.inneo.aisafecodesync.web.service.ProfileService;
import com.inneo.aisafecodesync.web.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ProfileService profileService;
    private final ReportService reportService;

    public DashboardController(ProfileService profileService, ReportService reportService) {
        this.profileService = profileService;
        this.reportService = reportService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("profiles", profileService.listProfiles());
        model.addAttribute("recentRuns", reportService.history());
        return "dashboard";
    }
}
