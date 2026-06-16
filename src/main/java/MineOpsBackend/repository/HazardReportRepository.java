package MineOpsBackend.repository;

import MineOpsBackend.model.HazardReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HazardReportRepository extends JpaRepository<HazardReport, Long> {
    List<HazardReport> findAllByOrderByCreatedAtDesc();

    List<HazardReport> findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(String reportedByEmail);
}
