package MineOpsBackend.controller;

import MineOpsBackend.model.IllegalMineReport;
import MineOpsBackend.repository.IllegalMineReportRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IllegalMineReportControllerTest {

    @Mock IllegalMineReportRepository repo;
    @InjectMocks IllegalMineReportController controller;

    // ── Security annotation ───────────────────────────────────────────────────────
    //
    // worker / supervisor / safetyOfficer / buyer → allowed (ROLE_WORKER etc.)
    // guest / government                          → 403 (not in the hasAnyAuthority list)
    // Spring Security AOP enforces this at runtime; the test below confirms the
    // annotation is correctly declared on the method.

    @Test
    void submitReport_permitsWorkerSupervisorSafetyOfficerBuyer_excludesGuestAndGovernment()
            throws NoSuchMethodException {
        PreAuthorize ann = IllegalMineReportController.class
            .getMethod("submitReport", AuthenticatedUser.class, Map.class)
            .getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        String expr = ann.value();
        assertThat(expr).contains("ROLE_WORKER");
        assertThat(expr).contains("ROLE_SUPERVISOR");
        assertThat(expr).contains("ROLE_SAFETY_OFFICER");
        assertThat(expr).contains("ROLE_BUYER");
        assertThat(expr).doesNotContain("ROLE_GUEST");
        assertThat(expr).doesNotContain("ROLE_GOVERNMENT");
    }

    // ── submitReport — 400 validation ─────────────────────────────────────────────

    @Test
    void submitReport_throws400_whenLocationDescriptionNull() {
        Map<String, String> body = new HashMap<>();
        body.put("locationDescription", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.submitReport(worker(), body));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void submitReport_throws400_whenLocationDescriptionBlank() {
        Map<String, String> body = Map.of("locationDescription", "   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.submitReport(worker(), body));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void submitReport_throws400_whenLocationDescriptionAbsent() {
        // key not present at all — body.get() returns null
        Map<String, String> body = Map.of("details", "Some details");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.submitReport(supervisor(), body));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── submitReport — principal fields set server-side ───────────────────────────

    @Test
    void submitReport_setsReporterEmailFromPrincipal_notBody() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IllegalMineReport result = controller.submitReport(
            worker(), Map.of("locationDescription", "Near Nsuta Junction"));

        assertThat(result.getReporterEmail()).isEqualTo("kofi@mine.com");
    }

    @Test
    void submitReport_setsReporterRoleFromPrincipal() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IllegalMineReport result = controller.submitReport(
            supervisor(), Map.of("locationDescription", "Along the Ankobra River"));

        assertThat(result.getReporterRole()).isEqualTo("supervisor");
    }

    @Test
    void submitReport_setsReporterRoleFromPrincipal_forBuyer() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IllegalMineReport result = controller.submitReport(
            buyer(), Map.of("locationDescription", "Tarkwa area"));

        assertThat(result.getReporterRole()).isEqualTo("buyer");
        assertThat(result.getReporterEmail()).isEqualTo("ama@buyer.com");
    }

    // ── submitReport — default fields ─────────────────────────────────────────────

    @Test
    void submitReport_setsStatusSubmitted() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IllegalMineReport result = controller.submitReport(
            safetyOfficer(), Map.of("locationDescription", "Tarkwa area"));

        assertThat(result.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    void submitReport_trimsLocationDescription() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<IllegalMineReport> cap = ArgumentCaptor.forClass(IllegalMineReport.class);
        controller.submitReport(worker(), Map.of("locationDescription", "  Near Nsuta Junction  "));

        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getLocationDescription()).isEqualTo("Near Nsuta Junction");
    }

    @Test
    void submitReport_setsDetailsWhenProvided() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IllegalMineReport result = controller.submitReport(worker(), Map.of(
            "locationDescription", "Tarkwa area",
            "details", "Heavy equipment seen at night"));

        assertThat(result.getDetails()).isEqualTo("Heavy equipment seen at night");
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private AuthenticatedUser worker() {
        return new AuthenticatedUser(1L, "Kofi", "kofi@mine.com", "worker", "Obuasi Mine", null);
    }

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(2L, "Kwame", "kwame@mine.com", "supervisor", "Obuasi Mine", null);
    }

    private AuthenticatedUser safetyOfficer() {
        return new AuthenticatedUser(3L, "Esi", "esi@mine.com", "safetyOfficer", "Obuasi Mine", null);
    }

    private AuthenticatedUser buyer() {
        return new AuthenticatedUser(4L, "Ama", "ama@buyer.com", "buyer", null, null);
    }
}
