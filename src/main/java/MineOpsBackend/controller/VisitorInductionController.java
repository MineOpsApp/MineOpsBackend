package MineOpsBackend.controller;

import MineOpsBackend.dto.CompleteInductionRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.VisitorInduction;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.VisitorInductionRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class VisitorInductionController {

    private final VisitorInductionRepository visitorInductionRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    public VisitorInductionController(
        VisitorInductionRepository visitorInductionRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService
    ) {
        this.visitorInductionRepository = visitorInductionRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/inductions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<VisitorInduction> getInductions() {
        return visitorInductionRepository.findAllByOrderByCompletedAtDesc();
    }

    @PostMapping("/api/inductions")
    @PreAuthorize("hasAuthority('ROLE_GUEST')")
    public VisitorInduction completeInduction(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CompleteInductionRequest request
    ) {
        VisitorInduction induction = new VisitorInduction(
            request.visitorType(),
            user.assignedSite()
        );

        VisitorInduction saved = visitorInductionRepository.save(induction);

        appUserRepository.findByEmailIgnoreCase(user.email()).ifPresent(appUser -> {
            if (appUser.getInductionCompletedAt() == null) {
                appUser.setInductionCompletedAt(LocalDateTime.now());
                appUserRepository.save(appUser);
            }
        });

        auditLogService.record(
            "VISITOR_INDUCTION_COMPLETED",
            user.role(),
            user.fullName(),
            user.email(),
            "VisitorInduction",
            saved.getId(),
            saved.getSite()
        );

        return saved;
    }
}