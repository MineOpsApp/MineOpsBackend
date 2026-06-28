package MineOpsBackend.repository;

import MineOpsBackend.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySiteAndStatusOrderByClockInAtDesc(String site, String status);
    Optional<AttendanceRecord> findTopByWorkerEmailIgnoreCaseAndStatusOrderByClockInAtDesc(String workerEmail, String status);
    List<AttendanceRecord> findByWorkerEmailIgnoreCaseOrderByClockInAtDesc(String workerEmail);

    List<AttendanceRecord> findAllBySiteNotNullAndStatus(String status);
}