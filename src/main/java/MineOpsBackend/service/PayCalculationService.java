package MineOpsBackend.service;

import MineOpsBackend.model.AttendanceRecord;
import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.PayCycle;
import MineOpsBackend.model.ShiftLog;
import MineOpsBackend.model.WorkerPayRecord;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.AttendanceRepository;
import MineOpsBackend.repository.PayCycleRepository;
import MineOpsBackend.repository.PaySplitConfigRepository;
import MineOpsBackend.repository.ShiftLogRepository;
import MineOpsBackend.repository.WorkerPayRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PayCalculationService {

    private final ShiftLogRepository shiftLogRepo;
    private final PayCycleRepository payCycleRepo;
    private final WorkerPayRecordRepository payRecordRepo;
    private final PaySplitConfigRepository configRepo;
    private final AttendanceRepository attendanceRepo;
    private final AppUserRepository userRepo;

    public PayCalculationService(
        ShiftLogRepository shiftLogRepo,
        PayCycleRepository payCycleRepo,
        WorkerPayRecordRepository payRecordRepo,
        PaySplitConfigRepository configRepo,
        AttendanceRepository attendanceRepo,
        AppUserRepository userRepo
    ) {
        this.shiftLogRepo = shiftLogRepo;
        this.payCycleRepo = payCycleRepo;
        this.payRecordRepo = payRecordRepo;
        this.configRepo = configRepo;
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public PayCycle preview(String site, String payDate, String mineralType, String unit,
                            BigDecimal pricePerUnit, String createdBy) {
        // Delete any existing DRAFT for this site/date/mineral so there's never a double-preview
        payCycleRepo.findBySiteIgnoreCaseAndPayDateAndMineralTypeIgnoreCaseAndStatus(
                site, payDate, mineralType, "DRAFT")
            .ifPresent(existing -> {
                payRecordRepo.deleteAll(payRecordRepo.findByPayCycleId(existing.getId()));
                payCycleRepo.delete(existing);
            });

        List<ShiftLog> logs = shiftLogRepo.findUnpaidApprovedLogs(site, payDate, mineralType);
        if (logs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No approved, unpaid shift logs found for " + mineralType + " on " + payDate);
        }

        BigDecimal totalVolume = logs.stream()
            .map(log -> log.getVolumeExtracted())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal grossTotal = totalVolume.multiply(pricePerUnit).setScale(2, RoundingMode.HALF_UP);

        String formulaType = configRepo.findBySiteIgnoreCase(site)
            .map(c -> c.getFormulaType())
            .orElse("EQUAL_PER_HEAD");

        PayCycle cycle = new PayCycle(site, payDate, mineralType, unit,
            totalVolume, pricePerUnit, grossTotal, formulaType, createdBy);
        PayCycle saved = payCycleRepo.save(cycle);

        List<WorkerPayRecord> records = buildRecords(saved, logs, formulaType, grossTotal, payDate, site);
        payRecordRepo.saveAll(records);

        return saved;
    }

    private List<WorkerPayRecord> buildRecords(PayCycle cycle, List<ShiftLog> logs,
                                                String formulaType, BigDecimal grossTotal,
                                                String payDate, String site) {
        // Collect distinct workers
        Map<String, String> workerNames = new HashMap<>();
        for (ShiftLog log : logs) {
            workerNames.put(log.getWorkerEmail().toLowerCase(), log.getWorkerName());
        }

        Map<String, BigDecimal> shares = new HashMap<>();

        if ("WEIGHTED_BY_HOURS".equals(formulaType)) {
            Map<String, BigDecimal> workerHours = computeHours(workerNames, payDate, site);
            BigDecimal totalHours = workerHours.values().stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

            if (totalHours.compareTo(BigDecimal.ZERO) == 0) {
                // Fall back to equal split if no attendance data
                equalSplit(workerNames, grossTotal, shares);
            } else {
                for (String email : workerNames.keySet()) {
                    BigDecimal hours = workerHours.getOrDefault(email, BigDecimal.valueOf(8));
                    BigDecimal share = grossTotal.multiply(hours)
                        .divide(totalHours, 2, RoundingMode.HALF_UP);
                    shares.put(email, share);
                }
            }
        } else {
            equalSplit(workerNames, grossTotal, shares);
        }

        // Recompute hours map for record storage regardless of formula
        Map<String, BigDecimal> hoursForRecord = computeHours(workerNames, payDate, site);

        List<WorkerPayRecord> records = new ArrayList<>();
        for (Map.Entry<String, String> entry : workerNames.entrySet()) {
            String email = entry.getKey();
            String name = entry.getValue();
            BigDecimal share = shares.getOrDefault(email, BigDecimal.ZERO);
            BigDecimal hours = hoursForRecord.get(email);
            BigDecimal insurance = BigDecimal.ZERO;
            BigDecimal net = share.subtract(insurance);

            // Pull momo details from AppUser if available
            String momoNumber = null;
            String momoNetwork = null;
            AppUser user = userRepo.findByEmailIgnoreCase(email).orElse(null);
            if (user != null) {
                momoNumber = user.getMomoNumber();
                momoNetwork = user.getMomoNetwork();
            }

            records.add(new WorkerPayRecord(cycle.getId(), email, name,
                hours, share, insurance, net, momoNumber, momoNetwork));
        }
        return records;
    }

    private void equalSplit(Map<String, String> workers, BigDecimal gross, Map<String, BigDecimal> out) {
        if (workers.isEmpty()) return;
        BigDecimal share = gross.divide(
            BigDecimal.valueOf(workers.size()), 2, RoundingMode.HALF_UP);
        for (String email : workers.keySet()) {
            out.put(email, share);
        }
    }

    private Map<String, BigDecimal> computeHours(Map<String, String> workers, String payDate, String site) {
        LocalDate date = LocalDate.parse(payDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<AttendanceRecord> records =
            attendanceRepo.findBySiteIgnoreCaseAndClockInAtBetween(site, start, end);

        Map<String, BigDecimal> hoursMap = new HashMap<>();
        for (String email : workers.keySet()) {
            hoursMap.put(email, BigDecimal.valueOf(8)); // default if no record
        }
        for (AttendanceRecord ar : records) {
            if (ar.getClockOutAt() == null) continue;
            String email = ar.getWorkerEmail().toLowerCase();
            if (!workers.containsKey(email)) continue;
            double hours = java.time.Duration.between(ar.getClockInAt(), ar.getClockOutAt())
                .toMinutes() / 60.0;
            hoursMap.put(email, BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));
        }
        return hoursMap;
    }
}
