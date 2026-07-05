package MineOpsBackend.repository;

import MineOpsBackend.model.SupervisorSiteAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupervisorSiteAccessRepository extends JpaRepository<SupervisorSiteAccess, Long> {
    List<SupervisorSiteAccess> findBySupervisorEmail(String supervisorEmail);
    Optional<SupervisorSiteAccess> findBySupervisorEmailAndSite(String supervisorEmail, String site);
}
