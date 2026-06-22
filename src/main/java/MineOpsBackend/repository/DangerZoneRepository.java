package MineOpsBackend.repository;

import MineOpsBackend.model.DangerZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DangerZoneRepository extends JpaRepository<DangerZone, Long> {
    List<DangerZone> findAllByOrderByCreatedAtDesc();

    List<DangerZone> findBySiteOrderByCreatedAtDesc(String site);

    Page<DangerZone> findBySiteOrderByCreatedAtDesc(String site, Pageable pageable);
}