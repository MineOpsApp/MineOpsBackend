package MineOpsBackend.service;

import MineOpsBackend.model.AttendanceRecord;
import MineOpsBackend.model.PaySplitConfig;
import MineOpsBackend.model.ShiftLog;
import MineOpsBackend.model.WorkerPayRecord;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.AttendanceRepository;
import MineOpsBackend.repository.PayCycleRepository;
import MineOpsBackend.repository.PaySplitConfigRepository;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.repository.WorkerPayRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayCalculationServiceTest {

    @Mock ShiftLogRepository shiftLogRepo;
    @Mock PayCycleRepository payCycleRepo;
    @Mock WorkerPayRecordRepository payRecordRepo;
    @Mock PaySplitConfigRepository configRepo;
    @Mock AttendanceRepository attendanceRepo;
    @Mock AppUserRepository userRepo;

    @InjectMocks PayCalculationService service;

    private static final String SITE = "TestMine";
    private static final String P_START = "2026-06-23";
    private static final String P_END = "2026-06-29";
    private static final String MINERAL = "Gold";
    private static final String UNIT = "oz";

    private ShiftLog approvedLog(String email, String name, double volume) {
        ShiftLog l = new ShiftLog(email, name, SITE, "Z1", "DAY", MINERAL,
            BigDecimal.valueOf(volume), UNIT, null, null, null);
        l.setStatus("APPROVED");
        l.setShiftDate("2026-06-25");
        return l;
    }

    private void stubSaveWithId() {
        when(payCycleRepo.save(any())).thenAnswer(inv -> {
            Object c = inv.getArgument(0);
            Field f = c.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, 1L);
            return c;
        });
    }

    @Test
    void equalPerHead_splitsGrossTotalEvenly() {
        // 3 workers × 10 oz each = 30 oz; GHS 10/oz → GHS 300 gross → GHS 100 each
        List<ShiftLog> logs = List.of(
            approvedLog("alice@mine.com", "Alice", 10.0),
            approvedLog("bob@mine.com",   "Bob",   10.0),
            approvedLog("carol@mine.com", "Carol", 10.0)
        );
        when(shiftLogRepo.findUnpaidApprovedLogs(SITE, P_START, P_END, MINERAL)).thenReturn(logs);
        stubSaveWithId();
        // configRepo returns Optional.empty() by default → EQUAL_PER_HEAD formula
        // attendanceRepo returns empty list by default
        // userRepo returns Optional.empty() by default

        service.preview(SITE, P_START, P_END, MINERAL, UNIT, BigDecimal.valueOf(10), "creator@mine.com");

        ArgumentCaptor<List<WorkerPayRecord>> captor = ArgumentCaptor.captor();
        verify(payRecordRepo).saveAll(captor.capture());

        List<WorkerPayRecord> records = captor.getValue();
        assertThat(records).hasSize(3);
        assertThat(records).allSatisfy(r ->
            assertThat(r.getGrossShare()).isEqualByComparingTo("100.00")
        );
    }

    @Test
    void weightedByHours_splitsProportionallyToAttendance() {
        // alice 8h + bob 4h; GHS 60/oz, 10 oz each = GHS 1200 gross
        // alice share = (8/12) × 1200 = GHS 800; bob = (4/12) × 1200 = GHS 400
        List<ShiftLog> logs = List.of(
            approvedLog("alice@mine.com", "Alice", 10.0),
            approvedLog("bob@mine.com",   "Bob",   10.0)
        );
        when(shiftLogRepo.findUnpaidApprovedLogs(SITE, P_START, P_END, MINERAL)).thenReturn(logs);
        stubSaveWithId();
        when(configRepo.findBySiteIgnoreCase(SITE))
            .thenReturn(Optional.of(new PaySplitConfig(SITE, "WEIGHTED_BY_HOURS", "admin")));

        AttendanceRecord aliceAtt = new AttendanceRecord("alice@mine.com", "Alice", "worker", SITE, "Z1");
        aliceAtt.setClockInAt(LocalDateTime.of(2026, 6, 25, 8, 0));
        aliceAtt.setClockOutAt(LocalDateTime.of(2026, 6, 25, 16, 0)); // 8 h

        AttendanceRecord bobAtt = new AttendanceRecord("bob@mine.com", "Bob", "worker", SITE, "Z1");
        bobAtt.setClockInAt(LocalDateTime.of(2026, 6, 25, 8, 0));
        bobAtt.setClockOutAt(LocalDateTime.of(2026, 6, 25, 12, 0)); // 4 h

        when(attendanceRepo.findBySiteIgnoreCaseAndClockInAtBetween(any(), any(), any()))
            .thenReturn(List.of(aliceAtt, bobAtt));

        service.preview(SITE, P_START, P_END, MINERAL, UNIT, BigDecimal.valueOf(60), "creator@mine.com");

        ArgumentCaptor<List<WorkerPayRecord>> captor = ArgumentCaptor.captor();
        verify(payRecordRepo).saveAll(captor.capture());

        List<WorkerPayRecord> records = captor.getValue();
        WorkerPayRecord alice = records.stream()
            .filter(r -> r.getWorkerEmail().equals("alice@mine.com")).findFirst().orElseThrow();
        WorkerPayRecord bob = records.stream()
            .filter(r -> r.getWorkerEmail().equals("bob@mine.com")).findFirst().orElseThrow();

        assertThat(alice.getGrossShare()).isEqualByComparingTo("800.00");
        assertThat(bob.getGrossShare()).isEqualByComparingTo("400.00");
    }
}
