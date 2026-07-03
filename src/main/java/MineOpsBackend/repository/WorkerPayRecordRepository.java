package MineOpsBackend.repository;

import MineOpsBackend.model.WorkerPayRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkerPayRecordRepository extends JpaRepository<WorkerPayRecord, Long> {
    List<WorkerPayRecord> findByPayCycleId(Long payCycleId);
    List<WorkerPayRecord> findByWorkerEmailIgnoreCaseOrderByIdDesc(String workerEmail);
}
