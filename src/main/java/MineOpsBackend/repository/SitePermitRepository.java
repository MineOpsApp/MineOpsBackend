package MineOpsBackend.repository;

import MineOpsBackend.model.SitePermit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SitePermitRepository extends JpaRepository<SitePermit, Long> {
    List<SitePermit> findBySiteIgnoreCaseOrderByExpiryDateAsc(String site);
}
