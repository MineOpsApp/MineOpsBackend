package MineOpsBackend.controller;

import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
class MarketplaceTransactionControllerTest {

    @Mock MarketplaceTransactionRepository transactionRepo;
    @Mock AuditLogService auditLogService;

    @InjectMocks MarketplaceTransactionController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE, null);
    }

    private AuthenticatedUser buyer() {
        return new AuthenticatedUser(10L, "Ama", "ama@buyer.com", "buyer", null, null);
    }

    private MarketplaceTransaction txForSite(String site, String status) {
        MarketplaceTransaction tx = new MarketplaceTransaction();
        tx.setSite(site);
        tx.setBuyerEmail("ama@buyer.com");
        tx.setBuyerName("Ama");
        tx.setMineralType("Gold");
        tx.setQuantity(BigDecimal.valueOf(400));
        tx.setAgreedPrice(BigDecimal.valueOf(200000));
        tx.setBatchStatus(status);
        return tx;
    }

    // ── getMyTransactions ──────────────────────────────────────────

    @Test
    void getMyTransactions_returnsBuyerTransactions() {
        MarketplaceTransaction tx = txForSite(SITE, "PREPARING");
        when(transactionRepo.findByBuyerEmailOrderByCreatedAtDesc("ama@buyer.com"))
            .thenReturn(List.of(tx));

        List<MarketplaceTransaction> result = controller.getMyTransactions(buyer());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuyerEmail()).isEqualTo("ama@buyer.com");
    }

    // ── getSiteTransactions ────────────────────────────────────────

    @Test
    void getSiteTransactions_returnsSiteTransactions() {
        MarketplaceTransaction tx = txForSite(SITE, "DISPATCHED");
        when(transactionRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(SITE))
            .thenReturn(List.of(tx));

        List<MarketplaceTransaction> result = controller.getSiteTransactions(supervisor());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBatchStatus()).isEqualTo("DISPATCHED");
    }

    // ── updateStatus ───────────────────────────────────────────────

    @Test
    void updateStatus_throws404_whenTransactionNotFound() {
        when(transactionRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.updateStatus(supervisor(), 99L, Map.of("batchStatus", "DISPATCHED")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatus_throws403_whenTransactionBelongsToDifferentSite() {
        when(transactionRepo.findById(1L)).thenReturn(Optional.of(txForSite("Tarkwa Mine", "PREPARING")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.updateStatus(supervisor(), 1L, Map.of("batchStatus", "DISPATCHED")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(transactionRepo, never()).save(any());
    }

    @Test
    void updateStatus_throws400_whenStatusIsInvalid() {
        when(transactionRepo.findById(1L)).thenReturn(Optional.of(txForSite(SITE, "PREPARING")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.updateStatus(supervisor(), 1L, Map.of("batchStatus", "LOST")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(transactionRepo, never()).save(any());
    }

    @Test
    void updateStatus_updatesStatusAndAudits() {
        MarketplaceTransaction tx = txForSite(SITE, "PREPARING");
        when(transactionRepo.findById(1L)).thenReturn(Optional.of(tx));
        when(transactionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarketplaceTransaction result = controller.updateStatus(supervisor(), 1L,
            Map.of("batchStatus", "DISPATCHED"));

        assertThat(result.getBatchStatus()).isEqualTo("DISPATCHED");
        assertThat(result.getUpdatedBy()).isEqualTo("kwame@mine.com");

        verify(auditLogService).record(
            "TRANSACTION_STATUS_UPDATED", "supervisor", "Kwame", "kwame@mine.com",
            "MARKETPLACE_TRANSACTION", 1L, "status=DISPATCHED"
        );
    }

    @Test
    void updateStatus_accepts_allValidStatuses() {
        for (String status : List.of("PREPARING", "DISPATCHED", "IN_TRANSIT", "DELIVERED")) {
            MarketplaceTransaction tx = txForSite(SITE, "PREPARING");
            when(transactionRepo.findById(1L)).thenReturn(Optional.of(tx));
            when(transactionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MarketplaceTransaction result = controller.updateStatus(supervisor(), 1L,
                Map.of("batchStatus", status));

            assertThat(result.getBatchStatus()).isEqualTo(status);
        }
    }
}
