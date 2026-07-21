package MineOpsBackend.repository;

import MineOpsBackend.model.InspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long> {
    List<InspectionRecord> findBySiteOrderByCreatedAtDesc(String site);
    List<InspectionRecord> findByInspectorUserIdOrderByCreatedAtDesc(Long inspectorUserId);
}
