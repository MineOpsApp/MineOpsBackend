package MineOpsBackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
public class MarketController {

    private record PriceCache(List<Map<String, Object>> prices, long expiresAt) {}

    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile PriceCache cache = null;

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private static final List<Map<String, String>> COMMODITIES = List.of(
        Map.of("name", "gold",        "label", "Gold",        "unit", "oz",    "symbol", "XAU"),
        Map.of("name", "silver",      "label", "Silver",      "unit", "oz",    "symbol", "XAG"),
        Map.of("name", "copper",      "label", "Copper",      "unit", "lb",    "symbol", "HG"),
        Map.of("name", "platinum",    "label", "Platinum",    "unit", "oz",    "symbol", "XPT"),
        Map.of("name", "palladium",   "label", "Palladium",   "unit", "oz",    "symbol", "XPD"),
        Map.of("name", "crude_oil",   "label", "Crude Oil",   "unit", "bbl",   "symbol", "WTI"),
        Map.of("name", "natural_gas", "label", "Natural Gas", "unit", "MMBtu", "symbol", "NG")
    );

    public MarketController(@Value("${mineops.market.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @GetMapping("/api/market/prices")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> getPrices() {
        PriceCache snapshot = cache;
        if (snapshot != null && System.currentTimeMillis() < snapshot.expiresAt()) {
            return snapshot.prices();
        }

        List<Map<String, Object>> results = COMMODITIES.stream()
            .map(c -> CompletableFuture.supplyAsync(() -> fetchCommodity(c)))
            .collect(Collectors.toList())
            .stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        cache = new PriceCache(results, System.currentTimeMillis() + CACHE_TTL_MS);
        return results;
    }

    private Map<String, Object> fetchCommodity(Map<String, String> commodity) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.api-ninjas.com/v1/commodityprice?name=" + commodity.get("name")))
                .timeout(Duration.ofSeconds(8))
                .header("X-Api-Key", apiKey)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name",      commodity.get("label"));
                entry.put("symbol",    commodity.get("symbol"));
                entry.put("unit",      commodity.get("unit"));
                entry.put("price",     node.has("price")   ? node.get("price").asDouble()   : null);
                entry.put("change",    node.has("change")  ? node.get("change").asDouble()  : null);
                entry.put("updatedAt", node.has("updated") ? node.get("updated").asText()   : null);
                return entry;
            }
        } catch (Exception ignored) {}

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name",   commodity.get("label"));
        entry.put("symbol", commodity.get("symbol"));
        entry.put("unit",   commodity.get("unit"));
        entry.put("price",  null);
        entry.put("error",  "Unavailable");
        return entry;
    }
}
