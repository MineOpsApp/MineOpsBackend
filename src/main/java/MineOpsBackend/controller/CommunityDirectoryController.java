package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.Site;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.SafetyScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/community")
public class CommunityDirectoryController {

    private final SiteRepository siteRepo;
    private final AppUserRepository userRepo;
    private final SafetyScoreService safetyScoreService;

    public CommunityDirectoryController(SiteRepository siteRepo,
                                        AppUserRepository userRepo,
                                        SafetyScoreService safetyScoreService) {
        this.siteRepo = siteRepo;
        this.userRepo = userRepo;
        this.safetyScoreService = safetyScoreService;
    }

    @GetMapping("/mines")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public List<Map<String, Object>> getMines() {
        return siteRepo.findAll().stream()
                .map(this::toMineProfile)
                .collect(Collectors.toList());
    }

    @GetMapping("/mines/{siteName}")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public Map<String, Object> getMine(@PathVariable String siteName) {
        Site site = siteRepo.findByNameIgnoreCase(siteName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));
        return toMineProfile(site);
    }

    @GetMapping("/buyers")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public List<Map<String, Object>> getVerifiedBuyers() {
        return userRepo.findByRoleAndBuyerVerificationStatus("buyer", "VERIFIED").stream()
                .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
                .map(this::toBuyerProfile)
                .collect(Collectors.toList());
    }

    @PatchMapping("/mine-profile")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Map<String, Object> updateMineProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @RequestBody Map<String, Object> body) {
        Site site = siteRepo.findByNameIgnoreCase(user.assignedSite())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));

        if (body.containsKey("mineralsProduced")) site.setMineralsProduced((String) body.get("mineralsProduced"));
        if (body.containsKey("productionCapacity")) site.setProductionCapacity((String) body.get("productionCapacity"));
        if (body.containsKey("establishedYear")) {
            Object yr = body.get("establishedYear");
            site.setEstablishedYear(yr instanceof Integer ? (Integer) yr : Integer.valueOf(yr.toString()));
        }
        if (body.containsKey("profileDescription")) site.setProfileDescription((String) body.get("profileDescription"));
        if (body.containsKey("contactEmail")) site.setContactEmail((String) body.get("contactEmail"));

        siteRepo.save(site);
        return toMineProfile(site);
    }

    private Map<String, Object> toMineProfile(Site site) {
        int score = safetyScoreService.computeScore(site.getName());
        return Map.of(
                "name", site.getName(),
                "mineralsProduced", site.getMineralsProduced() != null ? site.getMineralsProduced() : "",
                "productionCapacity", site.getProductionCapacity() != null ? site.getProductionCapacity() : "",
                "establishedYear", site.getEstablishedYear() != null ? site.getEstablishedYear() : 0,
                "profileDescription", site.getProfileDescription() != null ? site.getProfileDescription() : "",
                "contactEmail", site.getContactEmail() != null ? site.getContactEmail() : "",
                "safetyScore", score
        );
    }

    private Map<String, Object> toBuyerProfile(AppUser u) {
        return Map.of(
                "fullName", u.getFullName() != null ? u.getFullName() : "",
                "email", u.getEmail(),
                "companyName", u.getBusinessName() != null ? u.getBusinessName() : "",
                "buyerVerificationStatus", u.getBuyerVerificationStatus() != null ? u.getBuyerVerificationStatus() : ""
        );
    }
}
