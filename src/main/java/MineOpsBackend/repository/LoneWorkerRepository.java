package MineOpsBackend.repository;

import MineOpsBackend.model.LoneWorkerSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoneWorkerRepository extends JpaRepository<LoneWorkerSession, Long> {
    Optional<LoneWorkerSession> findTopByWorkerIdAndActiveOrderByStartedAtDesc(Long workerId, boolean active);
    List<LoneWorkerSession> findBySiteIgnoreCaseAndActive(String site, boolean active);
    List<LoneWorkerSession> findByActiveAndDeadlineBefore(boolean active, LocalDateTime time);
}
