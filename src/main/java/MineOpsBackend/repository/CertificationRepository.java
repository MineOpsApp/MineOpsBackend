package MineOpsBackend.repository;

import MineOpsBackend.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findBySiteIgnoreCaseOrderByExpiryDateAsc(String site);
    List<Certification> findByWorkerEmailIgnoreCaseOrderByExpiryDateAsc(String workerEmail);

    long countByWorkerEmailIgnoreCase(String workerEmail);
}
