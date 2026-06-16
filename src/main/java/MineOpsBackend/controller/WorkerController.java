package MineOpsBackend.controller;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.EquipmentFault;
import MineOpsBackend.model.MaintenanceRequest;
import MineOpsBackend.model.WorkerEquipment;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.EquipmentFaultRepository;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.repository.MaintenanceRequestRepository;
import MineOpsBackend.repository.WorkerEquipmentRepository;
import MineOpsBackend.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class WorkerController {

    private final AppUserRepository appUserRepository;
    private final EquipmentFaultRepository equipmentFaultRepository;
    private final HazardReportRepository hazardReportRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final WorkerEquipmentRepository workerEquipmentRepository;
    private final AuditLogService auditLogService;

    public WorkerController(
        AppUserRepository appUserRepository,
        EquipmentFaultRepository equipmentFaultRepository,
        HazardReportRepository hazardReportRepository,
        MaintenanceRequestRepository maintenanceRequestRepository,
        WorkerEquipmentRepository workerEquipmentRepository,
        AuditLogService auditLogService
    ) {
        this.appUserRepository = appUserRepository;
        this.equipmentFaultRepository = equipmentFaultRepository;
        this.hazardReportRepository = hazardReportRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.workerEquipmentRepository = workerEquipmentRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/workers/me")
    public Map<String, Object> getWorkerProfile(@RequestParam String email) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email).orElseThrow();
        ensureEquipment(email);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("fullName", user.getFullName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("assignedSite", "Obuasi Mine");
        profile.put("assignedZone", "Zone A");
        profile.put("assignedEquipment", workerEquipmentRepository.findByWorkerEmailIgnoreCase(email));
        profile.put("submittedHazards", hazardReportRepository.findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(email));
        profile.put("equipmentFaults", equipmentFaultRepository.findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(email));
        profile.put("maintenanceRequests", maintenanceRequestRepository.findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(email));
        profile.put("inspectionHistory", List.of(
            Map.of("title", "Daily pre-start inspection", "status", "Submitted"),
            Map.of("title", "PPE check", "status", "Completed")
        ));
        profile.put("trainingRecords", List.of(
            Map.of("title", "Site induction", "status", "Complete"),
            Map.of("title", "Hazard reporting", "status", "Complete")
        ));
        profile.put("shiftHistory", List.of(
            Map.of("title", "Morning shift", "date", "Today"),
            Map.of("title", "Day shift", "date", "Yesterday")
        ));
        profile.put("incidentInvolvementHistory", List.of(
            Map.of("title", "No active incident involvement", "status", "Clear")
        ));

        return profile;
    }

    @PatchMapping("/api/workers/equipment/status")
    public WorkerEquipment updateEquipmentStatus(@RequestBody Map<String, String> request) {
        WorkerEquipment equipment = workerEquipmentRepository.findById(Long.valueOf(request.get("equipmentId")))
            .orElseThrow();
        equipment.setStatus(request.getOrDefault("status", "Operational"));

        WorkerEquipment saved = workerEquipmentRepository.save(equipment);
        auditLogService.record(
            "EQUIPMENT_STATUS_UPDATED",
            "worker",
            request.getOrDefault("actorName", "Unknown User"),
            saved.getWorkerEmail(),
            "WorkerEquipment",
            saved.getId(),
            saved.getCode() + " set to " + saved.getStatus()
        );

        return saved;
    }

    @PostMapping("/api/workers/equipment/faults")
    public EquipmentFault reportEquipmentFault(@RequestBody Map<String, String> request) {
        EquipmentFault fault = equipmentFaultRepository.save(new EquipmentFault(
            request.getOrDefault("workerEmail", ""),
            request.getOrDefault("equipmentCode", "EQ-UNKNOWN"),
            request.getOrDefault("description", "Equipment fault reported")
        ));
        auditLogService.record(
            "EQUIPMENT_FAULT_REPORTED",
            "worker",
            request.getOrDefault("workerName", "Unknown User"),
            fault.getWorkerEmail(),
            "EquipmentFault",
            fault.getId(),
            fault.getEquipmentCode() + ": " + fault.getDescription()
        );

        return fault;
    }

    @PostMapping("/api/workers/equipment/maintenance")
    public MaintenanceRequest requestMaintenance(@RequestBody Map<String, String> request) {
        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.save(new MaintenanceRequest(
            request.getOrDefault("workerEmail", ""),
            request.getOrDefault("equipmentCode", "EQ-UNKNOWN"),
            request.getOrDefault("requestDetails", "Maintenance requested")
        ));
        auditLogService.record(
            "MAINTENANCE_REQUESTED",
            "worker",
            request.getOrDefault("workerName", "Unknown User"),
            maintenanceRequest.getWorkerEmail(),
            "MaintenanceRequest",
            maintenanceRequest.getId(),
            maintenanceRequest.getEquipmentCode() + ": " + maintenanceRequest.getRequestDetails()
        );

        return maintenanceRequest;
    }

    private void ensureEquipment(String email) {
        if (!workerEquipmentRepository.findByWorkerEmailIgnoreCase(email).isEmpty()) {
            return;
        }

        workerEquipmentRepository.save(new WorkerEquipment(
            email,
            "Excavator",
            "EX-01",
            "Operational",
            "Complete walkaround check before start. Report leaks, brake issues, or warning lights."
        ));
    }
}
