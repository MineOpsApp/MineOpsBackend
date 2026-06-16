package MineOpsBackend.controller;

import MineOpsBackend.model.Notice;
import MineOpsBackend.model.NoticeSeen;
import MineOpsBackend.repository.NoticeRepository;
import MineOpsBackend.repository.NoticeSeenRepository;
import MineOpsBackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class NoticeController {

    private final NoticeRepository noticeRepository;
    private final NoticeSeenRepository noticeSeenRepository;
    private final AuditLogService auditLogService;

    public NoticeController(
        NoticeRepository noticeRepository,
        NoticeSeenRepository noticeSeenRepository,
        AuditLogService auditLogService
    ) {
        this.noticeRepository = noticeRepository;
        this.noticeSeenRepository = noticeSeenRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/notices")
    public List<Map<String, Object>> getNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::noticeResponse)
            .toList();
    }

    @PostMapping("/api/notices")
    public Map<String, Object> createNotice(@RequestBody Map<String, String> request) {
        Notice notice = new Notice(
            request.getOrDefault("title", "Site Notice"),
            request.getOrDefault("message", "New site notice posted"),
            request.getOrDefault("postedByRole", "supervisor")
        );

        Notice saved = noticeRepository.save(notice);
        auditLogService.record(
            "NOTICE_POSTED",
            saved.getPostedByRole(),
            request.getOrDefault("actorName", "Unknown User"),
            request.getOrDefault("actorEmail", ""),
            "Notice",
            saved.getId(),
            saved.getTitle()
        );

        return noticeResponse(saved);
    }

    @PostMapping("/api/notices/{id}/seen")
    public Map<String, Object> markSeen(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Notice notice = noticeRepository.findById(id).orElseThrow();
        String email = request.getOrDefault("email", "").trim().toLowerCase();

        if (!email.isEmpty() && !noticeSeenRepository.existsByNoticeIdAndEmailIgnoreCase(id, email)) {
            NoticeSeen seen = noticeSeenRepository.save(new NoticeSeen(
                id,
                request.getOrDefault("fullName", "Unknown User"),
                email,
                request.getOrDefault("role", "unknown")
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

        return noticeResponse(notice);
    }

    private Map<String, Object> noticeResponse(Notice notice) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", notice.getId());
        response.put("title", notice.getTitle());
        response.put("message", notice.getMessage());
        response.put("postedByRole", notice.getPostedByRole());
        response.put("createdAt", notice.getCreatedAt());
        response.put("seenBy", noticeSeenRepository.findByNoticeIdOrderBySeenAtDesc(notice.getId()));

        return response;
    }
}
