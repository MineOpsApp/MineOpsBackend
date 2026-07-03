package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.WorkerMessage;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.WorkerMessageRepository;
import MineOpsBackend.security.AuthenticatedUser;
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

    public WorkerMessageController(
        WorkerMessageRepository messageRepo,
        AppUserRepository userRepo,
        PushNotificationService pushService
    ) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.pushService = pushService;
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
        return messageRepo.findBySenderEmailIgnoreCaseOrderByCreatedAtDesc(auth.email())
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

        if (supervisor.getAssignedSite() == null ||
            !supervisor.getAssignedSite().equalsIgnoreCase(msg.getSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This message is not from your site");
        }

        msg.setReply(reply.trim());
        msg.setRepliedAt(LocalDateTime.now());
        messageRepo.save(msg);

        userRepo.findByEmailIgnoreCase(msg.getSenderEmail()).ifPresent(worker -> {
            String token = worker.getPushToken();
            if (token != null && !token.isBlank()) {
                String replyPreview = reply.length() > 80 ? reply.substring(0, 77) + "..." : reply;
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
        return map;
    }
}
