package MineOpsBackend.repository;

import MineOpsBackend.model.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    List<SubscriptionPayment> findBySiteIgnoreCaseOrderByCreatedAtDesc(String site);
}
