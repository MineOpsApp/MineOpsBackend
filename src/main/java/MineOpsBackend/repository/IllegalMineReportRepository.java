package MineOpsBackend.repository;

import MineOpsBackend.model.IllegalMineReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IllegalMineReportRepository extends JpaRepository<IllegalMineReport, Long> {
}
