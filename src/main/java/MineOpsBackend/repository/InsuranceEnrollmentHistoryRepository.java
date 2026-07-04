package MineOpsBackend.repository;

import MineOpsBackend.model.InsuranceEnrollmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceEnrollmentHistoryRepository extends JpaRepository<InsuranceEnrollmentHistory, Long> {
}
