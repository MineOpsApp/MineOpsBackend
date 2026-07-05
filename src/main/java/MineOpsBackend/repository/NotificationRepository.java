package MineOpsBackend.repository;

import MineOpsBackend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);
    long countByRecipientEmailAndReadAtIsNull(String recipientEmail);
    List<Notification> findByRecipientEmailAndReadAtIsNull(String recipientEmail);
}
