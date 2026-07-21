package MineOpsBackend.service;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.model.LoneWorkerSession;
import MineOpsBackend.repository.AppUserRepository;
import MineOpsBackend.repository.LoneWorkerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoneWorkerAlertService {

    private final LoneWorkerRepository loneWorkerRepo;
    private final AppUserRepository userRepo;
    private final PushNotificationService push;

    public LoneWorkerAlertService(
        LoneWorkerRepository loneWorkerRepo,
        AppUserRepository userRepo,
        PushNotificationService push
    ) {
        this.loneWorkerRepo = loneWorkerRepo;
        this.userRepo = userRepo;
        this.push = push;
    }

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void checkOverdueSessions() {
        List<LoneWorkerSession> overdue = loneWorkerRepo
            .findByActiveAndDeadlineBefore(true, LocalDateTime.now());

        for (LoneWorkerSession session : overdue) {
            if (session.isAlerted()) continue;

            List<AppUser> supervisors = userRepo
                .findByRoleInAndAssignedSiteIgnoreCase(
                    List.of("supervisor", "safetyOfficer"), session.getSite());

            List<String> tokens = new java.util.ArrayList<>();
            for (AppUser sup : supervisors) {
                String t = sup.getPushToken();
                if (t != null && !t.isBlank()) tokens.add(t);
            }

            if (!tokens.isEmpty()) {
                String lastSeen = session.getLastCheckedInAt() != null
                    ? session.getLastCheckedInAt().toString().substring(11, 16)
                    : "never";
                push.sendToTokens(
                    tokens,
                    "LONE WORKER OVERDUE",
                    session.getWorkerName() + " has not checked in — last seen " + lastSeen,
                    "lone_worker"
                );
                session.setAlerted(true);
                loneWorkerRepo.save(session);
            }
        }
    }
}
