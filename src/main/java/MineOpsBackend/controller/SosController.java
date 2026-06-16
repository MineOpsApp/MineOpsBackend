package MineOpsBackend.controller;

import MineOpsBackend.model.SosAlert;
import MineOpsBackend.repository.SosAlertRepository;
import MineOpsBackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SosController {

    private final SosAlertRepository sosAlertRepository;
    private final AuditLogService auditLogService;

    public SosController(SosAlertRepository sosAlertRepository, AuditLogService auditLogService) {
        this.sosAlertRepository = sosAlertRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/sos")
    public SosAlert createAlert(@RequestBody Map<String, String> request) {
        SosAlert alert = new SosAlert(
            request.getOrDefault("role", "unknown"),
            request.getOrDefault("site", "Unassigned"),
            request.getOrDefault("message", "Emergency assistance requested")
        );

        SosAlert saved = sosAlertRepository.save(alert);
        auditLogService.record(
            "SOS_TRIGGERED",
            saved.getRole(),
            request.getOrDefault("actorName", "Unknown User"),
            request.getOrDefault("actorEmail", ""),
            "SosAlert",
            saved.getId(),
            saved.getSite() + ": " + saved.getMessage()
        );

        return saved;
    }

    @GetMapping("/api/sos")
    public List<SosAlert> getAlerts() {
        return sosAlertRepository.findAllByOrderByCreatedAtDesc();
    }
}
