package MineOpsBackend.repository;

import MineOpsBackend.model.MiningPermitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MiningPermitStatusRepository extends JpaRepository<MiningPermitStatus, Long> {
    Optional<MiningPermitStatus> findBySiteIgnoreCase(String site);
}
