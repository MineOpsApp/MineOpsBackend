package MineOpsBackend.repository;

import MineOpsBackend.model.DrillOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrillOperationRepository extends JpaRepository<DrillOperation, Long> {
    List<DrillOperation> findByWorkerEmailIgnoreCaseOrderByStartedAtDesc(String workerEmail);
    List<DrillOperation> findBySiteOrderByStartedAtDesc(String site);
}
