package MineOpsBackend.repository;

import MineOpsBackend.model.EquipmentFault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentFaultRepository extends JpaRepository<EquipmentFault, Long> {
    List<EquipmentFault> findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(String workerEmail);
    List<EquipmentFault> findAllByOrderByCreatedAtDesc();
}
