package MineOpsBackend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory, per-key fixed-window rate limiter for public/unauthenticated endpoints that
 * have no existing user record to attach a lockout counter to (unlike /api/auth/login, which
 * tracks failedLoginAttempts/lockedUntil directly on AppUser). Used to throttle brute-force and
 * spam attempts on endpoints like guest code redemption and registration.
 *
 * In-memory by design: this app runs as a single small Railway instance (no evidence of horizontal
 * scaling), so a shared cache/DB-backed limiter would be unnecessary complexity. State resets on
 * restart/deploy, which is an acceptable tradeoff for this purpose — it only needs to meaningfully
 * throttle automated abuse, not provide a perfectly durable audit trail of attempts.
 */
@Service
public class RateLimitService {

    private record Window(int count, long windowStartMs) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Returns true if the call under `key` is allowed (i.e. still within `maxAttempts` for the
     * current `window`), incrementing the counter as a side effect. Returns false once the caller
     * has exceeded maxAttempts within the current window, without incrementing further.
     */
    public boolean tryAcquire(String key, int maxAttempts, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();
        Window result = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs() > windowMs) {
                return new Window(1, now);
            }
            if (existing.count() >= maxAttempts) {
                return existing; // already over the limit — don't extend the window further
            }
            return new Window(existing.count() + 1, existing.windowStartMs());
        });
        return result.count() <= maxAttempts;
    }

    /**
     * Best-effort client IP extraction. Railway (and most PaaS hosts) terminate TLS at a proxy in
     * front of the app, so the real client IP arrives in X-Forwarded-For, not getRemoteAddr()
     * (which would just be the proxy's own address). Falls back to getRemoteAddr() if the header
     * is absent (e.g. local dev).
     */
    public String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
