package MineOpsBackend.repository;

import MineOpsBackend.model.ShiftLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShiftLogRepository extends JpaRepository<ShiftLog, Long> {
    List<ShiftLog> findByWorkerEmailIgnoreCaseOrderBySubmittedAtDesc(String workerEmail);
    Page<ShiftLog> findBySiteOrderBySubmittedAtDesc(String site, Pageable pageable);
    List<ShiftLog> findBySiteOrderBySubmittedAtDesc(String site);

    long countByWorkerEmailIgnoreCase(String workerEmail);
    long countBySiteIgnoreCaseAndStatus(String site, String status);

    @Query("SELECT s FROM ShiftLog s WHERE LOWER(s.site) = LOWER(:site) AND s.shiftDate BETWEEN :periodStart AND :periodEnd AND LOWER(s.mineralType) = LOWER(:mineralType) AND s.status = 'APPROVED' AND s.payCycleId IS NULL")
    List<ShiftLog> findUnpaidApprovedLogs(@Param("site") String site, @Param("periodStart") String periodStart, @Param("periodEnd") String periodEnd, @Param("mineralType") String mineralType);

    Optional<ShiftLog> findByClientRequestId(String clientRequestId);
}