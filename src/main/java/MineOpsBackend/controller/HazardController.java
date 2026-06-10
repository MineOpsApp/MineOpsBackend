package MineOpsBackend.controller;

import MineOpsBackend.model.HazardReport;
import MineOpsBackend.repository.HazardReportRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class HazardController {

    private final HazardReportRepository hazardReportRepository;

    public HazardController(HazardReportRepository hazardReportRepository) {
        this.hazardReportRepository = hazardReportRepository;
    }

    @GetMapping("/api/hazards")
    public List<HazardReport> getHazards() {
        return hazardReportRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/hazards")
    public HazardReport createHazard(@RequestBody Map<String, String> request) {
        HazardReport report = new HazardReport(
            request.getOrDefault("reportedByRole", "worker"),
            request.getOrDefault("site", "Obuasi Mine"),
            request.getOrDefault("description", "Hazard reported from MineOps app")
        );

        return hazardReportRepository.save(report);
    }

    @PatchMapping("/api/hazards/{id}/close")
    public HazardReport closeHazard(@PathVariable Long id) {
        HazardReport report = hazardReportRepository.findById(id).orElseThrow();
        report.setStatus("Closed");

        return hazardReportRepository.save(report);
    }
}
