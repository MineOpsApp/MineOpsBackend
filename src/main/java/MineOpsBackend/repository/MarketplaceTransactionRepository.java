package MineOpsBackend.repository;

import MineOpsBackend.model.MarketplaceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketplaceTransactionRepository extends JpaRepository<MarketplaceTransaction, Long> {
    List<MarketplaceTransaction> findByBuyerEmailOrderByCreatedAtDesc(String buyerEmail);
    List<MarketplaceTransaction> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
}
