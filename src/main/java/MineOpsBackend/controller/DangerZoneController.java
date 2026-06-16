package MineOpsBackend.controller;

import MineOpsBackend.model.DangerZone;
import MineOpsBackend.repository.DangerZoneRepository;
import MineOpsBackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DangerZoneController {

    private final DangerZoneRepository dangerZoneRepository;
    private final AuditLogService auditLogService;

    public DangerZoneController(DangerZoneRepository dangerZoneRepository, AuditLogService auditLogService) {
        this.dangerZoneRepository = dangerZoneRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/danger-zones")
    public List<DangerZone> getDangerZones() {
        return dangerZoneRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/danger-zones")
    public DangerZone createDangerZone(@RequestBody Map<String, String> request) {
        DangerZone zone = new DangerZone(
            request.getOrDefault("site", "Obuasi Mine"),
            request.getOrDefault("zoneName", "Restricted Area"),
            request.getOrDefault("riskLevel", "High")
        );

        DangerZone saved = dangerZoneRepository.save(zone);
        auditLogService.record(
            "DANGER_ZONE_CREATED",
            request.getOrDefault("actorRole", "safetyOfficer"),
            request.getOrDefault("actorName", "Unknown User"),
            request.getOrDefault("actorEmail", ""),
            "DangerZone",
            saved.getId(),
            saved.getZoneName() + " - " + saved.getRiskLevel()
        );

        return saved;
    }
}
