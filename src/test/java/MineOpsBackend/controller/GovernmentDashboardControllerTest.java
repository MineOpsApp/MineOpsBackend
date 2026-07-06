package MineOpsBackend.controller;

import MineOpsBackend.model.BulkPurchaseRequest;
import MineOpsBackend.model.IllegalMineReport;
import MineOpsBackend.repository.BulkPurchaseRequestRepository;
import MineOpsBackend.repository.IllegalMineReportRepository;
import MineOpsBackend.repository.MineralInventoryRepository;
import MineOpsBackend.repository.MiningPermitStatusRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernmentDashboardControllerTest {

    @Mock MineralInventoryRepository inventoryRepo;
    @Mock BulkPurchaseRequestRepository bulkPurchaseRepo;
    @Mock IllegalMineReportRepository illegalMineReportRepo;
    @Mock MiningPermitStatusRepository permitRepo;

    @InjectMocks GovernmentDashboardController controller;

    // ── Security annotation ───────────────────────────────────────────────────────
    //
    // @PreAuthorize("hasAuthority('ROLE_GOVERNMENT')") is declared at class level,
    // so it gates every endpoint: worker, supervisor, buyer, and guest callers all
    // receive HTTP 403 (enforced by Spring Security AOP at runtime).
    // The test below confirms the annotation is correctly declared on the class.

    @Test
    void class_requiresGovernmentAuthority() {
        PreAuthorize ann = GovernmentDashboardController.class.getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ROLE_GOVERNMENT");
    }

    // ── fulfillRequest ────────────────────────────────────────────────────────────

    @Test
    void fulfillRequest_throws404_whenRequestNotFound() {
        when(bulkPurchaseRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.fulfillRequest(government(), 99L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void fulfillRequest_throws409_whenAlreadyFulfilled() {
        when(bulkPurchaseRepo.findById(1L)).thenReturn(Optional.of(bulkRequest("FULFILLED")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.fulfillRequest(government(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void fulfillRequest_throws409_whenWithdrawn() {
        when(bulkPurchaseRepo.findById(1L)).thenReturn(Optional.of(bulkRequest("WITHDRAWN")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.fulfillRequest(government(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void fulfillRequest_setsStatusFulfilled_whenAvailable() {
        BulkPurchaseRequest req = bulkRequest("AVAILABLE");
        when(bulkPurchaseRepo.findById(1L)).thenReturn(Optional.of(req));
        when(bulkPurchaseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BulkPurchaseRequest result = controller.fulfillRequest(government(), 1L);

        assertThat(result.getStatus()).isEqualTo("FULFILLED");
        verify(bulkPurchaseRepo).save(req);
    }

    // ── reviewReport ──────────────────────────────────────────────────────────────

    @Test
    void reviewReport_throws404_whenReportNotFound() {
        when(illegalMineReportRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.reviewReport(government(), 99L, Map.of("status", "UNDER_REVIEW")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reviewReport_setsReviewedByEmailFromPrincipal_notBody() {
        IllegalMineReport report = new IllegalMineReport();
        report.setStatus("SUBMITTED");
        when(illegalMineReportRepo.findById(1L)).thenReturn(Optional.of(report));
        when(illegalMineReportRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Body has no reviewedByEmail key — the principal's email must be used regardless
        IllegalMineReport result = controller.reviewReport(
            government(), 1L, Map.of("status", "UNDER_REVIEW", "reviewNotes", "Investigating"));

        assertThat(result.getReviewedByEmail()).isEqualTo("inspector@goldbod.gov");
    }

    @Test
    void reviewReport_updatesStatusAndNotes() {
        IllegalMineReport report = new IllegalMineReport();
        report.setStatus("SUBMITTED");
        when(illegalMineReportRepo.findById(1L)).thenReturn(Optional.of(report));
        when(illegalMineReportRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IllegalMineReport result = controller.reviewReport(
            government(), 1L, Map.of("status", "CONFIRMED", "reviewNotes", "Verified on-site"));

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getReviewNotes()).isEqualTo("Verified on-site");
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private AuthenticatedUser government() {
        return new AuthenticatedUser(1L, "Inspector", "inspector@goldbod.gov", "government", null, null);
    }

    private BulkPurchaseRequest bulkRequest(String status) {
        BulkPurchaseRequest r = new BulkPurchaseRequest();
        r.setSite("Obuasi Mine");
        r.setMineralType("Gold");
        r.setStatus(status);
        return r;
    }
}
