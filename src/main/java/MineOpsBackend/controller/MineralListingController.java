package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.MineralListing;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.MineralListingRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class MineralListingController {

    private final MineralListingRepository listingRepo;
    private final AppUserRepository userRepo;
    private final AuditLogService auditLogService;

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
        @RequestBody Map<String, Object> body
    ) {
        MineralListing listing = new MineralListing();
        listing.setSite(user.assignedSite());
        listing.setMineralType((String) body.get("mineralType"));
        listing.setQuantity(new BigDecimal(body.get("quantity").toString()));
        listing.setUnit((String) body.get("unit"));
        listing.setGrade((String) body.get("grade"));
        listing.setAskingPrice(new BigDecimal(body.get("askingPrice").toString()));
        listing.setLocation((String) body.get("location"));
        if (body.get("availableFrom") != null) {
            listing.setAvailableFrom(LocalDate.parse(body.get("availableFrom").toString()));
        }
        if (body.get("minOrderQuantity") != null) {
            listing.setMinOrderQuantity(new BigDecimal(body.get("minOrderQuantity").toString()));
        }
        listing.setPhotoData((String) body.get("photoData"));
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
        if ("buyer".equals(user.role())) {
            AppUser buyer = userRepo.findById(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            if (!"VERIFIED".equals(buyer.getBuyerVerificationStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyer account not yet verified");
            }
            return listingRepo.findByStatusOrderByCreatedAtDesc("ACTIVE");
        }
        return listingRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(user.assignedSite());
    }

    @PatchMapping("/api/marketplace/listings/{id}/withdraw")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MineralListing withdrawListing(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        MineralListing listing = listingRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (!listing.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing belongs to a different site");
        }
        listing.setStatus("WITHDRAWN");
        MineralListing saved = listingRepo.save(listing);
        auditLogService.record("LISTING_WITHDRAWN", user.role(), user.fullName(), user.email(),
            "MINERAL_LISTING", saved.getId(), listing.getMineralType() + " @ " + user.assignedSite());
        return saved;
    }
}
