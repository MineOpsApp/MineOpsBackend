package MineOpsBackend.repository;

import MineOpsBackend.model.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionTierRepository extends JpaRepository<SubscriptionTier, Long> {
    List<SubscriptionTier> findByActiveTrue();
}
