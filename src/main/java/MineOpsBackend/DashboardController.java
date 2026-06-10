package MineOpsBackend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    @GetMapping("/api/dashboard")
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        dashboard.put("siteCount", 3);
        dashboard.put("equipmentCount", 18);
        dashboard.put("activeEquipment", 14);
        dashboard.put("openInspections", 5);
        dashboard.put("overdueMaintenance", 2);
        dashboard.put("alerts", List.of(
            Map.of("title", "Dump Truck DT-12 maintenance due", "level", "Warning"),
            Map.of("title", "Ahafo Operation inspection pending", "level", "Attention"),
            Map.of("title", "Drill Rig DR-04 operating normally", "level", "Stable")
        ));
        dashboard.put("recentActivity", List.of(
            Map.of("title", "Obuasi Mine shift report submitted", "time", "08:45"),
            Map.of("title", "Tarkwa Site equipment check completed", "time", "09:10"),
            Map.of("title", "Ahafo Operation fuel log updated", "time", "09:35")
        ));

        return dashboard;
    }
}
