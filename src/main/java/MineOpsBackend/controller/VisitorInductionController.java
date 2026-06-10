package MineOpsBackend.controller;

import MineOpsBackend.model.VisitorInduction;
import MineOpsBackend.repository.VisitorInductionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class VisitorInductionController {

    private final VisitorInductionRepository visitorInductionRepository;

    public VisitorInductionController(VisitorInductionRepository visitorInductionRepository) {
        this.visitorInductionRepository = visitorInductionRepository;
    }

    @GetMapping("/api/inductions")
    public List<VisitorInduction> getInductions() {
        return visitorInductionRepository.findAllByOrderByCompletedAtDesc();
    }

    @PostMapping("/api/inductions")
    public VisitorInduction completeInduction(@RequestBody Map<String, String> request) {
        VisitorInduction induction = new VisitorInduction(
            request.getOrDefault("visitorType", "Guest"),
            request.getOrDefault("site", "Obuasi Mine")
        );

        return visitorInductionRepository.save(induction);
    }
}
