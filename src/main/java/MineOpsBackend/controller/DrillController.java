package MineOpsBackend.controller;

import MineOpsBackend.dto.BlastDecisionRequest;
import MineOpsBackend.dto.SignOffStepRequest;
import MineOpsBackend.dto.StartDrillRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.DrillOperation;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.DrillOperationRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class DrillController {

    private final DrillOperationRepository drillOperationRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final PushNotificationService pushNotificationService;
    private final NotificationService notificationService;

    public DrillController(
        DrillOperationRepository drillOperationRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService,
        NotificationService notificationService
    ) {
        this.drillOperationRepository = drillOperationRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
        this.notificationService = notificationService;
    }

    @PostMapping("/api/drill-operations")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public DrillOperation startDrill(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody StartDrillRequest request
    ) {
        DrillOperation op = drillOperationRepository.save(new DrillOperation(
            user.email(), user.fullName(), user.assignedSite(),
            request.zone(), request.equipmentCode(), request.drillType()
        ));

        auditLogService.record("DRILL_STARTED", user.role(), user.fullName(), user.email(),
            "DrillOperation", op.getId(), request.drillType() + " in " + request.zone());

        notifySafetyStaff(op, "started", user.assignedSite());

        return op;
    }

    @PostMapping("/api/drill-operations/{id}/sign-off")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public DrillOperation signOffStep(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @Valid @RequestBody SignOffStepRequest request
    ) {
        DrillOperation op = drillOperationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drill operation not found"));

        if (!op.getWorkerEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot sign off another worker's drill operation");
        }

        LocalDateTime now = LocalDateTime.now();
        switch (request.step()) {
            case "setup" -> {
                op.setStepSetupComplete(true);
                op.setStepSetupAt(now);
                op.setStepSetupNotes(request.notes());
            }
            case "drilling" -> {
                if (!Boolean.TRUE.equals(op.getStepSetupComplete()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete setup step first");
                op.setStepDrillingComplete(true);
                op.setStepDrillingAt(now);
                op.setStepDrillingNotes(request.notes());
            }
            case "blasting" -> {
                if (!Boolean.TRUE.equals(op.getStepDrillingComplete()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete drilling step first");
                if (!"APPROVED".equals(op.getBlastDecision())) {
                    String reason = "STOP".equals(op.getBlastDecision())
                        ? "A safety officer has stopped this blast — it cannot be signed off"
                        : "WAIT".equals(op.getBlastDecision())
                        ? "A safety officer has asked you to wait — this blast is not yet cleared"
                        : "A supervisor or safety officer must approve this blast before it can be signed off";
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
                }
                op.setStepBlastingComplete(true);
                op.setStepBlastingAt(now);
                op.setStepBlastingNotes(request.notes());
            }
            case "cleanup" -> {
                if (!Boolean.TRUE.equals(op.getStepBlastingComplete()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complete blasting step first");
                op.setStepCleanupComplete(true);
                op.setStepCleanupAt(now);
                op.setStepCleanupNotes(request.notes());
                op.setStatus("COMPLETED");
                op.setCompletedAt(now);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid step");
        }

        DrillOperation saved = drillOperationRepository.save(op);

        auditLogService.record("DRILL_STEP_SIGNED_OFF", user.role(), user.fullName(), user.email(),
            "DrillOperation", id, "Step: " + request.step());

        if ("drilling".equals(request.step()) || "blasting".equals(request.step())) {
            notifySafetyStaff(saved, request.step(), user.assignedSite());
        }

        return saved;
    }

    @PostMapping("/api/drill-operations/{id}/approve-blast")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public DrillOperation approveBlast(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        DrillOperation op = drillOperationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drill operation not found"));

        if (!op.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Drill operation belongs to a different site");
        if (!Boolean.TRUE.equals(op.getStepDrillingComplete()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Worker hasn't completed the drilling step yet");
        if (Boolean.TRUE.equals(op.getStepBlastingComplete()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blasting step is already signed off");
        if ("APPROVED".equals(op.getBlastDecision()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This blast is already approved");

        op.setBlastApprovedBy(user.email());
        op.setBlastApprovedByName(user.fullName());
        op.setBlastApprovedAt(LocalDateTime.now());
        op.setBlastDecision("APPROVED");
        op.setBlastDecisionNote(null);
        DrillOperation saved = drillOperationRepository.save(op);

        auditLogService.record("DRILL_BLAST_APPROVED", user.role(), user.fullName(), user.email(),
            "DrillOperation", id, op.getZone() + " — worker=" + op.getWorkerName());

        notifyWorkerOfDecision(saved, user, "APPROVED", null);

        return saved;
    }

    /** Safety officer / supervisor call on a pending blast: APPROVED, WAIT, or STOP. Unlike the
     *  legacy approve-only endpoint, this can be called repeatedly as the situation changes
     *  (e.g. WAIT now, APPROVED once the area is clear) as long as blasting hasn't been signed off. */
    @PostMapping("/api/drill-operations/{id}/blast-decision")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public DrillOperation setBlastDecision(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @Valid @RequestBody BlastDecisionRequest request
    ) {
        DrillOperation op = drillOperationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drill operation not found"));

        if (!op.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Drill operation belongs to a different site");
        if (!Boolean.TRUE.equals(op.getStepDrillingComplete()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Worker hasn't completed the drilling step yet");
        if (Boolean.TRUE.equals(op.getStepBlastingComplete()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blasting step is already signed off");

        op.setBlastApprovedBy(user.email());
        op.setBlastApprovedByName(user.fullName());
        op.setBlastApprovedAt(LocalDateTime.now());
        op.setBlastDecision(request.decision());
        op.setBlastDecisionNote(request.note() != null && !request.note().isBlank() ? request.note().trim() : null);
        DrillOperation saved = drillOperationRepository.save(op);

        auditLogService.record("DRILL_BLAST_" + request.decision(), user.role(), user.fullName(), user.email(),
            "DrillOperation", id, op.getZone() + " — worker=" + op.getWorkerName()
                + (op.getBlastDecisionNote() != null ? " — " + op.getBlastDecisionNote() : ""));

        notifyWorkerOfDecision(saved, user, request.decision(), op.getBlastDecisionNote());

        return saved;
    }

    private void notifyWorkerOfDecision(DrillOperation op, AuthenticatedUser user, String decision, String note) {
        String title, body;
        if ("APPROVED".equals(decision)) {
            title = "Blast Approved — " + op.getZone();
            body = user.fullName() + " approved your blast. You may now sign off the blasting step.";
        } else if ("WAIT".equals(decision)) {
            title = "Hold — Blast Not Cleared — " + op.getZone();
            body = user.fullName() + " says wait — do not blast yet." + (note != null ? " " + note : "");
        } else {
            title = "Blast Stopped — " + op.getZone();
            body = user.fullName() + " has stopped this blast. Do not proceed." + (note != null ? " " + note : "");
        }

        notificationService.notify(op.getWorkerEmail(), "DRILL", title, body, "DrillOperation", op.getId());
        appUserRepository.findByEmailIgnoreCase(op.getWorkerEmail()).ifPresent(worker -> {
            String token = worker.getPushToken();
            if (token != null && !token.isBlank()) {
                pushNotificationService.sendToToken(token, title, body, "default");
            }
        });
    }

    private void notifySafetyStaff(DrillOperation op, String step, String site) {
        List<AppUser> recipients = appUserRepository
            .findByRoleInAndAssignedSiteIgnoreCase(List.of("supervisor", "safetyOfficer"), site)
            .stream()
            .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
            .toList();

        if (recipients.isEmpty()) return;

        String title, body;
        if ("started".equals(step)) {
            title = "Drill Started — " + op.getZone();
            body = op.getWorkerName() + " started a " + op.getDrillType() + " drill operation in " + op.getZone() + ".";
        } else if ("drilling".equals(step)) {
            title = "Blasting Step Ready — " + op.getZone();
            body = op.getWorkerName() + " (" + op.getDrillType() + ") has completed drilling and is ready to blast";
        } else {
            title = "Blasting Confirmed — " + op.getZone();
            body = op.getWorkerName() + " (" + op.getDrillType() + ") has signed off the blasting step";
        }

        List<String> tokens = recipients.stream()
            .map(AppUser::getPushToken)
            .filter(t -> t != null && !t.isBlank())
            .toList();
        if (!tokens.isEmpty()) {
            pushNotificationService.sendToTokens(tokens, title, body, "default");
        }
        for (AppUser recipient : recipients) {
            notificationService.notify(recipient.getEmail(), "DRILL", title, body, "DrillOperation", op.getId());
        }
    }

    @GetMapping("/api/drill-operations/mine")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<DrillOperation> getMyDrills(@AuthenticationPrincipal AuthenticatedUser user) {
        return drillOperationRepository.findByWorkerEmailIgnoreCaseOrderByStartedAtDesc(user.email());
    }

    @GetMapping("/api/drill-operations")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<DrillOperation> getSiteDrills(@AuthenticationPrincipal AuthenticatedUser user) {
        return drillOperationRepository.findBySiteOrderByStartedAtDesc(user.assignedSite());
    }
}
