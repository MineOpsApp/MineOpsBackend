package MineOpsBackend.repository;

import MineOpsBackend.model.MarketplaceOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketplaceOfferRepository extends JpaRepository<MarketplaceOffer, Long> {
    List<MarketplaceOffer> findByBuyerEmailOrderByCreatedAtAsc(String buyerEmail);
    List<MarketplaceOffer> findByListingIdOrderByCreatedAtAsc(Long listingId);
    long countByListingIdInAndStatus(List<Long> listingIds, String status);
}
