package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.SupervisorSiteAccess;
import MineOpsBackend.repository.*;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.SafetyIntelligenceService;
import MineOpsBackend.service.SafetyScoreService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SupervisorDashboardController {

    private final AppUserRepository userRepo;
    private final HazardReportRepository hazardRepo;
    private final NoticeRepository noticeRepo;
    private final AttendanceRepository attendanceRepo;
    private final ShiftLogRepository shiftLogRepo;
    private final WorkerMessageRepository messageRepo;
    private final ShiftAnnouncementRepository announcementRepo;
    private final CertificationRepository certRepo;
    private final LoneWorkerRepository loneWorkerRepo;
    private final SafetyScoreService safetyScoreService;
    private final SafetyIntelligenceService safetyIntelligenceService;
    private final MineralListingRepository listingRepo;
    private final MarketplaceOfferRepository offerRepo;
    private final MarketplaceTransactionRepository txRepo;
    private final TransactionDisputeRepository disputeRepo;
    private final SupervisorSiteAccessRepository siteAccessRepo;

    public SupervisorDashboardController(
        AppUserRepository userRepo,
        HazardReportRepository hazardRepo,
        NoticeRepository noticeRepo,
        AttendanceRepository attendanceRepo,
        ShiftLogRepository shiftLogRepo,
        WorkerMessageRepository messageRepo,
        ShiftAnnouncementRepository announcementRepo,
        CertificationRepository certRepo,
        LoneWorkerRepository loneWorkerRepo,
        SafetyScoreService safetyScoreService,
        SafetyIntelligenceService safetyIntelligenceService,
        MineralListingRepository listingRepo,
        MarketplaceOfferRepository offerRepo,
        MarketplaceTransactionRepository txRepo,
        TransactionDisputeRepository disputeRepo,
        SupervisorSiteAccessRepository siteAccessRepo
    ) {
        this.userRepo = userRepo;
        this.hazardRepo = hazardRepo;
        this.noticeRepo = noticeRepo;
        this.attendanceRepo = attendanceRepo;
        this.shiftLogRepo = shiftLogRepo;
        this.messageRepo = messageRepo;
        this.announcementRepo = announcementRepo;
        this.certRepo = certRepo;
        this.loneWorkerRepo = loneWorkerRepo;
        this.safetyScoreService = safetyScoreService;
        this.safetyIntelligenceService = safetyIntelligenceService;
        this.listingRepo = listingRepo;
        this.offerRepo = offerRepo;
        this.txRepo = txRepo;
        this.disputeRepo = disputeRepo;
        this.siteAccessRepo = siteAccessRepo;
    }

    @GetMapping("/api/supervisor/dashboard")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> getDashboard(@AuthenticationPrincipal AuthenticatedUser auth) {
        AppUser supervisor = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String site = supervisor.getAssignedSite() != null ? supervisor.getAssignedSite() : "";
        return computeSiteSummary(site);
    }

    @GetMapping("/api/supervisor/multi-site-dashboard")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public List<Map<String, Object>> getMultiSiteDashboard(@AuthenticationPrincipal AuthenticatedUser auth) {
        AppUser supervisor = userRepo.findById(auth.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String homeSite = supervisor.getHomeSite() != null ? supervisor.getHomeSite() : supervisor.getAssignedSite();

        List<String> sites = new ArrayList<>();
        if (homeSite != null) sites.add(homeSite);
        for (SupervisorSiteAccess grant : siteAccessRepo.findBySupervisorEmail(supervisor.getEmail())) {
            if (!grant.getSite().equals(homeSite)) sites.add(grant.getSite());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String site : sites) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("site", site);
            entry.put("isHome", site.equals(homeSite));
            entry.put("summary", computeSiteSummary(site));
            result.add(entry);
        }
        return result;
    }

    private Map<String, Object> computeSiteSummary(String site) {
        // Every hazard ever reported at the site, not just the open ones — a cleared hazard
        // never left this count, which is why "active hazards" on the dashboard never dropped
        // even after being cleared. Matches the same CLEARED-exclusion HazardController already
        // uses for its own active-hazards view.
        long hazardCount = hazardRepo.findBySiteOrderByCreatedAtDesc(site).stream()
            .filter(h -> !"CLEARED".equalsIgnoreCase(h.getStatus()))
            .count();
        long noticeCount = noticeRepo.countBySiteIgnoreCase(site);
        long workersOnSite = attendanceRepo.countBySiteIgnoreCaseAndStatus(site, "ON_SITE");
        long pendingShiftLogs = shiftLogRepo.countBySiteIgnoreCaseAndStatus(site, "PENDING");
        long unreadMessages = messageRepo.countBySiteIgnoreCaseAndReadAtIsNull(site);
        LocalDate today = LocalDate.now();
        long certExpired = certRepo.countExpiredBySite(site, today);
        long certExpiringSoon = certRepo.countExpiringSoonBySite(site, today, today.plusDays(30));

        int safetyScore = safetyScoreService.computeScore(site);
        int safetyRecommendationCount = safetyIntelligenceService.getRecommendations(site).size();
        long pendingBuyerVerifications = userRepo.findByRoleAndPending("buyer", true).size();

        List<Long> listingIds = listingRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(site)
                .stream().map(l -> (Long) l.getId()).collect(Collectors.toList());
        long pendingMarketplaceOffers = listingIds.isEmpty() ? 0
                : offerRepo.countByListingIdInAndStatus(listingIds, "PENDING");

        List<Long> txIds = txRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(site)
                .stream().map(t -> (Long) t.getId()).collect(Collectors.toList());
        long openDisputes = txIds.isEmpty() ? 0
                : disputeRepo.countByTransactionIdInAndStatus(txIds, "OPEN");

        List<Map<String, Object>> announcements = announcementRepo
            .findBySiteIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(site, LocalDateTime.now().minusHours(24))
            .stream().limit(5).map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.getId());
                m.put("content", a.getContent());
                m.put("createdByName", a.getCreatedByName());
                m.put("createdAt", a.getCreatedAt());
                return m;
            }).collect(Collectors.toList());

        List<Map<String, Object>> activeLoneWorkers = loneWorkerRepo
            .findBySiteIgnoreCaseAndActive(site, true).stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("workerName", s.getWorkerName());
                m.put("intervalMinutes", s.getIntervalMinutes());
                m.put("lastCheckedInAt", s.getLastCheckedInAt());
                m.put("deadline", s.getDeadline());
                m.put("alerted", s.isAlerted());
                return m;
            }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hazardCount", hazardCount);
        result.put("noticeCount", noticeCount);
        result.put("workersOnSite", workersOnSite);
        result.put("pendingShiftLogs", pendingShiftLogs);
        result.put("unreadMessages", unreadMessages);
        result.put("certExpired", certExpired);
        result.put("certExpiringSoon", certExpiringSoon);
        result.put("announcements", announcements);
        result.put("activeLoneWorkers", activeLoneWorkers);
        result.put("safetyScore", safetyScore);
        result.put("safetyRecommendationCount", safetyRecommendationCount);
        result.put("pendingBuyerVerifications", pendingBuyerVerifications);
        result.put("pendingMarketplaceOffers", pendingMarketplaceOffers);
        result.put("openDisputes", openDisputes);
        return result;
    }
}
