package MineOpsBackend.controller;

import MineOpsBackend.repository.*;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final String SEARCH_ROLES =
            "hasAnyAuthority('ROLE_WORKER', 'ROLE_SUPERVISOR', 'ROLE_SAFETY_OFFICER', 'ROLE_BUYER')";

    private final HazardReportRepository hazardRepo;
    private final IncidentReportRepository incidentRepo;
    private final AppUserRepository userRepo;
    private final MineralListingRepository listingRepo;
    private final ForumPostRepository postRepo;

    public SearchController(
            HazardReportRepository hazardRepo,
            IncidentReportRepository incidentRepo,
            AppUserRepository userRepo,
            MineralListingRepository listingRepo,
            ForumPostRepository postRepo) {
        this.hazardRepo = hazardRepo;
        this.incidentRepo = incidentRepo;
        this.userRepo = userRepo;
        this.listingRepo = listingRepo;
        this.postRepo = postRepo;
    }

    @GetMapping
    @PreAuthorize(SEARCH_ROLES)
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String q,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (q == null || q.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query must be at least 2 characters");
        }

        Pageable limit = PageRequest.of(0, 10);
        String term = q.trim();
        boolean isBuyer = "buyer".equals(user.role());
        String site = user.assignedSite();

        List<Map<String, Object>> hazards = List.of();
        List<Map<String, Object>> incidents = List.of();
        List<Map<String, Object>> workers = List.of();

        if (!isBuyer) {
            hazards = hazardRepo.searchBySite(site, term, limit).stream().map(h -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", h.getId());
                m.put("hazardType", h.getHazardType());
                m.put("location", h.getLocation());
                m.put("severity", h.getSeverity());
                m.put("status", h.getStatus());
                m.put("createdAt", String.valueOf(h.getCreatedAt()));
                return m;
            }).collect(Collectors.toList());

            incidents = incidentRepo.searchBySite(site, term, limit).stream().map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", i.getId());
                m.put("category", i.getCategory());
                m.put("severity", i.getSeverity());
                m.put("zone", i.getZone());
                m.put("description", i.getDescription());
                m.put("status", i.getStatus());
                m.put("reportedAt", String.valueOf(i.getReportedAt()));
                return m;
            }).collect(Collectors.toList());

            workers = userRepo.searchBySite(site, term, limit).stream().map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", u.getId());
                m.put("fullName", u.getFullName());
                m.put("email", u.getEmail());
                m.put("role", u.getRole());
                return m;
            }).collect(Collectors.toList());
        }

        List<Map<String, Object>> listings = listingRepo.search(term, limit).stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("mineralType", l.getMineralType());
            m.put("location", l.getLocation());
            m.put("quantity", l.getQuantity());
            m.put("unit", l.getUnit());
            m.put("askingPrice", l.getAskingPrice());
            m.put("site", l.getSite());
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> forumPosts = postRepo.searchGeneral(term, limit).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("body", p.getBody());
            m.put("authorName", p.getAuthorName());
            m.put("createdAt", String.valueOf(p.getCreatedAt()));
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hazards", hazards);
        result.put("incidents", incidents);
        result.put("workers", workers);
        result.put("listings", listings);
        result.put("forumPosts", forumPosts);

        return ResponseEntity.ok(result);
    }
}
