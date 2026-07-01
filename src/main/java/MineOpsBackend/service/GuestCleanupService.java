package MineOpsBackend.service;

import MineOpsBackend.model.AppUser;
import MineOpsBackend.repository.AppUserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GuestCleanupService {

    private final AppUserRepository appUserRepository;

    public GuestCleanupService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void deleteExpiredGuestAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<AppUser> stale = appUserRepository.findByRoleAndSessionExpiresAtBefore("guest", cutoff);
        if (!stale.isEmpty()) {
            appUserRepository.deleteAll(stale);
        }
    }
}
