package MineOpsBackend.repository;

import MineOpsBackend.model.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByEmailIgnoreCaseAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(String email, String otpCode);
    List<PasswordResetOtp> findByEmailIgnoreCaseAndUsedFalse(String email);
    Optional<PasswordResetOtp> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, LocalDateTime cutoff);
}
