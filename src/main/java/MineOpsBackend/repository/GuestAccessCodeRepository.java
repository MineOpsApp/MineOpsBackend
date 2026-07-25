package MineOpsBackend.repository;

import MineOpsBackend.model.GuestAccessCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuestAccessCodeRepository extends JpaRepository<GuestAccessCode, Long> {
    List<GuestAccessCode> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
    Optional<GuestAccessCode> findByCode(String code);

    // Row-locking read used by redemption: without this, two concurrent redeem() calls can both
    // read redemptionCount below maxRedemptions before either writes back the increment, letting
    // a code be redeemed more times than its configured limit (classic read-then-write race).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM GuestAccessCode c WHERE c.code = :code")
    Optional<GuestAccessCode> findByCodeForUpdate(@Param("code") String code);
}
