package MineOpsBackend.controller;

import MineOpsBackend.dto.ClockInRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.AttendanceRecord;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.AttendanceRepository;
import MineOpsBackend.repository.SafetyChecklistRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;
    private final SafetyChecklistRepository checklistRepository;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final AppUserRepository userRepo;

    public AttendanceController(
        AttendanceRepository attendanceRepository,
        AuditLogService auditLogService,
        SafetyChecklistRepository checklistRepository,
        NotificationService notificationService,
        PushNotificationService pushNotificationService,
        AppUserRepository userRepo
    ) {
        this.attendanceRepository = attendanceRepository;
        this.auditLogService = auditLogService;
        this.checklistRepository = checklistRepository;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
        this.userRepo = userRepo;
    }

    @PostMapping("/api/attendance/clock-in")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER','ROLE_GUEST')")
    public AttendanceRecord clockIn(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) ClockInRequest request
    ) {
        if (request != null && request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = attendanceRepository.findByClientRequestId(request.clientRequestId());
            if (existing.isPresent()) return existing.get();
        }

        // Check if already clocked in
        attendanceRepository.findTopByWorkerEmailIgnoreCaseAndStatusOrderByClockInAtDesc(user.email(), "ON_SITE")
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Already clocked in. Clock out first.");
            });

        String zone = (request != null && request.zone() != null) ? request.zone() : "Main Site";
        AttendanceRecord record = new AttendanceRecord(
            user.email(), user.fullName(), user.role(),
            user.assignedSite(), zone
        );
        if (request != null) record.setNotes(request.notes());
        if (request != null && request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            record.setClientRequestId(request.clientRequestId());
        }

        AttendanceRecord saved = attendanceRepository.save(record);
        auditLogService.record("CLOCK_IN", user.role(), user.fullName(), user.email(),
            "AttendanceRecord", saved.getId(), saved.getSite() + " — " + zone);

        if ("worker".equals(user.role())) {
            boolean submitted = checklistRepository.findByWorkerIdAndShiftDate(user.id(), LocalDate.now()).isPresent();
            if (!submitted) {
                String title = "Safety Checklist Reminder";
                String body = "You're signed in but haven't completed today's safety checklist yet. Complete it before starting work.";
                notificationService.notify(user.email(), "SAFETY_CHECKLIST", title, body, "AttendanceRecord", saved.getId());
                userRepo.findById(user.id()).ifPresent(u -> {
                    String token = u.getPushToken();
                    if (token != null && !token.isBlank()) {
                        pushNotificationService.sendToToken(token, title, body, "default");
                    }
                });
            }
        }

        return saved;
    }

    @PostMapping("/api/attendance/clock-out")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER','ROLE_GUEST')")
    public AttendanceRecord clockOut(@AuthenticationPrincipal AuthenticatedUser user) {
        AttendanceRecord record = attendanceRepository
            .findTopByWorkerEmailIgnoreCaseAndStatusOrderByClockInAtDesc(user.email(), "ON_SITE")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active clock-in found"));

        record.setStatus("OFF_SITE");
        record.setClockOutAt(LocalDateTime.now());

        AttendanceRecord saved = attendanceRepository.save(record);
        auditLogService.record("CLOCK_OUT", user.role(), user.fullName(), user.email(),
            "AttendanceRecord", saved.getId(), saved.getSite());

        return saved;
    }

    @GetMapping("/api/attendance/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getMyStatus(@AuthenticationPrincipal AuthenticatedUser user) {
        var active = attendanceRepository
            .findTopByWorkerEmailIgnoreCaseAndStatusOrderByClockInAtDesc(user.email(), "ON_SITE");
        return Map.of(
            "onSite", active.isPresent(),
            "record", active.isPresent() ? active.get() : Map.of()
        );
    }

    @GetMapping("/api/attendance/roster")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<AttendanceRecord> getRoster(@AuthenticationPrincipal AuthenticatedUser user) {
        return attendanceRepository.findBySiteAndStatusOrderByClockInAtDesc(user.assignedSite(), "ON_SITE");
    }

    @GetMapping("/api/attendance/history")
    @PreAuthorize("isAuthenticated()")
    public List<AttendanceRecord> getMyHistory(@AuthenticationPrincipal AuthenticatedUser user) {
        return attendanceRepository.findByWorkerEmailIgnoreCaseOrderByClockInAtDesc(user.email());
    }
}