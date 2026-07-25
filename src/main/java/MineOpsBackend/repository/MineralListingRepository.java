package MineOpsBackend.repository;

import MineOpsBackend.model.MineralListing;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MineralListingRepository extends JpaRepository<MineralListing, Long> {
    List<MineralListing> findByStatusOrderByCreatedAtDesc(String status);
    List<MineralListing> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);

    @Query("SELECT m FROM MineralListing m WHERE m.status = 'ACTIVE' AND (LOWER(m.mineralType) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(m.location) LIKE LOWER(CONCAT('%',:q,'%'))) ORDER BY m.createdAt DESC")
    List<MineralListing> search(@Param("q") String q, Pageable pageable);

    // Row-locking read used when accepting an offer: two offers on the same listing accepted at
    // the same instant could otherwise both read the same starting quantity, each compute their
    // own "remaining", and the second save silently overwrites the first's deduction — effectively
    // giving away more of the listing than actually exists.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MineralListing m WHERE m.id = :id")
    Optional<MineralListing> findByIdForUpdate(@Param("id") Long id);
}
