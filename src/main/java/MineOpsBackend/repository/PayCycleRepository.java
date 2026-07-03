package MineOpsBackend.repository;

import MineOpsBackend.model.PayCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayCycleRepository extends JpaRepository<PayCycle, Long> {
    List<PayCycle> findBySiteIgnoreCaseOrderByPayDateDesc(String site);

    Optional<PayCycle> findBySiteIgnoreCaseAndPayDateAndMineralTypeIgnoreCaseAndStatus(
        String site, String payDate, String mineralType, String status);
}
