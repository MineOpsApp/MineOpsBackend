package MineOpsBackend.repository;

import MineOpsBackend.model.HazardReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HazardReportRepository extends JpaRepository<HazardReport, Long> {
    List<HazardReport> findAllByOrderByCreatedAtDesc();

    List<HazardReport> findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(String reportedByEmail);

    List<HazardReport> findBySiteOrderByCreatedAtDesc(String site);

    Page<HazardReport> findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(String reportedByEmail, Pageable pageable);

    Page<HazardReport> findBySiteOrderByCreatedAtDesc(String site, Pageable pageable);
}

