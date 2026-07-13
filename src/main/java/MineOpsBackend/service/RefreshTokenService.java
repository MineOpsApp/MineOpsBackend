package MineOpsBackend.service;

import MineOpsBackend.model.RefreshToken;
import MineOpsBackend.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class RefreshTokenService {

    private static final int EXPIRY_DAYS = 30;
    private final RefreshTokenRepository repo;

    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    public String generate(Long userId, String deviceName, String platform) {
        repo.deleteExpiredBefore(LocalDateTime.now());

        String raw = randomBase64();
        String hash = sha256(raw);
        RefreshToken token = new RefreshToken(userId, hash, LocalDateTime.now().plusDays(EXPIRY_DAYS));
        token.setDeviceName(deviceName);
        token.setPlatform(platform);
        token.setLastUsedAt(LocalDateTime.now());
        repo.save(token);
        return raw;
    }

    public RefreshToken validate(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = repo.findByTokenHash(hash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (Boolean.TRUE.equals(token.getRevoked())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }
        token.setLastUsedAt(LocalDateTime.now());
        repo.save(token);
        return token;
    }

    public String rotate(RefreshToken old) {
        old.setRevoked(true);
        repo.save(old);
        return generate(old.getUserId(), old.getDeviceName(), old.getPlatform());
    }

    public void revokeAll(Long userId) {
        repo.revokeAllByUserId(userId);
    }

    public List<RefreshToken> listActiveSessions(Long userId) {
        return repo.findByUserIdAndRevokedFalseAndExpiresAtAfterOrderByCreatedAtDesc(userId, LocalDateTime.now());
    }

    public void revokeOne(Long userId, Long tokenId) {
        RefreshToken token = repo.findById(tokenId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!token.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot revoke another user's session");
        }
        token.setRevoked(true);
        repo.save(token);
    }

    private String randomBase64() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
