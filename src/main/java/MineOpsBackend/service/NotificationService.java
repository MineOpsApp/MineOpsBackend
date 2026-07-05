package MineOpsBackend.service;

import MineOpsBackend.model.Notification;
import MineOpsBackend.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(String recipientEmail, String type, String title, String body,
                       String relatedEntityType, Long relatedEntityId) {
        Notification n = new Notification();
        n.setRecipientEmail(recipientEmail);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body != null && body.length() > 500 ? body.substring(0, 497) + "..." : body);
        n.setRelatedEntityType(relatedEntityType);
        n.setRelatedEntityId(relatedEntityId);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }
}
