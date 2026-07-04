package MineOpsBackend.repository;

import MineOpsBackend.model.MineralListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MineralListingRepository extends JpaRepository<MineralListing, Long> {
    List<MineralListing> findByStatusOrderByCreatedAtDesc(String status);
    List<MineralListing> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
}
