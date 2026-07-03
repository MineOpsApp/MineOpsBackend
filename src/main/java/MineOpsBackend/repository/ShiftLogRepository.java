package MineOpsBackend.repository;

import MineOpsBackend.model.ShiftLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftLogRepository extends JpaRepository<ShiftLog, Long> {
    List<ShiftLog> findByWorkerEmailIgnoreCaseOrderBySubmittedAtDesc(String workerEmail);
    Page<ShiftLog> findBySiteOrderBySubmittedAtDesc(String site, Pageable pageable);
    List<ShiftLog> findBySiteOrderBySubmittedAtDesc(String site);

    long countByWorkerEmailIgnoreCase(String workerEmail);
    long countBySiteIgnoreCaseAndStatus(String site, String status);
}