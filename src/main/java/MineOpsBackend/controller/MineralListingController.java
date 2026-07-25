package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.MineralListing;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.MineralListingRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.dto.CreateListingRequest;
import MineOpsBackend.service.AuditLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class MineralListingController {

    private final MineralListingRepository listingRepo;
    private final AppUserRepository userRepo;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    public MineralListingController(
        MineralListingRepository listingRepo,
        AppUserRepository userRepo,
        AuditLogService auditLogService
    ) {
        this.listingRepo = listingRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/marketplace/listings")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MineralListing createListing(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateListingRequest request
    ) {
        if (user.assignedSite() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No site assigned to your account. Contact an administrator.");
        }
        if (request.photoData() != null && request.photoData().length() > 2_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo is too large. Use a smaller image.");
        }
        if (request.minOrderQuantity() != null
                && request.minOrderQuantity().compareTo(request.quantity()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "minOrderQuantity cannot exceed quantity");
        }
        MineralListing listing = new MineralListing();
        listing.setSite(user.assignedSite());
        listing.setMineralType(request.mineralType());
        listing.setQuantity(request.quantity());
        listing.setUnit(request.unit());
        listing.setGrade(request.grade());
        listing.setAskingPrice(request.askingPrice());
        listing.setLocation(request.location());
        if (request.availableFrom() != null) {
            try { listing.setAvailableFrom(LocalDate.parse(request.availableFrom())); }
            catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "availableFrom must be ISO date (yyyy-MM-dd)"); }
        }
        if (request.minOrderQuantity() != null) {
            listing.setMinOrderQuantity(request.minOrderQuantity());
        }
        listing.setPhotoData(request.photoData());
        listing.setStatus("ACTIVE");
        listing.setCreatedBy(user.email());
        listing.setCreatedAt(LocalDateTime.now());
        MineralListing saved = listingRepo.save(listing);
        auditLogService.record("LISTING_CREATED", user.role(), user.fullName(), user.email(),
            "MINERAL_LISTING", saved.getId(), listing.getMineralType() + " @ " + user.assignedSite());
        return saved;
    }

    @GetMapping("/api/marketplace/listings")
    @PreAuthorize("hasAnyAuthority('ROLE_BUYER', 'ROLE_SUPERVISOR')")
    public List<MineralListing> getListings(@AuthenticationPrincipal AuthenticatedUser user) {
        List<MineralListing> listings;
        if ("buyer".equals(user.role())) {
            AppUser buyer = userRepo.findById(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            if (!"VERIFIED".equals(buyer.getBuyerVerificationStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyer account not yet verified");
            }
            listings = listingRepo.findByStatusOrderByCreatedAtDesc("ACTIVE");
        } else {
            if (user.assignedSite() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No site assigned to your account. Contact an administrator.");
            }
            listings = listingRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(user.assignedSite());
        }
        // Photos are fetched on demand via /api/marketplace/listings/{id}/photo — this is the
        // highest-traffic browse endpoint in the app, so every listing's full base64 photo
        // getting pulled into memory on every browse was a real production risk.
        listings.forEach(l -> { entityManager.detach(l); l.stripPhotoDataForList(); });
        return listings;
    }

    // Fetch the (potentially large) base64 photo for one listing on demand — never returned
    // in bulk from the list endpoint above. Same visibility rule as the list: a verified buyer
    // can view any active listing's photo; a supervisor can only view their own site's.
    @GetMapping("/api/marketplace/listings/{id}/photo")
    @PreAuthorize("hasAnyAuthority('ROLE_BUYER', 'ROLE_SUPERVISOR')")
    public Map<String, String> getListingPhoto(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        MineralListing listing = listingRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        if ("buyer".equals(user.role())) {
            AppUser buyer = userRepo.findById(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            if (!"VERIFIED".equals(buyer.getBuyerVerificationStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyer account not yet verified");
            }
        } else if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing belongs to a different site");
        }

        return Map.of("photoData", listing.getPhotoData() != null ? listing.getPhotoData() : "");
    }

    @PatchMapping("/api/marketplace/listings/{id}/withdraw")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MineralListing withdrawListing(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MineralListing listing = listingRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!user.assignedSite().equalsIgnoreCase(listing.getSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing belongs to a different site");
        }
        if (!"ACTIVE".equals(listing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Only ACTIVE listings can be withdrawn");
        }
        listing.setStatus("WITHDRAWN");
        MineralListing saved = listingRepo.save(listing);
        auditLogService.record("LISTING_WITHDRAWN", user.role(), user.fullName(), user.email(),
            "MINERAL_LISTING", saved.getId(), listing.getMineralType() + " @ " + user.assignedSite());
        return saved;
    }
}
