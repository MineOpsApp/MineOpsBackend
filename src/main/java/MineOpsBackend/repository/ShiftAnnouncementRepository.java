package MineOpsBackend.repository;

import MineOpsBackend.model.ShiftAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShiftAnnouncementRepository extends JpaRepository<ShiftAnnouncement, Long> {
    List<ShiftAnnouncement> findBySiteIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(
        String site, LocalDateTime after);
}
