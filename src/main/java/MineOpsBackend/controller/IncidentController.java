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

        // Notify supervisors and safety officers for Serious and Critical incidents
        if ("Serious".equals(request.severity()) || "Critical".equals(request.severity())) {
            try {
                String emoji = "Critical".equals(request.severity()) ? "🔴" : "🟠";
                String notifTitle = emoji + " " + request.severity() + " Incident — " + user.assignedSite();
                String notifBody = request.category() + " in " + request.zone() + " reported by " + user.fullName();

                List<AppUser> recipients = appUserRepository.findByAssignedSiteIgnoreCase(user.assignedSite())
                    .stream()
                    .filter(u -> "supervisor".equals(u.getRole()) || "safetyOfficer".equals(u.getRole()))
                    .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
                    .collect(Collectors.toList());

                List<String> tokens = recipients.stream()
                    .map(AppUser::getPushToken)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.toList());
                pushNotificationService.sendToTokens(tokens, notifTitle, notifBody, "sos");

                for (AppUser recipient : recipients) {
                    notificationService.notify(recipient.getEmail(), "INCIDENT", notifTitle, notifBody, "IncidentReport", saved.getId());
                }
            } catch (Exception e) {
                log.warn("Push notification failed for incident: {}", e.getMessage());
            }
        }

        return saved;
    }

    @GetMapping("/api/incidents/mine")
    @PreAuthorize("isAuthenticated()")
    public List<IncidentReport> getMyIncidents(@AuthenticationPrincipal AuthenticatedUser user) {
        return incidentReportRepository.findByReportedByEmailIgnoreCaseOrderByReportedAtDesc(user.email());
    }

    @GetMapping("/api/incidents")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<IncidentReport> getSiteIncidents(@AuthenticationPrincipal AuthenticatedUser user) {
        return incidentReportRepository.findBySiteOrderByReportedAtDesc(user.assignedSite());
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