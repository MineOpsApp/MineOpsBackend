package MineOpsBackend.controller;

import MineOpsBackend.dto.UpdateZonePositionRequest;
import MineOpsBackend.dto.UpdateZonePositionRequest.MapPoint;
import MineOpsBackend.model.BlastSchedule;
import MineOpsBackend.model.DangerZone;
import MineOpsBackend.model.HazardReport;
import MineOpsBackend.repository.BlastScheduleRepository;
import MineOpsBackend.repository.DangerZoneRepository;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DangerZoneControllerTest {

    @Mock DangerZoneRepository zoneRepo;
    @Mock HazardReportRepository hazardRepo;
    @Mock BlastScheduleRepository blastRepo;
    @Mock AuditLogService auditLogService;
    @Spy  ObjectMapper objectMapper;

    @InjectMocks DangerZoneController controller;

    private static final String SITE = "TestMine";

    private AuthenticatedUser user(String site) {
        return new AuthenticatedUser(1L, "Safety Officer", "so@mine.com", "safety_officer", site, null);
    }

    private DangerZone zone(Long id, String site) {
        DangerZone z = new DangerZone(site, "Shaft A - Blasting Zone", "High");
        try {
            Field f = DangerZone.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(z, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        return z;
    }

    // ── updateZonePosition ────────────────────────────────────────────────────

    @Test
    void updateZonePosition_serializes_polygonPointsAsJson() throws Exception {
        DangerZone z = zone(1L, SITE);
        when(zoneRepo.findById(1L)).thenReturn(Optional.of(z));
        when(zoneRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateZonePositionRequest(List.of(
            new MapPoint(10.0, 20.0),
            new MapPoint(50.0, 20.0),
            new MapPoint(30.0, 60.0)
        ));

        DangerZone saved = controller.updateZonePosition(1L, user(SITE), request);

        assertThat(saved.getPolygonPoints()).isNotNull();
        // Deserialize and verify round-trip
        var parsed = objectMapper.readValue(saved.getPolygonPoints(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        assertThat(parsed).hasSize(3);
        assertThat(((Map<?, ?>) parsed.get(0)).get("x")).isEqualTo(10.0);
        assertThat(((Map<?, ?>) parsed.get(2)).get("y")).isEqualTo(60.0);
    }

    @Test
    void updateZonePosition_throws403_whenZoneBelongsToDifferentSite() {
        DangerZone z = zone(2L, "OtherMine");
        when(zoneRepo.findById(2L)).thenReturn(Optional.of(z));

        var request = new UpdateZonePositionRequest(List.of(
            new MapPoint(0.0, 0.0), new MapPoint(10.0, 0.0), new MapPoint(5.0, 10.0)
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.updateZonePosition(2L, user(SITE), request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateZonePosition_throws404_whenZoneNotFound() {
        when(zoneRepo.findById(99L)).thenReturn(Optional.empty());

        var request = new UpdateZonePositionRequest(List.of(
            new MapPoint(0.0, 0.0), new MapPoint(10.0, 0.0), new MapPoint(5.0, 10.0)
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.updateZonePosition(99L, user(SITE), request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── getZoneDetail ─────────────────────────────────────────────────────────

    @Test
    void getZoneDetail_returnsOnlyHazardsWhoseLocationContainsZoneName() {
        DangerZone z = zone(1L, SITE);
        when(zoneRepo.findById(1L)).thenReturn(Optional.of(z));

        HazardReport match = hazardWithLocation("Near Shaft A - Blasting Zone entrance");
        HazardReport noMatch = hazardWithLocation("Ventilation tunnel, eastern side");
        when(hazardRepo.findBySiteAndStatusOrderByCreatedAtDesc(SITE, "OPEN"))
            .thenReturn(List.of(match, noMatch));
        when(blastRepo.findBySiteAndStatusOrderByBlastTimeAsc(SITE, "SCHEDULED"))
            .thenReturn(List.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = controller.getZoneDetail(1L, user(SITE));

        @SuppressWarnings("unchecked")
        List<HazardReport> hazards = (List<HazardReport>) result.get("openHazards");
        assertThat(hazards).containsExactly(match);
        assertThat(hazards).doesNotContain(noMatch);
    }

    @Test
    void getZoneDetail_returnsOnlyBlastsWhoseZoneMatchesExactly() {
        DangerZone z = zone(1L, SITE);
        when(zoneRepo.findById(1L)).thenReturn(Optional.of(z));
        when(hazardRepo.findBySiteAndStatusOrderByCreatedAtDesc(SITE, "OPEN"))
            .thenReturn(List.of());

        BlastSchedule match   = blastForZone("Shaft A - Blasting Zone");
        BlastSchedule noMatch = blastForZone("Surface Quarry South");
        when(blastRepo.findBySiteAndStatusOrderByBlastTimeAsc(SITE, "SCHEDULED"))
            .thenReturn(List.of(match, noMatch));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = controller.getZoneDetail(1L, user(SITE));

        @SuppressWarnings("unchecked")
        List<BlastSchedule> blasts = (List<BlastSchedule>) result.get("scheduledBlasts");
        assertThat(blasts).containsExactly(match);
        assertThat(blasts).doesNotContain(noMatch);
    }

    @Test
    void getZoneDetail_throws403_whenZoneBelongsToDifferentSite() {
        DangerZone z = zone(1L, "OtherMine");
        when(zoneRepo.findById(1L)).thenReturn(Optional.of(z));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getZoneDetail(1L, user(SITE)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HazardReport hazardWithLocation(String location) {
        HazardReport h = new HazardReport("worker", "Alice", "alice@mine.com",
            "Cave-in", SITE, location, "Rocks falling", "High");
        return h;
    }

    private BlastSchedule blastForZone(String zone) {
        return new BlastSchedule(SITE, zone, "super@mine.com", "Supervisor",
            "Standard blast", LocalDateTime.now().plusHours(2));
    }
}
