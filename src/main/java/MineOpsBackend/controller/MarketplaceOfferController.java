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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@RestController
public class MarketplaceOfferController {

    private final MarketplaceOfferRepository offerRepo;
    private final MineralListingRepository listingRepo;
    private final MarketplaceTransactionRepository transactionRepo;
    private final AppUserRepository userRepo;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public MarketplaceOfferController(
        MarketplaceOfferRepository offerRepo,
        MineralListingRepository listingRepo,
        MarketplaceTransactionRepository transactionRepo,
        AppUserRepository userRepo,
        AuditLogService auditLogService,
        NotificationService notificationService
    ) {
        this.offerRepo = offerRepo;
        this.listingRepo = listingRepo;
        this.transactionRepo = transactionRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
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
        MarketplaceOffer offer = new MarketplaceOffer();
        offer.setListingId(listingId);
        offer.setBuyerEmail(user.email());
        offer.setBuyerName(user.fullName());
        offer.setOfferPrice(new BigDecimal(body.get("offerPrice").toString()));
        offer.setOfferQuantity(new BigDecimal(body.get("offerQuantity").toString()));
        offer.setMessage((String) body.get("message"));
        offer.setStatus("PENDING");
        offer.setCreatedAt(LocalDateTime.now());
        return offerRepo.save(offer);
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
    public MarketplaceOffer counterOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @RequestBody Map<String, Object> body
    ) {
        MarketplaceOffer original = offerRepo.findById(id)
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

        MarketplaceOffer counter = new MarketplaceOffer();
        counter.setListingId(original.getListingId());
        counter.setParentOfferId(id);
        counter.setBuyerEmail(original.getBuyerEmail());
        counter.setBuyerName(original.getBuyerName());
        counter.setOfferPrice(new BigDecimal(body.get("offerPrice").toString()));
        counter.setOfferQuantity(new BigDecimal(body.get("offerQuantity").toString()));
        counter.setMessage((String) body.get("message"));
        counter.setStatus("PENDING");
        counter.setCreatedAt(LocalDateTime.now());
        MarketplaceOffer saved = offerRepo.save(counter);
        notificationService.notify(original.getBuyerEmail(), "OFFER", "Counter Offer Received",
            "A counter offer has been made on your offer for " + listing.getMineralType() + ".", "MarketplaceOffer", saved.getId());
        auditLogService.record("OFFER_COUNTERED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_OFFER", id, "counter=" + saved.getId());
        return saved;
    }

    @PostMapping("/api/marketplace/offers/{id}/accept")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MarketplaceTransaction acceptOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MarketplaceOffer offer = offerRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        MineralListing listing = listingRepo.findById(offer.getListingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Offer belongs to a different site");
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer is not in PENDING state");
        }
        offer.setStatus("ACCEPTED");
        offer.setRespondedAt(LocalDateTime.now());
        offer.setRespondedBy(user.email());
        offerRepo.save(offer);

        listing.setStatus("SOLD");
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
        notificationService.notify(offer.getBuyerEmail(), "OFFER", "Offer Accepted",
            "Your offer for " + listing.getMineralType() + " has been accepted. A transaction has been created.", "MarketplaceTransaction", saved.getId());
        auditLogService.record("OFFER_ACCEPTED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_OFFER", id, "transaction=" + saved.getId());
        return saved;
    }

    @PostMapping("/api/marketplace/offers/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MarketplaceOffer rejectOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MarketplaceOffer offer = offerRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        MineralListing listing = listingRepo.findById(offer.getListingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Offer belongs to a different site");
        }
        if (!"PENDING".equals(offer.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offer is not in PENDING state");
        }
        offer.setStatus("REJECTED");
        offer.setRespondedAt(LocalDateTime.now());
        offer.setRespondedBy(user.email());
        offerRepo.save(offer);
        notificationService.notify(offer.getBuyerEmail(), "OFFER", "Offer Rejected",
            "Your offer for " + listing.getMineralType() + " was not accepted.", "MarketplaceOffer", id);
        auditLogService.record("OFFER_REJECTED", user.role(), user.fullName(), user.email(),
            "MARKETPLACE_OFFER", id, "buyer=" + offer.getBuyerEmail());
        return offer;
    }

    @PostMapping("/api/marketplace/offers/{id}/withdraw")
    @PreAuthorize("hasAuthority('ROLE_BUYER')")
    public MarketplaceOffer withdrawOffer(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MarketplaceOffer offer = offerRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (!offer.getBuyerEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your offer");
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
