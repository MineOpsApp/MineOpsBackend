package MineOpsBackend.repository;

import MineOpsBackend.model.VisitorVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitorVisitRepository extends JpaRepository<VisitorVisit, Long> {
    List<VisitorVisit> findByAssignedSiteOrderByVisitStartDesc(String site);
    List<VisitorVisit> findByGuestUserIdOrderByVisitStartDesc(Long guestUserId);
    Optional<VisitorVisit> findFirstByGuestUserIdOrderByCreatedAtDesc(Long guestUserId);
}
