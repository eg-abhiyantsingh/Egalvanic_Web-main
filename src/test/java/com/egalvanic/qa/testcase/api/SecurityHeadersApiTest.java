package com.egalvanic.qa.testcase.api;

import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.response.Response;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <b>Transport-contract audit</b> — headers, content types and CORS on the API surface.
 *
 * <p>Suite 3's other classes check availability, reachability, pagination and error
 * statuses. This class checks what travels WITH every response — spec-driven from
 * {@code /api/swagger.json}, strictly GET-only (zero mutation risk):</p>
 * <ol>
 *   <li><b>Content-Type contract:</b> a 2xx from an API operation must be
 *       {@code application/json} with a JSON body. This host's known systemic gap is the
 *       SPA catch-all answering API paths with 200 + HTML — clients parse the app shell
 *       as data and fail far from the cause. Counted and reported per endpoint.</li>
 *   <li><b>Security headers:</b> {@code X-Content-Type-Options: nosniff},
 *       {@code Strict-Transport-Security}, and a no-store/no-cache {@code Cache-Control}
 *       on authenticated JSON (tenant data must not land in shared caches).</li>
 *   <li><b>CORS:</b> an arbitrary evil {@code Origin} must not be reflected in
 *       {@code Access-Control-Allow-Origin} together with
 *       {@code Access-Control-Allow-Credentials: true} — that combination lets any
 *       website read this API with the victim's cookies (hard fail). Reflection
 *       without credentials is reported as a warning.</li>
 *   <li><b>Version leakage:</b> {@code Server} / {@code X-Powered-By} headers carrying
 *       version numbers hand attackers a CVE shopping list — reported.</li>
 * </ol>
 *
 * <p><b>Failure policy:</b> credentialed CORS reflection is the only hard fail in
 * report mode (it is an objective, exploitable defect). Header/content-type gaps go to
 * {@code reports/security-headers-report.md}; set {@code -DSTRICT_SECURITY_CONTRACT=true}
 * to escalate them once the backend complies.</p>
 */
public class SecurityHeadersApiTest extends BaseAPITest {

    private static final int SAMPLE_OPS = 30;   // parameterless catalog GETs sampled
    private static final String EVIL_ORIGIN = "https://evil.attacker-egalvanic.example";
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_SECURITY_CONTRACT",
                    System.getenv().getOrDefault("STRICT_SECURITY_CONTRACT", "false")));

    private static final String MODULE = "API — Transport Contract";

    private static final List<String[]> REPORT = Collections.synchronizedList(new ArrayList<>());

    /** Parameterless catalog GET paths (no {id} slots — safe to call verbatim). */
    private final List<String> plainGetPaths = new ArrayList<>();

    // ── catalog resolution (same pattern as ErrorContractApiTest) ────────────

    @BeforeClass(alwaysRun = true)
    public void loadCatalog() {
        try {
            Response spec = getAuthenticatedRequestSpec().when().get("/swagger.json").then().extract().response();
            if (spec.statusCode() != 200) return;
            JSONObject paths = new JSONObject(spec.asString()).getJSONObject("paths");
            for (Iterator<String> it = paths.keys(); it.hasNext(); ) {
                String p = it.next();
                if (paths.getJSONObject(p).has("get") && !p.contains("{")) {
                    plainGetPaths.add(p.replaceFirst("^/api", ""));
                }
            }
            Collections.sort(plainGetPaths); // deterministic sampling run-to-run
            System.out.println("[TransportContract] parameterless catalog GET ops: " + plainGetPaths.size());
        } catch (Exception e) {
            System.out.println("[TransportContract] swagger load failed: " + e.getMessage());
        }
    }

    /** Every k-th parameterless GET op — bounded, deterministic sample. */
    private List<String> samplePaths() {
        List<String> out = new ArrayList<>();
        if (plainGetPaths.isEmpty()) return out;
        int step = Math.max(1, plainGetPaths.size() / SAMPLE_OPS);
        for (int i = 0; i < plainGetPaths.size() && out.size() < SAMPLE_OPS; i += step) out.add(plainGetPaths.get(i));
        return out;
    }

    private static boolean isJsonBody(String body) {
        String t = body == null ? "" : body.trim();
        return t.startsWith("{") || t.startsWith("[");
    }

    // ── 1. Content-Type contract: 2xx must be JSON, never the SPA shell ─────

    @Test(description = "Transport contract: 2xx API responses are application/json with a JSON body (not the SPA HTML shell)")
    public void testContentTypeContract() {
        ExtentReportManager.createTest(MODULE, "Content-Type contract", "SEC_CONTENT_TYPE");
        if (!hasAuthToken()) throw new SkipException("No API auth token.");
        List<String> sample = samplePaths();
        if (sample.isEmpty()) throw new SkipException("Swagger catalog unavailable — cannot sample.");

        List<String> htmlLeaks = new ArrayList<>(); List<String> crashes = new ArrayList<>();
        int jsonOk = 0, non2xx = 0;
        for (String p : sample) {
            try {
                Response r = getAuthenticatedRequestSpec().when().get(p).then().extract().response();
                int c = r.statusCode();
                if (c >= 500) { crashes.add(p + " → " + c); continue; }
                if (c < 200 || c >= 300) { non2xx++; continue; }
                String ct = r.getContentType() == null ? "" : r.getContentType();
                if (ct.contains("application/json") && isJsonBody(r.asString())) jsonOk++;
                else htmlLeaks.add(p + " → 200 " + (ct.isEmpty() ? "(no content-type)" : ct));
            } catch (Exception e) { crashes.add(p + " → " + e.getClass().getSimpleName()); }
        }
        String summary = sample.size() + " ops: json-2xx=" + jsonOk + " non-2xx=" + non2xx
                + " html-or-mislabeled-2xx=" + htmlLeaks.size() + " 5xx=" + crashes.size();
        REPORT.add(new String[]{"content-type",
                crashes.isEmpty() && htmlLeaks.isEmpty() ? "OK" : (crashes.isEmpty() ? "SOFT-GAP" : "FAIL"), summary});
        Assert.assertTrue(crashes.isEmpty(), "5xx during content-type sweep (objective defect): " + crashes);
        if (STRICT) Assert.assertTrue(htmlLeaks.isEmpty(),
                "API 2xx responses that are not JSON (SPA catch-all swallowing API routes): " + htmlLeaks);
        else if (!htmlLeaks.isEmpty()) ExtentReportManager.logWarning(htmlLeaks.size()
                + "/" + sample.size() + " op(s) answered 2xx without JSON — clients parse the app shell as data: "
                + htmlLeaks);
        ExtentReportManager.logPass("Content-Type sweep: " + summary + ".");
    }

    // ── 2. Security headers on authenticated JSON ────────────────────────────

    @Test(description = "Transport contract: nosniff / HSTS / no-store cache headers on authenticated JSON")
    public void testSecurityHeaders() {
        ExtentReportManager.createTest(MODULE, "Security headers", "SEC_HEADERS");
        if (!hasAuthToken()) throw new SkipException("No API auth token.");

        // Panel: known-JSON authed endpoints + first sampled catalog ops that return JSON.
        List<String> panel = new ArrayList<>();
        panel.add("/auth/v2/me");
        panel.add("/health");
        for (String p : samplePaths()) { if (panel.size() >= 8) break; if (!panel.contains(p)) panel.add(p); }

        List<String> noSniff = new ArrayList<>(); List<String> noHsts = new ArrayList<>();
        List<String> cacheable = new ArrayList<>(); int checked = 0;
        for (String p : panel) {
            try {
                Response r = getAuthenticatedRequestSpec().when().get(p).then().extract().response();
                if (r.statusCode() != 200 || !isJsonBody(r.asString())) continue;
                checked++;
                String xcto = r.getHeader("X-Content-Type-Options");
                if (xcto == null || !xcto.toLowerCase().contains("nosniff")) noSniff.add(p);
                if (r.getHeader("Strict-Transport-Security") == null) noHsts.add(p);
                String cc = r.getHeader("Cache-Control");
                String ccl = cc == null ? "" : cc.toLowerCase();
                // tenant JSON must not be cacheable by shared caches
                if (!ccl.contains("no-store") && !ccl.contains("no-cache") && !ccl.contains("private")) {
                    cacheable.add(p + " → Cache-Control: " + (cc == null ? "(absent)" : cc));
                }
            } catch (Exception e) {
                System.out.println("[TransportContract] header probe failed on " + p + ": " + e.getMessage());
            }
        }
        if (checked == 0) throw new SkipException("No authenticated JSON 200s reachable — cannot audit headers.");
        String summary = checked + " JSON endpoints: nosniff-missing=" + noSniff.size()
                + " hsts-missing=" + noHsts.size() + " cacheable=" + cacheable.size();
        boolean clean = noSniff.isEmpty() && noHsts.isEmpty() && cacheable.isEmpty();
        REPORT.add(new String[]{"security-headers", clean ? "OK" : "SOFT-GAP", summary});
        if (STRICT) {
            Assert.assertTrue(clean, "Security-header gaps on authenticated JSON — nosniff-missing=" + noSniff
                    + " hsts-missing=" + noHsts + " cacheable=" + cacheable);
        } else if (!clean) {
            ExtentReportManager.logWarning("Header gaps (reported, STRICT_SECURITY_CONTRACT gates): " + summary
                    + (cacheable.isEmpty() ? "" : " — cacheable tenant JSON: " + cacheable));
        }
        ExtentReportManager.logPass("Security-header sweep: " + summary + ".");
    }

    // ── 3. CORS: evil Origin must not be reflected with credentials ─────────

    @Test(description = "CORS: arbitrary Origin reflected with Allow-Credentials=true is an exploitable defect (hard fail)")
    public void testCorsOriginReflection() {
        ExtentReportManager.createTest(MODULE, "CORS reflection", "SEC_CORS");
        if (!hasAuthToken()) throw new SkipException("No API auth token.");

        List<String> panel = new ArrayList<>();
        panel.add("/health");
        panel.add("/auth/v2/me");
        for (String p : samplePaths()) { if (panel.size() >= 8) break; if (!panel.contains(p)) panel.add(p); }

        List<String> credentialed = new ArrayList<>(); List<String> reflectedOnly = new ArrayList<>();
        List<String> wildcardCred = new ArrayList<>(); int checked = 0;
        for (String p : panel) {
            try {
                Response r = getAuthenticatedRequestSpec().header("Origin", EVIL_ORIGIN)
                        .when().get(p).then().extract().response();
                checked++;
                String acao = r.getHeader("Access-Control-Allow-Origin");
                String acac = r.getHeader("Access-Control-Allow-Credentials");
                boolean creds = acac != null && acac.trim().equalsIgnoreCase("true");
                if (acao != null && acao.contains(EVIL_ORIGIN)) {
                    if (creds) credentialed.add(p); else reflectedOnly.add(p);
                } else if ("*".equals(acao) && creds) {
                    wildcardCred.add(p); // invalid per spec; browsers reject, but flags sloppy config
                }
            } catch (Exception e) {
                System.out.println("[TransportContract] CORS probe failed on " + p + ": " + e.getMessage());
            }
        }
        if (checked == 0) throw new SkipException("No endpoints reachable — cannot audit CORS.");
        String summary = checked + " endpoints: credentialed-reflection=" + credentialed.size()
                + " reflected-no-creds=" + reflectedOnly.size() + " wildcard+creds=" + wildcardCred.size();
        REPORT.add(new String[]{"cors", credentialed.isEmpty()
                ? (reflectedOnly.isEmpty() && wildcardCred.isEmpty() ? "OK" : "SOFT-GAP") : "FAIL", summary});
        Assert.assertTrue(credentialed.isEmpty(),
                "Evil Origin reflected WITH Access-Control-Allow-Credentials=true — any website can read this API "
                + "with the victim's session (exploitable cross-origin data theft): " + credentialed);
        if (!reflectedOnly.isEmpty() || !wildcardCred.isEmpty()) {
            ExtentReportManager.logWarning("CORS looseness (no credential exposure, reported): reflected="
                    + reflectedOnly + " wildcard+creds=" + wildcardCred);
        }
        ExtentReportManager.logPass("CORS sweep: " + summary + ".");
    }

    // ── 4. Version leakage in Server / X-Powered-By ──────────────────────────

    @Test(description = "Info leakage: Server / X-Powered-By headers must not advertise software versions")
    public void testVersionLeakage() {
        ExtentReportManager.createTest(MODULE, "Version leakage", "SEC_VERSION_LEAK");
        if (!hasAuthToken()) throw new SkipException("No API auth token.");

        List<String> panel = new ArrayList<>();
        panel.add("/health");
        for (String p : samplePaths()) { if (panel.size() >= 5) break; if (!panel.contains(p)) panel.add(p); }

        List<String> leaks = new ArrayList<>(); int checked = 0;
        for (String p : panel) {
            try {
                Response r = getAuthenticatedRequestSpec().when().get(p).then().extract().response();
                checked++;
                for (String h : new String[]{"Server", "X-Powered-By", "X-AspNet-Version", "X-Runtime"}) {
                    String v = r.getHeader(h);
                    // a bare product name ("nginx") is tolerable; digits mean a version string
                    if (v != null && v.matches(".*\\d.*")) leaks.add(p + " → " + h + ": " + v);
                }
            } catch (Exception e) {
                System.out.println("[TransportContract] version probe failed on " + p + ": " + e.getMessage());
            }
        }
        if (checked == 0) throw new SkipException("No endpoints reachable — cannot audit version leakage.");
        String summary = checked + " endpoints: version-leaking-headers=" + leaks.size();
        REPORT.add(new String[]{"version-leakage", leaks.isEmpty() ? "OK" : "SOFT-GAP", summary});
        if (STRICT) {
            Assert.assertTrue(leaks.isEmpty(),
                    "Server/X-Powered-By headers advertise software versions (CVE shopping list): " + leaks);
        } else if (!leaks.isEmpty()) {
            ExtentReportManager.logWarning("Version leakage (reported, STRICT_SECURITY_CONTRACT gates): " + leaks);
        }
        ExtentReportManager.logPass("Version-leakage sweep: " + summary + ".");
    }

    // ── summary report ───────────────────────────────────────────────────────

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        try {
            new File("reports").mkdirs();
            StringBuilder md = new StringBuilder("# Transport / Security-Headers Audit\n\n| Probe | Result | Detail |\n|---|---|---|\n");
            for (String[] row : REPORT) md.append("| ").append(row[0]).append(" | ")
                    .append(row[1]).append(" | ").append(row[2]).append(" |\n");
            md.append("\nSTRICT_SECURITY_CONTRACT=").append(STRICT).append('\n');
            try (FileWriter w = new FileWriter("reports/security-headers-report.md")) { w.write(md.toString()); }
            System.out.println("[TransportContract] wrote reports/security-headers-report.md");
        } catch (Exception e) {
            System.out.println("[TransportContract] report write failed: " + e.getMessage());
        }
    }
}