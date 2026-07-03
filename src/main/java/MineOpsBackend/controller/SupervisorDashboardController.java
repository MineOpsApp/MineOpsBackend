package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.*;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SupervisorDashboardController {

    private final AppUserRepository userRepo;
    private final HazardReportRepository hazardRepo;
    private final NoticeRepository noticeRepo;
    private final AttendanceRepository attendanceRepo;
    private final ShiftLogRepository shiftLogRepo;
    private final WorkerMessageRepository messageRepo;
    private final ShiftAnnouncementRepository announcementRepo;
    private final CertificationRepository certRepo;
    private final LoneWorkerRepository loneWorkerRepo;

    public SupervisorDashboardController(
        AppUserRepository userRepo,
        HazardReportRepository hazardRepo,
        NoticeRepository noticeRepo,
        AttendanceRepository attendanceRepo,
        ShiftLogRepository shiftLogRepo,
        WorkerMessageRepository messageRepo,
        ShiftAnnouncementRepository announcementRepo,
        CertificationRepository certRepo,
        LoneWorkerRepository loneWorkerRepo
    ) {
        this.userRepo = userRepo;
        this.hazardRepo = hazardRepo;
        this.noticeRepo = noticeRepo;
        this.attendanceRepo = attendanceRepo;
        this.shiftLogRepo = shiftLogRepo;
        this.messageRepo = messageRepo;
        this.announcementRepo = announcementRepo;
        this.certRepo = certRepo;
        this.loneWorkerRepo = loneWorkerRepo;
    }

    @GetMapping("/api/supervisor/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> getDashboard(@AuthenticationPrincipal AuthenticatedUser auth) {
        AppUser supervisor = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String site = supervisor.getAssignedSite() != null ? supervisor.getAssignedSite() : "";

        long hazardCount = hazardRepo.findBySiteOrderByCreatedAtDesc(site).size();
        long noticeCount = noticeRepo.findAllByOrderByCreatedAtDesc().size();
        long workersOnSite = attendanceRepo.countBySiteIgnoreCaseAndStatus(site, "ON_SITE");
        long pendingShiftLogs = shiftLogRepo.countBySiteIgnoreCaseAndStatus(site, "PENDING");
        long unreadMessages = messageRepo.countBySiteIgnoreCaseAndReadAtIsNull(site);
        long certExpired = certRepo.countBySiteIgnoreCaseAndStatus(site, "EXPIRED");
        long certExpiringSoon = certRepo.countBySiteIgnoreCaseAndStatus(site, "EXPIRING_SOON");

        List<Map<String, Object>> announcements = announcementRepo
            .findBySiteIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(site, LocalDateTime.now().minusHours(24))
            .stream().limit(5).map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.getId());
                m.put("content", a.getContent());
                m.put("createdByName", a.getCreatedByName());
                m.put("createdAt", a.getCreatedAt());
                return m;
            }).collect(Collectors.toList());

        List<Map<String, Object>> activeLoneWorkers = loneWorkerRepo
            .findBySiteIgnoreCaseAndActive(site, true).stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("workerName", s.getWorkerName());
                m.put("intervalMinutes", s.getIntervalMinutes());
                m.put("lastCheckedInAt", s.getLastCheckedInAt());
                m.put("deadline", s.getDeadline());
                m.put("alerted", s.isAlerted());
                return m;
            }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hazardCount", hazardCount);
        result.put("noticeCount", noticeCount);
        result.put("workersOnSite", workersOnSite);
        result.put("pendingShiftLogs", pendingShiftLogs);
        result.put("unreadMessages", unreadMessages);
        result.put("certExpired", certExpired);
        result.put("certExpiringSoon", certExpiringSoon);
        result.put("announcements", announcements);
        result.put("activeLoneWorkers", activeLoneWorkers);
        return result;
    }
}
