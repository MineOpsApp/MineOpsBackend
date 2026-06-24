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
import MineOpsBackend.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import java.util.stream.Collectors;

@RestController
public class HazardController {

    private final HazardReportRepository hazardReportRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;

    public HazardController(
        HazardReportRepository hazardReportRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService
    ) {
        this.hazardReportRepository = hazardReportRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/api/hazards")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Page<HazardReport> getHazards(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        if ("worker".equals(user.role())) {
            return hazardReportRepository.findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(user.email(), pageable);
        }
        return hazardReportRepository.findBySiteOrderByCreatedAtDesc(user.assignedSite(), pageable);
    }

    @GetMapping("/api/hazards/site-alerts")
    @PreAuthorize("isAuthenticated()")
    public List<HazardReport> getSiteHazardAlerts(@AuthenticationPrincipal AuthenticatedUser user) {
        return hazardReportRepository.findBySiteOrderByCreatedAtDesc(user.assignedSite()).stream()
            .filter((hazard) -> !"CLEARED".equalsIgnoreCase(hazard.getStatus()))
            .limit(10)
            .toList();
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
            request.site(),
            request.location(),
            request.description(),
            request.severity()
        );
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setPhotoData(request.photoData());

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

        // Send push notification for High and Critical hazards
        try {
            String severity = request.severity() != null ? request.severity() : "Medium";
            if ("High".equals(severity) || "Critical".equals(severity)) {
                List<String> tokens = appUserRepository.findByAssignedSiteIgnoreCase(request.site())
                    .stream()
                    .filter(u -> "supervisor".equals(u.getRole()) || "safetyOfficer".equals(u.getRole()))
                    .map(AppUser::getPushToken)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.toList());

                String emoji = "Critical".equals(severity) ? "🔴" : "🟠";
                pushNotificationService.sendToTokens(
                    tokens,
                    emoji + " " + severity + " Hazard — " + request.site(),
                    user.fullName() + " reported: " + request.hazardType() + " at " + request.location(),
                    "default"
                );
            }
        } catch (Exception e) {
            System.err.println("Push notification failed for hazard: " + e.getMessage());
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
}