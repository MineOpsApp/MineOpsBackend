package MineOpsBackend.repository;

import MineOpsBackend.model.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    List<IncidentReport> findByReportedByEmailIgnoreCaseOrderByReportedAtDesc(String email);
    List<IncidentReport> findBySiteOrderByReportedAtDesc(String site);
    List<IncidentReport> findBySiteAndStatusInAndReportedAtAfter(String site, java.util.List<String> statuses, java.time.LocalDateTime after);
}