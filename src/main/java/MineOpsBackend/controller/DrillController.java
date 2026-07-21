package MineOpsBackend.controller;

import MineOpsBackend.dto.SignOffStepRequest;
import MineOpsBackend.dto.StartDrillRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.DrillOperation;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.DrillOperationRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
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

    public DrillController(
        DrillOperationRepository drillOperationRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        PushNotificationService pushNotificationService
    ) {
        this.drillOperationRepository = drillOperationRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.pushNotificationService = pushNotificationService;
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

    private void notifySafetyStaff(DrillOperation op, String step, String site) {
        List<String> tokens = appUserRepository
            .findByRoleInAndAssignedSiteIgnoreCase(List.of("supervisor", "safetyOfficer"), site)
            .stream()
            .map(AppUser::getPushToken)
            .filter(t -> t != null && !t.isBlank())
            .toList();

        if (tokens.isEmpty()) return;

        String title, body;
        if ("drilling".equals(step)) {
            title = "Blasting Step Ready — " + op.getZone();
            body = op.getWorkerName() + " (" + op.getDrillType() + ") has completed drilling and is ready to blast";
        } else {
            title = "Blasting Confirmed — " + op.getZone();
            body = op.getWorkerName() + " (" + op.getDrillType() + ") has signed off the blasting step";
        }

        pushNotificationService.sendToTokens(tokens, title, body, "default");
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
