package MineOpsBackend.repository;

import MineOpsBackend.model.PayCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayCycleRepository extends JpaRepository<PayCycle, Long> {
    List<PayCycle> findBySiteIgnoreCaseOrderByPeriodStartDesc(String site);

    Optional<PayCycle> findBySiteIgnoreCaseAndPeriodStartAndPeriodEndAndMineralTypeIgnoreCaseAndStatus(
        String site, String periodStart, String periodEnd, String mineralType, String status);
}
