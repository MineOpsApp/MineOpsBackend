package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateSosRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.SosAlert;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.SosAlertRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.PushNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SosController {

    private final SosAlertRepository sosAlertRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;

    public SosController(
        SosAlertRepository sosAlertRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService
    ) {
        this.sosAlertRepository = sosAlertRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/api/sos")
    @PreAuthorize("isAuthenticated()")
    public SosAlert createAlert(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) CreateSosRequest request
    ) {
        String site = (request != null && request.site() != null && !request.site().isBlank())
            ? request.site()
            : user.assignedSite() != null ? user.assignedSite() : "Unassigned";
        String message = (request != null && request.message() != null && !request.message().isBlank())
            ? request.message()
            : "Emergency assistance requested";

        SosAlert alert = new SosAlert(user.role(), site, message);
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

        // Notify everyone on the same site
        try {
            List<String> tokens = appUserRepository.findByAssignedSiteIgnoreCase(site)
                .stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(user.email()))
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());

            pushNotificationService.sendToTokens(
                tokens,
                "🚨 SOS ALERT — " + site,
                user.fullName() + " has triggered an emergency alert. Respond immediately.",
                "sos"
            );
        } catch (Exception e) {
            System.err.println("Push notification failed for SOS: " + e.getMessage());
        }

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