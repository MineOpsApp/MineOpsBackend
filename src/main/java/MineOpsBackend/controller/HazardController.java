package MineOpsBackend.controller;

import MineOpsBackend.dto.CloseHazardRequest;
import MineOpsBackend.dto.CreateHazardRequest;
import MineOpsBackend.dto.ReviewHazardRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.HazardReport;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import MineOpsBackend.util.CsvExportUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class HazardController {

    private static final Logger log = LoggerFactory.getLogger(HazardController.class);

    private final HazardReportRepository hazardReportRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    public HazardController(
        HazardReportRepository hazardReportRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService,
        NotificationService notificationService
    ) {
        this.hazardReportRepository = hazardReportRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
        this.notificationService = notificationService;
    }

    @GetMapping("/api/hazards")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Page<HazardReport> getHazards(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        Page<HazardReport> page = "worker".equals(user.role())
            ? hazardReportRepository.findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(user.email(), pageable)
            : hazardReportRepository.findBySiteOrderByCreatedAtDesc(user.assignedSite(), pageable);
        // Photos are fetched on demand via /api/hazards/{id}/photo — list views only need
        // the hasPhoto flag, not the (potentially large) base64 payload.
        page.getContent().forEach(h -> { entityManager.detach(h); h.stripPhotoDataForList(); });
        return page;
    }

    @GetMapping("/api/hazards/site-alerts")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER','ROLE_GUEST')")
    public List<HazardReport> getSiteHazardAlerts(@AuthenticationPrincipal AuthenticatedUser user) {
        List<HazardReport> alerts = hazardReportRepository.findBySiteOrderByCreatedAtDesc(user.assignedSite()).stream()
            .filter((hazard) -> !"CLEARED".equalsIgnoreCase(hazard.getStatus()))
            .limit(10)
            .toList();
        alerts.forEach(h -> { entityManager.detach(h); h.stripPhotoDataForList(); });
        return alerts;
    }

    // Fetch the (potentially large) base64 photo for one hazard report on demand — never
    // returned in bulk from the list endpoints above. Hazards are visible site-wide (via
    // site-alerts) so this only checks site membership, not report ownership.
    @GetMapping("/api/hazards/{id}/photo")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER','ROLE_GUEST')")
    public Map<String, String> getHazardPhoto(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        HazardReport report = hazardReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hazard report not found"));
        if (!report.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hazard belongs to a different site");
        return Map.of("photoData", report.getPhotoData() != null ? report.getPhotoData() : "");
    }

    @PostMapping("/api/hazards")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public HazardReport createHazard(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateHazardRequest request
    ) {
        HazardReport report = new HazardReport(
            user.role(),
            user.fullName(),
            user.email(),
            request.hazardType(),
            user.assignedSite(),
            request.location(),
            request.description(),
            request.severity()
        );
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setPhotoData(request.photoData());

        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = hazardReportRepository.findByClientRequestId(request.clientRequestId());
            if (existing.isPresent()) return existing.get();
            report.setClientRequestId(request.clientRequestId());
        }

        HazardReport saved = hazardReportRepository.save(report);
        auditLogService.record(
            "HAZARD_SUBMITTED",
            saved.getReportedByRole(),
            saved.getReportedByName(),
            saved.getReportedByEmail(),
            "HazardReport",
            saved.getId(),
            saved.getHazardType() + " at " + saved.getLocation()
        );

        // Send push + in-app notification for High and Critical hazards
        try {
            String severity = request.severity() != null ? request.severity() : "Medium";
            if ("High".equals(severity) || "Critical".equals(severity)) {
                List<AppUser> recipients = appUserRepository.findByAssignedSiteIgnoreCase(saved.getSite())
                    .stream()
                    .filter(u -> "supervisor".equals(u.getRole()) || "safetyOfficer".equals(u.getRole()))
                    .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
                    .collect(Collectors.toList());

                String notifTitle = severity + " Hazard — " + saved.getSite();
                String notifBody = user.fullName() + " reported: " + request.hazardType() + " at " + request.location();

                List<String> tokens = recipients.stream()
                    .filter(u -> !Boolean.FALSE.equals(u.getNotifyHazard()))
                    .map(AppUser::getPushToken)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.toList());
                pushNotificationService.sendToTokens(tokens, notifTitle, notifBody, "default");

                for (AppUser recipient : recipients) {
                    notificationService.notify(recipient.getEmail(), "HAZARD", notifTitle, notifBody, "HazardReport", saved.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Push notification failed for hazard: {}", e.getMessage());
        }

        return saved;
    }

    @PatchMapping("/api/hazards/{id}/review")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public HazardReport reviewHazard(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @Valid @RequestBody ReviewHazardRequest request
    ) {
        HazardReport report = hazardReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hazard report not found"));
        if (!report.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hazard belongs to a different site");
        report.setStatus("REVIEWED");
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedByRole(user.role());
        report.setReviewedByName(user.fullName());
        report.setReviewedByEmail(user.email());
        report.setActionTaken(request.actionTaken());

        HazardReport saved = hazardReportRepository.save(report);
        auditLogService.record(
            "HAZARD_REVIEWED",
            saved.getReviewedByRole(),
            saved.getReviewedByName(),
            saved.getReviewedByEmail(),
            "HazardReport",
            saved.getId(),
            saved.getActionTaken()
        );

        return saved;
    }

    @PatchMapping("/api/hazards/{id}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public HazardReport closeHazard(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @RequestBody(required = false) CloseHazardRequest request
    ) {
        String actionTaken = (request != null && request.actionTaken() != null && !request.actionTaken().isBlank())
            ? request.actionTaken()
            : "Hazard cleared";

        HazardReport report = hazardReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hazard report not found"));
        if (!report.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hazard belongs to a different site");
        report.setStatus("CLEARED");
        report.setClosedAt(LocalDateTime.now());
        report.setClosedByRole(user.role());
        report.setClosedByName(user.fullName());
        report.setClosedByEmail(user.email());
        report.setActionTaken(actionTaken);

        HazardReport saved = hazardReportRepository.save(report);
        auditLogService.record(
            "HAZARD_CLEARED",
            saved.getClosedByRole(),
            saved.getClosedByName(),
            saved.getClosedByEmail(),
            "HazardReport",
            saved.getId(),
            saved.getActionTaken()
        );

        return saved;
    }

    @GetMapping("/api/hazards/export/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public ResponseEntity<String> exportCsv(@AuthenticationPrincipal AuthenticatedUser user) {
        List<HazardReport> rows = hazardReportRepository.findBySiteOrderByCreatedAtDesc(user.assignedSite());
        StringBuilder csv = new StringBuilder();
        csv.append(CsvExportUtil.row("type", "location", "severity", "status", "reportedBy",
                "createdAt", "reviewedAt", "closedAt", "actionTaken"));
        for (HazardReport h : rows) {
            csv.append(CsvExportUtil.row(
                    h.getHazardType(), h.getLocation(), h.getSeverity(), h.getStatus(),
                    h.getReportedByName(), h.getCreatedAt(), h.getReviewedAt(),
                    h.getClosedAt(), h.getActionTaken()));
        }
        return CsvExportUtil.response("hazard-reports.csv", csv.toString());
    }
}