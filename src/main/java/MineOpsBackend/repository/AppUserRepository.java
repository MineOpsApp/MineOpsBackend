package MineOpsBackend.repository;

import MineOpsBackend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByEmailIgnoreCase(String email);
    List<AppUser> findByAssignedSiteIgnoreCase(String assignedSite);
    List<AppUser> findByRoleAndPendingAndAssignedSiteIgnoreCase(String role, boolean pending, String site);
    List<AppUser> findByAssignedSiteIgnoreCaseAndPendingFalseOrderByFullNameAsc(String site);
    List<AppUser> findByRoleAndSessionExpiresAtBefore(String role, java.time.LocalDateTime cutoff);
    List<AppUser> findByRoleAndAssignedSiteIgnoreCase(String role, String site);
    List<AppUser> findByRedeemedCodeId(Long redeemedCodeId);
    List<AppUser> findByRoleAndPending(String role, boolean pending);
    List<AppUser> findByRoleAndBuyerVerificationStatus(String role, String buyerVerificationStatus);
}
