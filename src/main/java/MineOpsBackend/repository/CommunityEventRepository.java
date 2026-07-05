package MineOpsBackend.repository;

import MineOpsBackend.model.CommunityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityEventRepository extends JpaRepository<CommunityEvent, Long> {
    List<CommunityEvent> findAllByOrderByEventDateAsc();
}
