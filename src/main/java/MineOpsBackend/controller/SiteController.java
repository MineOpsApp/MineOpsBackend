package MineOpsBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SiteController {

    @GetMapping("/api/sites")
    public List<Map<String, String>> getSites() {
        return List.of(
            Map.of("name", "Obuasi Mine", "status", "Active"),
            Map.of("name", "Tarkwa Site", "status", "Active"),
            Map.of("name", "Ahafo Operation", "status", "Monitoring")
        );
    }
}
