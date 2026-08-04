package MineOpsBackend.controller;

import MineOpsBackend.model.PayCycle;
import MineOpsBackend.model.WorkerPayRecord;
import MineOpsBackend.repository.PayCycleRepository;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.repository.WorkerPayRecordRepository;
import MineOpsBackend.security.AuthenticatedUser;
import MineOpsBackend.service.AuditLogService;
import MineOpsBackend.service.MomoDisbursementService;
import MineOpsBackend.service.PayCalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayControllerTest {

    @Mock PayCalculationService calcService;
    @Mock PayCycleRepository payCycleRepo;
    @Mock WorkerPayRecordRepository payRecordRepo;
    @Mock ShiftLogRepository shiftLogRepo;
    @Mock MomoDisbursementService disbursementService;
    @Mock AuditLogService auditLogService;

    @InjectMocks PayController controller;

    private static final String SITE = "TestMine";

    private AuthenticatedUser sup(String email) {
        return new AuthenticatedUser(1L, "Supervisor", email, "supervisor", SITE, null);
    }

    private PayCycle cycleWithId(Long id, String site, String status, String managerApprovedBy) {
        PayCycle c = new PayCycle(site, "2026-06-23", "2026-06-29", "Gold", "oz",
            BigDecimal.valueOf(100), BigDecimal.TEN, BigDecimal.valueOf(1000),
            "EQUAL_PER_HEAD", "creator@mine.com");
        c.setStatus(status);
        c.setManagerApprovedBy(managerApprovedBy);
        try {
            Field f = PayCycle.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    // ── single-approver flow ──────────────────────────────────────────────────
    //
    // Maker-checker segregation (requiring a different person for the second approval) was
    // intentionally removed — see the comments on approveManager()/approveSupervisor() in
    // PayController. Many sites run with a single supervisor account, now trusted to carry a
    // pay cycle through both approvals itself.

    @Test
    void approveSupervisor_succeeds_whenSamePersonGaveFirstApproval() {
        PayCycle cycle = cycleWithId(1L, SITE, "MANAGER_APPROVED", "sup@mine.com");
        when(payCycleRepo.findById(1L)).thenReturn(Optional.of(cycle));
        when(payRecordRepo.findByPayCycleId(1L)).thenReturn(List.of());
        when(shiftLogRepo.findUnpaidApprovedLogs(any(), any(), any(), any())).thenReturn(List.of());
        when(shiftLogRepo.saveAll(any())).thenReturn(List.of());
        when(payCycleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.approveSupervisor(sup("sup@mine.com"), 1L);

        assertThat(cycle.getSupervisorApprovedBy()).isEqualTo("sup@mine.com");
        assertThat(cycle.getStatus()).isEqualTo("DISBURSED");
    }

    @Test
    void approveSupervisor_marksCycleFailed_whenDisbursementServiceThrows() {
        PayCycle cycle = cycleWithId(1L, SITE, "MANAGER_APPROVED", "sup1@mine.com");
        when(payCycleRepo.findById(1L)).thenReturn(Optional.of(cycle));

        WorkerPayRecord record = new WorkerPayRecord(1L, "w@mine.com", "Worker",
            BigDecimal.valueOf(8), BigDecimal.valueOf(500), BigDecimal.ZERO,
            BigDecimal.valueOf(500), null, null); // no MoMo number

        when(payRecordRepo.findByPayCycleId(1L)).thenReturn(List.of(record));
        doThrow(new IllegalStateException("no MoMo number")).when(disbursementService).disburse(any());
        when(shiftLogRepo.findUnpaidApprovedLogs(any(), any(), any(), any())).thenReturn(List.of());
        when(shiftLogRepo.saveAll(any())).thenReturn(List.of());
        when(payCycleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.approveSupervisor(sup("sup2@mine.com"), 1L);

        // Worker record must be marked FAILED with the reason
        assertThat(record.getDisbursementStatus()).isEqualTo("FAILED");
        assertThat(record.getFailureReason()).isEqualTo("no MoMo number");

        // Cycle itself must also be FAILED
        ArgumentCaptor<PayCycle> savedCycle = ArgumentCaptor.forClass(PayCycle.class);
        verify(payCycleRepo).save(savedCycle.capture());
        assertThat(savedCycle.getValue().getStatus()).isEqualTo("FAILED");
    }

    // ── site-scoping (findAndCheckSite) ───────────────────────────────────────

    @Test
    void getCycle_throws404_whenCycleDoesNotExist() {
        when(payCycleRepo.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getCycle(sup("sup@mine.com"), 99L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCycle_throws403_whenCycleBelongsToDifferentSite() {
        PayCycle cycle = cycleWithId(1L, "OtherMine", "DRAFT", null);
        when(payCycleRepo.findById(1L)).thenReturn(Optional.of(cycle));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.getCycle(sup("sup@mine.com"), 1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
