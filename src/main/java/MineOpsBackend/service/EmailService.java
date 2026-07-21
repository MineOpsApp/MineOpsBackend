package MineOpsBackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

/**
 * Sends transactional emails (registration status, password reset, pay disbursement, etc.).
 *
 * All send methods swallow their own exceptions and log rather than throwing: a failed email
 * (bad SMTP creds, network blip) must never roll back or block the underlying business action
 * (approving a worker, disbursing pay, resetting a password) that triggered it.
 *
 * Every public method is @Async: the actual SMTP handshake (connect, auth, TLS, transfer) can
 * take several seconds, and callers (AuthController, AdminController, etc.) must not block the
 * HTTP response on that — the caller's request returns immediately and the email goes out on a
 * background thread. Requires @EnableAsync on the main application class.
 *
 * Configure real credentials in application.properties (mineops.mail.* / spring.mail.*).
 * With mineops.mail.enabled=false, sends are skipped and logged instead — useful for local dev
 * without SMTP credentials on hand.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final boolean enabled;

    public EmailService(
        JavaMailSender mailSender,
        @Value("${mineops.mail.from-address:no-reply@mineops.app}") String fromAddress,
        @Value("${mineops.mail.from-name:MineOps}") String fromName,
        @Value("${mineops.mail.enabled:true}") boolean enabled
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EMAIL sent] to={} subject={}", toEmail, subject);
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
