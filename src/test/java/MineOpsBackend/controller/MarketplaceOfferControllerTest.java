package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.MineralListing;
import MineOpsBackend.model.MarketplaceOffer;
import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.MineralListingRepository;
import MineOpsBackend.repository.MarketplaceOfferRepository;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
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
class MarketplaceOfferControllerTest {

    @Mock MarketplaceOfferRepository offerRepo;
    @Mock MineralListingRepository listingRepo;
    @Mock MarketplaceTransactionRepository transactionRepo;
    @Mock AppUserRepository userRepo;
    @Mock AuditLogService auditLogService;
    @Mock NotificationService notificationService;

    @InjectMocks MarketplaceOfferController controller;

    private static final String SITE = "Obuasi Mine";

    private AuthenticatedUser buyer() {
        return new AuthenticatedUser(10L, "Ama", "ama@buyer.com", "buyer", null, null);
    }

    private AuthenticatedUser supervisor() {
        return new AuthenticatedUser(1L, "Kwame", "kwame@mine.com", "supervisor", SITE, null);
    }

    private AppUser verifiedBuyer() {
        AppUser u = new AppUser("Ama", "ama@buyer.com", "hash", "buyer", null);
        u.setBuyerVerificationStatus("VERIFIED");
        u.setGoldbodLicenseNumber("GB-12345");
        return u;
    }

    private AppUser pendingBuyer() {
        AppUser u = new AppUser("Ama", "ama@buyer.com", "hash", "buyer", null);
        u.setBuyerVerificationStatus("PENDING_VERIFICATION");
        return u;
    }

    private MineralListing activeListing(String site) {
        MineralListing l = new MineralListing();
        l.setSite(site);
        l.setMineralType("Gold");
        l.setQuantity(BigDecimal.valueOf(500));
        l.setUnit("oz");
        l.setAskingPrice(BigDecimal.valueOf(250000));
        l.setStatus("ACTIVE");
        return l;
    }

    private MarketplaceOffer pendingOffer(String buyerEmail, Long listingId, String site) {
        MarketplaceOffer o = new MarketplaceOffer();
        o.setListingId(listingId);
        o.setBuyerEmail(buyerEmail);
        o.setBuyerName("Ama");
        o.setOfferPrice(BigDecimal.valueOf(200000));
        o.setOfferQuantity(BigDecimal.valueOf(400));
        o.setStatus("PENDING");
        return o;
    }

    // ── createOffer ────────────────────────────────────────────────

    @Test
    void createOffer_throws403_whenBuyerNotVerified() {
        when(userRepo.findById(10L)).thenReturn(Optional.of(pendingBuyer()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.createOffer(buyer(), 1L, Map.of(
                "offerPrice", "200000", "offerQuantity", "400")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(listingRepo, never()).findById(any());
        verify(offerRepo, never()).save(any());
    }

    @Test
    void createOffer_throws409_whenListingNotActive() {
        when(userRepo.findById(10L)).thenReturn(Optional.of(verifiedBuyer()));
        MineralListing sold = activeListing(SITE);
        sold.setStatus("SOLD");
        when(listingRepo.findById(1L)).thenReturn(Optional.of(sold));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.createOffer(buyer(), 1L, Map.of(
                "offerPrice", "200000", "offerQuantity", "400")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(offerRepo, never()).save(any());
    }

    @Test
    void createOffer_savesPendingOffer_whenBuyerVerifiedAndListingActive() {
        when(userRepo.findById(10L)).thenReturn(Optional.of(verifiedBuyer()));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing(SITE)));
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarketplaceOffer result = controller.createOffer(buyer(), 1L, Map.of(
            "offerPrice", "200000", "offerQuantity", "400", "message", "Interested"));

        ArgumentCaptor<MarketplaceOffer> cap = ArgumentCaptor.forClass(MarketplaceOffer.class);
        verify(offerRepo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(cap.getValue().getBuyerEmail()).isEqualTo("ama@buyer.com");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    // ── getListingOffers ───────────────────────────────────────────

    @Test
    void getListingOffers_throws403_whenListingBelongsToDifferentSite() {
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing("Tarkwa Mine")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getListingOffers(supervisor(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── counterOffer ───────────────────────────────────────────────

    @Test
    void counterOffer_throws403_whenListingBelongsToDifferentSite() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing("Tarkwa Mine")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.counterOffer(supervisor(), 1L, Map.of(
                "offerPrice", "220000", "offerQuantity", "400")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(offerRepo, never()).save(any());
    }

    @Test
    void counterOffer_throws409_whenOfferNotPending() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        offer.setStatus("REJECTED");
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing(SITE)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.counterOffer(supervisor(), 1L, Map.of(
                "offerPrice", "220000", "offerQuantity", "400")));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void counterOffer_marksOriginalCOUNTEREDandCreatesNewOffer() {
        MarketplaceOffer original = pendingOffer("ama@buyer.com", 1L, SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(original));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing(SITE)));
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarketplaceOffer counter = controller.counterOffer(supervisor(), 1L, Map.of(
            "offerPrice", "220000", "offerQuantity", "350"));

        assertThat(original.getStatus()).isEqualTo("COUNTERED");
        assertThat(counter.getStatus()).isEqualTo("PENDING");
        assertThat(counter.getParentOfferId()).isEqualTo(1L);
        assertThat(counter.getOfferPrice()).isEqualByComparingTo(BigDecimal.valueOf(220000));

        verify(auditLogService).record(
            "OFFER_COUNTERED", "supervisor", "Kwame", "kwame@mine.com",
            "MARKETPLACE_OFFER", 1L, "counter=" + counter.getId()
        );
    }

    // ── acceptOffer ────────────────────────────────────────────────

    @Test
    void acceptOffer_throws403_whenListingBelongsToDifferentSite() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing("Tarkwa Mine")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.acceptOffer(supervisor(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(transactionRepo, never()).save(any());
    }

    @Test
    void acceptOffer_throws409_whenOfferNotPending() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        offer.setStatus("COUNTERED");
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing(SITE)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.acceptOffer(supervisor(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void acceptOffer_createsTransactionMarksListingSoldAndAudits() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        MineralListing listing = activeListing(SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(listing));
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarketplaceTransaction tx = controller.acceptOffer(supervisor(), 1L);

        assertThat(offer.getStatus()).isEqualTo("ACCEPTED");
        assertThat(listing.getStatus()).isEqualTo("SOLD");
        assertThat(tx.getBatchStatus()).isEqualTo("PREPARING");
        assertThat(tx.getBuyerEmail()).isEqualTo("ama@buyer.com");
        assertThat(tx.getMineralType()).isEqualTo("Gold");
        assertThat(tx.getSite()).isEqualTo(SITE);

        verify(auditLogService).record(
            "OFFER_ACCEPTED", "supervisor", "Kwame", "kwame@mine.com",
            "MARKETPLACE_OFFER", 1L, "transaction=" + tx.getId()
        );
    }

    // ── rejectOffer ────────────────────────────────────────────────

    @Test
    void rejectOffer_throws403_whenWrongSite() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing("Tarkwa Mine")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.rejectOffer(supervisor(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectOffer_throws409_whenNotPending() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        offer.setStatus("ACCEPTED");
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing(SITE)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.rejectOffer(supervisor(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectOffer_marksRejectedAndAudits() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(listingRepo.findById(1L)).thenReturn(Optional.of(activeListing(SITE)));
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.rejectOffer(supervisor(), 1L);

        assertThat(offer.getStatus()).isEqualTo("REJECTED");
        verify(auditLogService).record(
            "OFFER_REJECTED", "supervisor", "Kwame", "kwame@mine.com",
            "MARKETPLACE_OFFER", 1L, "buyer=ama@buyer.com"
        );
    }

    // ── withdrawOffer ──────────────────────────────────────────────

    @Test
    void withdrawOffer_throws403_whenNotOwner() {
        MarketplaceOffer offer = pendingOffer("other@buyer.com", 1L, SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.withdrawOffer(buyer(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(offerRepo, never()).save(any());
    }

    @Test
    void withdrawOffer_throws409_whenNotPending() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        offer.setStatus("ACCEPTED");
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.withdrawOffer(buyer(), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void withdrawOffer_marksWithdrawn_whenOwnerAndPending() {
        MarketplaceOffer offer = pendingOffer("ama@buyer.com", 1L, SITE);
        when(offerRepo.findById(1L)).thenReturn(Optional.of(offer));
        when(offerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MarketplaceOffer result = controller.withdrawOffer(buyer(), 1L);

        assertThat(result.getStatus()).isEqualTo("WITHDRAWN");
    }
}
