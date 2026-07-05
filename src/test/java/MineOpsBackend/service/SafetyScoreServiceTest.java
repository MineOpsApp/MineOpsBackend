package MineOpsBackend.service;

import MineOpsBackend.model.HazardReport;
import MineOpsBackend.model.IncidentReport;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.repository.IncidentReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyScoreServiceTest {

    @Mock HazardReportRepository hazardRepo;
    @Mock IncidentReportRepository incidentRepo;

    @InjectMocks SafetyScoreService service;

    private static final String SITE = "Obuasi Mine";

    private HazardReport openHazard() {
        HazardReport h = new HazardReport("worker", "Kofi", "kofi@mine.com",
                "Fall Risk", SITE, "Shaft A", "desc", "Medium");
        h.setStatus("OPEN");
        return h;
    }

    private IncidentReport openIncident() {
        return new IncidentReport(
                "kofi@mine.com", "Kofi", "worker",
                SITE, "Shaft A", "Injury", "Minor", "desc",
                null, false, false, null, null, null, null, LocalDateTime.now()
        );
    }

    @Test
    void computeScore_returnsHundred_whenNoActiveHazardsOrIncidents() {
        when(hazardRepo.findBySiteAndStatusNotAndCreatedAtAfter(eq(SITE), eq("CLEARED"), any()))
                .thenReturn(List.of());
        when(incidentRepo.findBySiteAndStatusInAndReportedAtAfter(eq(SITE), any(), any()))
                .thenReturn(List.of());

        assertThat(service.computeScore(SITE)).isEqualTo(100);
    }

    @Test
    void computeScore_deductsFivePerHazard() {
        when(hazardRepo.findBySiteAndStatusNotAndCreatedAtAfter(eq(SITE), eq("CLEARED"), any()))
                .thenReturn(List.of(openHazard(), openHazard()));
        when(incidentRepo.findBySiteAndStatusInAndReportedAtAfter(eq(SITE), any(), any()))
                .thenReturn(List.of());

        assertThat(service.computeScore(SITE)).isEqualTo(90); // 100 - 5*2
    }

    @Test
    void computeScore_deductsTenPerIncident() {
        when(hazardRepo.findBySiteAndStatusNotAndCreatedAtAfter(eq(SITE), eq("CLEARED"), any()))
                .thenReturn(List.of());
        when(incidentRepo.findBySiteAndStatusInAndReportedAtAfter(eq(SITE), any(), any()))
                .thenReturn(List.of(openIncident(), openIncident(), openIncident()));

        assertThat(service.computeScore(SITE)).isEqualTo(70); // 100 - 10*3
    }

    @Test
    void computeScore_combinesHazardsAndIncidents() {
        when(hazardRepo.findBySiteAndStatusNotAndCreatedAtAfter(eq(SITE), eq("CLEARED"), any()))
                .thenReturn(List.of(openHazard(), openHazard(), openHazard())); // -15
        when(incidentRepo.findBySiteAndStatusInAndReportedAtAfter(eq(SITE), any(), any()))
                .thenReturn(List.of(openIncident())); // -10

        assertThat(service.computeScore(SITE)).isEqualTo(75); // 100 - 15 - 10
    }

    @Test
    void computeScore_floorsAtZero_whenPenaltiesExceedHundred() {
        when(hazardRepo.findBySiteAndStatusNotAndCreatedAtAfter(eq(SITE), eq("CLEARED"), any()))
                .thenReturn(List.of(openHazard(), openHazard(), openHazard(), openHazard(),
                        openHazard(), openHazard(), openHazard(), openHazard(),
                        openHazard(), openHazard(), openHazard())); // -55
        when(incidentRepo.findBySiteAndStatusInAndReportedAtAfter(eq(SITE), any(), any()))
                .thenReturn(List.of(openIncident(), openIncident(), openIncident(),
                        openIncident(), openIncident(), openIncident())); // -60

        assertThat(service.computeScore(SITE)).isEqualTo(0);
    }

    @Test
    void computeScore_queriesRepoWithCorrectStatusAndSite() {
        when(hazardRepo.findBySiteAndStatusNotAndCreatedAtAfter(eq(SITE), eq("CLEARED"), any()))
                .thenReturn(List.of());
        when(incidentRepo.findBySiteAndStatusInAndReportedAtAfter(eq(SITE), eq(List.of("Open", "Under Investigation")), any()))
                .thenReturn(List.of());

        service.computeScore(SITE);

        verify(hazardRepo).findBySiteAndStatusNotAndCreatedAtAfter(eq(SITE), eq("CLEARED"), any(LocalDateTime.class));
        verify(incidentRepo).findBySiteAndStatusInAndReportedAtAfter(
                eq(SITE), eq(List.of("Open", "Under Investigation")), any(LocalDateTime.class));
    }
}
