package MineOpsBackend.service;

import MineOpsBackend.model.AuditLog;
import MineOpsBackend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog record(
        String action,
        String actorRole,
        String actorName,
        String actorEmail,
        String targetType,
        Long targetId,
        String details
    ) {
        return auditLogRepository.save(new AuditLog(
            valueOrUnknown(action),
            valueOrUnknown(actorRole),
            valueOrUnknown(actorName),
            valueOrUnknown(actorEmail),
            valueOrUnknown(targetType),
            targetId,
            valueOrUnknown(details)
        ));
    }

    private String valueOrUnknown(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }

        return value.trim();
    }
}
