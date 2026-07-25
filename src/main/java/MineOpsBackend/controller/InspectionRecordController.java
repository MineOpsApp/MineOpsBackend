package MineOpsBackend.controller;

import MineOpsBackend.model.InspectionRecord;
import MineOpsBackend.repository.InspectionRecordRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspections")
public class InspectionRecordController {

    private final InspectionRecordRepository repo;
    private final AuditLogService auditLogService;

    public InspectionRecordController(InspectionRecordRepository repo, AuditLogService auditLogService) {
        this.repo = repo;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GOVERNMENT')")
    public InspectionRecord create(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> body
    ) {
        String site = getString(body, "site");
        String inspectionType = getString(body, "inspectionType");
        if (site == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "site is required");
        }
        if (inspectionType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inspectionType is required");
        }

        InspectionRecord r = new InspectionRecord();
        r.setInspectorUserId(user.id());
        r.setSite(site);
        r.setInspectionType(inspectionType);
        r.setInspectionReferenceNumber(getString(body, "inspectionReferenceNumber"));
        r.setScope(getString(body, "scope"));
        r.setLegalAuthorityReference(getString(body, "legalAuthorityReference"));
        r.setExpectedDuration(getString(body, "expectedDuration"));
        InspectionRecord saved = repo.save(r);
        auditLogService.record("INSPECTION_CREATED", user.role(), user.fullName(), user.email(),
            "InspectionRecord", saved.getId(), saved.getInspectionType() + " @ " + saved.getSite());
        return saved;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER','ROLE_GOVERNMENT')")
    public List<InspectionRecord> list(@AuthenticationPrincipal AuthenticatedUser user) {
        if ("government".equals(user.role())) {
            return repo.findByInspectorUserIdOrderByCreatedAtDesc(user.id());
        }
        // Supervisor/safety officer: always scoped to their own site — never trust a
        // client-supplied site param, and never fall back to returning every site's inspections.
        return repo.findBySiteOrderByCreatedAtDesc(user.assignedSite());
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAuthority('ROLE_GOVERNMENT')")
    public InspectionRecord start(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        InspectionRecord r = findOwned(id, user.id());
        r.setInspectionStartAt(LocalDateTime.now());
        return repo.save(r);
    }

    @PatchMapping("/{id}/end")
    @PreAuthorize("hasAuthority('ROLE_GOVERNMENT')")
    public InspectionRecord end(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        InspectionRecord r = findOwned(id, user.id());
        r.setInspectionEndAt(LocalDateTime.now());
        return repo.save(r);
    }

    @PatchMapping("/{id}/findings")
    @PreAuthorize("hasAuthority('ROLE_GOVERNMENT')")
    public InspectionRecord submitFindings(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> body
    ) {
        InspectionRecord r = findOwned(id, user.id());
        if (body.containsKey("zonesInspected")) r.setZonesInspected(getString(body, "zonesInspected"));
        if (body.containsKey("findingsSummary")) r.setFindingsSummary(getString(body, "findingsSummary"));
        if (body.containsKey("complianceStatus")) {
            String status = getString(body, "complianceStatus");
            if (status != null && !java.util.Set.of("COMPLIANT", "NON_COMPLIANT", "PARTIAL").contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "complianceStatus must be COMPLIANT, NON_COMPLIANT, or PARTIAL");
            }
            r.setComplianceStatus(status);
        }
        if (body.containsKey("followUpRequired")) r.setFollowUpRequired(Boolean.TRUE.equals(body.get("followUpRequired")));
        if (body.containsKey("reportSubmitted")) r.setReportSubmitted(Boolean.TRUE.equals(body.get("reportSubmitted")));
        if (body.containsKey("nextInspectionDate")) {
            Object v = body.get("nextInspectionDate");
            if (v != null && !v.toString().isBlank()) {
                try { r.setNextInspectionDate(LocalDate.parse(v.toString())); } catch (Exception ignored) {}
            }
        }
        InspectionRecord saved = repo.save(r);
        auditLogService.record("INSPECTION_FINDINGS_SUBMITTED", user.role(), user.fullName(), user.email(),
            "InspectionRecord", saved.getId(),
            saved.getSite() + " — compliance: " + (saved.getComplianceStatus() != null ? saved.getComplianceStatus() : "n/a"));
        return saved;
    }

    private InspectionRecord findOwned(Long id, Long userId) {
        InspectionRecord r = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found"));
        if (!r.getInspectorUserId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your inspection");
        return r;
    }

    private String getString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v instanceof String s && !s.isBlank() ? s.trim() : null;
    }
}
