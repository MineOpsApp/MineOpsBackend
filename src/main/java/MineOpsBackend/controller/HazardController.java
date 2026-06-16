package MineOpsBackend.controller;

import MineOpsBackend.model.HazardReport;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class HazardController {

    private final HazardReportRepository hazardReportRepository;
    private final AuditLogService auditLogService;

    public HazardController(HazardReportRepository hazardReportRepository, AuditLogService auditLogService) {
        this.hazardReportRepository = hazardReportRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/hazards")
    public List<HazardReport> getHazards(@RequestParam(required = false) String reportedByEmail) {
        if (reportedByEmail != null && !reportedByEmail.trim().isEmpty()) {
            return hazardReportRepository.findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(reportedByEmail);
        }

        return hazardReportRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/hazards")
    public HazardReport createHazard(@RequestBody Map<String, String> request) {
        HazardReport report = new HazardReport(
            request.getOrDefault("reportedByRole", "worker"),
            request.getOrDefault("reportedByName", "Unknown User"),
            request.getOrDefault("reportedByEmail", ""),
            request.getOrDefault("hazardType", "General"),
            request.getOrDefault("site", "Obuasi Mine"),
            request.getOrDefault("location", "Unspecified location"),
            request.getOrDefault("description", "Hazard reported from MineOps app")
        );

        HazardReport saved = hazardReportRepository.save(report);
        auditLogService.record(
            "HAZARD_SUBMITTED",
            saved.getReportedByRole(),
            saved.getReportedByName(),
            saved.getReportedByEmail(),
            "HazardReport",
            saved.getId(),
            saved.getHazardType() + " at " + saved.getLocation()
        );

        return saved;
    }

    @PatchMapping("/api/hazards/{id}/review")
    public HazardReport reviewHazard(@PathVariable Long id, @RequestBody Map<String, String> request) {
        HazardReport report = hazardReportRepository.findById(id).orElseThrow();
        report.setStatus("REVIEWED");
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedByRole(request.getOrDefault("actorRole", "supervisor"));
        report.setReviewedByName(request.getOrDefault("actorName", "Unknown User"));
        report.setReviewedByEmail(request.getOrDefault("actorEmail", ""));
        report.setActionTaken(request.getOrDefault("actionTaken", "Hazard reviewed"));

        HazardReport saved = hazardReportRepository.save(report);
        auditLogService.record(
            "HAZARD_REVIEWED",
            saved.getReviewedByRole(),
            saved.getReviewedByName(),
            saved.getReviewedByEmail(),
            "HazardReport",
            saved.getId(),
            saved.getActionTaken()
        );

        return saved;
    }

    @PatchMapping("/api/hazards/{id}/close")
    public HazardReport closeHazard(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        Map<String, String> details = request == null ? Map.of() : request;
        HazardReport report = hazardReportRepository.findById(id).orElseThrow();
        report.setStatus("CLEARED");
        report.setClosedAt(LocalDateTime.now());
        report.setClosedByRole(details.getOrDefault("actorRole", "safetyOfficer"));
        report.setClosedByName(details.getOrDefault("actorName", "Unknown User"));
        report.setClosedByEmail(details.getOrDefault("actorEmail", ""));
        report.setActionTaken(details.getOrDefault("actionTaken", "Hazard cleared"));

        HazardReport saved = hazardReportRepository.save(report);
        auditLogService.record(
            "HAZARD_CLEARED",
            saved.getClosedByRole(),
            saved.getClosedByName(),
            saved.getClosedByEmail(),
            "HazardReport",
            saved.getId(),
            saved.getActionTaken()
        );

        return saved;
    }
}
