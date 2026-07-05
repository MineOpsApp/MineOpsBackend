package MineOpsBackend.controller;

import MineOpsBackend.model.MarketplaceRating;
import MineOpsBackend.model.MarketplaceTransaction;
import MineOpsBackend.repository.MarketplaceRatingRepository;
import MineOpsBackend.repository.MarketplaceTransactionRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/community")
public class MarketplaceRatingController {

    private final MarketplaceTransactionRepository txRepo;
    private final MarketplaceRatingRepository ratingRepo;

    public MarketplaceRatingController(MarketplaceTransactionRepository txRepo,
                                       MarketplaceRatingRepository ratingRepo) {
        this.txRepo = txRepo;
        this.ratingRepo = ratingRepo;
    }

    @PostMapping("/transactions/{id}/ratings")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public MarketplaceRating submitRating(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser user,
                                          @RequestBody Map<String, Object> body) {
        MarketplaceTransaction tx = txRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (!"DELIVERED".equals(tx.getBatchStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Can only rate delivered transactions");
        }

        boolean isBuyer = user.email().equalsIgnoreCase(tx.getBuyerEmail());
        boolean isSite = user.assignedSite() != null && user.assignedSite().equalsIgnoreCase(tx.getSite());
        if (!isBuyer && !isSite) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a party to this transaction");
        }
        if (ratingRepo.existsByTransactionIdAndRaterEmail(id, user.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already rated this transaction");
        }

        int reliability = toInt(body.get("reliability"));
        int communication = toInt(body.get("communication"));
        if (reliability < 1 || reliability > 5 || communication < 1 || communication > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ratings must be between 1 and 5");
        }

        MarketplaceRating rating = new MarketplaceRating();
        rating.setTransactionId(id);
        rating.setRaterEmail(user.email());
        rating.setRaterRole(user.role());
        rating.setReliability(reliability);
        rating.setCommunication(communication);

        // productQuality and listingAccuracy are buyer-perspective metrics — only buyers set them
        if (isBuyer) {
            Object pq = body.get("productQuality");
            if (pq != null) rating.setProductQuality(toInt(pq));
            Object la = body.get("listingAccuracy");
            if (la != null) rating.setListingAccuracy(toInt(la));
        }

        Object comment = body.get("comment");
        if (comment instanceof String s) rating.setComment(s.trim());

        rating.setCreatedAt(LocalDateTime.now());
        return ratingRepo.save(rating);
    }

    @GetMapping("/mines/{siteName}/ratings")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public List<MarketplaceRating> getMineRatings(@PathVariable String siteName) {
        List<Long> txIds = txRepo.findBySiteIgnoreCaseOrderByCreatedAtDesc(siteName)
                .stream().map(MarketplaceTransaction::getId).collect(Collectors.toList());
        if (txIds.isEmpty()) return List.of();
        return ratingRepo.findByTransactionIdIn(txIds).stream()
                .filter(r -> !"BUYER".equalsIgnoreCase(r.getRaterRole()))
                .collect(Collectors.toList());
    }

    @GetMapping("/buyers/{email}/ratings")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')")
    public List<MarketplaceRating> getBuyerRatings(@PathVariable String email) {
        List<Long> txIds = txRepo.findByBuyerEmailOrderByCreatedAtDesc(email)
                .stream().map(MarketplaceTransaction::getId).collect(Collectors.toList());
        if (txIds.isEmpty()) return List.of();
        return ratingRepo.findByTransactionIdIn(txIds).stream()
                .filter(r -> "BUYER".equalsIgnoreCase(r.getRaterRole()))
                .collect(Collectors.toList());
    }

    private int toInt(Object v) {
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (NumberFormatException e) { return 0; }
    }
}
