package MineOpsBackend.repository;

import MineOpsBackend.model.DangerZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DangerZoneRepository extends JpaRepository<DangerZone, Long> {
    List<DangerZone> findAllByOrderByCreatedAtDesc();
}
