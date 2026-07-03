package MineOpsBackend.repository;

import MineOpsBackend.model.CertificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificationHistoryRepository extends JpaRepository<CertificationHistory, Long> {
    List<CertificationHistory> findByCertificationIdOrderByRenewedAtDesc(Long certificationId);
}
