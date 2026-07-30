package MineOpsBackend.controller;

import MineOpsBackend.model.Site;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    public SiteController(SiteRepository siteRepository, AuditLogService auditLogService) {
        this.siteRepository = siteRepository;
        this.auditLogService = auditLogService;
    }

    // General site directory — deliberately strips insurance/financial fields (premium,
    // provider) so any authenticated role (worker, buyer, guest, etc.) can't read another
    // site's confidential insurance arrangement just by calling this endpoint. A supervisor's
    // own full record (including insurance fields) is available via /sites/mine. Detach before
    // nulling so this never gets flushed back to the DB (open-in-view keeps the session alive
    // for the whole request).
    //
    // Deliberately public (see SecurityConfig — GET /api/sites is in permitAll): the unauthenticated
    // registration screen needs the real list of sites for its "Assigned Site" picker, instead of
    // the hardcoded list that used to drift out of sync with what's actually seeded. Nothing sensitive
    // is exposed here since insurance fields are already stripped above for every caller.
    @GetMapping("/api/sites")
    public List<Site> getSites() {
        List<Site> sites = siteRepository.findAll();
        sites.forEach(s -> {
            entityManager.detach(s);
            s.setInsuranceProviderName(null);
            s.setInsurancePremium(null);
        });
        return sites;
    }

    // A supervisor's own site, with the full record including insurance/financial fields.
    @GetMapping("/api/sites/mine")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Site getMySite(@AuthenticationPrincipal AuthenticatedUser user) {
        return siteRepository.findByNameIgnoreCase(user.assignedSite())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));
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
