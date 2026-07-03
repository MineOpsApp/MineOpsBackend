package MineOpsBackend.controller;

import MineOpsBackend.model.InventoryTransaction;
import MineOpsBackend.model.MineralInventory;
import MineOpsBackend.model.Site;
import MineOpsBackend.repository.InventoryTransactionRepository;
import MineOpsBackend.repository.MineralInventoryRepository;
import MineOpsBackend.repository.SiteRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class MineralInventoryController {

    private final MineralInventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final SiteRepository siteRepository;

    public MineralInventoryController(
        MineralInventoryRepository inventoryRepository,
        InventoryTransactionRepository transactionRepository,
        SiteRepository siteRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.siteRepository = siteRepository;
    }

    @GetMapping("/api/inventory")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<MineralInventory> getSiteInventory(@AuthenticationPrincipal AuthenticatedUser user) {
        return inventoryRepository.findBySiteIgnoreCaseOrderByMineralTypeAsc(user.assignedSite());
    }

    @GetMapping("/api/inventory/history")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Page<InventoryTransaction> getSiteHistory(
        @AuthenticationPrincipal AuthenticatedUser user,
        Pageable pageable
    ) {
        return transactionRepository.findBySiteIgnoreCaseOrderByCreatedAtDesc(user.assignedSite(), pageable);
    }

    @GetMapping("/api/inventory/my-contributions")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<InventoryTransaction> getMyContributions(@AuthenticationPrincipal AuthenticatedUser user) {
        return transactionRepository.findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(user.email());
    }

    @GetMapping("/api/inventory/public")
    @PreAuthorize("hasAuthority('ROLE_GUEST')")
    public List<MineralInventory> getPublicInventory(@AuthenticationPrincipal AuthenticatedUser user) {
        Site site = siteRepository.findByNameIgnoreCase(user.assignedSite())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site not found"));
        if (!site.isInventoryVisibleToGuests()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This site has not opted in to sharing inventory");
        }
        return inventoryRepository.findBySiteIgnoreCaseOrderByMineralTypeAsc(user.assignedSite());
    }
}
