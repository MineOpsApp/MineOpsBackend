package MineOpsBackend.controller;

import MineOpsBackend.model.MarketplaceRating;
import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.repository.MarketplaceRatingRepository;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.security.AuthenticatedUser;
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
class MarketplaceRatingControllerTest {

    @Mock MarketplaceTransactionRepository txRepo;
    @Mock MarketplaceRatingRepository ratingRepo;

    @InjectMocks MarketplaceRatingController controller;

    private static final String SITE = "Obuasi Mine";
    private static final String BUYER_EMAIL = "ama@buyer.com";

    private AuthenticatedUser buyer() {
        return new AuthenticatedUser(10L, "Ama", BUYER_EMAIL, "buyer", null, null);
    }

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE, null);
    }

    private MarketplaceTransaction deliveredTx() {
        MarketplaceTransaction tx = new MarketplaceTransaction();
        tx.setSite(SITE);
        tx.setBuyerEmail(BUYER_EMAIL);
        tx.setBuyerName("Ama");
        tx.setMineralType("Gold");
        tx.setQuantity(BigDecimal.valueOf(100));
        tx.setAgreedPrice(BigDecimal.valueOf(50000));
        tx.setBatchStatus("DELIVERED");
        return tx;
    }

    private Map<String, Object> validRatingBody() {
        return Map.of(
                "reliability", 4,
                "communication", 5,
                "productQuality", 3,
                "listingAccuracy", 4
        );
    }

    // ── submitRating — guards ─────────────────────────────────────────────────

    @Test
    void submitRating_throws404_whenTransactionNotFound() {
        when(txRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.submitRating(99L, buyer(), validRatingBody()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void submitRating_throws409_whenNotDelivered() {
        MarketplaceTransaction tx = deliveredTx();
        tx.setBatchStatus("IN_TRANSIT");
        when(txRepo.findById(1L)).thenReturn(Optional.of(tx));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.submitRating(1L, buyer(), validRatingBody()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(ratingRepo, never()).save(any());
    }

    @Test
    void submitRating_throws403_whenNotAParty() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(deliveredTx()));
        // outsider: not the buyer email and no assignedSite
        AuthenticatedUser outsider = new AuthenticatedUser(99L, "X", "x@other.com", "buyer", null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.submitRating(1L, outsider, validRatingBody()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(ratingRepo, never()).save(any());
    }

    @Test
    void submitRating_throws409_whenAlreadyRated() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(deliveredTx()));
        when(ratingRepo.existsByTransactionIdAndRaterEmail(1L, BUYER_EMAIL)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.submitRating(1L, buyer(), validRatingBody()));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(ratingRepo, never()).save(any());
    }

    @Test
    void submitRating_throws400_whenRatingOutOfRange() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(deliveredTx()));
        when(ratingRepo.existsByTransactionIdAndRaterEmail(1L, BUYER_EMAIL)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.submitRating(1L, buyer(), Map.of("reliability", 6, "communication", 5)));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── submitRating — productQuality / listingAccuracy buyer-only ────────────

    @Test
    void submitRating_setsProductQualityAndListingAccuracy_forBuyer() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(deliveredTx()));
        when(ratingRepo.existsByTransactionIdAndRaterEmail(1L, BUYER_EMAIL)).thenReturn(false);
        when(ratingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarketplaceRating saved = controller.submitRating(1L, buyer(), validRatingBody());

        assertThat(saved.getProductQuality()).isEqualTo(3);
        assertThat(saved.getListingAccuracy()).isEqualTo(4);
    }

    @Test
    void submitRating_doesNotSetProductQualityOrListingAccuracy_forSiteRater() {
        when(txRepo.findById(1L)).thenReturn(Optional.of(deliveredTx()));
        when(ratingRepo.existsByTransactionIdAndRaterEmail(1L, "kwame@mine.com")).thenReturn(false);
        when(ratingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Supervisor from the same site passes productQuality/listingAccuracy in the body
        MarketplaceRating saved = controller.submitRating(1L, supervisor(), validRatingBody());

        assertThat(saved.getProductQuality()).isNull();
        assertThat(saved.getListingAccuracy()).isNull();
    }

    // ── getMineRatings ────────────────────────────────────────────────────────

    @Test
    void getMineRatings_returnsOnlyNonBuyerRatings() {
        MarketplaceTransaction tx = deliveredTx();
        when(txRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(SITE)).thenReturn(List.of(tx));

        MarketplaceRating buyerRating = new MarketplaceRating();
        buyerRating.setRaterRole("buyer");

        MarketplaceRating supervisorRating = new MarketplaceRating();
        supervisorRating.setRaterRole("supervisor");

        when(ratingRepo.findByTransactionIdIn(any())).thenReturn(List.of(buyerRating, supervisorRating));

        List<MarketplaceRating> result = controller.getMineRatings(SITE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRaterRole()).isEqualTo("supervisor");
    }

    @Test
    void getMineRatings_returnsEmpty_whenNoTransactions() {
        when(txRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc("Ghost Mine")).thenReturn(List.of());

        List<MarketplaceRating> result = controller.getMineRatings("Ghost Mine");

        assertThat(result).isEmpty();
        verify(ratingRepo, never()).findByTransactionIdIn(any());
    }
}
