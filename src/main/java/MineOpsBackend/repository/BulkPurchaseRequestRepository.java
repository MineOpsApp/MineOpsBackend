package MineOpsBackend.repository;

import MineOpsBackend.model.BulkPurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BulkPurchaseRequestRepository extends JpaRepository<BulkPurchaseRequest, Long> {
    List<BulkPurchaseRequest> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
}
