package MineOpsBackend.repository;

import MineOpsBackend.model.SiteMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteMapRepository extends JpaRepository<SiteMap, Long> {
    Optional<SiteMap> findBySiteIgnoreCase(String site);
}
