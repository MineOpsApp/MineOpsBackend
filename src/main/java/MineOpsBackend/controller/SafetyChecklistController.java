package MineOpsBackend.controller;

import MineOpsBackend.dto.SubmitChecklistRequest;
import MineOpsBackend.model.SafetyChecklist;
import MineOpsBackend.repository.AttendanceRepository;
import MineOpsBackend.repository.SafetyChecklistRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class SafetyChecklistController {

    private final SafetyChecklistRepository checklistRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    public SafetyChecklistController(
        SafetyChecklistRepository checklistRepository,
        AttendanceRepository attendanceRepository,
        AuditLogService auditLogService
    ) {
        this.checklistRepository = checklistRepository;
        this.attendanceRepository = attendanceRepository;
        this.auditLogService = auditLogService;
    }

    // Worker: submit or re-submit today's checklist
    @PostMapping("/api/safety-checklist")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public SafetyChecklist submitChecklist(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody SubmitChecklistRequest req
    ) {
        LocalDate today = LocalDate.now();
        String site = user.assignedSite() != null ? user.assignedSite() : "Unassigned";

        SafetyChecklist checklist = checklistRepository
            .findByWorkerIdAndShiftDate(user.id(), today)
            .orElse(null);

        if (checklist == null) {
            checklist = new SafetyChecklist(
                user.id(), user.fullName(), user.email(), site, today,
                req.ppeHelmet(), req.ppeBoots(), req.ppeGloves(), req.ppeVest(),
                req.equipmentChecked(), req.communicationDevice(),
                req.emergencyExitsClear(), req.hazardousMaterialsSecured()
            );
        } else {
            checklist.update(
                req.ppeHelmet(), req.ppeBoots(), req.ppeGloves(), req.ppeVest(),
                req.equipmentChecked(), req.communicationDevice(),
                req.emergencyExitsClear(), req.hazardousMaterialsSecured()
            );
        }

        SafetyChecklist saved = checklistRepository.save(checklist);
        auditLogService.record(
            "SAFETY_CHECKLIST_SUBMITTED",
            user.role(), user.fullName(), user.email(),
            "SafetyChecklist", saved.getId(),
            today + " — all cleared: " + saved.isAllCleared()
        );
        return saved;
    }

    // Worker: get their own checklist for today
    @GetMapping("/api/safety-checklist/my/today")
    @PreAuthorize("hasAuthority('ROLE_WORKER')")
    public SafetyChecklist getMyToday(@AuthenticationPrincipal AuthenticatedUser user) {
        return checklistRepository
            .findByWorkerIdAndShiftDate(user.id(), LocalDate.now())
            .orElse(null);
    }

    // Supervisor: today's submissions + who on-site hasn't submitted
    @GetMapping("/api/safety-checklist/site/today")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public Map<String, Object> getSiteToday(@AuthenticationPrincipal AuthenticatedUser user) {
        String site = user.assignedSite() != null ? user.assignedSite() : "Unassigned";
        LocalDate today = LocalDate.now();

        List<SafetyChecklist> submitted = checklistRepository
            .findBySiteIgnoreCaseAndShiftDateOrderBySubmittedAtDesc(site, today);

        Set<String> submittedEmails = submitted.stream()
            .map(c -> c.getWorkerEmail().toLowerCase())
            .collect(Collectors.toSet());

        List<Map<String, String>> pending = attendanceRepository
            .findBySiteAndStatusOrderByClockInAtDesc(site, "ON_SITE")
            .stream()
            .filter(a -> !submittedEmails.contains(a.getWorkerEmail().toLowerCase()))
            .map(a -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("workerName", a.getWorkerName());
                m.put("workerEmail", a.getWorkerEmail());
                m.put("zone", a.getZone());
                return m;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today.toString());
        result.put("submitted", submitted);
        result.put("pending", pending);
        return result;
    }
}
