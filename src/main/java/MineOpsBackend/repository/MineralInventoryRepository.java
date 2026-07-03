package MineOpsBackend.repository;

import MineOpsBackend.model.MineralInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MineralInventoryRepository extends JpaRepository<MineralInventory, Long> {
    List<MineralInventory> findBySiteIgnoreCaseOrderByMineralTypeAsc(String site);
    Optional<MineralInventory> findBySiteIgnoreCaseAndMineralTypeIgnoreCaseAndUnitIgnoreCase(String site, String mineralType, String unit);
}
