package MineOpsBackend.repository;

import MineOpsBackend.model.TransactionDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionDisputeRepository extends JpaRepository<TransactionDispute, Long> {
    Optional<TransactionDispute> findByTransactionId(Long transactionId);
    boolean existsByTransactionId(Long transactionId);
    List<TransactionDispute> findByStatus(String status);
    long countByTransactionIdInAndStatus(List<Long> transactionIds, String status);
}
