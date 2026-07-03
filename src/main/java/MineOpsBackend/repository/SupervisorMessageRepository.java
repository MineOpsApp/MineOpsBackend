package MineOpsBackend.repository;

import MineOpsBackend.model.SupervisorMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupervisorMessageRepository extends JpaRepository<SupervisorMessage, Long> {
    List<SupervisorMessage> findAllByOrderByCreatedAtDesc();
    List<SupervisorMessage> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
}
