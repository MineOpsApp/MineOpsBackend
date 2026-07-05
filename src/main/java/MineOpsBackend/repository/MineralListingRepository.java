package MineOpsBackend.repository;

import MineOpsBackend.model.MineralListing;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MineralListingRepository extends JpaRepository<MineralListing, Long> {
    List<MineralListing> findByStatusOrderByCreatedAtDesc(String status);
    List<MineralListing> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);

    @Query("SELECT m FROM MineralListing m WHERE m.status = 'ACTIVE' AND (LOWER(m.mineralType) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(m.location) LIKE LOWER(CONCAT('%',:q,'%'))) ORDER BY m.createdAt DESC")
    List<MineralListing> search(@Param("q") String q, Pageable pageable);
}
