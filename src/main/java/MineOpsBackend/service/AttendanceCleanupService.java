package MineOpsBackend.service;

import MineOpsBackend.repository.AttendanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AttendanceCleanupService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceCleanupService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @Scheduled(fixedRate = 3600000)
public void autoClockOutStaleRecords() {
    LocalDateTime cutoff = LocalDateTime.now().minusHours(12);
    attendanceRepository.findAllBySiteNotNullAndStatus("ON_SITE").stream()
        .filter(r -> r.getClockInAt() != null && r.getClockInAt().isBefore(cutoff))
        .forEach(r -> {
            r.setStatus("OFF_SITE");
            r.setClockOutAt(LocalDateTime.now());
            r.setNotes("Auto clocked out after 12 hours");
            attendanceRepository.save(r);
        });
}
}