package MineOpsBackend.repository;

import MineOpsBackend.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findBySiteOrderByCodeAsc(String site);
    List<Equipment> findBySiteAndStatusOrderByCodeAsc(String site, String status);
    Optional<Equipment> findByCodeIgnoreCaseAndSiteIgnoreCase(String code, String site);
    boolean existsByCodeIgnoreCaseAndSiteIgnoreCase(String code, String site);
}