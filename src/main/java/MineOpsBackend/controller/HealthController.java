package MineOpsBackend.controller;

import MineOpsBackend.model.AuditOutboxEntry;
import MineOpsBackend.repository.AuditOutboxRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class HealthController {

    private final AuditOutboxRepository auditOutboxRepository;

    public HealthController(AuditOutboxRepository auditOutboxRepository) {
        this.auditOutboxRepository = auditOutboxRepository;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // TEMPORARY diagnostic — no auth, aggregate counts + action/status/attempts only (no
    // actorEmail/details, to avoid exposing anything sensitive on an open route). Added to
    // trace why nothing has reached the audit trail since ~17:41 despite the app itself working
    // normally. Remove once the relay issue is confirmed fixed.
    @GetMapping("/api/debug/outbox-status")
    public Map<String, Object> outboxStatus() {
        long pending = auditOutboxRepository.countByStatus("PENDING");
        long sent = auditOutboxRepository.countByStatus("SENT");
        long failed = auditOutboxRepository.countByStatus("FAILED");

        List<Map<String, Object>> recentPending = auditOutboxRepository.findTop5ByStatusOrderByCreatedAtDesc("PENDING")
            .stream().map(HealthController::summarize).collect(Collectors.toList());
        List<Map<String, Object>> recentFailed = auditOutboxRepository.findTop5ByStatusOrderByCreatedAtDesc("FAILED")
            .stream().map(HealthController::summarize).collect(Collectors.toList());
        List<Map<String, Object>> recentAny = auditOutboxRepository.findTop5ByOrderByCreatedAtDesc()
            .stream().map(HealthController::summarize).collect(Collectors.toList());

        return Map.of(
            "counts", Map.of("pending", pending, "sent", sent, "failed", failed),
            "recentPending", recentPending,
            "recentFailed", recentFailed,
            "mostRecentOverall", recentAny
        );
    }

    private static Map<String, Object> summarize(AuditOutboxEntry e) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("action", e.getAction());
        m.put("status", e.getStatus());
        m.put("attempts", e.getAttempts());
        m.put("createdAt", String.valueOf(e.getCreatedAt()));
        m.put("lastAttemptAt", e.getLastAttemptAt() == null ? null : String.valueOf(e.getLastAttemptAt()));
        return m;
    }
}
