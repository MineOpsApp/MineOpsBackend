package MineOpsBackend.controller;

import MineOpsBackend.dto.PreviewPayCycleRequest;
import MineOpsBackend.model.PayCycle;
import MineOpsBackend.model.ShiftLog;
import MineOpsBackend.model.WorkerPayRecord;
import MineOpsBackend.repository.PayCycleRepository;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.repository.WorkerPayRecordRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.MomoDisbursementService;
import MineOpsBackend.service.PayCalculationService;
import MineOpsBackend.util.CsvExportUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PayController {

    private final PayCalculationService calcService;
    private final PayCycleRepository payCycleRepo;
    private final WorkerPayRecordRepository payRecordRepo;
    private final ShiftLogRepository shiftLogRepo;
    private final MomoDisbursementService disbursementService;
    private final AuditLogService auditLogService;

    public PayController(
        PayCalculationService calcService,
        PayCycleRepository payCycleRepo,
        WorkerPayRecordRepository payRecordRepo,
        ShiftLogRepository shiftLogRepo,
        MomoDisbursementService disbursementService,
        AuditLogService auditLogService
    ) {
        this.calcService = calcService;
        this.payCycleRepo = payCycleRepo;
        this.payRecordRepo = payRecordRepo;
        this.shiftLogRepo = shiftLogRepo;
        this.disbursementService = disbursementService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/pay/preview")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Map<String, Object> preview(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody PreviewPayCycleRequest req
    ) {
        PayCycle cycle = calcService.preview(
            user.assignedSite(), req.periodStart(), req.periodEnd(), req.mineralType(),
            req.unit(), req.pricePerUnit(), user.email()
        );
        auditLogService.record("PAY_CYCLE_PREVIEWED", user.role(), user.fullName(), user.email(),
            "PayCycle", cycle.getId(),
            req.mineralType() + " " + req.periodStart() + "/" + req.periodEnd());
        return buildCycleDetail(cycle);
    }

    @PostMapping("/api/pay/{id}/approve-manager")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Map<String, Object> approveManager(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        PayCycle cycle = findAndCheckSite(id, user.assignedSite());

        if (!"DRAFT".equals(cycle.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Only DRAFT cycles can receive first approval (current status: " + cycle.getStatus() + ")");

        if (user.email().equalsIgnoreCase(cycle.getCreatedBy()) == false) {
            // anyone can give first approval — just record who it was
        }

        cycle.setManagerApprovedBy(user.email());
        cycle.setManagerApprovedAt(LocalDateTime.now());
        cycle.setStatus("MANAGER_APPROVED");
        payCycleRepo.save(cycle);

        auditLogService.record("PAY_CYCLE_MANAGER_APPROVED", user.role(), user.fullName(), user.email(),
            "PayCycle", id,
            cycle.getMineralType() + " " + cycle.getPeriodStart() + "/" + cycle.getPeriodEnd()
                + " GHS " + cycle.getGrossTotal());
        return buildCycleDetail(cycle);
    }

    @PostMapping("/api/pay/{id}/approve-supervisor")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Map<String, Object> approveSupervisor(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        PayCycle cycle = findAndCheckSite(id, user.assignedSite());

        if (!"MANAGER_APPROVED".equals(cycle.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cycle must have first approval before second sign-off");

        if (user.email().equalsIgnoreCase(cycle.getManagerApprovedBy()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Second approver must be a different person from the first approver");

        cycle.setSupervisorApprovedBy(user.email());
        cycle.setSupervisorApprovedAt(LocalDateTime.now());

        // Disburse each worker record
        List<WorkerPayRecord> records = payRecordRepo.findByPayCycleId(id);
        boolean anyFailed = false;
        for (WorkerPayRecord record : records) {
            try {
                disbursementService.disburse(record);
            } catch (Exception e) {
                record.setDisbursementStatus("FAILED");
                record.setFailureReason(e.getMessage());
                payRecordRepo.save(record);
                anyFailed = true;
            }
        }

        // Mark shift logs as belonging to this pay cycle
        List<ShiftLog> logs = shiftLogRepo.findUnpaidApprovedLogs(
            cycle.getSite(), cycle.getPeriodStart(), cycle.getPeriodEnd(), cycle.getMineralType());
        for (ShiftLog log : logs) {
            log.setPayCycleId(id);
        }
        shiftLogRepo.saveAll(logs);

        cycle.setStatus(anyFailed ? "FAILED" : "DISBURSED");
        payCycleRepo.save(cycle);

        auditLogService.record("PAY_CYCLE_DISBURSED", user.role(), user.fullName(), user.email(),
            "PayCycle", id,
            cycle.getMineralType() + " " + cycle.getPeriodStart() + "/" + cycle.getPeriodEnd()
                + " GHS " + cycle.getGrossTotal() + " status=" + cycle.getStatus());
        return buildCycleDetail(cycle);
    }

    @GetMapping("/api/pay/mine")
    @PreAuthorize("isAuthenticated()")
    public List<WorkerPayRecord> getMyPay(@AuthenticationPrincipal AuthenticatedUser user) {
        return payRecordRepo.findByWorkerEmailIgnoreCaseOrderByIdDesc(user.email());
    }

    @GetMapping("/api/pay/site")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public List<PayCycle> getSiteCycles(@AuthenticationPrincipal AuthenticatedUser user) {
        return payCycleRepo.findBySiteIgnoreCaseOrderByPeriodStartDesc(user.assignedSite());
    }

    @GetMapping("/api/pay/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public Map<String, Object> getCycle(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable Long id
    ) {
        PayCycle cycle = findAndCheckSite(id, user.assignedSite());
        return buildCycleDetail(cycle);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PayCycle findAndCheckSite(Long id, String site) {
        PayCycle cycle = payCycleRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pay cycle not found"));
        if (!cycle.getSite().equalsIgnoreCase(site))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pay cycle belongs to a different site");
        return cycle;
    }

    private Map<String, Object> buildCycleDetail(PayCycle cycle) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cycle", cycle);
        m.put("records", payRecordRepo.findByPayCycleId(cycle.getId()));
        return m;
    }

    @GetMapping("/api/pay/{id}/export/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR','ROLE_SAFETY_OFFICER')")
    public ResponseEntity<String> exportCsv(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        PayCycle cycle = findAndCheckSite(id, user.assignedSite());
        List<WorkerPayRecord> records = payRecordRepo.findByPayCycleId(id);
        StringBuilder csv = new StringBuilder();
        csv.append(CsvExportUtil.row("workerName", "workerEmail", "hoursWorked",
                "grossShare", "insuranceDeduction", "netPay", "momoNumber", "momoNetwork"));
        for (WorkerPayRecord r : records) {
            csv.append(CsvExportUtil.row(
                    r.getWorkerName(), r.getWorkerEmail(), r.getHoursWorked(),
                    r.getGrossShare(), r.getInsuranceDeduction(), r.getNetPay(),
                    r.getMomoNumber(), r.getMomoNetwork()));
        }
        String filename = "pay-cycle-" + cycle.getId() + ".csv";
        return CsvExportUtil.response(filename, csv.toString());
    }
}
