package MineOpsBackend.controller;

import MineOpsBackend.model.Site;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
public class SiteController {

    private final SiteRepository siteRepository;
    private final AuditLogService auditLogService;

    public SiteController(SiteRepository siteRepository, AuditLogService auditLogService) {
        this.siteRepository = siteRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/sites")
    @PreAuthorize("isAuthenticated()")
    public List<Site> getSites() {
        return siteRepository.findAll();
    }

    @PatchMapping("/api/sites/visibility")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Site updateVisibility(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Boolean> body
    ) {
        Site site = siteRepository.findByNameIgnoreCase(user.assignedSite())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));
        boolean visible = Boolean.TRUE.equals(body.get("visible"));
        site.setInventoryVisibleToGuests(visible);
        Site saved = siteRepository.save(site);
        auditLogService.record(
            "INVENTORY_VISIBILITY_CHANGED",
            user.role(), user.fullName(), user.email(),
            "SITE", saved.getId(),
            "inventoryVisibleToGuests=" + visible
        );
        return saved;
    }
}
