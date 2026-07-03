package MineOpsBackend.controller;

import MineOpsBackend.model.PaySplitConfig;
import MineOpsBackend.repository.PaySplitConfigRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@RestController
public class PaySplitConfigController {

    private static final Set<String> VALID_FORMULAS = Set.of("EQUAL_PER_HEAD", "WEIGHTED_BY_HOURS");

    private final PaySplitConfigRepository configRepo;
    private final AuditLogService auditLogService;

    public PaySplitConfigController(PaySplitConfigRepository configRepo, AuditLogService auditLogService) {
        this.configRepo = configRepo;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/pay/split-config")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public PaySplitConfig getConfig(@AuthenticationPrincipal AuthenticatedUser user) {
        return configRepo.findBySiteIgnoreCase(user.assignedSite())
            .orElseGet(() -> new PaySplitConfig(user.assignedSite(), "EQUAL_PER_HEAD", null));
    }

    @PutMapping("/api/pay/split-config")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public PaySplitConfig updateConfig(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, String> body
    ) {
        String formulaType = body.get("formulaType");
        if (formulaType == null || !VALID_FORMULAS.contains(formulaType))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "formulaType must be one of: " + VALID_FORMULAS);

        PaySplitConfig config = configRepo.findBySiteIgnoreCase(user.assignedSite())
            .orElseGet(() -> new PaySplitConfig(user.assignedSite(), formulaType, user.email()));

        config.setFormulaType(formulaType);
        config.setUpdatedBy(user.email());
        config.setUpdatedAt(LocalDateTime.now());
        PaySplitConfig saved = configRepo.save(config);

        auditLogService.record("PAY_CONFIG_UPDATED", user.role(), user.fullName(), user.email(),
            "PaySplitConfig", saved.getId(), formulaType);
        return saved;
    }
}
