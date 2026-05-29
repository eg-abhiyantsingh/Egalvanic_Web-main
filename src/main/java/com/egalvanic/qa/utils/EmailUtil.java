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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Email Utility — sends test report HTML files via Gmail SMTP.
 *
 * Email format matches the eGalvanic iOS Automation test report style:
 *   Subject: eGalvanic Web Automation - Test Report - yyyyMMdd_HHmmss
 *   Body: Execution Date, Browser, Platform, Attached Client Report
 *
 * Attaches Client report only (Detailed report stays internal for QA).
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
     * Send test report email with per-module Detailed Reports + Client report attached.
     * Each module gets its own zipped Detailed Report so reviewers can open just the
     * module they care about. Modules with failures are attached FIRST so they always
     * make it under the 20 MB email cap; passing modules are dropped if needed.
     *
     * @param detailedPaths        ordered list of all per-module Detailed Report paths
     * @param pathsByModule        module name → Detailed Report path (for naming attachments)
     * @param failsByModule        module name → failure count (used for prioritization)
     * @param clientReportPath     path to the client HTML report (always attached)
     * @param passed               total passed tests
     * @param failed               total failed tests
     * @param skipped              total skipped tests
     */
    public static void sendReportEmail(List<String> detailedPaths,
                                       Map<String, String> pathsByModule,
                                       Map<String, Integer> failsByModule,
                                       String clientReportPath,
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

            // Build email body — attachments are evaluated first so the body
            // can accurately reflect what's attached (HTML body is added LAST).
            Multipart multipart = new MimeMultipart();

            long totalAttachedBytes = 0L;

            // Attach client report (always small — no screenshots)
            if (clientReportPath != null) {
                File clientFile = new File(clientReportPath);
                if (clientFile.exists()) {
                    MimeBodyPart attachment = new MimeBodyPart();
                    attachment.attachFile(clientFile);
                    attachment.setFileName("Client_Report_" + reportTimestamp + ".html");
                    multipart.addBodyPart(attachment);
                    totalAttachedBytes += clientFile.length();
                    System.out.println("[Email] Attached: " + clientFile.getName()
                            + " (" + formatBytes(clientFile.length()) + ")");
                }
            }

            // Per-module Detailed Reports are NOT attached — they live as a CI artifact
            // (workflow uploads reports/detail-report/) and the email body links to them.
            // This avoids the 25 MB Gmail cap and lets us capture as many screenshots as we want.
            // Backward-compat: if ATTACH_DETAILED_REPORT=true, still ZIP-attach (fails-DESC order, 20 MB cap).
            List<String> moduleSummaries = new ArrayList<>();
            if (detailedPaths != null && !detailedPaths.isEmpty() && pathsByModule != null) {
                List<Map.Entry<String, String>> ordered = new ArrayList<>(pathsByModule.entrySet());
                ordered.sort((a, b) -> {
                    int fa = failsByModule == null ? 0 : failsByModule.getOrDefault(a.getKey(), 0);
                    int fb = failsByModule == null ? 0 : failsByModule.getOrDefault(b.getKey(), 0);
                    if (fa != fb) return Integer.compare(fb, fa); // fails-DESC
                    return a.getKey().compareTo(b.getKey());      // then alpha
                });
                for (Map.Entry<String, String> e : ordered) {
                    String module = e.getKey();
                    File html = new File(e.getValue());
                    if (!html.exists()) continue;
                    int fc = failsByModule == null ? 0 : failsByModule.getOrDefault(module, 0);

                    // Body summary line for the email
                    moduleSummaries.add(module + " &mdash; "
                            + fc + " failure" + (fc == 1 ? "" : "s") + ", "
                            + formatBytes(html.length()) + " HTML  "
                            + "<code style='font-size:11px;color:#999;'>"
                            + html.getName() + "</code>");

                    // Optional ZIP attach if explicitly enabled
                    if (AppConstants.ATTACH_DETAILED_REPORT) {
                        String safeName = module.replaceAll("[^a-zA-Z0-9_-]+", "_");
                        File zipped = zipFile(html, safeName + "_Detailed_" + reportTimestamp + ".zip");
                        if (zipped == null || !zipped.exists()) continue;
                        long zipSize = zipped.length();
                        long remaining = AppConstants.EMAIL_ATTACHMENT_MAX_BYTES - totalAttachedBytes;
                        if (zipSize <= remaining) {
                            MimeBodyPart attachment = new MimeBodyPart();
                            attachment.attachFile(zipped);
                            attachment.setFileName(zipped.getName());
                            multipart.addBodyPart(attachment);
                            totalAttachedBytes += zipSize;
                            System.out.println("[Email] Attached: " + zipped.getName()
                                    + " — module='" + module + "', fails=" + fc
                                    + ", " + formatBytes(html.length()) + " → " + formatBytes(zipSize));
                        } else {
                            System.out.println("[Email] Detailed report for module '" + module
                                    + "' over budget (" + formatBytes(zipSize)
                                    + " > " + formatBytes(remaining) + ") — not attached");
                        }
                    }
                }
            }

            // Stash for the body builder.
            System.setProperty("egalvanic.email.modulesList", String.join("|", moduleSummaries));

            // Now build the HTML body (after attachments so it can reflect their state)
            // and add it as the FIRST part of the multipart so mail clients render it.
            MimeBodyPart htmlBody = new MimeBodyPart();
            htmlBody.setContent(buildEmailBody(reportTimestamp), "text/html; charset=utf-8");
            // Add to the front by rebuilding the multipart with body first.
            MimeMultipart finalParts = new MimeMultipart();
            finalParts.addBodyPart(htmlBody);
            for (int i = 0; i < multipart.getCount(); i++) {
                finalParts.addBodyPart(multipart.getBodyPart(i));
            }

            message.setContent(finalParts);
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
     *   Please find the attached test reports:
     *   • Client Report
     */
    private static String buildEmailBody(String reportTimestamp) {
        String executionDate = new SimpleDateFormat("MMMM dd, yyyy HH:mm").format(new Date());
        String browser = capitalize(AppConstants.BROWSER);
        String platform = System.getProperty("os.name") + " " + System.getProperty("os.arch");

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
        // EXECUTION INFO — Matches iOS format
        // ============================================================
        sb.append("<div style='padding:24px 30px;'>");

        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:20px;'>");
        appendInfoRow(sb, "Execution Date", executionDate);
        appendInfoRow(sb, "Browser", browser);
        appendInfoRow(sb, "Platform", platform);
        sb.append("</table>");

        // ============================================================
        // ATTACHED REPORTS — Matches iOS format
        // ============================================================
        sb.append("<p style='color:#333;font-size:14px;margin:20px 0 8px;font-weight:500;'>");
        sb.append("Reports:</p>");
        sb.append("<ul style='color:#555;font-size:13px;margin:0 0 10px 0;padding-left:20px;line-height:1.8;'>");
        sb.append("<li><strong>Client Report</strong> (attached) &mdash; module/feature pass-fail summary</li>");

        // Per-module Detailed Reports — link to CI artifact rather than attach (saves email size,
        // gives reviewers browser-preview + indefinite retention).
        String modulesList = System.getProperty("egalvanic.email.modulesList", "");
        String repoUrl = System.getenv("GITHUB_SERVER_URL");
        String repoName = System.getenv("GITHUB_REPOSITORY");
        String runId = System.getenv("GITHUB_RUN_ID");
        if (!modulesList.isEmpty()) {
            sb.append("<li><strong>Detailed Reports (per module, with screenshots)</strong> &mdash; ");
            if (repoUrl != null && repoName != null && runId != null) {
                String artifactsUrl = repoUrl + "/" + repoName + "/actions/runs/" + runId
                        + "#artifacts";
                sb.append("download from the <a href='").append(artifactsUrl)
                        .append("'>CI run artifacts</a>:");
            } else {
                sb.append("available in local <code>reports/detail-report/</code>:");
            }
            sb.append("</li>");
            sb.append("<ul style='color:#555;font-size:12px;margin:4px 0 0 0;padding-left:24px;line-height:1.6;'>");
            for (String m : modulesList.split("\\|")) {
                if (!m.isEmpty()) {
                    sb.append("<li>").append(m).append("</li>");
                }
            }
            sb.append("</ul>");
        }
        sb.append("</ul>");

        // ============================================================
        // GITHUB ACTIONS LINK (CI only) — reuses repoUrl/repoName/runId resolved above
        // ============================================================
        if (repoUrl != null && repoName != null && runId != null) {
            String actionsUrl = repoUrl + "/" + repoName + "/actions/runs/" + runId;
            sb.append("<p style='margin:10px 0 0;'>");
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
        sb.append("Sent by <strong>eGalvanic Web Automation Framework</strong>");
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

    /**
     * Zip a single file into the same parent directory under {@code zipName}.
     * Returns the zip File, or null on failure.
     */
    private static File zipFile(File source, String zipName) {
        File zip = new File(source.getParentFile(), zipName);
        try (FileOutputStream fos = new FileOutputStream(zip);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(source)) {
            ZipEntry entry = new ZipEntry(source.getName());
            zos.putNextEntry(entry);
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) > 0) {
                zos.write(buf, 0, n);
            }
            zos.closeEntry();
            return zip;
        } catch (Exception e) {
            System.out.println("[Email] Failed to zip " + source.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Human-readable byte size: 12.4 MB / 850 KB / 421 B.
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
