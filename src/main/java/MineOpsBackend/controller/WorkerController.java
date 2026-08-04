package MineOpsBackend.controller;

import MineOpsBackend.dto.LogEquipmentShiftRequest;
import MineOpsBackend.dto.ReportFaultRequest;
import MineOpsBackend.dto.RequestMaintenanceRequest;
import MineOpsBackend.dto.UpdateEquipmentStatusRequest;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.EquipmentFault;
import MineOpsBackend.model.EquipmentShiftLog;
import MineOpsBackend.model.MaintenanceRequest;
import MineOpsBackend.model.WorkerEquipment;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.EquipmentFaultRepository;
import MineOpsBackend.repository.EquipmentShiftLogRepository;
import MineOpsBackend.repository.HazardReportRepository;
import MineOpsBackend.repository.MaintenanceRequestRepository;
import MineOpsBackend.repository.WorkerEquipmentRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.NotificationService;
import MineOpsBackend.service.PushNotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class WorkerController {

    private static final Logger log = LoggerFactory.getLogger(WorkerController.class);

    private final EquipmentFaultRepository equipmentFaultRepository;
    private final EquipmentShiftLogRepository equipmentShiftLogRepository;
    private final HazardReportRepository hazardReportRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final WorkerEquipmentRepository workerEquipmentRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    public WorkerController(
        EquipmentFaultRepository equipmentFaultRepository,
        EquipmentShiftLogRepository equipmentShiftLogRepository,
        HazardReportRepository hazardReportRepository,
        MaintenanceRequestRepository maintenanceRequestRepository,
        WorkerEquipmentRepository workerEquipmentRepository,
        AppUserRepository appUserRepository,
        AuditLogService auditLogService,
        NotificationService notificationService,
        PushNotificationService pushNotificationService
    ) {
        this.equipmentFaultRepository = equipmentFaultRepository;
        this.equipmentShiftLogRepository = equipmentShiftLogRepository;
        this.hazardReportRepository = hazardReportRepository;
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.workerEquipmentRepository = workerEquipmentRepository;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Supervisors and safety officers on the worker's site — the audience for equipment
     * fault/maintenance alerts. A faulty or unmaintained tool is a safety concern, not just an
     * operations one, so safety officers must see these too.
     */
    private List<AppUser> supervisorsOnSite(String site) {
        return appUserRepository.findByAssignedSiteIgnoreCase(site).stream()
            .filter(u -> List.of("supervisor", "safetyOfficer").contains(u.getRole()))
            .filter(u -> u.getDeletedAt() == null && !Boolean.FALSE.equals(u.getActive()))
            .collect(Collectors.toList());
    }

    private void notifySupervisors(String site, String type, String title, String body, String targetType, Long targetId) {
        try {
            List<AppUser> recipients = supervisorsOnSite(site);
            List<String> tokens = recipients.stream()
                .map(AppUser::getPushToken)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
            pushNotificationService.sendToTokens(tokens, title, body, "default");
            for (AppUser recipient : recipients) {
                notificationService.notify(recipient.getEmail(), type, title, body, targetType, targetId);
            }
        } catch (Exception e) {
            log.warn("Push notification failed for {}: {}", type, e.getMessage());
        }
    }

    @GetMapping("/api/workers/me")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public Map<String, Object> getWorkerProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("fullName", user.fullName());
        profile.put("email", user.email());
        profile.put("role", user.role());
        profile.put("assignedSite", user.assignedSite());
        profile.put("assignedEquipment", workerEquipmentRepository.findByWorkerEmailIgnoreCase(user.email()));
        profile.put("submittedHazards", hazardReportRepository.findByReportedByEmailIgnoreCaseOrderByCreatedAtDesc(user.email()));
        profile.put("equipmentFaults", equipmentFaultRepository.findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(user.email()));
        profile.put("maintenanceRequests", maintenanceRequestRepository.findByWorkerEmailIgnoreCaseOrderByCreatedAtDesc(user.email()));
        profile.put("shiftLogs", equipmentShiftLogRepository.findByWorkerEmailIgnoreCaseOrderByLoggedAtDesc(user.email()));
        return profile;
    }

    @GetMapping("/api/workers/equipment/shift-logs")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public List<EquipmentShiftLog> getShiftLogs(@AuthenticationPrincipal AuthenticatedUser user) {
        return equipmentShiftLogRepository.findByWorkerEmailIgnoreCaseOrderByLoggedAtDesc(user.email());
    }

    @PostMapping("/api/workers/equipment/shift-log")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public EquipmentShiftLog logEquipmentShift(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody LogEquipmentShiftRequest request
    ) {
        EquipmentShiftLog log = equipmentShiftLogRepository.save(new EquipmentShiftLog(
            request.equipmentCode(),
            request.equipmentName(),
            user.email(),
            user.fullName(),
            request.status(),
            request.checkType(),
            request.notes()
        ));

        auditLogService.record(
            "EQUIPMENT_SHIFT_LOG",
            user.role(),
            user.fullName(),
            user.email(),
            "EquipmentShiftLog",
            log.getId(),
            request.checkType() + " — " + request.equipmentCode() + ": " + request.status()
        );

        return log;
    }

    @PatchMapping("/api/workers/equipment/status")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public WorkerEquipment updateEquipmentStatus(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody UpdateEquipmentStatusRequest request
    ) {
        Long equipmentId;
        try {
            equipmentId = Long.valueOf(request.equipmentId());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "equipmentId must be numeric");
        }

        WorkerEquipment equipment = workerEquipmentRepository.findById(equipmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));

        if (!equipment.getWorkerEmail().equalsIgnoreCase(user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another worker's equipment");
        }

        String status = (request.status() == null || request.status().isBlank())
            ? "Operational"
            : request.status();
        equipment.setStatus(status);

        WorkerEquipment saved = workerEquipmentRepository.save(equipment);
        auditLogService.record(
            "EQUIPMENT_STATUS_UPDATED",
            user.role(),
            user.fullName(),
            user.email(),
            "WorkerEquipment",
            saved.getId(),
            saved.getCode() + " set to " + saved.getStatus()
        );

        return saved;
    }

    @PostMapping("/api/workers/equipment/faults")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public EquipmentFault reportEquipmentFault(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody ReportFaultRequest request
    ) {
        EquipmentFault fault = equipmentFaultRepository.save(new EquipmentFault(
            user.email(),
            request.equipmentCode(),
            request.description()
        ));
        auditLogService.record(
            "EQUIPMENT_FAULT_REPORTED",
            user.role(),
            user.fullName(),
            user.email(),
            "EquipmentFault",
            fault.getId(),
            fault.getEquipmentCode() + ": " + fault.getDescription()
        );

        notifySupervisors(
            user.assignedSite(),
            "EQUIPMENT_FAULT",
            "Equipment fault — " + fault.getEquipmentCode(),
            user.fullName() + " reported: " + fault.getDescription(),
            "EquipmentFault",
            fault.getId()
        );

        return fault;
    }

    @PostMapping("/api/workers/equipment/maintenance")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public MaintenanceRequest requestMaintenance(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody RequestMaintenanceRequest request
    ) {
        MaintenanceRequest maintenanceRequest = maintenanceRequestRepository.save(new MaintenanceRequest(
            user.email(),
            request.equipmentCode(),
            request.requestDetails()
        ));
        auditLogService.record(
            "MAINTENANCE_REQUESTED",
            user.role(),
            user.fullName(),
            user.email(),
            "MaintenanceRequest",
            maintenanceRequest.getId(),
            maintenanceRequest.getEquipmentCode() + ": " + maintenanceRequest.getRequestDetails()
        );

        notifySupervisors(
            user.assignedSite(),
            "MAINTENANCE_REQUESTED",
            "Maintenance requested — " + maintenanceRequest.getEquipmentCode(),
            user.fullName() + " requested: " + maintenanceRequest.getRequestDetails(),
            "MaintenanceRequest",
            maintenanceRequest.getId()
        );

        return maintenanceRequest;
    }


}
