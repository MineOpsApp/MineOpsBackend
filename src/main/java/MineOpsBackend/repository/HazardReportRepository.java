package MineOpsBackend.repository;

import MineOpsBackend.model.HazardReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HazardReportRepository extends JpaRepository<HazardReport, Long> {
    List<HazardReport> findAllByOrderByCreatedAtDesc();

    List<HazardReport> findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(String reportedByEmail);

    List<HazardReport> findBySiteOrderByCreatedAtDesc(String site);

    Page<HazardReport> findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(String reportedByEmail, Pageable pageable);

    Page<HazardReport> findBySiteOrderByCreatedAtDesc(String site, Pageable pageable);

    List<HazardReport> findBySiteAndStatusOrderByCreatedAtDesc(String site, String status);
    List<HazardReport> findBySiteAndStatusNotAndCreatedAtAfter(String site, String status, java.time.LocalDateTime after);

    @Query("SELECT h FROM HazardReport h WHERE h.site = :site AND (LOWER(h.location) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(h.hazardType) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(h.description) LIKE LOWER(CONCAT('%',:q,'%'))) ORDER BY h.createdAt DESC")
    List<HazardReport> searchBySite(@Param("site") String site, @Param("q") String q, Pageable pageable);

    Optional<HazardReport> findByClientRequestId(String clientRequestId);
}

