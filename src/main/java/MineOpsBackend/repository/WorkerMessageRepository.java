package MineOpsBackend.repository;

import MineOpsBackend.model.WorkerMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerMessageRepository extends JpaRepository<WorkerMessage, Long> {
    List<WorkerMessage> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
    List<WorkerMessage> findBySenderEmailIgnoreCaseOrderByCreatedAtDesc(String senderEmail);
}
