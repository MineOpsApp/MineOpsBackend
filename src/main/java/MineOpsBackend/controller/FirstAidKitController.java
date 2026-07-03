package MineOpsBackend.controller;

import MineOpsBackend.dto.UpdateFirstAidKitRequest;
import MineOpsBackend.model.FirstAidKit;
import MineOpsBackend.repository.FirstAidKitRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class FirstAidKitController {

    private final FirstAidKitRepository kitRepository;
    private final AuditLogService auditLogService;

    public FirstAidKitController(FirstAidKitRepository kitRepository, AuditLogService auditLogService) {
        this.kitRepository = kitRepository;
        this.auditLogService = auditLogService;
    }

    // All authenticated users can view kits (workers need to know where kits are)
    @GetMapping("/api/first-aid-kits")
    @PreAuthorize("isAuthenticated()")
    public List<FirstAidKit> getKits(@AuthenticationPrincipal AuthenticatedUser user) {
        String site = user.assignedSite() != null ? user.assignedSite() : "Unassigned";
        return kitRepository.findBySiteIgnoreCaseOrderByZoneAsc(site);
    }

    // Supervisor: create or update a kit for a zone (upsert by zone)
    @PostMapping("/api/first-aid-kits")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public FirstAidKit upsertKit(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody UpdateFirstAidKitRequest req
    ) {
        if (req.zone() == null || req.zone().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zone is required");
        if (req.location() == null || req.location().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "location is required");

        String site = user.assignedSite() != null ? user.assignedSite() : "Unassigned";

        FirstAidKit kit = kitRepository
            .findBySiteIgnoreCaseAndZoneIgnoreCase(site, req.zone().trim())
            .orElse(new FirstAidKit(site, req.zone().trim(), req.location().trim()));

        applyUpdate(kit, req, user.fullName());
        FirstAidKit saved = kitRepository.save(kit);

        auditLogService.record(
            "FIRST_AID_KIT_UPDATED",
            user.role(), user.fullName(), user.email(),
            "FirstAidKit", saved.getId(),
            saved.getZone() + " — fully stocked: " + saved.isFullyStocked()
        );
        return saved;
    }

    // Supervisor: update an existing kit by id
    @PutMapping("/api/first-aid-kits/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public FirstAidKit updateKit(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @RequestBody UpdateFirstAidKitRequest req
    ) {
        FirstAidKit kit = kitRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kit not found"));

        String site = user.assignedSite() != null ? user.assignedSite() : "Unassigned";
        if (!kit.getSite().equalsIgnoreCase(site))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kit belongs to a different site");

        if (req.location() != null && !req.location().isBlank())
            kit.setLocation(req.location().trim());

        applyUpdate(kit, req, user.fullName());
        FirstAidKit saved = kitRepository.save(kit);

        auditLogService.record(
            "FIRST_AID_KIT_UPDATED",
            user.role(), user.fullName(), user.email(),
            "FirstAidKit", saved.getId(),
            saved.getZone() + " — fully stocked: " + saved.isFullyStocked()
        );
        return saved;
    }

    // Supervisor: remove a kit record
    @DeleteMapping("/api/first-aid-kits/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public void deleteKit(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        FirstAidKit kit = kitRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kit not found"));

        String site = user.assignedSite() != null ? user.assignedSite() : "Unassigned";
        if (!kit.getSite().equalsIgnoreCase(site))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kit belongs to a different site");

        kitRepository.delete(kit);
        auditLogService.record(
            "FIRST_AID_KIT_DELETED",
            user.role(), user.fullName(), user.email(),
            "FirstAidKit", id,
            kit.getZone()
        );
    }

    private void applyUpdate(FirstAidKit kit, UpdateFirstAidKitRequest req, String checkedBy) {
        kit.setHasBandages(req.hasBandages());
        kit.setHasGloves(req.hasGloves());
        kit.setHasAntiseptic(req.hasAntiseptic());
        kit.setHasOxygen(req.hasOxygen());
        kit.setHasStretcher(req.hasStretcher());
        kit.setNotes(req.notes());
        kit.setLastCheckedBy(checkedBy);
        kit.setLastCheckedAt(LocalDateTime.now());
    }
}
