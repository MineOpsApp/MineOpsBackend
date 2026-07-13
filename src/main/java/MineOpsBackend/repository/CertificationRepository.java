package MineOpsBackend.repository;

import MineOpsBackend.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findBySiteIgnoreCaseOrderByExpiryDateAsc(String site);
    List<Certification> findByWorkerEmailIgnoreCaseOrderByExpiryDateAsc(String workerEmail);

    long countByWorkerEmailIgnoreCase(String workerEmail);

    @Query("SELECT COUNT(c) FROM Certification c WHERE UPPER(c.site) = UPPER(:site) AND c.expiryDate < :today")
    long countExpiredBySite(@Param("site") String site, @Param("today") LocalDate today);

    @Query("SELECT COUNT(c) FROM Certification c WHERE UPPER(c.site) = UPPER(:site) AND c.expiryDate >= :today AND c.expiryDate < :cutoff")
    long countExpiringSoonBySite(@Param("site") String site, @Param("today") LocalDate today, @Param("cutoff") LocalDate cutoff);
}
