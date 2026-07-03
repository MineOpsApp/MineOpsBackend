package MineOpsBackend.repository;

import MineOpsBackend.model.GuestAccessCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuestAccessCodeRepository extends JpaRepository<GuestAccessCode, Long> {
    List<GuestAccessCode> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
    Optional<GuestAccessCode> findByCode(String code);
}
