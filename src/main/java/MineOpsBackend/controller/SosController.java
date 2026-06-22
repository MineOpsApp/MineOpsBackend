package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateSosRequest;
import MineOpsBackend.model.SosAlert;
import MineOpsBackend.repository.SosAlertRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
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
public class SosController {

    private final SosAlertRepository sosAlertRepository;
    private final AuditLogService auditLogService;

    public SosController(SosAlertRepository sosAlertRepository, AuditLogService auditLogService) {
        this.sosAlertRepository = sosAlertRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/sos")
    @PreAuthorize("isAuthenticated()")
    public SosAlert createAlert(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) CreateSosRequest request
    ) {
        String site = (request != null && request.site() != null && !request.site().isBlank())
            ? request.site()
            : "Unassigned";
        String message = (request != null && request.message() != null && !request.message().isBlank())
            ? request.message()
            : "Emergency assistance requested";

        SosAlert alert = new SosAlert(
            user.role(),
            site,
            message
        );

        SosAlert saved = sosAlertRepository.save(alert);
        auditLogService.record(
            "SOS_TRIGGERED",
            saved.getRole(),
            user.fullName(),
            user.email(),
            "SosAlert",
            saved.getId(),
            saved.getSite() + ": " + saved.getMessage()
        );

        return saved;
    }

    @GetMapping("/api/sos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Page<SosAlert> getAlerts(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        return sosAlertRepository.findBySiteInOrderByCreatedAtDesc(
            List.of(user.assignedSite(), "Unassigned"),
            pageable
        );
    }
}