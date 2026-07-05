package MineOpsBackend.repository;

import MineOpsBackend.model.SosAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SosAlertRepository extends JpaRepository<SosAlert, Long> {
    List<SosAlert> findAllByOrderByCreatedAtDesc();

    List<SosAlert> findBySiteOrderByCreatedAtDesc(String site);

    Page<SosAlert> findBySiteInOrderByCreatedAtDesc(List<String> sites, Pageable pageable);

    Optional<SosAlert> findByClientRequestId(String clientRequestId);
}