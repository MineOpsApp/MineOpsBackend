package MineOpsBackend.controller;

import MineOpsBackend.dto.ScheduleBlastRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.BlastSchedule;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.BlastScheduleRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import jakarta.validation.Valid;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class BlastController {

    private static final Logger log = LoggerFactory.getLogger(BlastController.class);

    private final BlastScheduleRepository blastScheduleRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    public BlastController(
        BlastScheduleRepository blastScheduleRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService,
        NotificationService notificationService
    ) {
        this.blastScheduleRepository = blastScheduleRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
        this.notificationService = notificationService;
    }

    @PostMapping("/api/blasts")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public BlastSchedule scheduleBlast(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody ScheduleBlastRequest request
    ) {
        LocalDateTime blastTime = LocalDateTime.parse(request.blastTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if (blastTime.isBefore(LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blast must be scheduled at least 5 minutes in advance");
        }

        BlastSchedule blast = new BlastSchedule(
            user.assignedSite(), request.zone(),
            user.email(), user.fullName(),
            request.notes(), blastTime
        );

        BlastSchedule saved = blastScheduleRepository.save(blast);

        auditLogService.record("BLAST_SCHEDULED", user.role(), user.fullName(), user.email(),
            "BlastSchedule", saved.getId(), request.zone() + " at " + request.blastTime());

        // Notify all site users
        try {
            long minutesUntil = java.time.Duration.between(
                LocalDateTime.now(java.time.ZoneOffset.UTC), blastTime).toMinutes();
            String notifTitle = "Blast Scheduled — " + request.zone();
            String notifBody = "Blasting in " + request.zone() + " in " + minutesUntil + " minutes. Clear the area immediately.";

            List<AppUser> recipients = appUserRepository.findByAssignedSiteIgnoreCase(user.assignedSite())
                .stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(user.email()))
                .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
                .collect(Collectors.toList());

            List<String> tokens = recipients.stream()
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
            pushNotificationService.sendToTokens(tokens, notifTitle, notifBody, "sos");

            for (AppUser recipient : recipients) {
                notificationService.notify(recipient.getEmail(), "BLAST", notifTitle, notifBody, "BlastSchedule", saved.getId());
            }
        } catch (Exception e) {
            log.warn("Push notification failed for blast: {}", e.getMessage());
        }

        return saved;
    }

    @GetMapping("/api/blasts")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<BlastSchedule> getBlasts(@AuthenticationPrincipal AuthenticatedUser user) {
        return blastScheduleRepository.findBySiteAndStatusOrderByBlastTimeAsc(user.assignedSite(), "SCHEDULED");
    }

    @GetMapping("/api/blasts/all")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<BlastSchedule> getAllBlasts(@AuthenticationPrincipal AuthenticatedUser user) {
        return blastScheduleRepository.findBySiteOrderByBlastTimeDesc(user.assignedSite());
    }

    @PatchMapping("/api/blasts/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public BlastSchedule cancelBlast(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        BlastSchedule blast = blastScheduleRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blast schedule not found"));
        if (!blast.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blast belongs to a different site");

        if (!"SCHEDULED".equals(blast.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only scheduled blasts can be cancelled");
        }

        blast.setStatus("CANCELLED");
        blast.setUpdatedAt(LocalDateTime.now());
        BlastSchedule saved = blastScheduleRepository.save(blast);

        auditLogService.record("BLAST_CANCELLED", user.role(), user.fullName(), user.email(),
            "BlastSchedule", id, blast.getZone());

        // Notify site users of cancellation
        try {
            String notifTitle = "Blast Cancelled — " + blast.getZone();
            String notifBody = "The scheduled blast in " + blast.getZone() + " has been cancelled. Area is clear.";

            List<AppUser> recipients = appUserRepository.findByAssignedSiteIgnoreCase(user.assignedSite())
                .stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(user.email()))
                .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
                .collect(Collectors.toList());

            List<String> tokens = recipients.stream()
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
            pushNotificationService.sendToTokens(tokens, notifTitle, notifBody, "default");

            for (AppUser recipient : recipients) {
                notificationService.notify(recipient.getEmail(), "BLAST", notifTitle, notifBody, "BlastSchedule", id);
            }
        } catch (Exception e) {
            log.warn("Push notification failed for blast cancellation: {}", e.getMessage());
        }

        return saved;
    }

    @PatchMapping("/api/blasts/{id}/execute")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public BlastSchedule executeBlast(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        BlastSchedule blast = blastScheduleRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blast schedule not found"));
        if (!blast.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blast belongs to a different site");

        if (!"SCHEDULED".equals(blast.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only scheduled blasts can be executed");
        }

        blast.setStatus("EXECUTED");
        blast.setUpdatedAt(LocalDateTime.now());
        BlastSchedule saved = blastScheduleRepository.save(blast);

        auditLogService.record("BLAST_EXECUTED", user.role(), user.fullName(), user.email(),
            "BlastSchedule", id, blast.getZone());

        return saved;
    }

}
