package MineOpsBackend.service;

import MineOpsBackend.model.HazardReport;
import MineOpsBackend.model.IncidentReport;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.repository.IncidentReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SafetyIntelligenceService {

    private static final int WINDOW_DAYS = 30;
    private static final int HOTSPOT_THRESHOLD = 3;
    private static final int TREND_MIN_COUNT = 3;

    private final HazardReportRepository hazardRepo;
    private final IncidentReportRepository incidentRepo;

    public SafetyIntelligenceService(
        HazardReportRepository hazardRepo,
        IncidentReportRepository incidentRepo
    ) {
        this.hazardRepo = hazardRepo;
        this.incidentRepo = incidentRepo;
    }

    public record HotspotResult(
        String location,
        int hazardCount,
        int incidentCount,
        int totalCount,
        String mostRecentSeverity,
        String mostRecentAt
    ) {}

    public record TrendingHazardType(
        String hazardType,
        int currentCount,
        int priorCount,
        String trend
    ) {}

    public record SafetyIntelligenceSummary(
        List<HotspotResult> hotspots,
        List<TrendingHazardType> trends,
        List<String> recommendations
    ) {}

    public List<HotspotResult> getHotspots(String site) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(WINDOW_DAYS);

        List<HazardReport> hazards = hazardRepo.findBySiteOrderByCreatedAtDesc(site).stream()
            .filter(h -> h.getCreatedAt() != null && h.getCreatedAt().isAfter(cutoff))
            .filter(h -> !"CLOSED".equalsIgnoreCase(h.getStatus()))
            .toList();

        List<IncidentReport> incidents = incidentRepo.findBySiteOrderByReportedAtDesc(site).stream()
            .filter(i -> i.getReportedAt() != null && i.getReportedAt().isAfter(cutoff))
            .filter(i -> !"Closed".equalsIgnoreCase(i.getStatus()))
            .toList();

        // [hazardCount, incidentCount] per normalized location key
        Map<String, int[]> counts = new LinkedHashMap<>();
        Map<String, String> displayLocation = new HashMap<>();
        Map<String, LocalDateTime> mostRecentAt = new HashMap<>();
        Map<String, String> mostRecentSeverity = new HashMap<>();

        for (HazardReport h : hazards) {
            String key = normalize(h.getLocation());
            if (key.isBlank()) continue;
            counts.computeIfAbsent(key, k -> new int[2])[0]++;
            displayLocation.putIfAbsent(key, h.getLocation().trim());
            LocalDateTime at = h.getCreatedAt();
            if (!mostRecentAt.containsKey(key) || at.isAfter(mostRecentAt.get(key))) {
                mostRecentAt.put(key, at);
                mostRecentSeverity.put(key, h.getSeverity());
            }
        }

        for (IncidentReport i : incidents) {
            String key = normalize(i.getZone());
            if (key.isBlank()) continue;
            counts.computeIfAbsent(key, k -> new int[2])[1]++;
            displayLocation.putIfAbsent(key, i.getZone().trim());
            LocalDateTime at = i.getReportedAt();
            if (!mostRecentAt.containsKey(key) || at.isAfter(mostRecentAt.get(key))) {
                mostRecentAt.put(key, at);
                mostRecentSeverity.put(key, i.getSeverity());
            }
        }

        return counts.entrySet().stream()
            .map(e -> {
                String key = e.getKey();
                int[] c = e.getValue();
                int total = c[0] + c[1];
                LocalDateTime lat = mostRecentAt.get(key);
                return new HotspotResult(
                    displayLocation.get(key),
                    c[0], c[1], total,
                    mostRecentSeverity.getOrDefault(key, "Unknown"),
                    lat != null ? lat.toString() : null
                );
            })
            .filter(r -> r.totalCount() >= HOTSPOT_THRESHOLD)
            .sorted(Comparator.comparingInt(HotspotResult::totalCount).reversed())
            .collect(Collectors.toList());
    }

    public List<TrendingHazardType> getTrendingHazardTypes(String site) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart = now.minusDays(WINDOW_DAYS);
        LocalDateTime priorStart = now.minusDays(WINDOW_DAYS * 2L);

        List<HazardReport> all60 = hazardRepo.findBySiteOrderByCreatedAtDesc(site).stream()
            .filter(h -> h.getCreatedAt() != null && h.getCreatedAt().isAfter(priorStart))
            .toList();

        Map<String, Long> currentCounts = all60.stream()
            .filter(h -> h.getCreatedAt().isAfter(currentStart))
            .collect(Collectors.groupingBy(
                h -> h.getHazardType() != null ? h.getHazardType() : "Unknown",
                Collectors.counting()
            ));

        Map<String, Long> priorCounts = all60.stream()
            .filter(h -> !h.getCreatedAt().isAfter(currentStart))
            .collect(Collectors.groupingBy(
                h -> h.getHazardType() != null ? h.getHazardType() : "Unknown",
                Collectors.counting()
            ));

        return currentCounts.entrySet().stream()
            .filter(e -> e.getValue() >= TREND_MIN_COUNT)
            .filter(e -> {
                long prior = priorCounts.getOrDefault(e.getKey(), 0L);
                return prior == 0 || e.getValue() >= prior * 2;
            })
            .map(e -> {
                long current = e.getValue();
                long prior = priorCounts.getOrDefault(e.getKey(), 0L);
                return new TrendingHazardType(
                    e.getKey(),
                    (int) current,
                    (int) prior,
                    prior == 0 ? "NEW" : "RISING"
                );
            })
            .sorted(Comparator.comparingInt(TrendingHazardType::currentCount).reversed())
            .collect(Collectors.toList());
    }

    public List<String> getRecommendations(String site) {
        List<String> recs = new ArrayList<>();
        for (HotspotResult h : getHotspots(site)) {
            recs.add(String.format(
                "%d reports at %s in the last 30 days — recommend a supervisor inspection.",
                h.totalCount(), h.location()
            ));
        }
        for (TrendingHazardType t : getTrendingHazardTypes(site)) {
            recs.add(String.format(
                "%s hazards are up %d vs %d in the prior period — consider a toolbox talk on this hazard type.",
                t.hazardType(), t.currentCount(), t.priorCount()
            ));
        }
        return recs;
    }

    public SafetyIntelligenceSummary getSummary(String site) {
        List<HotspotResult> hotspots = getHotspots(site);
        List<TrendingHazardType> trends = getTrendingHazardTypes(site);
        List<String> recommendations = new ArrayList<>();
        for (HotspotResult h : hotspots) {
            recommendations.add(String.format(
                "%d reports at %s in the last 30 days — recommend a supervisor inspection.",
                h.totalCount(), h.location()
            ));
        }
        for (TrendingHazardType t : trends) {
            recommendations.add(String.format(
                "%s hazards are up %d vs %d in the prior period — consider a toolbox talk on this hazard type.",
                t.hazardType(), t.currentCount(), t.priorCount()
            ));
        }
        return new SafetyIntelligenceSummary(hotspots, trends, recommendations);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
