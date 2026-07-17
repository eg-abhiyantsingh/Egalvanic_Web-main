package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Sentry 502 correlation</b> (Parallel Suite 3): pulls the org's <b>unresolved 502 issues from
 * the last 24h</b> straight from Sentry's REST API — the inside view that pairs with Suite 3's
 * outside probes (all-pages API sweep + browser screenshot sweep). A 502 the sweeps miss because it
 * fired between runs still lands here, with the Sentry permalink for one-click triage.
 *
 * <p><b>Auth:</b> {@code SENTRY_AUTH_TOKEN} env var / {@code -Dsentry.auth.token} (org-level token,
 * scopes {@code org:read, project:read, event:read}). Absent → SKIP with setup instructions (the
 * rest of Suite 3 is unaffected). Org/projects default to the eGalvanic Sentry org and are
 * overridable via {@code SENTRY_ORG} / {@code SENTRY_PROJECT_IDS} (comma-separated).</p>
 *
 * <p>Report-mode: findings → {@code reports/sentry-502-report.md} + the consolidated findings
 * dashboard. {@code -DSTRICT_SENTRY_502=true} hard-fails when unresolved 502s exist.</p>
 */
public class Sentry502CorrelationApiTest extends BaseAPITest {

    private static final String MODULE = "API — Sentry 502 Correlation";

    private static final String SENTRY_API = "https://sentry.io/api/0";
    private static final String ORG = System.getProperty("sentry.org",
            System.getenv().getOrDefault("SENTRY_ORG", "egalvanic-yb"));
    /** Project ids from the team's saved Sentry view (frontend + backends). */
    private static final String PROJECT_IDS = System.getProperty("sentry.project.ids",
            System.getenv().getOrDefault("SENTRY_PROJECT_IDS",
                    "4509671301644289,4510345735766016,4510436760748032,4510464365101056"));
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_SENTRY_502",
                    System.getenv().getOrDefault("STRICT_SENTRY_502", "false")));

    private final List<String[]> rows = new ArrayList<>();
    private String skippedReason;

    private static String token() {
        String t = System.getProperty("sentry.auth.token", System.getenv("SENTRY_AUTH_TOKEN"));
        return (t == null || t.trim().isEmpty()) ? null : t.trim();
    }

    @Test(description = "Pull unresolved 502 issues (last 24h) from Sentry and correlate with Suite-3 sweeps")
    public void unresolved502IssuesLast24h() {
        ExtentReportManager.createTest(MODULE, "sentry", "SENTRY_502_LAST_24H");
        String tok = token();
        if (tok == null) {
            skippedReason = "SENTRY_AUTH_TOKEN not set. Create an org auth token in Sentry "
                    + "(Settings > Auth Tokens; scopes org:read, project:read, event:read) and add it as "
                    + "a repo secret SENTRY_AUTH_TOKEN (CI) or export it locally.";
            ExtentReportManager.logWarning("SKIPPED — " + skippedReason);
            throw new SkipException(skippedReason);
        }

        StringBuilder url = new StringBuilder(SENTRY_API + "/organizations/" + ORG + "/issues/?");
        for (String pid : PROJECT_IDS.split(",")) {
            if (!pid.trim().isEmpty()) url.append("project=").append(pid.trim()).append("&");
        }
        url.append("query=").append(java.net.URLEncoder.encode("is:unresolved 502", java.nio.charset.StandardCharsets.UTF_8))
           .append("&statsPeriod=24h&sort=date&limit=50");

        Response r = RestAssured.given().relaxedHTTPSValidation()
                .header("Authorization", "Bearer " + tok)
                .when().get(url.toString())
                .then().extract().response();

        if (r.statusCode() == 401 || r.statusCode() == 403) {
            skippedReason = "Sentry token rejected (HTTP " + r.statusCode() + ") — check token scopes "
                    + "(org:read, project:read, event:read) and org slug '" + ORG + "'.";
            ExtentReportManager.logWarning("SKIPPED — " + skippedReason);
            throw new SkipException(skippedReason);
        }
        org.testng.Assert.assertEquals(r.statusCode(), 200,
                "Sentry issues API should answer 200; got " + r.statusCode() + ": "
                + r.asString().substring(0, Math.min(200, r.asString().length())));

        JSONArray issues = new JSONArray(r.asString());
        for (int i = 0; i < issues.length(); i++) {
            JSONObject is = issues.getJSONObject(i);
            rows.add(new String[]{
                    is.optString("shortId", "?"),
                    is.optString("title", "?").replace("|", "/"),
                    is.optString("count", "?"),
                    is.optString("userCount", "0") + " users",
                    is.optString("lastSeen", "?"),
                    is.optJSONObject("project") != null ? is.getJSONObject("project").optString("slug", "?") : "?",
                    is.optString("permalink", "-"),
            });
        }
        String summary = issues.length() + " unresolved 502 issue(s) in Sentry (last 24h, org " + ORG + ").";
        System.out.println("[Sentry502] " + summary);
        for (String[] row : rows) System.out.println("[Sentry502]   " + row[0] + " " + row[1] + " count=" + row[2]);

        if (issues.length() == 0) {
            ExtentReportManager.logPass("Sentry clean: " + summary);
            return;
        }
        String msg = summary + " See reports/sentry-502-report.md for permalinks — correlate with the "
                + "all-pages sweep + screenshot sweep in this run.";
        if (STRICT) { ExtentReportManager.logFail(msg); org.testng.Assert.fail(msg); }
        else ExtentReportManager.logWarning(msg + "  (reported; -DSTRICT_SENTRY_502=true enforces)");
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Sentry 502 Correlation (last 24h)\n\n");
        md.append("Unresolved 502 issues pulled from Sentry (org `").append(ORG).append("`) — the inside ")
          .append("view pairing with Suite 3's outside 502 probes.\n\n");
        if (skippedReason != null) {
            md.append("**SKIPPED:** ").append(skippedReason).append("\n");
        } else if (rows.isEmpty()) {
            md.append("**No unresolved 502 issues in Sentry for the last 24h.**\n");
        } else {
            md.append("| Sentry ID | Title | Events | Users | Last seen | Project | Link |\n")
              .append("|---|---|---|---|---|---|---|\n");
            for (String[] r : rows) md.append("| ").append(String.join(" | ", r)).append(" |\n");
            md.append("\n**").append(rows.size()).append(" unresolved 502 issue(s)")
              .append(STRICT ? " (STRICT: enforced)" : " (reported)").append(".**\n");
        }
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/sentry-502-report.md")) { w.write(md.toString()); }
            System.out.println("[Sentry502] Report → reports/sentry-502-report.md");
        } catch (Exception e) { System.out.println("[Sentry502] report write failed: " + e.getMessage()); }
    }
}
