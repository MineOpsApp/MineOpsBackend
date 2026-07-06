package MineOpsBackend.controller;

import MineOpsBackend.model.BulkPurchaseRequest;
import MineOpsBackend.repository.BulkPurchaseRequestRepository;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkPurchaseRequestControllerTest {

    @Mock BulkPurchaseRequestRepository repo;
    @InjectMocks BulkPurchaseRequestController controller;

    private static final String SITE = "Obuasi Mine";

    // ── Security annotation ───────────────────────────────────────────────────────

    @Test
    void class_requiresSupervisorAuthority() {
        PreAuthorize ann = BulkPurchaseRequestController.class.getAnnotation(PreAuthorize.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).contains("ROLE_SUPERVISOR");
    }

    // ── getMySiteRequests ─────────────────────────────────────────────────────────

    @Test
    void getMySiteRequests_queriesByCallerSiteOnly() {
        when(repo.findBySiteIgnoreCaseOrderByCreatedAtDesc(SITE)).thenReturn(List.of());

        controller.getMySiteRequests(supervisorAt(SITE));

        verify(repo).findBySiteIgnoreCaseOrderByCreatedAtDesc(SITE);
        verify(repo, never()).findBySiteIgnoreCaseOrderByCreatedAtDesc("Other Site");
    }

    // ── flagForBulkPurchase — 400 validation ──────────────────────────────────────

    @Test
    void flagForBulkPurchase_throws400_whenMineralTypeMissing() {
        Map<String, Object> body = new HashMap<>();
        body.put("unit", "kg");
        body.put("quantityAvailable", "100");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.flagForBulkPurchase(supervisorAt(SITE), body));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void flagForBulkPurchase_throws400_whenUnitMissing() {
        Map<String, Object> body = new HashMap<>();
        body.put("mineralType", "Gold");
        body.put("quantityAvailable", "100");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.flagForBulkPurchase(supervisorAt(SITE), body));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void flagForBulkPurchase_throws400_whenQuantityMissing() {
        Map<String, Object> body = new HashMap<>();
        body.put("mineralType", "Gold");
        body.put("unit", "kg");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.flagForBulkPurchase(supervisorAt(SITE), body));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void flagForBulkPurchase_throws400_whenQuantityNotNumeric() {
        Map<String, Object> body = Map.of(
            "mineralType", "Gold", "unit", "kg", "quantityAvailable", "not-a-number");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.flagForBulkPurchase(supervisorAt(SITE), body));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void flagForBulkPurchase_setSiteAndEmailFromPrincipal() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Map<String, Object> body = Map.of(
            "mineralType", "Gold", "unit", "kg", "quantityAvailable", "150.5");

        controller.flagForBulkPurchase(supervisorAt(SITE), body);

        ArgumentCaptor<BulkPurchaseRequest> cap = ArgumentCaptor.forClass(BulkPurchaseRequest.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getSite()).isEqualTo(SITE);
        assertThat(cap.getValue().getFlaggedByEmail()).isEqualTo("kwame@mine.com");
        assertThat(cap.getValue().getMineralType()).isEqualTo("Gold");
        assertThat(cap.getValue().getQuantityAvailable()).isEqualByComparingTo("150.5");
    }

    // ── withdrawRequest ───────────────────────────────────────────────────────────

    @Test
    void withdrawRequest_throws404_whenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.withdrawRequest(supervisorAt(SITE), 99L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void withdrawRequest_throws403_whenRequestBelongsToDifferentSite() {
        BulkPurchaseRequest req = availableRequest("Tarkwa Mine");
        when(repo.findById(1L)).thenReturn(Optional.of(req));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.withdrawRequest(supervisorAt("Obuasi Mine"), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).save(any());
    }

    @Test
    void withdrawRequest_setsStatusWithdrawn_forOwnSiteRequest() {
        BulkPurchaseRequest req = availableRequest(SITE);
        when(repo.findById(1L)).thenReturn(Optional.of(req));

        controller.withdrawRequest(supervisorAt(SITE), 1L);

        ArgumentCaptor<BulkPurchaseRequest> cap = ArgumentCaptor.forClass(BulkPurchaseRequest.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("WITHDRAWN");
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private AuthenticatedUser supervisorAt(String site) {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", site, null);
    }

    private BulkPurchaseRequest availableRequest(String site) {
        BulkPurchaseRequest r = new BulkPurchaseRequest();
        r.setSite(site);
        r.setMineralType("Gold");
        r.setQuantityAvailable(BigDecimal.valueOf(100));
        r.setUnit("kg");
        return r;
    }
}
