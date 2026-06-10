package MineOpsBackend.repository;

import MineOpsBackend.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(String workerEmail);
}
