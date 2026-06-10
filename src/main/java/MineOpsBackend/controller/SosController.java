package MineOpsBackend.controller;

import MineOpsBackend.model.SosAlert;
import MineOpsBackend.repository.SosAlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SosController {

    private final SosAlertRepository sosAlertRepository;

    public SosController(SosAlertRepository sosAlertRepository) {
        this.sosAlertRepository = sosAlertRepository;
    }

    @PostMapping("/api/sos")
    public SosAlert createAlert(@RequestBody Map<String, String> request) {
        SosAlert alert = new SosAlert(
            request.getOrDefault("role", "unknown"),
            request.getOrDefault("site", "Unassigned"),
            request.getOrDefault("message", "Emergency assistance requested")
        );

        return sosAlertRepository.save(alert);
    }

    @GetMapping("/api/sos")
    public List<SosAlert> getAlerts() {
        return sosAlertRepository.findAllByOrderByCreatedAtDesc();
    }
}
