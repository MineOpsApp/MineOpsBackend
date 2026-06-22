package MineOpsBackend.repository;

import MineOpsBackend.model.EquipmentShiftLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentShiftLogRepository extends JpaRepository<EquipmentShiftLog, Long> {
    List<EquipmentShiftLog> findByWorkerEmailIgnoreCaseOrderByLoggedAtDesc(String workerEmail);
    List<EquipmentShiftLog> findByEquipmentCodeOrderByLoggedAtDesc(String equipmentCode);
}
