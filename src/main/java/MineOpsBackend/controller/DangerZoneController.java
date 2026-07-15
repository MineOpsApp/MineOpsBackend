package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateDangerZoneRequest;
import MineOpsBackend.dto.UpdateZonePositionRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.BlastSchedule;
import MineOpsBackend.model.DangerZone;
import MineOpsBackend.model.HazardReport;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.BlastScheduleRepository;
import MineOpsBackend.repository.DangerZoneRepository;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DangerZoneController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DangerZoneRepository dangerZoneRepository;
    private final HazardReportRepository hazardReportRepository;
    private final BlastScheduleRepository blastScheduleRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    public DangerZoneController(
        DangerZoneRepository dangerZoneRepository,
        HazardReportRepository hazardReportRepository,
        BlastScheduleRepository blastScheduleRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService,
        NotificationService notificationService
    ) {
        this.dangerZoneRepository = dangerZoneRepository;
        this.hazardReportRepository = hazardReportRepository;
        this.blastScheduleRepository = blastScheduleRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
        this.notificationService = notificationService;
    }

    @GetMapping("/api/danger-zones")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Page<DangerZone> getDangerZones(@AuthenticationPrincipal AuthenticatedUser user, Pageable pageable) {
        return dangerZoneRepository.findBySiteOrderByCreatedAtDesc(user.assignedSite(), pageable);
    }

    @PostMapping("/api/danger-zones")
    @PreAuthorize("hasAuthority('ROLE_SAFETY_OFFICER')")
    public DangerZone createDangerZone(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateDangerZoneRequest request
    ) {
        DangerZone zone = new DangerZone(
            user.assignedSite(),
            request.zoneName(),
            request.riskLevel()
        );

        DangerZone saved = dangerZoneRepository.save(zone);
        auditLogService.record(
            "DANGER_ZONE_CREATED",
            user.role(),
            user.fullName(),
            user.email(),
            "DangerZone",
            saved.getId(),
            saved.getZoneName() + " - " + saved.getRiskLevel()
        );

        try {
            String notifTitle = "🚧 Danger Zone — " + saved.getZoneName();
            String notifBody = saved.getRiskLevel() + " risk zone active at " + saved.getSite() + ". Avoid this area.";

            List<AppUser> recipients = appUserRepository.findByAssignedSiteIgnoreCase(user.assignedSite())
                .stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(user.email()))
                .collect(Collectors.toList());

            List<String> tokens = recipients.stream()
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
            pushNotificationService.sendToTokens(tokens, notifTitle, notifBody, "danger-zone");

            for (AppUser recipient : recipients) {
                notificationService.notify(recipient.getEmail(), "DANGER_ZONE", notifTitle, notifBody, "DangerZone", saved.getId());
            }
        } catch (Exception e) {
            System.err.println("Push notification failed for danger zone: " + e.getMessage());
        }

        return saved;
    }

    @PatchMapping("/api/danger-zones/{id}/position")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public DangerZone updateZonePosition(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody UpdateZonePositionRequest request
    ) {
        DangerZone zone = findAndCheckSite(id, user);
        try {
            zone.setPolygonPoints(MAPPER.writeValueAsString(request.points()));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize polygon points");
        }
        DangerZone saved = dangerZoneRepository.save(zone);
        auditLogService.record(
            "ZONE_POSITION_SET",
            user.role(),
            user.fullName(),
            user.email(),
            "DangerZone",
            saved.getId(),
            saved.getZoneName() + " (" + request.points().size() + " vertices)"
        );
        return saved;
    }

    @GetMapping("/api/danger-zones/{id}/detail")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> getZoneDetail(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        DangerZone zone = findAndCheckSite(id, user);

        String zoneName = zone.getZoneName().toLowerCase();

        List<HazardReport> openHazards = hazardReportRepository
            .findBySiteAndStatusOrderByCreatedAtDesc(zone.getSite(), "OPEN")
            .stream()
            .filter(h -> h.getLocation() != null && h.getLocation().toLowerCase().contains(zoneName))
            .collect(Collectors.toList());

        List<BlastSchedule> scheduledBlasts = blastScheduleRepository
            .findBySiteAndStatusOrderByBlastTimeAsc(zone.getSite(), "SCHEDULED")
            .stream()
            .filter(b -> b.getZone() != null && b.getZone().equalsIgnoreCase(zone.getZoneName()))
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("zone", zone);
        result.put("openHazards", openHazards);
        result.put("scheduledBlasts", scheduledBlasts);
        return result;
    }

    private DangerZone findAndCheckSite(Long id, AuthenticatedUser user) {
        DangerZone zone = dangerZoneRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danger zone not found"));
        if (!zone.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Danger zone belongs to a different site");
        }
        return zone;
    }
}
