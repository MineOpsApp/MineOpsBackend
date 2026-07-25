package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateIncidentRequest;
import MineOpsBackend.dto.UpdateIncidentStatusRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.IncidentReport;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.IncidentReportRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import MineOpsBackend.util.CsvExportUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class IncidentController {

    private static final Logger log = LoggerFactory.getLogger(IncidentController.class);

    private final IncidentReportRepository incidentReportRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    public IncidentController(
        IncidentReportRepository incidentReportRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService,
        NotificationService notificationService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
        this.notificationService = notificationService;
    }

    @PostMapping("/api/incidents")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public IncidentReport createIncident(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateIncidentRequest request
    ) {
        LocalDateTime incidentAt = null;
        if (request.incidentAt() != null) {
            try { incidentAt = LocalDateTime.parse(request.incidentAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
            catch (Exception ignored) {}
        }

        IncidentReport report = new IncidentReport(
            user.email(), user.fullName(), user.role(),
            user.assignedSite(), request.zone(),
            request.category(), request.severity(),
            request.description(), request.involvedPersons(),
            request.firstAidGiven(), request.hospitalRequired(),
            request.immediateAction(),
            request.latitude(), request.longitude(),
            request.photoData(), incidentAt
        );

        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = incidentReportRepository.findByClientRequestId(request.clientRequestId());
            if (existing.isPresent()) return existing.get();
            report.setClientRequestId(request.clientRequestId());
        }

        IncidentReport saved = incidentReportRepository.save(report);

        auditLogService.record("INCIDENT_REPORTED", user.role(), user.fullName(), user.email(),
            "IncidentReport", saved.getId(),
            request.category() + " (" + request.severity() + ") in " + request.zone());

        // Notify supervisors and safety officers for every incident report, regardless of
        // severity — a supervisor needs to know about a Minor incident too, just not with the
        // same urgency as a Serious/Critical one. Previously this block only ran for
        // "Serious"/"Critical", so a Minor report (the default-selected severity in the app)
        // silently notified no one.
        try {
            String notifTitle = request.severity() + " Incident — " + user.assignedSite();
            String notifBody = request.category() + " in " + request.zone() + " reported by " + user.fullName();
            boolean urgent = "Serious".equals(request.severity()) || "Critical".equals(request.severity());

            List<AppUser> recipients = appUserRepository.findByAssignedSiteIgnoreCase(user.assignedSite())
                .stream()
                .filter(u -> "supervisor".equals(u.getRole()) || "safetyOfficer".equals(u.getRole()))
                .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
                .collect(Collectors.toList());

            List<String> tokens = recipients.stream()
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
            // Only Serious/Critical bypass DND on the "sos" channel — a Minor incident shouldn't
            // buzz a supervisor's phone at full emergency urgency, but they should still know.
            pushNotificationService.sendToTokens(tokens, notifTitle, notifBody, urgent ? "sos" : "default");

            for (AppUser recipient : recipients) {
                notificationService.notify(recipient.getEmail(), "INCIDENT", notifTitle, notifBody, "IncidentReport", saved.getId());
            }
        } catch (Exception e) {
            log.warn("Push notification failed for incident: {}", e.getMessage());
        }

        return saved;
    }

    @GetMapping("/api/incidents/mine")
    @PreAuthorize("isAuthenticated()")
    public List<IncidentReport> getMyIncidents(@AuthenticationPrincipal AuthenticatedUser user) {
        List<IncidentReport> reports = incidentReportRepository.findByReportedByEmailIgnoreCaseOrderByReportedAtDesc(user.email());
        reports.forEach(r -> { entityManager.detach(r); r.stripPhotoDataForList(); });
        return reports;
    }

    @GetMapping("/api/incidents")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<IncidentReport> getSiteIncidents(@AuthenticationPrincipal AuthenticatedUser user) {
        List<IncidentReport> reports = incidentReportRepository.findBySiteOrderByReportedAtDesc(user.assignedSite());
        reports.forEach(r -> { entityManager.detach(r); r.stripPhotoDataForList(); });
        return reports;
    }

    // Fetch the (potentially large) base64 photo for one incident report on demand — never
    // returned in bulk from the list endpoints above. Visible to the reporting worker (own
    // incidents) or any supervisor/safety officer on the same site.
    @GetMapping("/api/incidents/{id}/photo")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> getIncidentPhoto(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        IncidentReport report = incidentReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident report not found"));

        boolean isOwnReport = report.getReportedByEmail().equalsIgnoreCase(user.email());
        boolean isSiteReviewer = ("supervisor".equals(user.role()) || "safetyOfficer".equals(user.role()))
            && report.getSite().equalsIgnoreCase(user.assignedSite());
        if (!isOwnReport && !isSiteReviewer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return Map.of("photoData", report.getPhotoData() != null ? report.getPhotoData() : "");
    }

    @PatchMapping("/api/incidents/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public IncidentReport updateStatus(
        
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @Valid @RequestBody UpdateIncidentStatusRequest request

        
    ) {
        IncidentReport report = incidentReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident report not found"));
        if (!report.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incident belongs to a different site");

        report.setStatus(request.status());
report.setUpdatedAt(LocalDateTime.now());
if (request.notes() != null && !request.notes().isBlank()) {
    report.setInvestigationNotes(request.notes());
}
IncidentReport saved = incidentReportRepository.save(report);

auditLogService.record("INCIDENT_STATUS_UPDATED", user.role(), user.fullName(), user.email(),
    "IncidentReport", id, request.status());

return saved;
    }

    @GetMapping("/api/incidents/export/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public ResponseEntity<String> exportCsv(@AuthenticationPrincipal AuthenticatedUser user) {
        List<IncidentReport> rows = incidentReportRepository.findBySiteOrderByReportedAtDesc(user.assignedSite());
        StringBuilder csv = new StringBuilder();
        csv.append(CsvExportUtil.row("category", "severity", "zone", "description", "status",
                "firstAidGiven", "hospitalRequired", "reportedAt"));
        for (IncidentReport r : rows) {
            csv.append(CsvExportUtil.row(
                    r.getCategory(), r.getSeverity(), r.getZone(), r.getDescription(),
                    r.getStatus(), r.getFirstAidGiven(), r.getHospitalRequired(), r.getReportedAt()));
        }
        return CsvExportUtil.response("incident-reports.csv", csv.toString());
    }
}