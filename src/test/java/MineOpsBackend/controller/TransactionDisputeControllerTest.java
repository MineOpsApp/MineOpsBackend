package MineOpsBackend.controller;

import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.model.TransactionDispute;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.repository.TransactionDisputeRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionDisputeControllerTest {

    @Mock MarketplaceTransactionRepository txRepo;
    @Mock TransactionDisputeRepository disputeRepo;
    @Mock AuditLogService auditLogService;

    @InjectMocks TransactionDisputeController controller;

    private static final String SITE = "Obuasi Mine";
    private static final String OTHER_SITE = "Bibiani Mine";
    private static final String BUYER_EMAIL = "ama@buyer.com";

    private AuthenticatedUser buyer() {
        return new AuthenticatedUser(10L, "Ama", BUYER_EMAIL, "buyer", null, null);
    }

    private AuthenticatedUser supervisor(String site) {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", site, null);
    }

    private MarketplaceTransaction tx(String site, String buyerEmail) {
        MarketplaceTransaction t = new MarketplaceTransaction();
        t.setSite(site);
        t.setBuyerEmail(buyerEmail);
        t.setBatchStatus("DELIVERED");
        t.setQuantity(BigDecimal.valueOf(100));
        t.setAgreedPrice(BigDecimal.valueOf(50000));
        return t;
    }

    private TransactionDispute openDispute(Long txId) {
        TransactionDispute d = new TransactionDispute();
        d.setTransactionId(txId);
        d.setStatus("OPEN");
        d.setRaisedByEmail(BUYER_EMAIL);
        return d;
    }

    private TransactionDispute resolvedDispute(Long txId) {
        TransactionDispute d = openDispute(txId);
        d.setStatus("RESOLVED");
        return d;
    }

    // ── raiseDispute ──────────────────────────────────────────────────────────

    @Test
    void raiseDispute_throws404_whenTransactionNotFound() {
        when(txRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.raiseDispute(99L, buyer(), Map.of("reason", "No delivery")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void raiseDispute_throws403_whenNotAParty() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        AuthenticatedUser outsider = new AuthenticatedUser(99L, "X", "x@other.com", "buyer", null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.raiseDispute(1L, outsider, Map.of("reason", "Issue")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(disputeRepo, never()).save(any());
    }

    @Test
    void raiseDispute_throws409_whenDisputeAlreadyExists() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        when(disputeRepo.existsByTransactionId(1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.raiseDispute(1L, buyer(), Map.of("reason", "Duplicate")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(disputeRepo, never()).save(any());
    }

    @Test
    void raiseDispute_throws400_whenReasonMissing() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        when(disputeRepo.existsByTransactionId(1L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.raiseDispute(1L, buyer(), Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(disputeRepo, never()).save(any());
    }

    @Test
    void raiseDispute_savesDispute_forBuyer() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        when(disputeRepo.existsByTransactionId(1L)).thenReturn(false);
        when(disputeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.raiseDispute(1L, buyer(), Map.of("reason", "Goods not received"));

        ArgumentCaptor<TransactionDispute> cap = ArgumentCaptor.forClass(TransactionDispute.class);
        verify(disputeRepo).save(cap.capture());
        assertThat(cap.getValue().getReason()).isEqualTo("Goods not received");
        assertThat(cap.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(cap.getValue().getRaisedByEmail()).isEqualTo(BUYER_EMAIL);
    }

    @Test
    void raiseDispute_savesDispute_forSiteSupervisor() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        when(disputeRepo.existsByTransactionId(1L)).thenReturn(false);
        when(disputeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.raiseDispute(1L, supervisor(SITE), Map.of("reason", "Payment issue"));

        verify(disputeRepo).save(any());
    }

    // ── resolveDispute — site-check ───────────────────────────────────────────

    @Test
    void resolveDispute_throws404_whenDisputeNotFound() {
        when(disputeRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolveDispute(99L, supervisor(SITE), Map.of("resolutionNotes", "N")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resolveDispute_throws403_whenSupervisorIsFromDifferentSite() {
        TransactionDispute dispute = openDispute(1L);
        when(disputeRepo.findById(5L)).thenReturn(Optional.of(dispute));
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolveDispute(5L, supervisor(OTHER_SITE),
                        Map.of("resolutionNotes", "Resolved")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(disputeRepo, never()).save(any());
    }

    @Test
    void resolveDispute_throws409_whenAlreadyResolved() {
        TransactionDispute dispute = resolvedDispute(1L);
        when(disputeRepo.findById(5L)).thenReturn(Optional.of(dispute));
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolveDispute(5L, supervisor(SITE),
                        Map.of("resolutionNotes", "Again")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void resolveDispute_throws400_whenNotesMissing() {
        when(disputeRepo.findById(5L)).thenReturn(Optional.of(openDispute(1L)));
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.resolveDispute(5L, supervisor(SITE), Map.of()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(disputeRepo, never()).save(any());
    }

    @Test
    void resolveDispute_resolvesAndSaves_whenSameSite() {
        TransactionDispute dispute = openDispute(1L);
        when(disputeRepo.findById(5L)).thenReturn(Optional.of(dispute));
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        when(disputeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDispute result = controller.resolveDispute(5L, supervisor(SITE),
                Map.of("resolutionNotes", "Replacement shipped"));

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolutionNotes()).isEqualTo("Replacement shipped");
        assertThat(result.getResolvedAt()).isNotNull();
        verify(disputeRepo).save(dispute);
    }

    // ── getDispute ────────────────────────────────────────────────────────────

    @Test
    void getDispute_throws403_whenNotAParty() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        AuthenticatedUser outsider = new AuthenticatedUser(99L, "X", "x@other.com", "buyer", null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getDispute(1L, outsider));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getDispute_throws404_whenNoDisputeExists() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx(SITE, BUYER_EMAIL)));
        when(disputeRepo.findByTransactionId(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getDispute(1L, buyer()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
