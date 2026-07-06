package MineOpsBackend.repository;

import MineOpsBackend.model.SiteSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteSubscriptionRepository extends JpaRepository<SiteSubscription, Long> {
    Optional<SiteSubscription> findBySiteIgnoreCase(String site);
}
