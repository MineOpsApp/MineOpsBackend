package MineOpsBackend.controller;

import MineOpsBackend.dto.SubmitShiftLogRequest;
import MineOpsBackend.model.ShiftLog;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ShiftLogController {

    private final ShiftLogRepository shiftLogRepository;
    private final AuditLogService auditLogService;

    public ShiftLogController(ShiftLogRepository shiftLogRepository, AuditLogService auditLogService) {
        this.shiftLogRepository = shiftLogRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/shift-logs")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public ShiftLog submitShiftLog(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody SubmitShiftLogRequest request
    ) {
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
    public Page<ShiftLog> getSiteShiftLogs(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        return shiftLogRepository.findBySiteOrderBySubmittedAtDesc(user.assignedSite(), pageable);
    }
}