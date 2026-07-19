package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.WorkerMessage;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.WorkerMessageRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/worker-messages")
public class WorkerMessageController {

    private final WorkerMessageRepository messageRepo;
    private final AppUserRepository userRepo;
    private final PushNotificationService pushService;
    private final NotificationService notificationService;

    public WorkerMessageController(
        WorkerMessageRepository messageRepo,
        AppUserRepository userRepo,
        PushNotificationService pushService,
        NotificationService notificationService
    ) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.pushService = pushService;
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> sendMessage(
        @AuthenticationPrincipal AuthenticatedUser auth,
        @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }
        if (content.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be 500 characters or less");
        }

        AppUser sender = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (sender.getAssignedSite() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not assigned to a site");
        }

        WorkerMessage msg = new WorkerMessage(
            sender.getEmail(),
            sender.getFullName(),
            sender.getAssignedSite(),
            content.trim()
        );
        messageRepo.save(msg);

        List<String> supervisorTokens = new java.util.ArrayList<>();
        for (AppUser sup : userRepo.findByRoleAndAssignedSiteIgnoreCase("supervisor", sender.getAssignedSite())) {
            String token = sup.getPushToken();
            if (token != null && !token.isBlank()) supervisorTokens.add(token);
        }

        if (!supervisorTokens.isEmpty()) {
            pushService.sendToTokens(supervisorTokens,
                "New message from " + sender.getFullName(),
                content.length() > 80 ? content.substring(0, 77) + "..." : content,
                "default");
        }

        return toMap(msg);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<Map<String, Object>> getMyMessages(@AuthenticationPrincipal AuthenticatedUser auth) {
        return messageRepo.findBySenderEmailIgnoreCaseOrRecipientEmailIgnoreCaseOrderByCreatedAtDesc(
                auth.email(), auth.email())
            .stream().map(this::toMap).collect(Collectors.toList());
    }

    @GetMapping("/site")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Map<String, Object>> getSiteMessages(@AuthenticationPrincipal AuthenticatedUser auth) {
        AppUser supervisor = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (supervisor.getAssignedSite() == null) return List.of();
        return messageRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(supervisor.getAssignedSite())
            .stream().map(this::toMap).collect(Collectors.toList());
    }

    @GetMapping("/site-workers")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Map<String, Object>> getSiteWorkersForMessaging(@AuthenticationPrincipal AuthenticatedUser auth) {
        AppUser staff = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (staff.getAssignedSite() == null) return List.of();
        return userRepo.findByRoleAndAssignedSiteIgnoreCase("worker", staff.getAssignedSite())
            .stream()
            .filter(w -> !Boolean.TRUE.equals(w.getPending()) && !Boolean.FALSE.equals(w.getActive()))
            .map(w -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("email", w.getEmail());
                m.put("fullName", w.getFullName());
                return m;
            })
            .collect(Collectors.toList());
    }

    @PostMapping("/to-worker")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> sendToWorker(
        @AuthenticationPrincipal AuthenticatedUser auth,
        @RequestBody Map<String, String> body
    ) {
        String workerEmail = body.get("workerEmail");
        String content = body.get("content");
        if (workerEmail == null || workerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient is required");
        }
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }
        if (content.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be 500 characters or less");
        }

        AppUser sender = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        AppUser worker = userRepo.findByEmailIgnoreCase(workerEmail.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));

        if (!"worker".equals(worker.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient must be a worker");
        }
        if (sender.getAssignedSite() == null || worker.getAssignedSite() == null ||
            !sender.getAssignedSite().equalsIgnoreCase(worker.getAssignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker is not at your site");
        }

        WorkerMessage msg = new WorkerMessage(
            sender.getEmail(), sender.getFullName(), sender.getAssignedSite(),
            content.trim(), worker.getEmail(), worker.getFullName()
        );
        messageRepo.save(msg);

        String preview = content.length() > 80 ? content.substring(0, 77) + "..." : content;
        notificationService.notify(worker.getEmail(), "MESSAGE",
            sender.getFullName() + " sent you a message", preview, "WorkerMessage", msg.getId());
        String token = worker.getPushToken();
        if (token != null && !token.isBlank()) {
            pushService.sendToToken(token, sender.getFullName() + " sent you a message", preview, "default");
        }

        return toMap(msg);
    }

    @PostMapping("/{id}/worker-reply")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> workerReply(
        @AuthenticationPrincipal AuthenticatedUser auth,
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) {
        String reply = body.get("reply");
        if (reply == null || reply.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply cannot be empty");
        }
        if (reply.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply must be 500 characters or less");
        }

        WorkerMessage msg = messageRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!"STAFF".equals(msg.getInitiatedBy()) || msg.getRecipientEmail() == null ||
            !msg.getRecipientEmail().equalsIgnoreCase(auth.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This message was not sent to you");
        }

        msg.setReply(reply.trim());
        msg.setRepliedAt(LocalDateTime.now());
        messageRepo.save(msg);

        String preview = reply.length() > 80 ? reply.substring(0, 77) + "..." : reply;
        notificationService.notify(msg.getSenderEmail(), "MESSAGE",
            msg.getRecipientName() + " replied to your message", preview, "WorkerMessage", id);
        userRepo.findByEmailIgnoreCase(msg.getSenderEmail()).ifPresent(staff -> {
            String token = staff.getPushToken();
            if (token != null && !token.isBlank()) {
                pushService.sendToToken(token, msg.getRecipientName() + " replied to your message", preview, "default");
            }
        });

        return toMap(msg);
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> replyToMessage(
        @AuthenticationPrincipal AuthenticatedUser auth,
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) {
        String reply = body.get("reply");
        if (reply == null || reply.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply cannot be empty");
        }
        if (reply.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply must be 500 characters or less");
        }

        AppUser supervisor = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        WorkerMessage msg = messageRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if ("STAFF".equals(msg.getInitiatedBy())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the worker-reply endpoint for staff-initiated messages");
        }

        if (supervisor.getAssignedSite() == null ||
            !supervisor.getAssignedSite().equalsIgnoreCase(msg.getSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This message is not from your site");
        }

        msg.setReply(reply.trim());
        msg.setRepliedAt(LocalDateTime.now());
        messageRepo.save(msg);

        String replyPreview = reply.length() > 80 ? reply.substring(0, 77) + "..." : reply;
        notificationService.notify(msg.getSenderEmail(), "MESSAGE",
            supervisor.getFullName() + " replied to your message", replyPreview, "WorkerMessage", id);

        userRepo.findByEmailIgnoreCase(msg.getSenderEmail()).ifPresent(worker -> {
            String token = worker.getPushToken();
            if (token != null && !token.isBlank()) {
                pushService.sendToToken(token,
                    supervisor.getFullName() + " replied to your message",
                    replyPreview,
                    "default");
            }
        });

        return toMap(msg);
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> markRead(
        @AuthenticationPrincipal AuthenticatedUser auth,
        @PathVariable Long id
    ) {
        AppUser supervisor = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        WorkerMessage msg = messageRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (supervisor.getAssignedSite() == null ||
            !supervisor.getAssignedSite().equalsIgnoreCase(msg.getSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This message is not from your site");
        }

        if (msg.getReadAt() == null) {
            msg.setReadAt(LocalDateTime.now());
            messageRepo.save(msg);
        }

        return toMap(msg);
    }

    private Map<String, Object> toMap(WorkerMessage m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("senderEmail", m.getSenderEmail());
        map.put("senderName", m.getSenderName());
        map.put("site", m.getSite());
        map.put("content", m.getContent());
        map.put("reply", m.getReply());
        map.put("repliedAt", m.getRepliedAt());
        map.put("readAt", m.getReadAt());
        map.put("createdAt", m.getCreatedAt());
        map.put("recipientEmail", m.getRecipientEmail());
        map.put("recipientName", m.getRecipientName());
        map.put("initiatedBy", m.getInitiatedBy());
        return map;
    }
}
