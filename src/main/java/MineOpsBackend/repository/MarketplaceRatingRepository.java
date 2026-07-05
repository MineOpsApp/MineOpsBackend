package MineOpsBackend.repository;

import MineOpsBackend.model.MarketplaceRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceRatingRepository extends JpaRepository<MarketplaceRating, Long> {
    boolean existsByTransactionIdAndRaterEmail(Long transactionId, String raterEmail);
    Optional<MarketplaceRating> findByTransactionIdAndRaterEmail(Long transactionId, String raterEmail);
    List<MarketplaceRating> findByTransactionIdIn(List<Long> transactionIds);
}
