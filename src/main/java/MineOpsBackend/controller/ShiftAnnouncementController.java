package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.ShiftAnnouncement;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.ShiftAnnouncementRepository;
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
@RequestMapping("/api/announcements")
public class ShiftAnnouncementController {

    private final ShiftAnnouncementRepository announcementRepo;
    private final AppUserRepository userRepo;
    private final PushNotificationService pushService;

    public ShiftAnnouncementController(
        ShiftAnnouncementRepository announcementRepo,
        AppUserRepository userRepo,
        PushNotificationService pushService
    ) {
        this.announcementRepo = announcementRepo;
        this.userRepo = userRepo;
        this.pushService = pushService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> postAnnouncement(
        @AuthenticationPrincipal AuthenticatedUser auth,
        @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement cannot be empty");
        }
        content = content.trim();
        if (content.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement must be 200 characters or less");
        }

        AppUser poster = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (poster.getAssignedSite() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not assigned to a site");
        }

        ShiftAnnouncement announcement = new ShiftAnnouncement(
            poster.getAssignedSite(), content, poster.getFullName(), poster.getEmail()
        );
        announcementRepo.save(announcement);

        List<String> workerTokens = new java.util.ArrayList<>();
        for (AppUser u : userRepo.findByAssignedSiteIgnoreCaseAndPendingFalseOrderByFullNameAsc(poster.getAssignedSite())) {
            if ("worker".equals(u.getRole())) {
                String token = u.getPushToken();
                if (token != null && !token.isBlank()) workerTokens.add(token);
            }
        }
        if (!workerTokens.isEmpty()) {
            pushService.sendToTokens(workerTokens,
                "📢 " + poster.getFullName(),
                content.length() > 90 ? content.substring(0, 87) + "..." : content,
                "default");
        }

        return toMap(announcement);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> getAnnouncements(@AuthenticationPrincipal AuthenticatedUser auth) {
        AppUser user = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getAssignedSite() == null) return List.of();

        List<ShiftAnnouncement> all = announcementRepo
            .findBySiteIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(
                user.getAssignedSite(),
                LocalDateTime.now().minusHours(24)
            );

        return all.stream().limit(5).map(this::toMap).collect(Collectors.toList());
    }

    private Map<String, Object> toMap(ShiftAnnouncement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("site", a.getSite());
        m.put("content", a.getContent());
        m.put("createdByName", a.getCreatedByName());
        m.put("createdByEmail", a.getCreatedByEmail());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }
}
