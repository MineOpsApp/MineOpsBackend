package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.Site;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.InsuranceProviderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class InsuranceController {

    private final AppUserRepository userRepo;
    private final SiteRepository siteRepo;
    private final InsuranceProviderService insuranceProviderService;
    private final AuditLogService auditLogService;

    public InsuranceController(
        AppUserRepository userRepo,
        SiteRepository siteRepo,
        InsuranceProviderService insuranceProviderService,
        AuditLogService auditLogService
    ) {
        this.userRepo = userRepo;
        this.siteRepo = siteRepo;
        this.insuranceProviderService = insuranceProviderService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/insurance/status")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> getStatus(@AuthenticationPrincipal AuthenticatedUser user) {
        Site site = siteRepo.findByNameIgnoreCase(user.assignedSite()).orElse(null);
        if (site == null || !site.isInsuranceEnabled()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "NOT_AVAILABLE");
            return result;
        }
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", appUser.getInsuranceStatus() != null ? appUser.getInsuranceStatus() : "NOT_INSURED");
        result.put("enrolledAt", appUser.getInsuranceEnrolledAt());
        return result;
    }

    @PostMapping("/api/insurance/apply")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> apply(@AuthenticationPrincipal AuthenticatedUser user) {
        Site site = siteRepo.findByNameIgnoreCase(user.assignedSite())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));
        if (!site.isInsuranceEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insurance is not enabled for this site");
        }
        AppUser appUser = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if ("INSURED".equals(appUser.getInsuranceStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in insurance");
        }
        insuranceProviderService.enroll(appUser);
        userRepo.save(appUser);
        auditLogService.record(
            "INSURANCE_ENROLLED",
            user.role(), user.fullName(), user.email(),
            "APP_USER", appUser.getId(),
            "site=" + site.getName()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", appUser.getInsuranceStatus());
        result.put("enrolledAt", appUser.getInsuranceEnrolledAt());
        return result;
    }
}
