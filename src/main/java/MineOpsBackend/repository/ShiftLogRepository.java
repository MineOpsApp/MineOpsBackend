package MineOpsBackend.repository;

import MineOpsBackend.model.ShiftLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShiftLogRepository extends JpaRepository<ShiftLog, Long> {
    List<ShiftLog> findByWorkerEmailIgnoreCaseOrderBySubmittedAtDesc(String workerEmail);
    Page<ShiftLog> findBySiteOrderBySubmittedAtDesc(String site, Pageable pageable);
    List<ShiftLog> findBySiteOrderBySubmittedAtDesc(String site);

    long countByWorkerEmailIgnoreCase(String workerEmail);
    long countBySiteIgnoreCaseAndStatus(String site, String status);

    @Query("SELECT s FROM ShiftLog s WHERE LOWER(s.site) = LOWER(:site) AND s.shiftDate = :shiftDate AND LOWER(s.mineralType) = LOWER(:mineralType) AND s.status = 'APPROVED' AND s.payCycleId IS NULL")
    List<ShiftLog> findUnpaidApprovedLogs(@Param("site") String site, @Param("shiftDate") String shiftDate, @Param("mineralType") String mineralType);
}