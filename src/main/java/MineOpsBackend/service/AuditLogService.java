package MineOpsBackend.service;

import MineOpsBackend.model.AuditOutboxEntry;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.AuditOutboxRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditOutboxRepository auditOutboxRepository;
    private final AppUserRepository appUserRepository;

    public AuditLogService(AuditOutboxRepository auditOutboxRepository, AppUserRepository appUserRepository) {
        this.auditOutboxRepository = auditOutboxRepository;
        this.appUserRepository = appUserRepository;
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

        // Resolved here (not passed in by every call site) so the audit service can scope reads
        // to a site without a signature change across the ~50 controllers that call record().
        if (actorEmail != null && !actorEmail.isBlank()) {
            appUserRepository.findByEmailIgnoreCase(actorEmail.trim())
                .ifPresent(u -> entry.setSite(u.getAssignedSite()));
        }

        auditOutboxRepository.save(entry);
    }

    private String valueOrUnknown(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }

        return value.trim();
    }
}
