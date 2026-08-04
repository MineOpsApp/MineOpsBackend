package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateSosRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.SosAlert;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.SosAlertRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class SosController {

    private static final Logger log = LoggerFactory.getLogger(SosController.class);

    private final SosAlertRepository sosAlertRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    public SosController(
        SosAlertRepository sosAlertRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService,
        NotificationService notificationService
    ) {
        this.sosAlertRepository = sosAlertRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
        this.notificationService = notificationService;
    }

    @PostMapping("/api/sos")
    @PreAuthorize("isAuthenticated()")
    public SosAlert createAlert(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) CreateSosRequest request
    ) {
        String site = user.assignedSite() != null ? user.assignedSite() : "Unassigned";
        String message = (request != null && request.message() != null && !request.message().isBlank())
            ? request.message()
            : "Emergency assistance requested";

        if (request != null && request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = sosAlertRepository.findByClientRequestId(request.clientRequestId());
            // Only replay for the same person who raised it — clientRequestId is client-generated,
            // so a guessed/reused id must not let one user fetch another user's SOS alert record.
            if (existing.isPresent() && existing.get().getActorEmail().equalsIgnoreCase(user.email())) {
                return existing.get();
            }
        }

        SosAlert alert = new SosAlert(user.role(), site, message, user.fullName(), user.email());
        if (request != null && request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            alert.setClientRequestId(request.clientRequestId());
        }
        if (request != null) {
            alert.setLatitude(request.latitude());
            alert.setLongitude(request.longitude());
        }
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
            String notifTitle = "SOS ALERT — " + site;
            String notifBody = user.fullName() + " has triggered an emergency alert. Respond immediately.";

            // Site personnel only — a guest or buyer account sharing this site should never
            // receive an emergency broadcast that implies they're part of the crew.
            List<AppUser> recipients = appUserRepository.findByAssignedSiteIgnoreCase(site)
                .stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(user.email()))
                .filter(u -> List.of("worker", "supervisor", "safetyOfficer").contains(u.getRole()))
                .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
                .collect(Collectors.toList());

            List<String> tokens = recipients.stream()
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
            pushNotificationService.sendToTokens(tokens, notifTitle, notifBody, "sos");

            for (AppUser recipient : recipients) {
                notificationService.notify(recipient.getEmail(), "SOS", notifTitle, notifBody, "SosAlert", saved.getId());
            }
        } catch (Exception e) {
            log.warn("Push notification failed for SOS: {}", e.getMessage());
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

    // Without this, an SOS alert has no way to ever leave the "Open" state it's created with —
    // it stays permanently flagged as an active emergency in the app.
    @PostMapping("/api/sos/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public SosAlert resolveAlert(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        SosAlert alert = sosAlertRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOS alert not found"));
        if (!"Unassigned".equals(alert.getSite()) && user.assignedSite() != null
            && !alert.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This alert is not from your site");
        }
        alert.setStatus("Resolved");
        SosAlert saved = sosAlertRepository.save(alert);
        auditLogService.record(
            "SOS_RESOLVED", user.role(), user.fullName(), user.email(),
            "SosAlert", saved.getId(), saved.getSite() + ": " + saved.getMessage()
        );
        return saved;
    }
}