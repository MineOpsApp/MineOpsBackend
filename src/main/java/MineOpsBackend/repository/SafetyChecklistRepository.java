package MineOpsBackend.repository;

import MineOpsBackend.model.SafetyChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SafetyChecklistRepository extends JpaRepository<SafetyChecklist, Long> {

    Optional<SafetyChecklist> findByWorkerIdAndShiftDate(Long workerId, LocalDate shiftDate);

    List<SafetyChecklist> findBySiteIgnoreCaseAndShiftDateOrderBySubmittedAtDesc(String site, LocalDate shiftDate);

    List<SafetyChecklist> findByWorkerEmailIgnoreCaseOrderByShiftDateDesc(String workerEmail);
}
