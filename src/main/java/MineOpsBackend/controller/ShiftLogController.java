package MineOpsBackend.controller;

import MineOpsBackend.dto.SubmitShiftLogRequest;
import MineOpsBackend.model.ShiftLog;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.MineralInventoryService;
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
import java.util.List;
import java.util.Map;

@RestController
public class ShiftLogController {

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
    private final AuditLogService auditLogService;
    private final MineralInventoryService inventoryService;

    public ShiftLogController(
        ShiftLogRepository shiftLogRepository,
        AuditLogService auditLogService,
        MineralInventoryService inventoryService
    ) {
        this.shiftLogRepository = shiftLogRepository;
        this.auditLogService = auditLogService;
        this.inventoryService = inventoryService;
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
ShiftLog saved = shiftLogRepository.save(log);

        auditLogService.record(
    "SHIFT_LOG_SUBMITTED",
    user.role(),
    user.fullName(),
    user.email(),
    "ShiftLog",
    saved.getId(),
    request.mineralType() + " " + request.volumeExtracted() + request.unit() + " — " + request.zone()
);

return saved;
    }

    @GetMapping("/api/shift-logs/mine")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<ShiftLog> getMyShiftLogs(@AuthenticationPrincipal AuthenticatedUser user) {
        return shiftLogRepository.findByWorkerEmailIgnoreCaseOrderBySubmittedAtDesc(user.email());
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
        List<ShiftLog> logs = shiftLogRepository.findBySiteOrderBySubmittedAtDesc(user.assignedSite());

        if (dateFrom != null && !dateFrom.isBlank()) {
            java.time.LocalDate from = java.time.LocalDate.parse(dateFrom);
            logs = logs.stream().filter(l -> !l.getSubmittedAt().toLocalDate().isBefore(from)).collect(java.util.stream.Collectors.toList());
        }
        if (dateTo != null && !dateTo.isBlank()) {
            java.time.LocalDate to = java.time.LocalDate.parse(dateTo);
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
        return logs;
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
        List<ShiftLog> rows = shiftLogRepository.findBySiteOrderBySubmittedAtDesc(user.assignedSite());
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