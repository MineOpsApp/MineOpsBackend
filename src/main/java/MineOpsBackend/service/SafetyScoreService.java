package MineOpsBackend.service;

import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.repository.IncidentReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SafetyScoreService {

    private static final int WINDOW_DAYS = 90;
    private static final int HAZARD_PENALTY = 5;
    private static final int INCIDENT_PENALTY = 10;

    private final HazardReportRepository hazardRepo;
    private final IncidentReportRepository incidentRepo;

    public SafetyScoreService(HazardReportRepository hazardRepo, IncidentReportRepository incidentRepo) {
        this.hazardRepo = hazardRepo;
        this.incidentRepo = incidentRepo;
    }

    public int computeScore(String site) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(WINDOW_DAYS);

        int activeHazards = hazardRepo
                .findBySiteAndStatusNotAndCreatedAtAfter(site, "CLEARED", cutoff)
                .size();

        int activeIncidents = incidentRepo
                .findBySiteAndStatusInAndReportedAtAfter(site, List.of("Open", "Under Investigation"), cutoff)
                .size();

        int score = 100 - (HAZARD_PENALTY * activeHazards) - (INCIDENT_PENALTY * activeIncidents);
        return Math.max(0, score);
    }
}
