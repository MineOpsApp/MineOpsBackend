package MineOpsBackend.controller;

import MineOpsBackend.model.Notification;
import MineOpsBackend.repository.NotificationRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<Notification> getNotifications(
            @AuthenticationPrincipal AuthenticatedUser user,
            Pageable pageable) {
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(user.email(), pageable);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("count", notificationRepository.countByRecipientEmailAndReadAtIsNull(user.email()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public Notification markRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getRecipientEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return notification;
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        List<Notification> unread = notificationRepository.findByRecipientEmailAndReadAtIsNull(user.email());
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
        return Map.of("marked", unread.size());
    }
}
