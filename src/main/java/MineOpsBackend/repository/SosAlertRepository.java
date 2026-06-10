package MineOpsBackend.repository;

import MineOpsBackend.model.SosAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SosAlertRepository extends JpaRepository<SosAlert, Long> {
    List<SosAlert> findAllByOrderByCreatedAtDesc();
}
