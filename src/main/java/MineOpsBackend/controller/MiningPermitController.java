package MineOpsBackend.controller;

import MineOpsBackend.model.MiningPermitStatus;
import MineOpsBackend.repository.MiningPermitStatusRepository;
import MineOpsBackend.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/permits")
public class MiningPermitController {

    private final MiningPermitStatusRepository permitRepo;

    public MiningPermitController(MiningPermitStatusRepository permitRepo) {
        this.permitRepo = permitRepo;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MiningPermitStatus getMyPermit(@AuthenticationPrincipal AuthenticatedUser user) {
        return permitRepo.findBySiteIgnoreCase(user.assignedSite()).orElseGet(() -> {
            MiningPermitStatus blank = new MiningPermitStatus();
            blank.setSite(user.assignedSite());
            return blank;
        });
    }

    @PatchMapping("/mine")
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    public MiningPermitStatus updateMyPermit(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody Map<String, Object> body
    ) {
        MiningPermitStatus permit = permitRepo.findBySiteIgnoreCase(user.assignedSite())
            .orElseGet(() -> {
                MiningPermitStatus p = new MiningPermitStatus();
                p.setSite(user.assignedSite());
                return p;
            });

        if (body.containsKey("applicationSubmitted"))
            permit.setApplicationSubmitted(Boolean.parseBoolean(String.valueOf(body.get("applicationSubmitted"))));
        if (body.containsKey("communityNotificationDone"))
            permit.setCommunityNotificationDone(Boolean.parseBoolean(String.valueOf(body.get("communityNotificationDone"))));
        if (body.containsKey("ministerialReviewStatus"))
            permit.setMinisterialReviewStatus((String) body.get("ministerialReviewStatus"));
        if (body.containsKey("epaPermitObtained"))
            permit.setEpaPermitObtained(Boolean.parseBoolean(String.valueOf(body.get("epaPermitObtained"))));
        if (body.containsKey("photoData"))
            permit.setPhotoData((String) body.get("photoData"));

        permit.setUpdatedByEmail(user.email());
        permit.setUpdatedAt(LocalDateTime.now());
        return permitRepo.save(permit);
    }
}
