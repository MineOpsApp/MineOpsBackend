package MineOpsBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class SosController {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final List<Map<String, Object>> alerts = new ArrayList<>();

    @PostMapping("/api/sos")
    public Map<String, Object> createAlert(@RequestBody Map<String, String> request) {
        Map<String, Object> alert = new LinkedHashMap<>();

        alert.put("id", nextId.getAndIncrement());
        alert.put("role", request.getOrDefault("role", "unknown"));
        alert.put("site", request.getOrDefault("site", "Unassigned"));
        alert.put("message", request.getOrDefault("message", "Emergency assistance requested"));
        alert.put("status", "Open");
        alert.put("createdAt", LocalDateTime.now().toString());

        alerts.add(0, alert);

        return alert;
    }

    @GetMapping("/api/sos")
    public List<Map<String, Object>> getAlerts() {
        return alerts;
    }
}
