package MineOpsBackend.repository;

import MineOpsBackend.model.BlastSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlastScheduleRepository extends JpaRepository<BlastSchedule, Long> {
    List<BlastSchedule> findBySiteAndStatusOrderByBlastTimeAsc(String site, String status);
    List<BlastSchedule> findBySiteOrderByBlastTimeDesc(String site);
}