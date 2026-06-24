package MineOpsBackend.controller;

import MineOpsBackend.dto.ClockInRequest;
import MineOpsBackend.model.AttendanceRecord;
import MineOpsBackend.repository.AttendanceRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    public AttendanceController(AttendanceRepository attendanceRepository, AuditLogService auditLogService) {
        this.attendanceRepository = attendanceRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/attendance/clock-in")
    @PreAuthorize("isAuthenticated()")
    public AttendanceRecord clockIn(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) ClockInRequest request
    ) {
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

        AttendanceRecord saved = attendanceRepository.save(record);
        auditLogService.record("CLOCK_IN", user.role(), user.fullName(), user.email(),
            "AttendanceRecord", saved.getId(), saved.getSite() + " — " + zone);

        return saved;
    }

    @PostMapping("/api/attendance/clock-out")
    @PreAuthorize("isAuthenticated()")
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