package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.SupervisorSiteAccess;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.SupervisorSiteAccessRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/my-sites")
public class SiteAccessController {

    private final AppUserRepository appUserRepository;
    private final SupervisorSiteAccessRepository siteAccessRepository;
    private final AuditLogService auditLogService;

    public SiteAccessController(AppUserRepository appUserRepository,
                                SupervisorSiteAccessRepository siteAccessRepository,
                                AuditLogService auditLogService) {
        this.appUserRepository = appUserRepository;
        this.siteAccessRepository = siteAccessRepository;
        this.auditLogService = auditLogService;
    }

    record SiteEntry(Long id, String site, boolean isCurrent, boolean isHome, String grantedByEmail, String grantedAt) {}

    record SwitchRequest(String site) {}

    record GrantRequest(String supervisorEmail, String site) {}

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public List<SiteEntry> getMySites(@AuthenticationPrincipal AuthenticatedUser principal) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(principal.email()).orElseThrow();
        String currentSite = user.getAssignedSite();
        String homeSite = user.getHomeSite() != null ? user.getHomeSite() : currentSite;
        if (homeSite == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No site assigned to your account");
        }

        List<SiteEntry> result = new ArrayList<>();
        // home site first
        result.add(new SiteEntry(null, homeSite, homeSite.equals(currentSite), true, null, null));

        for (SupervisorSiteAccess grant : siteAccessRepository.findBySupervisorEmail(principal.email())) {
            if (grant.getSite().equals(homeSite)) continue; // skip if granted site == home (shouldn't happen but be safe)
            result.add(new SiteEntry(
                grant.getId(),
                grant.getSite(),
                grant.getSite().equals(currentSite),
                false,
                grant.getGrantedByEmail(),
                grant.getGrantedAt().toString()
            ));
        }
        return result;
    }

    @PostMapping("/switch")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public void switchSite(@AuthenticationPrincipal AuthenticatedUser principal,
                           @RequestBody SwitchRequest req) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(principal.email()).orElseThrow();
        String homeSite = user.getHomeSite() != null ? user.getHomeSite() : user.getAssignedSite();
        if (req.site() == null || req.site().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "site is required");
        }
        String targetSite = req.site();

        boolean isHome = targetSite.equals(homeSite);
        boolean isGranted = siteAccessRepository.findBySupervisorEmailAndSite(principal.email(), targetSite).isPresent();

        if (!isHome && !isGranted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to site: " + targetSite);
        }

        user.setAssignedSite(targetSite);
        appUserRepository.save(user);

        auditLogService.record(
            "SITE_SWITCHED",
            "supervisor",
            principal.fullName(),
            principal.email(),
            "SITE",
            null,
            "Switched active site to: " + targetSite
        );
    }

    @PostMapping("/grant")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public SiteEntry grantAccess(@AuthenticationPrincipal AuthenticatedUser principal,
                                 @RequestBody GrantRequest req) {
        AppUser target = appUserRepository.findByEmailIgnoreCase(req.supervisorEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supervisor not found"));

        if (!"supervisor".equals(target.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not a supervisor");
        }
        if (!req.site().equalsIgnoreCase(principal.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You can only grant access to your current site: " + principal.assignedSite());
        }

        // idempotent — return existing if already granted
        var existing = siteAccessRepository.findBySupervisorEmailAndSite(req.supervisorEmail(), req.site());
        if (existing.isPresent()) {
            SupervisorSiteAccess g = existing.get();
            return new SiteEntry(g.getId(), g.getSite(), false, false, g.getGrantedByEmail(), g.getGrantedAt().toString());
        }

        SupervisorSiteAccess grant = new SupervisorSiteAccess();
        grant.setSupervisorEmail(req.supervisorEmail());
        grant.setSite(req.site());
        grant.setGrantedByEmail(principal.email());
        grant.setGrantedAt(LocalDateTime.now());
        SupervisorSiteAccess saved = siteAccessRepository.save(grant);

        auditLogService.record(
            "SITE_ACCESS_GRANTED",
            "supervisor",
            principal.fullName(),
            principal.email(),
            "SUPERVISOR",
            target.getId(),
            "Granted access to site: " + req.site() + " for supervisor: " + req.supervisorEmail()
        );

        return new SiteEntry(saved.getId(), saved.getSite(), false, false, saved.getGrantedByEmail(), saved.getGrantedAt().toString());
    }

    @DeleteMapping("/grant/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public void revokeAccess(@AuthenticationPrincipal AuthenticatedUser principal,
                             @PathVariable Long id) {
        SupervisorSiteAccess grant = siteAccessRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grant not found"));

        // Mirrors grantAccess: a supervisor can only manage grants for the site they're currently at.
        if (!grant.getSite().equalsIgnoreCase(principal.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You can only revoke access grants for your current site: " + principal.assignedSite());
        }

        AppUser target = appUserRepository.findByEmailIgnoreCase(grant.getSupervisorEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supervisor not found"));

        // 409 if this is their currently active site — must switch away first
        if (grant.getSite().equals(target.getAssignedSite())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot revoke access while this is the supervisor's active site. They must switch away first.");
        }

        siteAccessRepository.delete(grant);

        auditLogService.record(
            "SITE_ACCESS_REVOKED",
            "supervisor",
            principal.fullName(),
            principal.email(),
            "SUPERVISOR",
            target.getId(),
            "Revoked access to site: " + grant.getSite() + " from supervisor: " + grant.getSupervisorEmail()
        );
    }
}
