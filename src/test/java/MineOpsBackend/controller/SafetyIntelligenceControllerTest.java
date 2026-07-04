package MineOpsBackend.controller;

import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.SafetyIntelligenceService;
import MineOpsBackend.service.SafetyIntelligenceService.HotspotResult;
import MineOpsBackend.service.SafetyIntelligenceService.SafetyIntelligenceSummary;
import MineOpsBackend.service.SafetyIntelligenceService.TrendingHazardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyIntelligenceControllerTest {

    @Mock SafetyIntelligenceService service;

    @InjectMocks SafetyIntelligenceController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE, null);
    }

    private AuthenticatedUser safetyOfficer() {
        return new AuthenticatedUser(2L, "Ama", "ama@mine.com", "safetyOfficer", SITE, null);
    }

    // ── site-scoping ──────────────────────────────────────────────

    @Test
    void getHotspots_passesAssignedSiteToService() {
        HotspotResult r = new HotspotResult("Shaft A", 2, 1, 3, "High", null);
        when(service.getHotspots(SITE)).thenReturn(List.of(r));

        List<HotspotResult> result = controller.getHotspots(supervisor());

        verify(service).getHotspots(SITE);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).location()).isEqualTo("Shaft A");
    }

    @Test
    void getTrends_passesAssignedSiteToService() {
        TrendingHazardType t = new TrendingHazardType("Gas Leak", 4, 0, "NEW");
        when(service.getTrendingHazardTypes(SITE)).thenReturn(List.of(t));

        List<TrendingHazardType> result = controller.getTrends(supervisor());

        verify(service).getTrendingHazardTypes(SITE);
        assertThat(result.get(0).hazardType()).isEqualTo("Gas Leak");
    }

    @Test
    void getRecommendations_passesAssignedSiteToService() {
        when(service.getRecommendations(SITE)).thenReturn(List.of("3 reports at Shaft A — recommend inspection."));

        List<String> result = controller.getRecommendations(supervisor());

        verify(service).getRecommendations(SITE);
        assertThat(result).hasSize(1);
    }

    @Test
    void getSummary_passesAssignedSiteToService() {
        SafetyIntelligenceSummary summary = new SafetyIntelligenceSummary(List.of(), List.of(), List.of());
        when(service.getSummary(SITE)).thenReturn(summary);

        SafetyIntelligenceSummary result = controller.getSummary(supervisor());

        verify(service).getSummary(SITE);
        assertThat(result).isSameAs(summary);
    }

    // ── safety officer gets same site-scoped results ───────────────

    @Test
    void getHotspots_worksForSafetyOfficer() {
        when(service.getHotspots(SITE)).thenReturn(List.of());

        controller.getHotspots(safetyOfficer());

        verify(service).getHotspots(SITE);
    }

    @Test
    void getSummary_worksForSafetyOfficer() {
        SafetyIntelligenceSummary summary = new SafetyIntelligenceSummary(List.of(), List.of(), List.of());
        when(service.getSummary(SITE)).thenReturn(summary);

        controller.getSummary(safetyOfficer());

        verify(service).getSummary(SITE);
    }

    // ── empty results pass through cleanly ───────────────────────

    @Test
    void getHotspots_returnsEmptyList_whenNoHotspots() {
        when(service.getHotspots(SITE)).thenReturn(List.of());

        assertThat(controller.getHotspots(supervisor())).isEmpty();
    }

    @Test
    void getRecommendations_returnsEmptyList_whenNone() {
        when(service.getRecommendations(SITE)).thenReturn(List.of());

        assertThat(controller.getRecommendations(supervisor())).isEmpty();
    }
}
