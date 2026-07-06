package MineOpsBackend.controller;

import MineOpsBackend.model.BulkPurchaseRequest;
import MineOpsBackend.repository.BulkPurchaseRequestRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bulk-purchase-requests")
@PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
public class BulkPurchaseRequestController {

    private final BulkPurchaseRequestRepository repo;

    public BulkPurchaseRequestController(BulkPurchaseRequestRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<BulkPurchaseRequest> getMySiteRequests(@AuthenticationPrincipal AuthenticatedUser user) {
        return repo.findBySiteIgnoreCaseOrderByCreatedAtDesc(user.assignedSite());
    }

    @PostMapping
    public BulkPurchaseRequest flagForBulkPurchase(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> body
    ) {
        String mineralType = (String) body.get("mineralType");
        String unit = (String) body.get("unit");
        String qtyStr = body.get("quantityAvailable") != null ? body.get("quantityAvailable").toString() : null;
        if (mineralType == null || mineralType.isBlank() || unit == null || unit.isBlank() || qtyStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mineralType, unit, and quantityAvailable are required");
        }
        BigDecimal qty;
        try { qty = new BigDecimal(qtyStr); } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantityAvailable");
        }
        BulkPurchaseRequest req = new BulkPurchaseRequest();
        req.setSite(user.assignedSite());
        req.setMineralType(mineralType.trim());
        req.setQuantityAvailable(qty);
        req.setUnit(unit.trim());
        req.setFlaggedByEmail(user.email());
        req.setCreatedAt(LocalDateTime.now());
        return repo.save(req);
    }

    @DeleteMapping("/{id}")
    public void withdrawRequest(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        BulkPurchaseRequest req = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!req.getSite().equalsIgnoreCase(user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This request belongs to a different site");
        }
        req.setStatus("WITHDRAWN");
        repo.save(req);
    }
}
