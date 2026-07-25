package MineOpsBackend.controller;

import MineOpsBackend.dto.SitePermitRequest;
import MineOpsBackend.model.SitePermit;
import MineOpsBackend.repository.SitePermitRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Internal supervisor/safety-officer record of site permits and licenses (mining lease,
 * environmental permit, explosives license, etc.) with an optional scanned document. Not
 * exposed to workers/buyers/guests — this is a compliance record, not a worker-facing feature.
 */
@RestController
public class SitePermitController {

    private final SitePermitRepository permitRepo;
    private final AuditLogService auditLogService;

    @PersistenceContext
    private EntityManager entityManager;

    public SitePermitController(SitePermitRepository permitRepo, AuditLogService auditLogService) {
        this.permitRepo = permitRepo;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/site-permits")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<SitePermit> getSitePermits(@AuthenticationPrincipal AuthenticatedUser user) {
        List<SitePermit> permits = permitRepo.findBySiteIgnoreCaseOrderByExpiryDateAsc(user.assignedSite());
        // Documents are fetched on demand via /api/site-permits/{id}/document — list views only
        // need the hasDocument flag. Detach first so nulling documentData here never flushes to DB.
        permits.forEach(p -> { entityManager.detach(p); p.stripDocumentDataForList(); });
        return permits;
    }

    @GetMapping("/api/site-permits/{id}/document")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, String> getPermitDocument(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SitePermit permit = permitRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permit not found"));
        if (!permit.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permit belongs to a different site");

        return Map.of("documentData", permit.getDocumentData() != null ? permit.getDocumentData() : "");
    }

    @PostMapping("/api/site-permits")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public SitePermit addPermit(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody SitePermitRequest req
    ) {
        LocalDate issueDate = parseDate(req.issueDate());
        LocalDate expiryDate = parseDate(req.expiryDate());
        if (!expiryDate.isAfter(issueDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date must be after issue date");

        SitePermit permit = new SitePermit(
            user.assignedSite(), req.permitName(), req.permitNumber(), req.issuingAuthority(),
            issueDate, expiryDate, req.notes(), user.fullName()
        );
        permit.setDocumentData(req.documentData());
        SitePermit saved = permitRepo.save(permit);

        auditLogService.record("SITE_PERMIT_ADDED", user.role(), user.fullName(), user.email(),
            "SitePermit", saved.getId(), req.permitName() + " — expires " + req.expiryDate());
        return saved;
    }

    @PutMapping("/api/site-permits/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public SitePermit updatePermit(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody SitePermitRequest req
    ) {
        SitePermit permit = permitRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permit not found"));
        if (!permit.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permit belongs to a different site");

        LocalDate newIssue = parseDate(req.issueDate());
        LocalDate newExpiry = parseDate(req.expiryDate());
        if (!newExpiry.isAfter(newIssue))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date must be after issue date");

        permit.setPermitName(req.permitName());
        permit.setPermitNumber(req.permitNumber());
        permit.setIssuingAuthority(req.issuingAuthority());
        permit.setIssueDate(newIssue);
        permit.setExpiryDate(newExpiry);
        permit.setNotes(req.notes());
        if (req.documentData() != null) {
            permit.setDocumentData(req.documentData());
        }
        permit.setUpdatedAt(LocalDateTime.now());
        SitePermit saved = permitRepo.save(permit);

        auditLogService.record("SITE_PERMIT_UPDATED", user.role(), user.fullName(), user.email(),
            "SitePermit", id, req.permitName() + " → expires " + req.expiryDate());
        return saved;
    }

    @DeleteMapping("/api/site-permits/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public void deletePermit(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SitePermit permit = permitRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permit not found"));
        if (!permit.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permit belongs to a different site");

        permitRepo.delete(permit);

        auditLogService.record("SITE_PERMIT_DELETED", user.role(), user.fullName(), user.email(),
            "SitePermit", id, permit.getPermitName());
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format, expected YYYY-MM-DD: " + dateStr);
        }
    }
}
