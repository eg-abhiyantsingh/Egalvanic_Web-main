package com.egalvanic.qa.utils;

import com.egalvanic.qa.constants.AppConstants;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Email Utility — sends test report HTML files via Gmail SMTP.
 *
 * Attaches both Detailed (QA) and Client reports as HTML files.
 * Controlled by AppConstants.SEND_EMAIL_ENABLED flag.
 *
 * Gmail requires an App Password (not your regular password).
 * Generate one at: https://myaccount.google.com/apppasswords
 *
 * For CI: pass credentials via GitHub Secrets:
 *   EMAIL_PASSWORD → repository secret
 *   SEND_EMAIL_ENABLED=true → env variable
 */
public class EmailUtil {

    private EmailUtil() {}

    /**
     * Send test report email with both report files attached.
     *
     * @param detailedReportPath path to the detailed HTML report
     * @param clientReportPath   path to the client HTML report
     * @param passed             number of passed tests
     * @param failed             number of failed tests
     * @param skipped            number of skipped tests
     */
    public static void sendReportEmail(String detailedReportPath, String clientReportPath,
                                       int passed, int failed, int skipped) {
        if (!AppConstants.SEND_EMAIL_ENABLED) {
            System.out.println("[Email] Email sending is disabled (SEND_EMAIL_ENABLED=false)");
            return;
        }

        String password = AppConstants.EMAIL_PASSWORD;
        if (password == null || password.isEmpty()) {
            System.out.println("[Email] EMAIL_PASSWORD not set — skipping email");
            return;
        }

        try {
            System.out.println("[Email] Preparing to send report email...");

            Properties props = new Properties();
            props.put("mail.smtp.host", AppConstants.SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(AppConstants.SMTP_PORT));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(AppConstants.EMAIL_FROM, password);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(AppConstants.EMAIL_FROM));

            // Parse comma-separated recipients
            String[] recipients = AppConstants.EMAIL_TO.split("\\s*,\\s*");
            for (String recipient : recipients) {
                String trimmed = recipient.trim();
                if (!trimmed.isEmpty()) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(trimmed));
                }
            }

            // Subject with date and pass/fail summary
            int total = passed + failed + skipped;
            String status = failed > 0 ? "FAILED" : "PASSED";
            String dateStr = new SimpleDateFormat("dd MMM yyyy").format(new Date());
            message.setSubject(AppConstants.EMAIL_SUBJECT
                    + " — " + status + " (" + passed + "/" + total + ") — " + dateStr);

            // Build email body
            Multipart multipart = new MimeMultipart();

            // HTML body
            MimeBodyPart htmlBody = new MimeBodyPart();
            htmlBody.setContent(buildEmailBody(passed, failed, skipped, detailedReportPath, clientReportPath),
                    "text/html; charset=utf-8");
            multipart.addBodyPart(htmlBody);

            // Attach client report only (detail report is for internal QA, not emailed)
            if (clientReportPath != null) {
                File clientFile = new File(clientReportPath);
                if (clientFile.exists()) {
                    MimeBodyPart attachment = new MimeBodyPart();
                    attachment.attachFile(clientFile);
                    attachment.setFileName("Client_Report_" + dateStr.replace(" ", "_") + ".html");
                    multipart.addBodyPart(attachment);
                    System.out.println("[Email] Attached: " + clientFile.getName());
                }
            }

            message.setContent(multipart);
            message.setSentDate(new Date());

            // Send
            Transport.send(message);

            System.out.println("[Email] Report email sent successfully to: " + AppConstants.EMAIL_TO);

        } catch (Exception e) {
            System.out.println("[Email] Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build a clean HTML email body with test summary.
     */
    private static String buildEmailBody(int passed, int failed, int skipped,
                                         String detailedPath, String clientPath) {
        int total = passed + failed + skipped;
        String status = failed > 0 ? "SOME TESTS FAILED" : "ALL TESTS PASSED";
        String statusColor = failed > 0 ? "#dc3545" : "#28a745";
        String dateStr = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' h:mm a").format(new Date());

        // GitHub repo link (if running in CI)
        String repoUrl = System.getenv("GITHUB_SERVER_URL");
        String repoName = System.getenv("GITHUB_REPOSITORY");
        String runId = System.getenv("GITHUB_RUN_ID");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'></head><body ")
          .append("style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Arial,sans-serif;")
          .append("background:#f5f6fa;padding:20px;'>");

        // Header
        sb.append("<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:8px;")
          .append("box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;'>");
        sb.append("<div style='background:#2c3e50;color:#fff;padding:20px 30px;'>")
          .append("<h2 style='margin:0;font-size:20px;'>eGalvanic Web Automation</h2>")
          .append("<p style='margin:5px 0 0;opacity:0.8;font-size:13px;'>").append(dateStr).append("</p>")
          .append("</div>");

        // Status banner
        sb.append("<div style='background:").append(statusColor)
          .append(";color:#fff;padding:15px 30px;font-size:16px;font-weight:600;'>")
          .append(failed > 0 ? "&#10060; " : "&#9989; ").append(status)
          .append("</div>");

        // Results table
        sb.append("<div style='padding:25px 30px;'>");
        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px;'>");
        sb.append("<tr><td style='padding:8px 0;color:#666;'>Total Tests</td>")
          .append("<td style='padding:8px 0;font-weight:600;text-align:right;'>").append(total).append("</td></tr>");
        sb.append("<tr><td style='padding:8px 0;color:#28a745;'>Passed</td>")
          .append("<td style='padding:8px 0;font-weight:600;text-align:right;color:#28a745;'>").append(passed).append("</td></tr>");
        if (failed > 0) {
            sb.append("<tr><td style='padding:8px 0;color:#dc3545;'>Failed</td>")
              .append("<td style='padding:8px 0;font-weight:600;text-align:right;color:#dc3545;'>").append(failed).append("</td></tr>");
        }
        if (skipped > 0) {
            sb.append("<tr><td style='padding:8px 0;color:#ffc107;'>Skipped</td>")
              .append("<td style='padding:8px 0;font-weight:600;text-align:right;color:#ffc107;'>").append(skipped).append("</td></tr>");
        }
        sb.append("</table>");

        // Attachments note
        sb.append("<p style='color:#666;font-size:13px;margin:15px 0 5px;'>")
          .append("<strong>Attached Report:</strong></p>");
        sb.append("<ul style='color:#666;font-size:13px;margin:0;padding-left:20px;'>");
        if (clientPath != null) sb.append("<li>Client Report — pass/fail summary</li>");
        sb.append("</ul>");

        // GitHub Actions link (if in CI)
        if (repoUrl != null && repoName != null && runId != null) {
            String actionsUrl = repoUrl + "/" + repoName + "/actions/runs/" + runId;
            sb.append("<p style='margin:20px 0 0;'><a href='").append(actionsUrl)
              .append("' style='display:inline-block;background:#007bff;color:#fff;")
              .append("padding:10px 20px;border-radius:5px;text-decoration:none;font-size:13px;'>")
              .append("View Full Run on GitHub Actions</a></p>");
        }

        sb.append("</div>");

        // Footer
        sb.append("<div style='background:#f8f9fa;padding:15px 30px;border-top:1px solid #e9ecef;")
          .append("font-size:11px;color:#999;'>")
          .append("Sent by eGalvanic Web Automation Framework &bull; ")
          .append(AppConstants.BASE_URL)
          .append("</div>");

        sb.append("</div></body></html>");
        return sb.toString();
    }
}
