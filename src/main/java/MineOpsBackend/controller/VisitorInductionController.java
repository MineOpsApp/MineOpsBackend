package MineOpsBackend.controller;

import MineOpsBackend.model.VisitorInduction;
import MineOpsBackend.repository.VisitorInductionRepository;
import MineOpsBackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class VisitorInductionController {

    private final VisitorInductionRepository visitorInductionRepository;
    private final AuditLogService auditLogService;

    public VisitorInductionController(
        VisitorInductionRepository visitorInductionRepository,
        AuditLogService auditLogService
    ) {
        this.visitorInductionRepository = visitorInductionRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/inductions")
    public List<VisitorInduction> getInductions() {
        return visitorInductionRepository.findAllByOrderByCompletedAtDesc();
    }

    @PostMapping("/api/inductions")
    public VisitorInduction completeInduction(@RequestBody Map<String, String> request) {
        VisitorInduction induction = new VisitorInduction(
            request.getOrDefault("visitorType", "Guest"),
            request.getOrDefault("site", "Obuasi Mine")
        );

        VisitorInduction saved = visitorInductionRepository.save(induction);
        auditLogService.record(
            "VISITOR_INDUCTION_COMPLETED",
            request.getOrDefault("actorRole", "guest"),
            request.getOrDefault("actorName", request.getOrDefault("visitorType", "Guest")),
            request.getOrDefault("actorEmail", ""),
            "VisitorInduction",
            saved.getId(),
            saved.getSite()
        );

        return saved;
    }
}
