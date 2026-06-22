package MineOpsBackend.controller;

import MineOpsBackend.model.Site;
import MineOpsBackend.repository.SiteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SiteController {

    private final SiteRepository siteRepository;

    public SiteController(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @GetMapping("/api/sites")
    @PreAuthorize("isAuthenticated()")
    public List<Site> getSites() {
        return siteRepository.findAll();
    }
}
