package MineOpsBackend.repository;

import MineOpsBackend.model.MarketplaceOffer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketplaceOfferRepository extends JpaRepository<MarketplaceOffer, Long> {
    List<MarketplaceOffer> findByBuyerEmailOrderByCreatedAtAsc(String buyerEmail);
    List<MarketplaceOffer> findByListingIdOrderByCreatedAtAsc(Long listingId);
    long countByListingIdInAndStatus(List<Long> listingIds, String status);

    // Row-locking read used by accept/reject/counter: prevents a double-tap or two near-simultaneous
    // requests (e.g. supervisor and buyer both acting at once) from both passing the "status is
    // still PENDING" check before either write commits.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM MarketplaceOffer o WHERE o.id = :id")
    Optional<MarketplaceOffer> findByIdForUpdate(@Param("id") Long id);
}
