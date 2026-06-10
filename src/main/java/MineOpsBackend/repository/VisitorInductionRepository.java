package MineOpsBackend.repository;

import MineOpsBackend.model.VisitorInduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitorInductionRepository extends JpaRepository<VisitorInduction, Long> {
    List<VisitorInduction> findAllByOrderByCompletedAtDesc();
}
