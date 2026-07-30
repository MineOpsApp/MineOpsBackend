package MineOpsBackend.repository;

import MineOpsBackend.model.MarketplaceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceTransactionRepository extends JpaRepository<MarketplaceTransaction, Long> {
    List<MarketplaceTransaction> findByBuyerEmailOrderByCreatedAtDesc(String buyerEmail);
    List<MarketplaceTransaction> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
    Optional<MarketplaceTransaction> findByPaystackReference(String paystackReference);
}
