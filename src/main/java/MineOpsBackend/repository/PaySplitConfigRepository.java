package MineOpsBackend.repository;

import MineOpsBackend.model.PaySplitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaySplitConfigRepository extends JpaRepository<PaySplitConfig, Long> {
    Optional<PaySplitConfig> findBySiteIgnoreCase(String site);
}
