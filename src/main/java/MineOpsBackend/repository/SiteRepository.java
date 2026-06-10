package MineOpsBackend.repository;

import MineOpsBackend.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
}
