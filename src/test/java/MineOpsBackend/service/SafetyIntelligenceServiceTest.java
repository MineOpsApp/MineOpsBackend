package MineOpsBackend.service;

import MineOpsBackend.model.HazardReport;
import MineOpsBackend.model.IncidentReport;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.repository.IncidentReportRepository;
import MineOpsBackend.service.SafetyIntelligenceService.HotspotResult;
import MineOpsBackend.service.SafetyIntelligenceService.SafetyIntelligenceSummary;
import MineOpsBackend.service.SafetyIntelligenceService.TrendingHazardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyIntelligenceServiceTest {

    @Mock HazardReportRepository hazardRepo;
    @Mock IncidentReportRepository incidentRepo;

    @InjectMocks SafetyIntelligenceService service;

    private static final String SITE = "Obuasi Mine";
    private static final LocalDateTime NOW = LocalDateTime.now();

    private HazardReport hazard(String location, String status, String severity, LocalDateTime at) {
        HazardReport h = new HazardReport("worker", "Kofi", "kofi@mine.com",
            "Fall Risk", SITE, location, "desc", severity);
        h.setStatus(status);
        h.setCreatedAt(at);
        return h;
    }

    private HazardReport hazardTyped(String location, String hazardType, String status, LocalDateTime at) {
        HazardReport h = new HazardReport("worker", "Kofi", "kofi@mine.com",
            hazardType, SITE, location, "desc", "Medium");
        h.setStatus(status);
        h.setCreatedAt(at);
        return h;
    }

    private IncidentReport incident(String zone, String status, LocalDateTime at) {
        IncidentReport i = new IncidentReport(
            "kofi@mine.com", "Kofi", "worker",
            SITE, zone, "Injury", "Minor", "desc",
            null, false, false, null, null, null, null, at
        );
        i.setStatus(status);
        i.setReportedAt(at);
        return i;
    }

    // ── getHotspots — threshold ────────────────────────────────────

    @Test
    void getHotspots_excludesLocationsBelow3Reports() {
        // 2 hazards at same location — below threshold
        LocalDateTime recent = NOW.minusDays(5);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazard("Shaft A", "OPEN", "Medium", recent),
            hazard("Shaft A", "OPEN", "High", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        assertThat(service.getHotspots(SITE)).isEmpty();
    }

    @Test
    void getHotspots_includesLocationAtExactlyThreshold() {
        LocalDateTime recent = NOW.minusDays(5);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazard("Shaft A", "OPEN", "High", recent),
            hazard("Shaft A", "REVIEWED", "Medium", recent),
            hazard("Shaft A", "OPEN", "Low", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        List<HotspotResult> result = service.getHotspots(SITE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).location()).isEqualTo("Shaft A");
        assertThat(result.get(0).hazardCount()).isEqualTo(3);
        assertThat(result.get(0).totalCount()).isEqualTo(3);
    }

    @Test
    void getHotspots_sortsByTotalCountDescending() {
        LocalDateTime recent = NOW.minusDays(3);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazard("Zone B", "OPEN", "Medium", recent),
            hazard("Zone B", "OPEN", "Medium", recent),
            hazard("Zone B", "OPEN", "Medium", recent),
            hazard("Zone A", "OPEN", "High", recent),
            hazard("Zone A", "OPEN", "High", recent),
            hazard("Zone A", "OPEN", "High", recent),
            hazard("Zone A", "OPEN", "High", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        List<HotspotResult> result = service.getHotspots(SITE);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).location()).isEqualTo("Zone A");
        assertThat(result.get(0).totalCount()).isEqualTo(4);
        assertThat(result.get(1).totalCount()).isEqualTo(3);
    }

    // ── getHotspots — CLEARED exclusion ───────────────────────────

    @Test
    void getHotspots_excludesClearedHazards() {
        LocalDateTime recent = NOW.minusDays(2);
        // 2 open + 1 CLEARED — CLEARED must not count toward threshold
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazard("Shaft B", "OPEN", "High", recent),
            hazard("Shaft B", "OPEN", "Medium", recent),
            hazard("Shaft B", "CLEARED", "Low", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        assertThat(service.getHotspots(SITE)).isEmpty();
    }

    @Test
    void getHotspots_excludesClosedIncidents() {
        LocalDateTime recent = NOW.minusDays(2);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of());
        // 2 active + 1 Closed incident
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of(
            incident("Pit 1", "Open", recent),
            incident("Pit 1", "Under Investigation", recent),
            incident("Pit 1", "Closed", recent)
        ));

        assertThat(service.getHotspots(SITE)).isEmpty();
    }

    // ── getHotspots — 30-day window ───────────────────────────────

    @Test
    void getHotspots_excludesReportsOlderThan30Days() {
        LocalDateTime old = NOW.minusDays(31);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazard("Old Zone", "OPEN", "High", old),
            hazard("Old Zone", "OPEN", "High", old),
            hazard("Old Zone", "OPEN", "High", old)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        assertThat(service.getHotspots(SITE)).isEmpty();
    }

    // ── getHotspots — hazard + incident merging ───────────────────

    @Test
    void getHotspots_combinesHazardsAndIncidentsAtSameLocation() {
        LocalDateTime recent = NOW.minusDays(1);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazard("Shaft A", "OPEN", "High", recent),
            hazard("Shaft A", "OPEN", "Medium", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of(
            incident("Shaft A", "Open", recent)
        ));

        List<HotspotResult> result = service.getHotspots(SITE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).hazardCount()).isEqualTo(2);
        assertThat(result.get(0).incidentCount()).isEqualTo(1);
        assertThat(result.get(0).totalCount()).isEqualTo(3);
    }

    // ── getHotspots — case-insensitive location merging ───────────

    @Test
    void getHotspots_normalizesLocationCaseForGrouping() {
        LocalDateTime recent = NOW.minusDays(1);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazard("shaft a", "OPEN", "High", recent),
            hazard("SHAFT A", "OPEN", "Medium", recent),
            hazard("Shaft A", "OPEN", "Low", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        List<HotspotResult> result = service.getHotspots(SITE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalCount()).isEqualTo(3);
    }

    // ── getTrendingHazardTypes — NEW ──────────────────────────────

    @Test
    void getTrending_marksNewType_whenNoPriorOccurrences() {
        LocalDateTime recent = NOW.minusDays(5);
        // 3 "Gas Leak" hazards this period, none in prior period
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazardTyped("Shaft", "Gas Leak", "OPEN", recent),
            hazardTyped("Shaft", "Gas Leak", "OPEN", recent),
            hazardTyped("Shaft", "Gas Leak", "OPEN", recent)
        ));

        List<TrendingHazardType> result = service.getTrendingHazardTypes(SITE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).hazardType()).isEqualTo("Gas Leak");
        assertThat(result.get(0).currentCount()).isEqualTo(3);
        assertThat(result.get(0).priorCount()).isEqualTo(0);
        assertThat(result.get(0).trend()).isEqualTo("NEW");
    }

    // ── getTrendingHazardTypes — RISING ───────────────────────────

    @Test
    void getTrending_marksRising_whenCurrentDoublesPrior() {
        LocalDateTime recent = NOW.minusDays(5);
        LocalDateTime prior = NOW.minusDays(35);
        // 2 in prior period, 4 in current period (doubled)
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazardTyped("Shaft", "Fall Risk", "OPEN", recent),
            hazardTyped("Shaft", "Fall Risk", "OPEN", recent),
            hazardTyped("Shaft", "Fall Risk", "OPEN", recent),
            hazardTyped("Shaft", "Fall Risk", "OPEN", recent),
            hazardTyped("Shaft", "Fall Risk", "OPEN", prior),
            hazardTyped("Shaft", "Fall Risk", "OPEN", prior)
        ));

        List<TrendingHazardType> result = service.getTrendingHazardTypes(SITE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).trend()).isEqualTo("RISING");
        assertThat(result.get(0).currentCount()).isEqualTo(4);
        assertThat(result.get(0).priorCount()).isEqualTo(2);
    }

    @Test
    void getTrending_excludesType_whenBelowMinCount() {
        LocalDateTime recent = NOW.minusDays(5);
        // Only 2 this period — below TREND_MIN_COUNT of 3
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazardTyped("Shaft", "Gas Leak", "OPEN", recent),
            hazardTyped("Shaft", "Gas Leak", "OPEN", recent)
        ));

        assertThat(service.getTrendingHazardTypes(SITE)).isEmpty();
    }

    @Test
    void getTrending_excludesType_whenNotDoubled() {
        LocalDateTime recent = NOW.minusDays(5);
        LocalDateTime prior = NOW.minusDays(35);
        // 3 current, 2 prior — not doubled (3 < 2*2=4)
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazardTyped("Shaft", "Fall Risk", "OPEN", recent),
            hazardTyped("Shaft", "Fall Risk", "OPEN", recent),
            hazardTyped("Shaft", "Fall Risk", "OPEN", recent),
            hazardTyped("Shaft", "Fall Risk", "OPEN", prior),
            hazardTyped("Shaft", "Fall Risk", "OPEN", prior)
        ));

        assertThat(service.getTrendingHazardTypes(SITE)).isEmpty();
    }

    // ── getRecommendations ────────────────────────────────────────

    @Test
    void getRecommendations_includesHotspotAndTrendStrings() {
        LocalDateTime recent = NOW.minusDays(3);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazardTyped("Shaft A", "Gas Leak", "OPEN", recent),
            hazardTyped("Shaft A", "Gas Leak", "OPEN", recent),
            hazardTyped("Shaft A", "Gas Leak", "OPEN", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        List<String> recs = service.getRecommendations(SITE);

        assertThat(recs).anyMatch(r -> r.contains("Shaft A") && r.contains("supervisor inspection"));
        assertThat(recs).anyMatch(r -> r.contains("Gas Leak") && r.contains("toolbox talk"));
    }

    @Test
    void getRecommendations_returnsEmpty_whenNoHotspotsOrTrends() {
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of());
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        assertThat(service.getRecommendations(SITE)).isEmpty();
    }

    // ── getSummary ────────────────────────────────────────────────

    @Test
    void getSummary_combinesAllThreeResults() {
        LocalDateTime recent = NOW.minusDays(2);
        when(hazardRepo.findBySiteOrderByCreatedAtDesc(SITE)).thenReturn(List.of(
            hazardTyped("Pit 1", "Electrical", "OPEN", recent),
            hazardTyped("Pit 1", "Electrical", "OPEN", recent),
            hazardTyped("Pit 1", "Electrical", "OPEN", recent)
        ));
        when(incidentRepo.findBySiteOrderByReportedAtDesc(SITE)).thenReturn(List.of());

        SafetyIntelligenceSummary summary = service.getSummary(SITE);

        assertThat(summary.hotspots()).hasSize(1);
        assertThat(summary.trends()).hasSize(1);
        assertThat(summary.recommendations()).hasSize(2); // one hotspot rec + one trend rec
    }
}
