package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateDangerZoneRequest;
import MineOpsBackend.model.DangerZone;
import MineOpsBackend.repository.DangerZoneRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DangerZoneController {

    private final DangerZoneRepository dangerZoneRepository;
    private final AuditLogService auditLogService;

    public DangerZoneController(DangerZoneRepository dangerZoneRepository, AuditLogService auditLogService) {
        this.dangerZoneRepository = dangerZoneRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/danger-zones")
    @PreAuthorize("isAuthenticated()")
    public Page<DangerZone> getDangerZones(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        return dangerZoneRepository.findBySiteOrderByCreatedAtDesc(user.assignedSite(), pageable);
    }

    @PostMapping("/api/danger-zones")
    @PreAuthorize("hasAuthority('ROLE_SAFETY_OFFICER')")
    public DangerZone createDangerZone(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateDangerZoneRequest request
    ) {
        DangerZone zone = new DangerZone(
            user.assignedSite(),
            request.zoneName(),
            request.riskLevel()
        );

        DangerZone saved = dangerZoneRepository.save(zone);
        auditLogService.record(
            "DANGER_ZONE_CREATED",
            user.role(),
            user.fullName(),
            user.email(),
            "DangerZone",
            saved.getId(),
            saved.getZoneName() + " - " + saved.getRiskLevel()
        );

        return saved;
    }
}

