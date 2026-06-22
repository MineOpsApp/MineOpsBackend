package MineOpsBackend.service;

import MineOpsBackend.model.AuditOutboxEntry;
import MineOpsBackend.repository.AuditOutboxRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditOutboxRepository auditOutboxRepository;

    public AuditLogService(AuditOutboxRepository auditOutboxRepository) {
        this.auditOutboxRepository = auditOutboxRepository;
    }

    public void record(
        String action,
        String actorRole,
        String actorName,
        String actorEmail,
        String targetType,
        Long targetId,
        String details
    ) {
        AuditOutboxEntry entry = new AuditOutboxEntry();
        entry.setAction(valueOrUnknown(action));
        entry.setActorRole(valueOrUnknown(actorRole));
        entry.setActorName(valueOrUnknown(actorName));
        entry.setActorEmail(valueOrUnknown(actorEmail));
        entry.setTargetType(valueOrUnknown(targetType));
        entry.setTargetId(targetId);
        entry.setDetails(valueOrUnknown(details));
        entry.setStatus("PENDING");
        entry.setAttempts(0);
        entry.setCreatedAt(java.time.LocalDateTime.now());

        auditOutboxRepository.save(entry);
    }

    private String valueOrUnknown(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }

        return value.trim();
    }
}
