package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.EquipmentFault;
import MineOpsBackend.model.MaintenanceRequest;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.EquipmentFaultRepository;
import MineOpsBackend.repository.MaintenanceRequestRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A site-wide view of worker-flagged equipment faults/maintenance requests, for supervisors and
 * safety officers. WorkerController already saves these and notifies supervisors on creation, but
 * — until now — nothing let a supervisor or safety officer actually look at the list.
 *
 * EquipmentFault/MaintenanceRequest only store workerEmail, not a site column (see FIXES.md for
 * why that was deliberately left alone rather than migrated). Site-scoping is done here instead,
 * at read time, by looking up each reporting worker's assignedSite — same cost as a migration
 * would have saved, but zero schema risk.
 */
@RestController
public class EquipmentIssuesController {

    private final EquipmentFaultRepository equipmentFaultRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    public EquipmentIssuesController(
        EquipmentFaultRepository equipmentFaultRepository,
        MaintenanceRequestRepository maintenanceRequestRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService
    ) {
        this.equipmentFaultRepository = equipmentFaultRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/equipment-issues/site")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Map<String, Object>> getSiteEquipmentIssues(@AuthenticationPrincipal AuthenticatedUser user) {
        String site = user.assignedSite();
        if (site == null || site.isBlank()) return List.of();

        // Small per-request cache so a site with several open items doesn't re-query the same
        // worker's assignedSite once per row.
        Map<String, String> siteByEmail = new LinkedHashMap<>();

        List<Map<String, Object>> faults = equipmentFaultRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(f -> site.equalsIgnoreCase(resolveSite(f.getWorkerEmail(), siteByEmail)))
            .map(f -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", f.getId());
                m.put("kind", "FAULT");
                m.put("workerEmail", f.getWorkerEmail());
                m.put("equipmentCode", f.getEquipmentCode());
                m.put("detail", f.getDescription());
                m.put("status", f.getStatus());
                m.put("createdAt", f.getCreatedAt());
                return m;
            })
            .collect(Collectors.toList());

        List<Map<String, Object>> maintenance = maintenanceRequestRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(r -> site.equalsIgnoreCase(resolveSite(r.getWorkerEmail(), siteByEmail)))
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("kind", "MAINTENANCE");
                m.put("workerEmail", r.getWorkerEmail());
                m.put("equipmentCode", r.getEquipmentCode());
                m.put("detail", r.getRequestDetails());
                m.put("status", r.getStatus());
                m.put("createdAt", r.getCreatedAt());
                return m;
            })
            .collect(Collectors.toList());

        List<Map<String, Object>> combined = new java.util.ArrayList<>(faults.size() + maintenance.size());
        combined.addAll(faults);
        combined.addAll(maintenance);
        combined.sort((a, b) ->
            ((java.time.LocalDateTime) b.get("createdAt")).compareTo((java.time.LocalDateTime) a.get("createdAt")));
        return combined;
    }

    @PostMapping("/api/equipment-issues/fault/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> resolveFault(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        EquipmentFault fault = equipmentFaultRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        assertSameSite(user, fault.getWorkerEmail());
        fault.setStatus("Resolved");
        equipmentFaultRepository.save(fault);
        auditLogService.record(
            "EQUIPMENT_FAULT_RESOLVED", user.role(), user.fullName(), user.email(),
            "EquipmentFault", fault.getId(), fault.getEquipmentCode()
        );
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", fault.getId());
        m.put("status", fault.getStatus());
        return m;
    }

    @PostMapping("/api/equipment-issues/maintenance/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> resolveMaintenance(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        assertSameSite(user, request.getWorkerEmail());
        request.setStatus("Resolved");
        maintenanceRequestRepository.save(request);
        auditLogService.record(
            "MAINTENANCE_RESOLVED", user.role(), user.fullName(), user.email(),
            "MaintenanceRequest", request.getId(), request.getEquipmentCode()
        );
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", request.getId());
        m.put("status", request.getStatus());
        return m;
    }

    private String resolveSite(String workerEmail, Map<String, String> cache) {
        if (workerEmail == null) return null;
        return cache.computeIfAbsent(workerEmail.toLowerCase(), key ->
            appUserRepository.findByEmailIgnoreCase(workerEmail)
                .map(AppUser::getAssignedSite)
                .orElse(null)
        );
    }

    private void assertSameSite(AuthenticatedUser user, String reporterEmail) {
        String reporterSite = appUserRepository.findByEmailIgnoreCase(reporterEmail)
            .map(AppUser::getAssignedSite)
            .orElse(null);
        if (user.assignedSite() == null || reporterSite == null ||
            !user.assignedSite().equalsIgnoreCase(reporterSite)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This item is not from your site");
        }
    }
}
