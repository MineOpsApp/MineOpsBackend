package MineOpsBackend.repository;

import MineOpsBackend.model.FirstAidKit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FirstAidKitRepository extends JpaRepository<FirstAidKit, Long> {
    List<FirstAidKit> findBySiteIgnoreCaseOrderByZoneAsc(String site);
    Optional<FirstAidKit> findBySiteIgnoreCaseAndZoneIgnoreCase(String site, String zone);
}
