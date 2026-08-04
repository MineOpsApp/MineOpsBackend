package MineOpsBackend.controller;

import MineOpsBackend.dto.SubmitShiftLogRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.ShiftLog;
import MineOpsBackend.model.ShiftLogGroupMember;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.ShiftLogGroupMemberRepository;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.MineralInventoryService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import MineOpsBackend.util.CsvExportUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class ShiftLogController {

    // Named `logger`, not `log` — several methods below already use a local variable named
    // `log` for the ShiftLog entity itself, which would otherwise shadow a field of the same name.
    private static final Logger logger = LoggerFactory.getLogger(ShiftLogController.class);

    // A group can name at most this many co-workers on one entry — generous for a real work
    // crew, small enough to keep a single log from becoming a way to pay out the whole site.
    private static final int MAX_GROUP_SIZE = 20;

    private static final Map<String, BigDecimal> UNIT_LIMITS = Map.of(
        "kg",     new BigDecimal("50000"),
        "tonnes", new BigDecimal("500"),
        "t",      new BigDecimal("500"),
        "oz",     new BigDecimal("10000"),
        "g",      new BigDecimal("50000"),
        "lb",     new BigDecimal("100000"),
        "carats", new BigDecimal("500000"),
        "ct",     new BigDecimal("500000")
    );
    private static final BigDecimal DEFAULT_LIMIT = new BigDecimal("100000");

    private final ShiftLogRepository shiftLogRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final MineralInventoryService inventoryService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final ShiftLogGroupMemberRepository groupMemberRepository;

    public ShiftLogController(
        ShiftLogRepository shiftLogRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        MineralInventoryService inventoryService,
        NotificationService notificationService,
        PushNotificationService pushNotificationService,
        ShiftLogGroupMemberRepository groupMemberRepository
    ) {
        this.shiftLogRepository = shiftLogRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.groupMemberRepository = groupMemberRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/api/shift-logs")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public ShiftLog submitShiftLog(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody SubmitShiftLogRequest request
    ) {
        BigDecimal limit = UNIT_LIMITS.getOrDefault(request.unit().toLowerCase(), DEFAULT_LIMIT);
        if (request.volumeExtracted().compareTo(limit) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Volume " + request.volumeExtracted() + " " + request.unit()
                + " exceeds the maximum allowed per shift ("
                + limit.stripTrailingZeros().toPlainString() + " " + request.unit() + ")");
        }

        if (request.shiftDate() != null && !request.shiftDate().isBlank()) {
            java.time.LocalDate parsedShiftDate;
            try {
                parsedShiftDate = java.time.LocalDate.parse(request.shiftDate());
            } catch (java.time.format.DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "shiftDate must be ISO date format (yyyy-MM-dd)");
            }
            if (parsedShiftDate.isAfter(java.time.LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Shift date cannot be in the future");
            }
        }

        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = shiftLogRepository.findByClientRequestId(request.clientRequestId());
            // Only replay this idempotency shortcut for the same worker who created it —
            // clientRequestId is client-generated, so a guessed/reused id must not let one
            // worker fetch another worker's shift log record.
            if (existing.isPresent() && existing.get().getWorkerEmail().equalsIgnoreCase(user.email())) {
                return existing.get();
            }
        }

        ShiftLog log = new ShiftLog(
    user.email(),
    user.fullName(),
    user.assignedSite(),
    request.zone(),
    request.shiftType(),
    request.mineralType(),
    request.volumeExtracted(),
    request.unit(),
    request.equipmentCode(),
    request.equipmentName(),
    request.notes()
);
log.setShiftDate(request.shiftDate());
if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
    log.setClientRequestId(request.clientRequestId());
}
ShiftLog saved = shiftLogRepository.save(log);

        List<ShiftLogGroupMember> groupMembers = resolveGroupMembers(saved.getId(), request.groupMemberEmails(), user);
        saved.setGroupMembers(groupMembers);
        if (!groupMembers.isEmpty()) {
            auditLogService.record("SHIFT_LOG_GROUP_TAGGED", user.role(), user.fullName(), user.email(),
                "ShiftLog", saved.getId(),
                groupMembers.stream().map(ShiftLogGroupMember::getWorkerName).collect(Collectors.joining(", ")));
        }

        auditLogService.record(
    "SHIFT_LOG_SUBMITTED",
    user.role(),
    user.fullName(),
    user.email(),
    "ShiftLog",
    saved.getId(),
    request.mineralType() + " " + request.volumeExtracted() + request.unit() + " — " + request.zone()
);

// Previously nothing notified the supervisor/safety officer that a shift log was waiting
// for review — approve/reject notified the worker back, but submission itself was silent.
//
// DEBUG (temporary): also writes a DEBUG_SHIFT_LOG_NOTIFY audit log entry on both the success
// and failure path, visible in the app's own Audit Log screen. Railway's log viewer has been
// unreliable while diagnosing this specific issue (delayed/hidden lines), so this routes the
// same diagnostic info through a channel we've already confirmed works end-to-end. Remove once
// the root cause is confirmed fixed.
try {
    notifySiteReviewers(saved, user);
} catch (Exception e) {
    logger.warn("Failed to notify site reviewers of shift log submission (site={}, id={}): {}",
        user.assignedSite(), saved.getId(), e.getMessage());
    auditLogService.record("DEBUG_SHIFT_LOG_NOTIFY", user.role(), user.fullName(), user.email(),
        "ShiftLog", saved.getId(),
        "EXCEPTION: " + e.getClass().getSimpleName() + ": " + e.getMessage());
}

return saved;
    }

    /** Validates and persists the named co-workers for a group-logged shift, silently
     *  dropping anyone who isn't a real, active worker on the submitter's own site — a
     *  mistyped email should not fail the whole submission. */
    private List<ShiftLogGroupMember> resolveGroupMembers(
        Long shiftLogId, List<String> rawEmails, AuthenticatedUser submitter
    ) {
        List<ShiftLogGroupMember> members = new java.util.ArrayList<>();
        if (rawEmails == null || rawEmails.isEmpty()) return members;
        if (rawEmails.size() > MAX_GROUP_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A shift log can name at most " + MAX_GROUP_SIZE + " co-workers");
        }

        Set<String> seen = new LinkedHashSet<>();
        seen.add(submitter.email().toLowerCase());
        for (String raw : rawEmails) {
            if (raw == null || raw.isBlank()) continue;
            String email = raw.trim().toLowerCase();
            if (!seen.add(email)) continue; // dedupe, and drop the submitter's own email

            AppUser member = appUserRepository.findByEmailIgnoreCase(email).orElse(null);
            boolean eligible = member != null
                && "worker".equals(member.getRole())
                && member.getAssignedSite() != null
                && member.getAssignedSite().equalsIgnoreCase(submitter.assignedSite())
                && member.getDeletedAt() == null
                && !Boolean.FALSE.equals(member.getActive());
            if (!eligible) continue;

            members.add(new ShiftLogGroupMember(shiftLogId, member.getEmail(), member.getFullName()));
        }
        if (!members.isEmpty()) {
            groupMemberRepository.saveAll(members);
        }
        return members;
    }

    private void notifySiteReviewers(ShiftLog saved, AuthenticatedUser submitter) {
        List<AppUser> allAtSite = appUserRepository.findByAssignedSiteIgnoreCase(submitter.assignedSite());
        List<AppUser> recipients = allAtSite
            .stream()
            .filter(u -> "supervisor".equals(u.getRole()) || "safetyOfficer".equals(u.getRole()))
            .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
            .collect(Collectors.toList());

        if (recipients.isEmpty()) {
            String detail = "site='" + submitter.assignedSite() + "' matched " + allAtSite.size()
                + " users at that site, ZERO eligible. Roles present: "
                + allAtSite.stream().map(u -> u.getEmail() + "=" + u.getRole()
                    + (u.getDeletedAt() != null ? "(deleted)" : "")
                    + (Boolean.FALSE.equals(u.getActive()) ? "(inactive)" : "")).collect(Collectors.toList());
            logger.warn("shift log {} submitted by {} — {}", saved.getId(), submitter.email(), detail);
            auditLogService.record("DEBUG_SHIFT_LOG_NOTIFY", submitter.role(), submitter.fullName(), submitter.email(),
                "ShiftLog", saved.getId(), "ZERO_RECIPIENTS: " + detail);
            return;
        }

        String title = "Shift Log Submitted — " + submitter.assignedSite();
        String body = submitter.fullName() + " logged " + saved.getVolumeExtracted() + " " + saved.getUnit()
            + " of " + saved.getMineralType() + " in " + saved.getZone() + ". Awaiting review.";

        List<String> tokens = recipients.stream()
            .map(AppUser::getPushToken)
            .filter(t -> t != null && !t.isBlank())
            .collect(Collectors.toList());
        String recipientList = recipients.stream().map(AppUser::getEmail).collect(Collectors.toList()).toString();
        logger.info("shift log {} submitted by {} — notifying {} recipient(s), {} with a push token: {}",
            saved.getId(), submitter.email(), recipients.size(), tokens.size(), recipientList);
        auditLogService.record("DEBUG_SHIFT_LOG_NOTIFY", submitter.role(), submitter.fullName(), submitter.email(),
            "ShiftLog", saved.getId(),
            "OK: notifying " + recipients.size() + " recipient(s), " + tokens.size() + " with push token: " + recipientList);
        pushNotificationService.sendToTokens(tokens, title, body, "default");

        for (AppUser recipient : recipients) {
            notificationService.notify(recipient.getEmail(), "SHIFT_LOG", title, body, "ShiftLog", saved.getId());
        }
    }

    @GetMapping("/api/shift-logs/mine")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<ShiftLog> getMyShiftLogs(@AuthenticationPrincipal AuthenticatedUser user) {
        return attachGroupMembers(shiftLogRepository.findByWorkerEmailIgnoreCaseOrderBySubmittedAtDesc(user.email()));
    }

    // Every other active worker on the caller's own site — the pool a worker picks their
    // "worked with" group from when logging shift production.
    @GetMapping("/api/shift-logs/site-coworkers")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<Map<String, Object>> getSiteCoworkers(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user.assignedSite() == null) return List.of();
        return appUserRepository.findByAssignedSiteIgnoreCase(user.assignedSite())
            .stream()
            .filter(u -> "worker".equals(u.getRole()))
            .filter(u -> !u.getEmail().equalsIgnoreCase(user.email()))
            .filter(u -> !Boolean.TRUE.equals(u.getPending()) && !Boolean.FALSE.equals(u.getActive()) && u.getDeletedAt() == null)
            .map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("email", u.getEmail());
                m.put("fullName", u.getFullName());
                return m;
            })
            .collect(Collectors.toList());
    }

    private List<ShiftLog> attachGroupMembers(List<ShiftLog> logs) {
        List<Long> ids = logs.stream().map(ShiftLog::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return logs;
        Map<Long, List<ShiftLogGroupMember>> byLog = groupMemberRepository.findByShiftLogIdIn(ids)
            .stream()
            .collect(Collectors.groupingBy(ShiftLogGroupMember::getShiftLogId));
        for (ShiftLog log : logs) {
            log.setGroupMembers(byLog.getOrDefault(log.getId(), List.of()));
        }
        return logs;
    }

    @GetMapping("/api/shift-logs")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<ShiftLog> getSiteShiftLogs(
        @AuthenticationPrincipal AuthenticatedUser user,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String dateFrom,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String dateTo,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String mineralType,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String workerName,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String status
    ) {
        List<ShiftLog> logs = shiftLogRepository.findBySiteIgnoreCaseOrderBySubmittedAtDesc(user.assignedSite());

        if (dateFrom != null && !dateFrom.isBlank()) {
            java.time.LocalDate from;
            try {
                from = java.time.LocalDate.parse(dateFrom);
            } catch (java.time.format.DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFrom must be ISO date format (yyyy-MM-dd)");
            }
            logs = logs.stream().filter(l -> !l.getSubmittedAt().toLocalDate().isBefore(from)).collect(java.util.stream.Collectors.toList());
        }
        if (dateTo != null && !dateTo.isBlank()) {
            java.time.LocalDate to;
            try {
                to = java.time.LocalDate.parse(dateTo);
            } catch (java.time.format.DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateTo must be ISO date format (yyyy-MM-dd)");
            }
            logs = logs.stream().filter(l -> !l.getSubmittedAt().toLocalDate().isAfter(to)).collect(java.util.stream.Collectors.toList());
        }
        if (mineralType != null && !mineralType.isBlank()) {
            String m = mineralType.toLowerCase();
            logs = logs.stream().filter(l -> l.getMineralType() != null && l.getMineralType().toLowerCase().contains(m)).collect(java.util.stream.Collectors.toList());
        }
        if (workerName != null && !workerName.isBlank()) {
            String w = workerName.toLowerCase();
            logs = logs.stream().filter(l -> l.getWorkerName() != null && l.getWorkerName().toLowerCase().contains(w)).collect(java.util.stream.Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            String s = status.toUpperCase();
            logs = logs.stream().filter(l -> s.equals(l.getStatus())).collect(java.util.stream.Collectors.toList());
        }
        return attachGroupMembers(logs);
    }

    @PatchMapping("/api/shift-logs/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public ShiftLog approveShiftLog(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ShiftLog log = shiftLogRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift log not found"));

        if (!log.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Log belongs to a different site");

        if ("APPROVED".equals(log.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already approved");

        if ("REJECTED".equals(log.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot approve a rejected log");

        log.setStatus("APPROVED");
        log.setApprovedBy(user.fullName());
        log.setApprovedAt(LocalDateTime.now());
        ShiftLog saved = shiftLogRepository.save(log);

        inventoryService.applyApprovedShiftLog(saved, user.fullName());
        notificationService.notify(saved.getWorkerEmail(), "SHIFT_LOG", "Shift Log Approved",
            "Your " + saved.getMineralType() + " shift log has been approved.", "ShiftLog", saved.getId());

        auditLogService.record(
            "SHIFT_LOG_APPROVED",
            user.role(), user.fullName(), user.email(),
            "ShiftLog", id,
            log.getMineralType() + " " + log.getVolumeExtracted() + " " + log.getUnit()
                + " — " + log.getWorkerName()
        );
        return saved;
    }

    @PatchMapping("/api/shift-logs/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public ShiftLog rejectShiftLog(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ShiftLog log = shiftLogRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift log not found"));

        if (!log.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Log belongs to a different site");

        if ("APPROVED".equals(log.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reject an already approved log");

        log.setStatus("REJECTED");
        log.setRejectedBy(user.fullName());
        log.setRejectedAt(LocalDateTime.now());
        ShiftLog saved = shiftLogRepository.save(log);

        notificationService.notify(saved.getWorkerEmail(), "SHIFT_LOG", "Shift Log Rejected",
            "Your " + saved.getMineralType() + " shift log was rejected.", "ShiftLog", saved.getId());

        auditLogService.record(
            "SHIFT_LOG_REJECTED",
            user.role(), user.fullName(), user.email(),
            "ShiftLog", id,
            log.getMineralType() + " " + log.getVolumeExtracted() + " " + log.getUnit()
                + " — " + log.getWorkerName()
        );
        return saved;
    }

    @GetMapping("/api/shift-logs/export/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public ResponseEntity<String> exportCsv(@AuthenticationPrincipal AuthenticatedUser user) {
        List<ShiftLog> rows = shiftLogRepository.findBySiteIgnoreCaseOrderBySubmittedAtDesc(user.assignedSite());
        StringBuilder csv = new StringBuilder();
        csv.append(CsvExportUtil.row("worker", "mineralType", "volume", "unit", "shiftDate", "status"));
        for (ShiftLog s : rows) {
            csv.append(CsvExportUtil.row(
                    s.getWorkerName(), s.getMineralType(), s.getVolumeExtracted(),
                    s.getUnit(), s.getShiftDate(), s.getStatus()));
        }
        return CsvExportUtil.response("shift-logs.csv", csv.toString());
    }
}