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

    @Test(timeOut = 900_000,
          description = "Walk every sidebar page, capture any 5xx/502 its API calls return, screenshot the page as evidence")
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
                    // Reset the recorder for per-page attribution, then client-side navigate.
                    AssetLoadVerifier.installFailedRequestCapture(driver);
                    js.executeScript("window.__failedRequests = [];");
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
    }
}
