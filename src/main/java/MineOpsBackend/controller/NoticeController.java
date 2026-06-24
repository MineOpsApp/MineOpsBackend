package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateNoticeRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.Notice;
import MineOpsBackend.model.NoticeSeen;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.NoticeRepository;
import MineOpsBackend.repository.NoticeSeenRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class NoticeController {

    private final NoticeRepository noticeRepository;
    private final NoticeSeenRepository noticeSeenRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;

    public NoticeController(
        NoticeRepository noticeRepository,
        NoticeSeenRepository noticeSeenRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService
    ) {
        this.noticeRepository = noticeRepository;
        this.noticeSeenRepository = noticeSeenRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/api/notices")
    @PreAuthorize("isAuthenticated()")
    public Page<Map<String, Object>> getNotices(Pageable pageable) {
        Page<Notice> noticesPage = noticeRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<Long> noticeIds = noticesPage.getContent().stream().map(Notice::getId).toList();

        Map<Long, List<NoticeSeen>> seenByNotice = noticeSeenRepository
            .findByNoticeIdInOrderBySeenAtDesc(noticeIds)
            .stream()
            .collect(Collectors.groupingBy(NoticeSeen::getNoticeId));

        return noticesPage.map((notice) -> noticeResponse(notice, seenByNotice.getOrDefault(notice.getId(), List.of())));
    }

    @PostMapping("/api/notices")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> createNotice(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateNoticeRequest request
    ) {
        Notice notice = new Notice(
            request.title(),
            request.message(),
            user.role()
        );

        Notice saved = noticeRepository.save(notice);
        auditLogService.record(
            "NOTICE_POSTED",
            saved.getPostedByRole(),
            user.fullName(),
            user.email(),
            "Notice",
            saved.getId(),
            saved.getTitle()
        );

        // Notify all workers on the same site
        try {
            List<String> tokens = appUserRepository.findByAssignedSiteIgnoreCase(user.assignedSite())
                .stream()
                .filter(u -> "worker".equals(u.getRole()) || "guest".equals(u.getRole()))
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());

            pushNotificationService.sendToTokens(
                tokens,
                "📢 New Notice — " + user.assignedSite(),
                saved.getTitle() + ": " + saved.getMessage(),
                "default"
            );
        } catch (Exception e) {
            System.err.println("Push notification failed for notice: " + e.getMessage());
        }

        return noticeResponse(saved, noticeSeenRepository.findByNoticeIdOrderBySeenAtDesc(saved.getId()));
    }

    @PostMapping("/api/notices/{id}/seen")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> markSeen(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        Notice notice = noticeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found"));
        String email = user.email().trim().toLowerCase();

        if (!email.isEmpty() && !noticeSeenRepository.existsByNoticeIdAndEmailIgnoreCase(id, email)) {
            NoticeSeen seen = noticeSeenRepository.save(new NoticeSeen(
                id,
                user.fullName(),
                email,
                user.role()
            ));
            auditLogService.record(
                "NOTICE_SEEN",
                seen.getRole(),
                seen.getFullName(),
                seen.getEmail(),
                "Notice",
                id,
                notice.getTitle()
            );
        }

        return noticeResponse(notice, noticeSeenRepository.findByNoticeIdOrderBySeenAtDesc(id));
    }

    private Map<String, Object> noticeResponse(Notice notice, List<NoticeSeen> seenBy) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", notice.getId());
        response.put("title", notice.getTitle());
        response.put("message", notice.getMessage());
        response.put("postedByRole", notice.getPostedByRole());
        response.put("createdAt", notice.getCreatedAt());
        response.put("seenBy", seenBy);
        return response;
    }
}