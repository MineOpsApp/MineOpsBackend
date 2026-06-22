package MineOpsBackend.repository;

import MineOpsBackend.model.AuditOutboxEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditOutboxRepository extends JpaRepository<AuditOutboxEntry, Long> {
    List<AuditOutboxEntry> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
