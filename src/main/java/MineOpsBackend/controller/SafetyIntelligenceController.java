package MineOpsBackend.controller;

import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.SafetyIntelligenceService;
import MineOpsBackend.service.SafetyIntelligenceService.HotspotResult;
import MineOpsBackend.service.SafetyIntelligenceService.SafetyIntelligenceSummary;
import MineOpsBackend.service.SafetyIntelligenceService.TrendingHazardType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SafetyIntelligenceController {

    private final SafetyIntelligenceService service;

    public SafetyIntelligenceController(SafetyIntelligenceService service) {
        this.service = service;
    }

    @GetMapping("/api/safety-intelligence/hotspots")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER')")
    public List<HotspotResult> getHotspots(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getHotspots(user.assignedSite());
    }

    @GetMapping("/api/safety-intelligence/trends")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER')")
    public List<TrendingHazardType> getTrends(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getTrendingHazardTypes(user.assignedSite());
    }

    @GetMapping("/api/safety-intelligence/recommendations")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER')")
    public List<String> getRecommendations(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getRecommendations(user.assignedSite());
    }

    @GetMapping("/api/safety-intelligence/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER')")
    public SafetyIntelligenceSummary getSummary(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getSummary(user.assignedSite());
    }
}
