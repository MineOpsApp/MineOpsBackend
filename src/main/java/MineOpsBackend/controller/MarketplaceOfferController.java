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
import MineOpsBackend.service.PushNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class MarketplaceOfferController {

    private final MarketplaceOfferRepository offerRepo;
    private final MineralListingRepository listingRepo;
    private final MarketplaceTransactionRepository transactionRepo;
    private final AppUserRepository userRepo;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    public MarketplaceOfferController(
        MarketplaceOfferRepository offerRepo,
        MineralListingRepository listingRepo,
        MarketplaceTransactionRepository transactionRepo,
        AppUserRepository userRepo,
        AuditLogService auditLogService,
        NotificationService notificationService,
        PushNotificationService pushNotificationService
    ) {
        this.offerRepo = offerRepo;
        this.listingRepo = listingRepo;
        this.transactionRepo = transactionRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
    }

    /** Notify every active supervisor at a site (used when a buyer acts on a counter-offer). */
    private void notifySiteSupervisors(String site, String type, String title, String body, String entityType, Long entityId) {
        List<AppUser> recipients = userRepo.findByAssignedSiteIgnoreCase(site)
            .stream()
            .filter(u -> "supervisor".equals(u.getRole()))
            .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
            .collect(Collectors.toList());
        List<String> tokens = recipients.stream()
            .map(AppUser::getPushToken)
            .filter(t -> t != null && !t.isBlank())
            .collect(Collectors.toList());
        pushNotificationService.sendToTokens(tokens, title, body, "default");
        for (AppUser recipient : recipients) {
            notificationService.notify(recipient.getEmail(), type, title, body, entityType, entityId);
        }
    }

    /** Notify a single buyer by email, both in-app and as a real push to their phone. */
    private void notifyBuyer(String buyerEmail, String type, String title, String body, String entityType, Long entityId) {
        notificationService.notify(buyerEmail, type, title, body, entityType, entityId);
        userRepo.findByEmailIgnoreCase(buyerEmail).ifPresent(u -> {
            String token = u.getPushToken();
            if (token != null && !token.isBlank()) {
                pushNotificationService.sendToToken(token, title, body, "default");
            }
        });
    }

    @PostMapping("/api/marketplace/listings/{listingId}/offers")
    @PreAuthorize("hasAuthority('ROLE_BUYER')")
    public MarketplaceOffer createOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long listingId,
        @RequestBody Map<String, Object> body
    ) {
        AppUser buyer = userRepo.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!"VERIFIED".equals(buyer.getBuyerVerificationStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyer account not yet verified");
        }
        MineralListing listing = listingRepo.findById(listingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!"ACTIVE".equals(listing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Listing is no longer active");
        }
        if ("gold".equalsIgnoreCase(listing.getMineralType())) {
            String license = buyer.getGoldbodLicenseNumber();
            if (license == null || license.isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A GoldBod license number is required to offer on gold listings");
            }
        }
        if (body.get("offerPrice") == null || body.get("offerQuantity") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offerPrice and offerQuantity are required");
        }
        BigDecimal offerPrice, offerQuantity;
        try {
            offerPrice = new BigDecimal(body.get("offerPrice").toString());
            offerQuantity = new BigDecimal(body.get("offerQuantity").toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offerPrice and offerQuantity must be valid numbers");
        }
        if (listing.getQuantity() != null && offerQuantity.compareTo(listing.getQuantity()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Offer quantity cannot exceed the listed quantity (" + listing.getQuantity() + " " + listing.getUnit() + " available)");
        }
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setListingId(listingId);
        offer.setBuyerEmail(user.email());
        offer.setBuyerName(user.fullName());
        offer.setOfferPrice(offerPrice);
        offer.setOfferQuantity(offerQuantity);
        offer.setMessage((String) body.get("message"));
        offer.setStatus("PENDING");
        offer.setProposedByRole("BUYER");
        offer.setCreatedAt(LocalDateTime.now());
        MarketplaceOffer saved = offerRepo.save(offer);
        notifySiteSupervisors(listing.getSite(), "OFFER", "New Offer Received",
            buyer.getFullName() + " offered GHS " + offerPrice + " for " + offerQuantity + " " + listing.getUnit() + " of " + listing.getMineralType() + ".",
            "MarketplaceOffer", saved.getId());
        return saved;
    }

    @GetMapping("/api/marketplace/offers/mine")
    @PreAuthorize("hasAuthority('ROLE_BUYER')")
    public List<MarketplaceOffer> getMyOffers(@AuthenticationPrincipal AuthenticatedUser user) {
        return offerRepo.findByBuyerEmailOrderByCreatedAtAsc(user.email());
    }

    @GetMapping("/api/marketplace/listings/{listingId}/offers")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public List<MarketplaceOffer> getListingOffers(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long listingId
    ) {
        MineralListing listing = listingRepo.findById(listingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing belongs to a different site");
        }
        return offerRepo.findByListingIdOrderByCreatedAtAsc(listingId);
    }

    @PostMapping("/api/marketplace/offers/{id}/counter")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    @Transactional
    public MarketplaceOffer counterOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        MarketplaceOffer original = offerRepo.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        MineralListing listing = listingRepo.findById(original.getListingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Offer belongs to a different site");
        }
        if (!"PENDING".equals(original.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer is not in PENDING state");
        }
        original.setStatus("COUNTERED");
        original.setRespondedAt(LocalDateTime.now());
        original.setRespondedBy(user.email());
        offerRepo.save(original);

        if (body.get("offerPrice") == null || body.get("offerQuantity") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offerPrice and offerQuantity are required");
        }
        BigDecimal counterPrice, counterQuantity;
        try {
            counterPrice = new BigDecimal(body.get("offerPrice").toString());
            counterQuantity = new BigDecimal(body.get("offerQuantity").toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offerPrice and offerQuantity must be valid numbers");
        }
        MarketplaceOffer counter = new MarketplaceOffer();
        counter.setListingId(original.getListingId());
        counter.setParentOfferId(id);
        counter.setBuyerEmail(original.getBuyerEmail());
        counter.setBuyerName(original.getBuyerName());
        counter.setOfferPrice(counterPrice);
        counter.setOfferQuantity(counterQuantity);
        counter.setMessage((String) body.get("message"));
        counter.setStatus("PENDING");
        counter.setProposedByRole("SUPERVISOR");
        counter.setCreatedAt(LocalDateTime.now());
        MarketplaceOffer saved = offerRepo.save(counter);
        notifyBuyer(original.getBuyerEmail(), "OFFER", "Counter Offer Received",
            "A counter offer has been made on your offer for " + listing.getMineralType() + ".", "MarketplaceOffer", saved.getId());
        auditLogService.record("OFFER_COUNTERED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_OFFER", id, "counter=" + saved.getId());
        return saved;
    }

    @PostMapping("/api/marketplace/offers/{id}/accept")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_BUYER')")
    @Transactional
    public MarketplaceTransaction acceptOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        // Both rows are locked for the rest of this transaction: the offer so a double-tap or a
        // simultaneous reject/withdraw can't race the status check below, and the listing so two
        // offers on it being accepted at once can't both compute "remaining" off the same stale
        // quantity (see findByIdForUpdate javadoc on each repository).
        MarketplaceOffer offer = offerRepo.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        MineralListing listing = listingRepo.findByIdForUpdate(offer.getListingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        boolean isSupervisor = "supervisor".equals(user.role());
        if (isSupervisor) {
            if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Offer belongs to a different site");
            }
            if (!"BUYER".equals(offer.getProposedByRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiting on the buyer to respond to this offer");
            }
        } else {
            if (!offer.getBuyerEmail().equalsIgnoreCase(user.email())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your offer");
            }
            if (!"SUPERVISOR".equals(offer.getProposedByRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiting on the supervisor to respond to this offer");
            }
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer is not in PENDING state");
        }
        offer.setStatus("ACCEPTED");
        offer.setRespondedAt(LocalDateTime.now());
        offer.setRespondedBy(user.email());
        offerRepo.save(offer);

        BigDecimal remaining = listing.getQuantity() != null
            ? listing.getQuantity().subtract(offer.getOfferQuantity())
            : BigDecimal.ZERO;
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            listing.setQuantity(BigDecimal.ZERO);
            listing.setStatus("SOLD");
        } else {
            listing.setQuantity(remaining);
            // stays ACTIVE — remainder is still purchasable by other buyers
        }
        listingRepo.save(listing);

        MarketplaceTransaction tx = new MarketplaceTransaction();
        tx.setListingId(listing.getId());
        tx.setOfferId(offer.getId());
        tx.setSite(listing.getSite());
        tx.setBuyerEmail(offer.getBuyerEmail());
        tx.setBuyerName(offer.getBuyerName());
        tx.setMineralType(listing.getMineralType());
        tx.setQuantity(offer.getOfferQuantity());
        tx.setAgreedPrice(offer.getOfferPrice());
        tx.setBatchStatus("PREPARING");
        tx.setCreatedAt(LocalDateTime.now());
        MarketplaceTransaction saved = transactionRepo.save(tx);

        if (isSupervisor) {
            notifyBuyer(offer.getBuyerEmail(), "OFFER", "Offer Accepted",
                "Your offer for " + listing.getMineralType() + " has been accepted. A transaction has been created.", "MarketplaceTransaction", saved.getId());
        } else {
            notifySiteSupervisors(listing.getSite(), "OFFER", "Counter Offer Accepted",
                offer.getBuyerName() + " accepted your counter offer for " + listing.getMineralType() + ".", "MarketplaceTransaction", saved.getId());
        }
        auditLogService.record("OFFER_ACCEPTED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_OFFER", id, "transaction=" + saved.getId());
        return saved;
    }

    @PostMapping("/api/marketplace/offers/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_BUYER')")
    @Transactional
    public MarketplaceOffer rejectOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        MarketplaceOffer offer = offerRepo.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        MineralListing listing = listingRepo.findById(offer.getListingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        boolean isSupervisor = "supervisor".equals(user.role());
        if (isSupervisor) {
            if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Offer belongs to a different site");
            }
            if (!"BUYER".equals(offer.getProposedByRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiting on the buyer to respond to this offer");
            }
        } else {
            if (!offer.getBuyerEmail().equalsIgnoreCase(user.email())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your offer");
            }
            if (!"SUPERVISOR".equals(offer.getProposedByRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiting on the supervisor to respond to this offer");
            }
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer is not in PENDING state");
        }
        String reason = body != null ? body.get("reason") : null;
        offer.setStatus("REJECTED");
        offer.setRespondedAt(LocalDateTime.now());
        offer.setRespondedBy(user.email());
        if (reason != null && !reason.isBlank()) offer.setRejectionReason(reason.trim());
        offerRepo.save(offer);

        if (isSupervisor) {
            String body2 = "Your offer for " + listing.getMineralType() + " was not accepted."
                + (reason != null && !reason.isBlank() ? " Reason: " + reason.trim() : "");
            notifyBuyer(offer.getBuyerEmail(), "OFFER", "Offer Rejected", body2, "MarketplaceOffer", id);
        } else {
            notifySiteSupervisors(listing.getSite(), "OFFER", "Counter Offer Declined",
                offer.getBuyerName() + " declined your counter offer for " + listing.getMineralType() + ".", "MarketplaceOffer", id);
        }
        auditLogService.record("OFFER_REJECTED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_OFFER", id, "buyer=" + offer.getBuyerEmail());
        return offer;
    }

    @PostMapping("/api/marketplace/offers/{id}/withdraw")
    @PreAuthorize("hasAuthority('ROLE_BUYER')")
    @Transactional
    public MarketplaceOffer withdrawOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MarketplaceOffer offer = offerRepo.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (!offer.getBuyerEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your offer");
        }
        if (offer.getProposedByRole() != null && !"BUYER".equals(offer.getProposedByRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is a counter offer from your supervisor — accept or decline it instead of withdrawing");
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer is not in PENDING state");
        }
        offer.setStatus("WITHDRAWN");
        offer.setRespondedAt(LocalDateTime.now());
        offer.setRespondedBy(user.email());
        return offerRepo.save(offer);
    }
}
