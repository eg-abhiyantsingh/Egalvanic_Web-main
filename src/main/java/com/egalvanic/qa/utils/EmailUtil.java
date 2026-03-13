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
 * Email format matches the eGalvanic iOS Automation test report style:
 *   Subject: eGalvanic Web Automation - Test Report - yyyyMMdd_HHmmss
 *   Body: Execution Date, Browser, Platform, Results, Attached Reports
 *
 * Attaches Client report as HTML file.
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
     * Send test report email with report files attached.
     * Matches the eGalvanic iOS Automation email format.
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

            // SMTP configuration
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

            // Subject: matches iOS format — "eGalvanic Web Automation - Test Report - yyyyMMdd_HHmmss"
            String reportTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            message.setSubject(AppConstants.EMAIL_SUBJECT + " - " + reportTimestamp);

            // Build email body
            Multipart multipart = new MimeMultipart();

            // HTML body (matching iOS test report email format)
            MimeBodyPart htmlBody = new MimeBodyPart();
            htmlBody.setContent(
                    buildEmailBody(passed, failed, skipped, detailedReportPath, clientReportPath, reportTimestamp),
                    "text/html; charset=utf-8");
            multipart.addBodyPart(htmlBody);

            // Attach client report
            if (clientReportPath != null) {
                File clientFile = new File(clientReportPath);
                if (clientFile.exists()) {
                    MimeBodyPart attachment = new MimeBodyPart();
                    attachment.attachFile(clientFile);
                    attachment.setFileName("Client_Report_" + reportTimestamp + ".html");
                    multipart.addBodyPart(attachment);
                    System.out.println("[Email] Attached: " + clientFile.getName());
                }
            }

            // Attach detailed report
            if (detailedReportPath != null) {
                File detailedFile = new File(detailedReportPath);
                if (detailedFile.exists()) {
                    MimeBodyPart attachment = new MimeBodyPart();
                    attachment.attachFile(detailedFile);
                    attachment.setFileName("Detailed_Report_" + reportTimestamp + ".html");
                    multipart.addBodyPart(attachment);
                    System.out.println("[Email] Attached: " + detailedFile.getName());
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
     * Build HTML email body matching eGalvanic iOS Automation test report format.
     *
     * Format:
     *   eGalvanic Web Automation - Test Report
     *   Execution Date: [date/time]
     *   Browser: [browser]
     *   Platform: [platform]
     *   [Results Table]
     *   Please find the attached test reports:
     *   • Client Report
     *   • Detailed Report
     */
    private static String buildEmailBody(int passed, int failed, int skipped,
                                         String detailedPath, String clientPath,
                                         String reportTimestamp) {
        int total = passed + failed + skipped;
        String status = failed > 0 ? "SOME TESTS FAILED" : "ALL TESTS PASSED";
        String statusColor = failed > 0 ? "#dc3545" : "#28a745";
        String statusEmoji = failed > 0 ? "&#10060;" : "&#9989;";
        String executionDate = new SimpleDateFormat("MMMM dd, yyyy HH:mm").format(new Date());
        String browser = capitalize(AppConstants.BROWSER);
        String platform = System.getProperty("os.name") + " " + System.getProperty("os.arch");
        String executedBy = System.getProperty("user.name");

        // GitHub Actions info (if running in CI)
        String repoUrl = System.getenv("GITHUB_SERVER_URL");
        String repoName = System.getenv("GITHUB_REPOSITORY");
        String runId = System.getenv("GITHUB_RUN_ID");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'></head>");
        sb.append("<body style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Arial,sans-serif;");
        sb.append("background:#f5f6fa;padding:20px;margin:0;'>");

        // Main container
        sb.append("<div style='max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;");
        sb.append("box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;'>");

        // ============================================================
        // HEADER — Dark banner with report title
        // ============================================================
        sb.append("<div style='background:#2c3e50;color:#ffffff;padding:24px 30px;'>");
        sb.append("<h2 style='margin:0;font-size:20px;font-weight:600;letter-spacing:0.3px;'>");
        sb.append("eGalvanic Web Automation - Test Report</h2>");
        sb.append("</div>");

        // ============================================================
        // STATUS BANNER
        // ============================================================
        sb.append("<div style='background:").append(statusColor);
        sb.append(";color:#ffffff;padding:14px 30px;font-size:15px;font-weight:600;'>");
        sb.append(statusEmoji).append(" ").append(status);
        sb.append("</div>");

        // ============================================================
        // EXECUTION INFO — Matches iOS format
        // ============================================================
        sb.append("<div style='padding:24px 30px 0 30px;'>");

        // Execution Date
        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px;'>");
        appendInfoRow(sb, "Execution Date", executionDate);
        appendInfoRow(sb, "Browser", browser);
        appendInfoRow(sb, "Platform", platform);
        appendInfoRow(sb, "Environment", AppConstants.BASE_URL);
        appendInfoRow(sb, "Executed By", executedBy);
        sb.append("</table>");

        // ============================================================
        // RESULTS TABLE
        // ============================================================
        sb.append("<div style='background:#f8f9fa;border-radius:6px;padding:16px 20px;margin-bottom:20px;'>");
        sb.append("<h3 style='margin:0 0 12px 0;font-size:14px;color:#2c3e50;font-weight:600;'>Test Results</h3>");
        sb.append("<table style='width:100%;border-collapse:collapse;'>");

        // Total
        sb.append("<tr>");
        sb.append("<td style='padding:8px 0;color:#333;font-size:14px;'>Total Tests</td>");
        sb.append("<td style='padding:8px 0;font-weight:700;text-align:right;font-size:14px;color:#333;'>");
        sb.append(total).append("</td></tr>");

        // Passed
        sb.append("<tr>");
        sb.append("<td style='padding:8px 0;color:#28a745;font-size:14px;'>&#9989; Passed</td>");
        sb.append("<td style='padding:8px 0;font-weight:700;text-align:right;font-size:14px;color:#28a745;'>");
        sb.append(passed).append("</td></tr>");

        // Failed
        if (failed > 0) {
            sb.append("<tr>");
            sb.append("<td style='padding:8px 0;color:#dc3545;font-size:14px;'>&#10060; Failed</td>");
            sb.append("<td style='padding:8px 0;font-weight:700;text-align:right;font-size:14px;color:#dc3545;'>");
            sb.append(failed).append("</td></tr>");
        }

        // Skipped
        if (skipped > 0) {
            sb.append("<tr>");
            sb.append("<td style='padding:8px 0;color:#ffc107;font-size:14px;'>&#9888; Skipped</td>");
            sb.append("<td style='padding:8px 0;font-weight:700;text-align:right;font-size:14px;color:#ffc107;'>");
            sb.append(skipped).append("</td></tr>");
        }

        sb.append("</table>");
        sb.append("</div>"); // end results box

        // ============================================================
        // ATTACHED REPORTS — Matches iOS format
        // ============================================================
        sb.append("<p style='color:#333;font-size:14px;margin:20px 0 8px;font-weight:500;'>");
        sb.append("Please find the attached test reports:</p>");
        sb.append("<ul style='color:#555;font-size:13px;margin:0 0 20px 0;padding-left:20px;line-height:1.8;'>");
        if (clientPath != null) {
            sb.append("<li><strong>Client Report</strong> — Pass/Fail summary for stakeholders</li>");
        }
        if (detailedPath != null) {
            sb.append("<li><strong>Detailed Report</strong> — Full execution details with screenshots</li>");
        }
        sb.append("</ul>");

        // ============================================================
        // GITHUB ACTIONS LINK (CI only)
        // ============================================================
        if (repoUrl != null && repoName != null && runId != null) {
            String actionsUrl = repoUrl + "/" + repoName + "/actions/runs/" + runId;
            sb.append("<p style='margin:10px 0 20px;'>");
            sb.append("<a href='").append(actionsUrl).append("' ");
            sb.append("style='display:inline-block;background:#007bff;color:#ffffff;");
            sb.append("padding:10px 20px;border-radius:5px;text-decoration:none;font-size:13px;font-weight:500;'>");
            sb.append("View Full Run on GitHub Actions</a></p>");
        }

        sb.append("</div>"); // end content padding

        // ============================================================
        // FOOTER
        // ============================================================
        sb.append("<div style='background:#f8f9fa;padding:16px 30px;border-top:1px solid #e9ecef;");
        sb.append("font-size:11px;color:#999;text-align:center;'>");
        sb.append("Sent by <strong>eGalvanic Web Automation Framework</strong><br>");
        sb.append(AppConstants.BASE_URL);
        sb.append("</div>");

        sb.append("</div>"); // end main container
        sb.append("</body></html>");

        return sb.toString();
    }

    /**
     * Append an info row to the execution details table.
     */
    private static void appendInfoRow(StringBuilder sb, String label, String value) {
        sb.append("<tr>");
        sb.append("<td style='padding:6px 0;color:#666;font-size:13px;width:130px;vertical-align:top;'>");
        sb.append("<strong>").append(label).append(":</strong></td>");
        sb.append("<td style='padding:6px 0;color:#333;font-size:13px;'>");
        sb.append(value != null ? value : "—").append("</td>");
        sb.append("</tr>");
    }

    /**
     * Capitalize first letter of a string.
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
