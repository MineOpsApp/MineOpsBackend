package MineOpsBackend.repository;

import MineOpsBackend.model.IncidentReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    List<IncidentReport> findByReportedByEmailIgnoreCaseOrderByReportedAtDesc(String email);
    List<IncidentReport> findBySiteOrderByReportedAtDesc(String site);
    List<IncidentReport> findBySiteAndStatusInAndReportedAtAfter(String site, java.util.List<String> statuses, java.time.LocalDateTime after);

    @Query("SELECT i FROM IncidentReport i WHERE i.site = :site AND (LOWER(i.description) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(i.category) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(i.zone) LIKE LOWER(CONCAT('%',:q,'%'))) ORDER BY i.reportedAt DESC")
    List<IncidentReport> searchBySite(@Param("site") String site, @Param("q") String q, Pageable pageable);

    java.util.Optional<IncidentReport> findByClientRequestId(String clientRequestId);
}