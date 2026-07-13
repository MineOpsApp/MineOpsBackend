package MineOpsBackend.repository;

import MineOpsBackend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = TRUE WHERE rt.userId = :userId AND rt.revoked = FALSE")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);

    List<RefreshToken> findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime now);
}
