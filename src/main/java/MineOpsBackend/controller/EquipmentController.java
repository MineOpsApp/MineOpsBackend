package MineOpsBackend.controller;

import MineOpsBackend.dto.CreateEquipmentRequest;
import MineOpsBackend.dto.UpdateEquipmentStatusRequest;
import MineOpsBackend.model.Equipment;
import MineOpsBackend.repository.EquipmentRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
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
import MineOpsBackend.dto.UpdateEquipmentRegistryStatusRequest;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;
    private final AuditLogService auditLogService;

    public EquipmentController(EquipmentRepository equipmentRepository, AuditLogService auditLogService) {
        this.equipmentRepository = equipmentRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/equipment")
    @PreAuthorize("hasAnyAuthority('ROLE_WORKER','ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public List<Equipment> getEquipment(@AuthenticationPrincipal AuthenticatedUser user) {
        return equipmentRepository.findBySiteOrderByCodeAsc(user.assignedSite());
    }

    @PostMapping("/api/equipment")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Equipment createEquipment(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateEquipmentRequest request
    ) {
        if (equipmentRepository.existsByCodeIgnoreCaseAndSiteIgnoreCase(request.code(), user.assignedSite())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Equipment with this code already exists on this site");
        }

        Equipment equipment = new Equipment(
            request.code().toUpperCase().trim(),
            request.name().trim(),
            request.type(),
            user.assignedSite()
        );
        equipment.setNotes(request.notes());

        Equipment saved = equipmentRepository.save(equipment);
        auditLogService.record("EQUIPMENT_ADDED", user.role(), user.fullName(), user.email(),
            "Equipment", saved.getId(), saved.getCode() + " — " + saved.getName());

        return saved;
    }

    @PatchMapping("/api/equipment/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Equipment updateStatus(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id,
        @Valid @RequestBody UpdateEquipmentRegistryStatusRequest request
    ) {
        Equipment equipment = equipmentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        if (!equipment.getSite().equalsIgnoreCase(user.assignedSite()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Equipment belongs to a different site");

        equipment.setStatus(request.status());
        if (request.notes() != null) equipment.setNotes(request.notes());
        equipment.setUpdatedAt(LocalDateTime.now());

        Equipment saved = equipmentRepository.save(equipment);
        auditLogService.record("EQUIPMENT_STATUS_UPDATED", user.role(), user.fullName(), user.email(),
            "Equipment", id, equipment.getCode() + " → " + request.status());

        return saved;
    }
}