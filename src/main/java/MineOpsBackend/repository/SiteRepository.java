package MineOpsBackend.repository;

import MineOpsBackend.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByNameIgnoreCase(String name);
}
