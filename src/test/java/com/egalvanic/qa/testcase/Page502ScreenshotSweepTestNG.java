package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;
import com.egalvanic.qa.utils.verify.AssetLoadVerifier;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Browser 502 sweep with SCREENSHOT evidence</b> (Parallel Suite 3 companion to
 * {@code AllPagesStabilityApiTest}).
 *
 * <p>Logs in once (BaseTest), then walks EVERY sidebar page via client-side routing (sidebar link
 * clicks — keeps the SPA document alive so the injected fetch/XHR recorder catches every backing
 * API call the page makes). For each page, any captured <b>5xx (esp. 502)</b> response is recorded
 * and the page is <b>screenshotted as user-visible evidence</b> → {@code reports/evidence/*.png},
 * which the consolidated Suite-3 report embeds automatically in its Visual Evidence section.</p>
 *
 * <p>Two rounds: repeat-load latency degradation and intermittent gateway errors showed up on
 * later rounds in the API sweep, so a single pass under-detects. Report-mode by default
 * ({@code -DSTRICT_PAGE_502=true} hard-fails); findings land in
 * {@code reports/page-502-screenshot-report.md} + the findings dashboard.</p>
 */
public class Page502ScreenshotSweepTestNG extends BaseTest {

    private static final String MODULE = "API — Page 502 Screenshot Sweep";
    private static final int ROUNDS = 2;
    private static final boolean STRICT =
            Boolean.parseBoolean(System.getProperty("STRICT_PAGE_502",
                    System.getenv().getOrDefault("STRICT_PAGE_502", "false")));

    /** Sidebar-reachable app pages: {label, route}. */
    private static final String[][] PAGES = {
            {"Dashboard", "/dashboard"},
            {"Assets", "/assets"},
            {"Connections", "/connections"},
            {"Locations", "/locations"},
            {"Tasks", "/tasks"},
            {"Issues", "/issues"},
            {"SLDs", "/slds"},
            {"Work Orders", "/sessions"},
            {"Arc Flash", "/arc-flash"},
            {"Condition Assessment", "/pm-readiness"},
            {"EMPs", "/emps"},
            {"Opportunities", "/opportunities"},
            {"Accounts", "/accounts"},
    };

    private final List<String[]> reportRows = new ArrayList<>();
    // Per-page DevTools-style evidence for the consolidated report: the Network calls the page fired
    // (method/url/status/ms) and the Console errors/warnings — so a developer sees the actual data.
    private final Map<String, List<String[]>> networkByPage = new LinkedHashMap<>();
    private final Map<String, List<String>> consoleByPage = new LinkedHashMap<>();

    /** Injected once (persists across client-side nav): record every request's method/url/status/ms + console errors. */
    private static final String RECORDER_JS =
        "if(!window.__allReqInstalled){window.__allReqInstalled=true;window.__allReq=[];window.__consoleLog=[];" +
        "var of=window.fetch;window.fetch=function(){var a=arguments,t=performance.now();" +
        " return of.apply(this,a).then(function(r){try{window.__allReq.push({m:(a[1]&&a[1].method)||'GET',u:String(r.url||a[0]),s:r.status,ms:Math.round(performance.now()-t)});}catch(e){}return r;});};" +
        "var oo=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){this.__m=m;this.__u=u;this.__t=performance.now();return oo.apply(this,arguments);};" +
        "var os=XMLHttpRequest.prototype.send;XMLHttpRequest.prototype.send=function(){var x=this;x.addEventListener('loadend',function(){try{window.__allReq.push({m:x.__m||'GET',u:x.__u,s:x.status,ms:Math.round(performance.now()-x.__t)});}catch(e){}});return os.apply(this,arguments);};" +
        "var ce=console.error;console.error=function(){try{window.__consoleLog.push('ERROR '+Array.prototype.slice.call(arguments).join(' ').slice(0,300));}catch(e){}return ce.apply(this,arguments);};" +
        "var cw=console.warn;console.warn=function(){try{window.__consoleLog.push('WARN '+Array.prototype.slice.call(arguments).join(' ').slice(0,300));}catch(e){}return cw.apply(this,arguments);};}";

    @Test(timeOut = 900_000,
          description = "Walk every sidebar page, capture any 5xx/502 its API calls return, screenshot the page + record its Network & Console")
    public void page502SweepWithScreenshots() {
        ExtentReportManager.createTest(MODULE, "502 sweep", "PAGE_502_SCREENSHOT_SWEEP");
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // page label → incident details across all rounds
        Map<String, List<String>> incidents = new LinkedHashMap<>();
        Map<String, String> shots = new LinkedHashMap<>();
        int total5xx = 0, total502 = 0;

        for (int round = 1; round <= ROUNDS; round++) {
            for (int pi = 0; pi < PAGES.length; pi++) {
                String label = PAGES[pi][0], route = PAGES[pi][1];
                try {
                    // Reset the recorders for per-page attribution, then client-side navigate.
                    AssetLoadVerifier.installFailedRequestCapture(driver);
                    js.executeScript(RECORDER_JS);
                    js.executeScript("window.__failedRequests = []; window.__allReq = []; window.__consoleLog = [];");
                    Object clicked = js.executeScript(
                            "var a = document.querySelector(\"a[href='" + route + "']\");" +
                            "if (a) { a.click(); return true; } return false;");
                    if (!Boolean.TRUE.equals(clicked)) {
                        // Fallback: full load (re-install after; initial calls may be missed — noted).
                        driver.get(AppConstants.BASE_URL + route);
                        AssetLoadVerifier.installFailedRequestCapture(driver);
                    }
                    // Let the page mount + its data calls finish (readyState + settle).
                    for (int i = 0; i < 20; i++) {
                        pause(500);
                        Object ready = js.executeScript(
                                "return document.readyState === 'complete' && window.location.pathname === '" + route + "';");
                        if (Boolean.TRUE.equals(ready) && i >= 4) break;   // ≥2.5s settle for async data
                    }

                    // Collect this page's failed APP api calls (>=500; 3rd-party noise excluded by host).
                    List<String> pageIncidents = new ArrayList<>();
                    for (AssetLoadVerifier.FailedRequest f : AssetLoadVerifier.getFailedRequests(driver)) {
                        boolean appApi = f.url != null && f.url.contains("/api/")
                                && !f.url.contains("sentry") && !f.url.contains("devrev") && !f.url.contains("beamer");
                        if (appApi && f.status >= 500) {
                            pageIncidents.add("round" + round + " HTTP " + f.status + " " + f.url);
                            total5xx++;
                            if (f.status == 502) total502++;
                        }
                    }
                    // ALWAYS screenshot each page (last round = settled state) so the consolidated
                    // report carries visual proof every page was swept, not only on a 502. Stable
                    // per-page filename (index-prefixed for gallery order); overwritten each round.
                    if (round == ROUNDS) {
                        String slug = String.format("%02d-%s", pi + 1, label.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
                        String path = saveShot("evidence-page-" + slug + ".png");
                        if (path != null) shots.putIfAbsent(label, path);
                        captureNetworkAndConsole(label);   // DevTools-style Network + Console data for the report
                    }

                    if (!pageIncidents.isEmpty()) {
                        incidents.computeIfAbsent(label, k -> new ArrayList<>()).addAll(pageIncidents);
                        // Additional dedicated shot of the BROKEN page (distinct filename → its own report tile).
                        String path = saveShot("evidence-502-" + label.toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".png");
                        if (path != null) shots.put(label, path);
                        System.out.println("[Page502][incident] [" + label + "] " + pageIncidents);
                        logStepWithScreenshot("5xx on " + label + ": " + pageIncidents);
                    } else {
                        System.out.println("[Page502] round" + round + " " + label + " (" + route + ") clean");
                    }
                } catch (Exception e) {
                    incidents.computeIfAbsent(label, k -> new ArrayList<>())
                             .add("round" + round + " SWEEP-ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    System.out.println("[Page502][sweep-error] " + label + ": " + e.getMessage());
                }
            }
        }

        for (Map.Entry<String, List<String>> e : incidents.entrySet()) {
            reportRows.add(new String[]{ e.getKey(), String.valueOf(e.getValue().size()),
                    shots.getOrDefault(e.getKey(), "-"), String.join(" | ", e.getValue()) });
        }
        String summary = PAGES.length + " pages x " + ROUNDS + " rounds; " + total502 + " × 502, "
                + total5xx + " × 5xx; " + incidents.size() + " page(s) affected.";
        System.out.println("[Page502] " + summary);
        logStep(summary);

        if (total5xx == 0) {
            ExtentReportManager.logPass("No 502/5xx on any page's API calls — " + summary);
            return;
        }
        String msg = (total502 > 0 ? "502 BAD GATEWAY seen by real pages — " : "5xx seen by real pages — ")
                + summary + " Pages: " + incidents.keySet() + " (screenshots in reports/evidence/)";
        if (STRICT) { ExtentReportManager.logFail(msg); org.testng.Assert.fail(msg); }
        else ExtentReportManager.logWarning(msg + "  (reported; -DSTRICT_PAGE_502=true enforces)");
    }

    // Scrub secrets before anything is written to the (shared, 30-day) report artifact: JWTs, bearer
    // tokens, emails, and the values of sensitive query params. Keeps the evidence useful (paths,
    // status, timing, param NAMES) without leaking credentials/PII.
    private static final java.util.regex.Pattern JWT = java.util.regex.Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final java.util.regex.Pattern BEARER = java.util.regex.Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._-]+");
    private static final java.util.regex.Pattern SENSITIVE_QP = java.util.regex.Pattern.compile(
            "(?i)([?&](?:access_token|refresh_token|id_token|token|api_?key|secret|password|pwd|auth|jwt|code|state|signature|sig|session|email|phone)=)[^&#\\s]*");

    private static String redact(String s) {
        if (s == null || s.isEmpty()) return s;
        s = JWT.matcher(s).replaceAll("[JWT]");
        s = BEARER.matcher(s).replaceAll("Bearer [REDACTED]");
        s = SENSITIVE_QP.matcher(s).replaceAll("$1[REDACTED]");
        s = EMAIL.matcher(s).replaceAll("[EMAIL]");
        return s;
    }

    /** Read the injected recorder for the current page: app /api Network rows + Console errors/warnings. */
    @SuppressWarnings("unchecked")
    private void captureNetworkAndConsole(String label) {
        try {
            Object net = ((JavascriptExecutor) driver).executeScript("return window.__allReq || [];");
            List<String[]> rows = new ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            if (net instanceof List) {
                for (Object o : (List<Object>) net) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    String url = String.valueOf(m.get("u"));
                    if (url == null || !url.contains("/api/")) continue;
                    if (url.contains("sentry") || url.contains("devrev") || url.contains("beamer")) continue;
                    String method = String.valueOf(m.get("m"));
                    String status = m.get("s") == null ? "?" : String.valueOf(((Number) m.get("s")).intValue());
                    String ms = m.get("ms") == null ? "?" : String.valueOf(((Number) m.get("ms")).intValue());
                    String path = redact(url.replaceFirst("^https?://[^/]+", ""));   // strip host, scrub secrets
                    String key = method + " " + path;
                    // annotate exact-URL duplicates (same call fired 2x+ on this load)
                    rows.add(new String[]{ method, path, status, ms + "ms", seen.add(key) ? "" : "DUPLICATE" });
                }
            }
            if (rows.size() > 40) rows = new ArrayList<>(rows.subList(0, 40));   // keep the report readable
            networkByPage.put(label, rows);

            Object con = ((JavascriptExecutor) driver).executeScript("return window.__consoleLog || [];");
            List<String> logs = new ArrayList<>();
            if (con instanceof List) for (Object o : (List<Object>) con) {
                String s = redact(String.valueOf(o));
                if (!logs.contains(s)) logs.add(s);   // de-dupe repeated console spam
            }
            if (logs.size() > 25) logs = new ArrayList<>(logs.subList(0, 25));
            consoleByPage.put(label, logs);
        } catch (Exception e) {
            System.out.println("[Page502] network/console capture failed for " + label + ": " + e.getMessage());
        }
    }

    /** Save a screenshot of the current page to reports/evidence/{fileName}; returns its path or null. */
    private String saveShot(String fileName) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dst = new File("reports/evidence/" + fileName);
            dst.getParentFile().mkdirs();
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "reports/evidence/" + fileName;
        } catch (Exception se) {
            System.out.println("[Page502] screenshot save failed (" + fileName + "): " + se.getMessage());
            return null;
        }
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Page 502 Screenshot Sweep (browser)\n\n");
        md.append("Every sidebar page visited ").append(ROUNDS).append("x in a real logged-in browser; the ")
          .append("injected fetch/XHR recorder captures each page's backing API responses. Any 5xx/502 is ")
          .append("recorded AND the page is screenshotted as user-visible evidence (embedded in the ")
          .append("consolidated report's Visual Evidence section).\n\n");
        if (reportRows.isEmpty()) {
            md.append("**No 502/5xx incidents — every page's API calls were clean on all rounds.**\n");
        } else {
            md.append("| Page | Incidents | Screenshot | Detail |\n|---|---|---|---|\n");
            for (String[] r : reportRows) md.append("| ").append(String.join(" | ", r)).append(" |\n");
            md.append("\n**").append(reportRows.size()).append(" page(s) with 5xx incidents")
              .append(STRICT ? " (STRICT: enforced)" : " (reported)").append(".**\n");
        }
        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/page-502-screenshot-report.md")) { w.write(md.toString()); }
            System.out.println("[Page502] Report → reports/page-502-screenshot-report.md");
        } catch (Exception e) { System.out.println("[Page502] report write failed: " + e.getMessage()); }

        writeNetworkConsoleReport();
    }

    /** Per-page Network + Console evidence (DevTools-style) → the consolidated report renders it. */
    private void writeNetworkConsoleReport() {
        StringBuilder md = new StringBuilder();
        md.append("# Per-Page Network & Console (browser evidence)\n\n");
        md.append("Captured live in a logged-in browser as each page loaded — the app `/api` calls it fired ")
          .append("(method, path, status, timing; `DUPLICATE` = same call repeated on one load) and any ")
          .append("console errors/warnings. This is the DevTools Network + Console data, for developer triage.\n");
        for (String[] page : PAGES) {
            String label = page[0];
            List<String[]> net = networkByPage.get(label);
            List<String> con = consoleByPage.get(label);
            if (net == null && con == null) continue;
            md.append("\n## ").append(label).append("  `").append(page[1]).append("`\n\n");
            md.append("**Network — ").append(net == null ? 0 : net.size()).append(" app API call(s)");
            if (net != null) {
                long dup = net.stream().filter(r -> "DUPLICATE".equals(r[4])).count();
                if (dup > 0) md.append(" · ").append(dup).append(" DUPLICATE");
            }
            md.append("**\n\n| Method | Path | Status | Time | Note |\n|---|---|---|---|---|\n");
            if (net != null) for (String[] r : net) md.append("| ").append(String.join(" | ", r)).append(" |\n");
            md.append("\n**Console — ").append(con == null ? 0 : con.size()).append(" error/warning(s)**\n\n");
            if (con == null || con.isEmpty()) {
                md.append("_(none captured)_\n");
            } else {
                md.append("```\n");
                for (String s : con) md.append(s).append("\n");
                md.append("```\n");
            }
        }
        try (FileWriter w = new FileWriter("reports/page-network-console-report.md")) { w.write(md.toString()); }
        catch (Exception e) { System.out.println("[Page502] network/console report write failed: " + e.getMessage()); }
        System.out.println("[Page502] Report → reports/page-network-console-report.md");
    }
}
