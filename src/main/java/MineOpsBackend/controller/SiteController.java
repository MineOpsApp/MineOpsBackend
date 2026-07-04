package MineOpsBackend.controller;

import MineOpsBackend.model.Site;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class SiteController {

    private final SiteRepository siteRepository;
    private final AuditLogService auditLogService;

    public SiteController(SiteRepository siteRepository, AuditLogService auditLogService) {
        this.siteRepository = siteRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/sites")
    @PreAuthorize("isAuthenticated()")
    public List<Site> getSites() {
        return siteRepository.findAll();
    }

    @PatchMapping("/api/sites/visibility")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Site updateVisibility(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Boolean> body
    ) {
        Site site = siteRepository.findByNameIgnoreCase(user.assignedSite())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));
        boolean visible = Boolean.TRUE.equals(body.get("visible"));
        site.setInventoryVisibleToGuests(visible);
        Site saved = siteRepository.save(site);
        auditLogService.record(
            "INVENTORY_VISIBILITY_CHANGED",
            user.role(), user.fullName(), user.email(),
            "SITE", saved.getId(),
            "inventoryVisibleToGuests=" + visible
        );
        return saved;
    }

    @PatchMapping("/api/sites/insurance-config")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Site updateInsuranceConfig(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> body
    ) {
        Site site = siteRepository.findByNameIgnoreCase(user.assignedSite())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));

        if (body.containsKey("insuranceEnabled")) {
            site.setInsuranceEnabled(Boolean.TRUE.equals(body.get("insuranceEnabled")));
        }
        if (body.containsKey("insuranceProviderName")) {
            site.setInsuranceProviderName((String) body.get("insuranceProviderName"));
        }
        if (body.containsKey("insurancePremium")) {
            Object premium = body.get("insurancePremium");
            site.setInsurancePremium(premium != null ? new BigDecimal(premium.toString()) : null);
        }
        if (body.containsKey("insuranceDeductionMode")) {
            String mode = (String) body.get("insuranceDeductionMode");
            if (mode != null && !Set.of("DEDUCT_FROM_PAY", "BILL_TO_MINE").contains(mode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid deduction mode");
            }
            site.setInsuranceDeductionMode(mode);
        }

        Site saved = siteRepository.save(site);
        auditLogService.record(
            "INSURANCE_CONFIG_CHANGED",
            user.role(), user.fullName(), user.email(),
            "SITE", saved.getId(),
            "insuranceEnabled=" + saved.isInsuranceEnabled()
        );
        return saved;
    }
}
