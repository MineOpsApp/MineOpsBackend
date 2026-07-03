package MineOpsBackend.repository;

import MineOpsBackend.model.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    Page<InventoryTransaction> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site, Pageable pageable);
    List<InventoryTransaction> findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(String workerEmail);
}
