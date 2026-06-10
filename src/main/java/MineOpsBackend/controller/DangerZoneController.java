package MineOpsBackend.controller;

import MineOpsBackend.model.DangerZone;
import MineOpsBackend.repository.DangerZoneRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DangerZoneController {

    private final DangerZoneRepository dangerZoneRepository;

    public DangerZoneController(DangerZoneRepository dangerZoneRepository) {
        this.dangerZoneRepository = dangerZoneRepository;
    }

    @GetMapping("/api/danger-zones")
    public List<DangerZone> getDangerZones() {
        return dangerZoneRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/danger-zones")
    public DangerZone createDangerZone(@RequestBody Map<String, String> request) {
        DangerZone zone = new DangerZone(
            request.getOrDefault("site", "Obuasi Mine"),
            request.getOrDefault("zoneName", "Restricted Area"),
            request.getOrDefault("riskLevel", "High")
        );

        return dangerZoneRepository.save(zone);
    }
}
