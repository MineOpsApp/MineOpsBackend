package MineOpsBackend.repository;

import MineOpsBackend.model.AppUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT u FROM AppUser u WHERE u.assignedSite = :site AND u.role != 'guest' AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%'))) ORDER BY u.fullName ASC")
    List<AppUser> searchBySite(@Param("site") String site, @Param("q") String q, Pageable pageable);
}
