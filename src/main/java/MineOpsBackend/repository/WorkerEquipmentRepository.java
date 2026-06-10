package MineOpsBackend.repository;

import MineOpsBackend.model.WorkerEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerEquipmentRepository extends JpaRepository<WorkerEquipment, Long> {
    List<WorkerEquipment> findByWorkerEmailIgnoreCase(String workerEmail);
}
