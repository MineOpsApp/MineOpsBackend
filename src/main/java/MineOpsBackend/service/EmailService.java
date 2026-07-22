package MineOpsBackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends transactional emails (registration status, password reset, pay disbursement, etc.) via
 * Resend's HTTP API (https://api.resend.com/emails) rather than direct SMTP. Two reasons:
 * Railway blocks outbound SMTP ports below its Pro plan (this is a plain HTTPS POST instead, so
 * that's a non-issue), and any "From" address at a real gmail.com/yahoo.com/etc mailbox gets
 * silently dropped by that provider's own strict DMARC policy when relayed by anyone else's
 * servers — no third party can pass DKIM/SPF alignment for a domain they don't own. Resend's
 * default sender (onboarding@resend.dev) sidesteps that: it's Resend's own authenticated domain,
 * works immediately with no card and no custom domain to verify.
 *
 * All send methods swallow their own exceptions and log rather than throwing: a failed email
 * (bad API key, network blip) must never roll back or block the underlying business action
 * (approving a worker, disbursing pay, resetting a password) that triggered it.
 *
 * Every public method is @Async: callers (AuthController, AdminController, etc.) must not block
 * the HTTP response on the Resend call — the caller's request returns immediately and the send
 * happens on a background thread. Requires @EnableAsync on the main application class.
 *
 * Configure mineops.resend.api-key (RESEND_API_KEY env var) — get one free at resend.com, no
 * card required. With mineops.mail.enabled=false, sends are skipped and logged instead — useful
 * for local dev without a Resend key on hand.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey;
    private final String fromAddress;
    private final boolean enabled;

    public EmailService(
        @Value("${mineops.resend.api-key:}") String apiKey,
        @Value("${mineops.resend.from-address:MineOps <onboarding@resend.dev>}") String fromAddress,
        @Value("${mineops.mail.enabled:true}") boolean enabled
    ) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.enabled = enabled;
    }

    private void send(String toEmail, String subject, String htmlBody) {
        if (!enabled) {
            log.info("[EMAIL disabled] to={} subject={}", toEmail, subject);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[EMAIL skipped] no recipient address for subject={}", subject);
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[EMAIL skipped] mineops.resend.api-key not configured — to={} subject={}", toEmail, subject);
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", fromAddress);
            body.put("to", List.of(toEmail));
            body.put("subject", subject);
            body.put("html", htmlBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(RESEND_ENDPOINT)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[EMAIL sent via Resend] to={} subject={}", toEmail, subject);
            } else {
                log.error("[EMAIL FAILED] to={} subject={} status={} body={}", toEmail, subject, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("[EMAIL FAILED] to={} subject={} reason={}", toEmail, subject, e.toString());
        }
    }

    private String wrap(String title, String bodyHtml) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f2ede6;font-family:Arial,Helvetica,sans-serif;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f2ede6;padding:24px 0;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:12px;overflow:hidden;\">"
            + "<tr><td style=\"background:#8a4b1f;padding:20px 28px;\">"
            + "<span style=\"color:#ffffff;font-size:20px;font-weight:800;\">MineOps</span>"
            + "</td></tr>"
            + "<tr><td style=\"padding:28px;\">"
            + "<h2 style=\"margin:0 0 12px;color:#241b12;font-size:18px;\">" + title + "</h2>"
            + "<div style=\"color:#4a4038;font-size:14px;line-height:22px;\">" + bodyHtml + "</div>"
            + "</td></tr>"
            + "<tr><td style=\"padding:16px 28px;background:#f7f3ef;\">"
            + "<span style=\"color:#9a8f83;font-size:11px;\">This is an automated message from MineOps. Please do not reply to this email.</span>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    @Async
    public void sendRegistrationPending(String email, String fullName, String role) {
        String title = "Registration received";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Thanks for registering with MineOps as a <strong>" + safe(role) + "</strong>. "
            + "Your account is now awaiting review by a supervisor or safety officer.</p>"
            + "<p>You'll receive another email as soon as a decision is made, and you'll be able to sign in once approved.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendRegistrationApproved(String email, String fullName) {
        String title = "Your account has been approved";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Good news — your MineOps account has been approved. You can now sign in with your email and password.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendRegistrationRejected(String email, String fullName) {
        String title = "Registration not approved";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your MineOps registration was not approved by your site's supervisor or safety officer. "
            + "If you believe this is a mistake, please contact your site administrator directly.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendAccountSuspended(String email, String fullName) {
        String title = "Your account has been suspended";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your MineOps account has been suspended by your site administrator and you will not be able to sign in until it is reinstated. "
            + "Contact your supervisor or safety officer for details.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendAccountReinstated(String email, String fullName) {
        String title = "Your account has been reinstated";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your MineOps account has been reinstated. You can now sign in as usual.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendBuyerVerified(String email, String fullName) {
        String title = "Your buyer account is verified";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your buyer account has been verified. You can now access the MineOps marketplace.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendBuyerRejected(String email, String fullName) {
        String title = "Buyer application not approved";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Your buyer account application was not approved. Contact MineOps support if you have questions.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendPasswordResetOtp(String email, String fullName, String otp, int validityMinutes) {
        String title = "Your password reset code";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>Use the code below to reset your MineOps password. This code expires in " + validityMinutes + " minutes.</p>"
            + "<p style=\"font-size:28px;font-weight:800;letter-spacing:6px;color:#8a4b1f;margin:20px 0;\">" + otp + "</p>"
            + "<p>If you didn't request this, you can safely ignore this email — your password will not be changed.</p>";
        send(email, title, wrap(title, body));
    }

    @Async
    public void sendPayDisbursed(String email, String fullName, BigDecimal netPay, String momoNumber) {
        String title = "Your pay has been disbursed";
        String amount = netPay != null ? String.format("GHS %.2f", netPay) : "your pay";
        String body = "<p>Hi " + safe(fullName) + ",</p>"
            + "<p>" + amount + " has been sent to your mobile money account"
            + (momoNumber != null && !momoNumber.isBlank() ? " (" + safe(momoNumber) + ")" : "") + ".</p>"
            + "<p>Check your MoMo account to confirm receipt.</p>";
        send(email, title, wrap(title, body));
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
