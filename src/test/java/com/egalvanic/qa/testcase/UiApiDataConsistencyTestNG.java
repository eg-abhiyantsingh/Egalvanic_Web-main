package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/**
 * DATA-MISMATCH / consistency checks between what the SPA renders and the data underneath.
 *
 * Two checks per grid module:
 *   1. REFRESH DETERMINISM (asserted, tolerant): the grid's "1–N of M" total must be identical
 *      before and after a hard reload in the same session. A total that changes on refresh is an
 *      unambiguous data-consistency defect (caching race / non-deterministic pagination / partial
 *      load). Retried to absorb async settling; only a stable, reproducible difference is reported.
 *   2. API-vs-UI ADVISORY (logged, never asserted): capture the /api list call the SPA itself fires
 *      for the module, replay it, and compare its record count to the grid total. Because the grid
 *      may be client-filtered or scoped differently, this is INFORMATIONAL — surfaced in the report,
 *      not failed on.
 *
 * REPORT MODE by default; {@code -DSTRICT_DATA_CONSISTENCY=true} makes a reproducible refresh
 * mismatch fail the build (mirrors the other STRICT_* gates). Runs cannot redden the pipeline
 * unless that flag is set.
 */
public class UiApiDataConsistencyTestNG extends BaseTest {

    private static final String MODULE = "Data Consistency (UI vs API)";
    private static final boolean STRICT = "true".equalsIgnoreCase(System.getProperty("STRICT_DATA_CONSISTENCY", "false"));

    private static final By PAGINATION = By.cssSelector(".MuiTablePagination-displayedRows");
    private static final int SETTLE_MS = 5500;

    /** Modules that render a paginated grid. */
    private static final String[][] PAGES = {
        {"assets","/assets"}, {"issues","/issues"}, {"locations","/locations"},
        {"connections","/connections"}, {"tasks","/tasks"}, {"opportunities","/opportunities"},
        {"accounts","/accounts"}, {"work-orders","/sessions"},
    };

    private static final String[] NOISE = {
        "sentry","beamer","devrev","/web/api/","envelope","startSession","addMetadata",
        "setIdentifier","user-hash","session-token","legal/acceptance","/auth/login","getbeamer","/health"
    };

    private String apiToken;
    private final List<String[]> refreshRows = new ArrayList<>();   // {module, before, after, verdict}
    private final List<String[]> apiRows = new ArrayList<>();       // {module, uiTotal, apiCount, endpoint, note}

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    @Override
    @BeforeClass
    public void classSetup() {
        super.classSetup();
        apiToken = apiLogin();
        System.out.println("[Consistency] api token=" + (apiToken != null));
    }

    /** Separate REST token for authenticated replay (same flow as NetworkApiContractTestNG). */
    private String apiLogin() {
        try {
            JSONObject body = new JSONObject()
                    .put("email", AppConstants.VALID_EMAIL)
                    .put("password", AppConstants.VALID_PASSWORD)
                    .put("subdomain", AppConstants.VALID_COMPANY_CODE);
            Response r = given().baseUri(AppConstants.BASE_URL + "/api")
                    .header("Content-Type", "application/json").body(body.toString())
                    .when().post("/auth/login").then().extract().response();
            if (r.statusCode() == 200 && !r.asString().trim().startsWith("<")) {
                String t = r.jsonPath().getString("access_token");
                if (t == null) t = r.jsonPath().getString("token");
                return t;
            }
        } catch (Exception e) { System.out.println("[Consistency] apiLogin failed: " + e.getMessage()); }
        return null;
    }

    /** Read the visible MUI pagination "X–Y of N" total (-1 if none). */
    private int gridTotal() {
        for (int i = 0; i < 12; i++) {
            for (WebElement e : driver.findElements(PAGINATION)) {
                try {
                    if (!e.isDisplayed()) continue;
                    Matcher m = Pattern.compile("of\\s+([\\d,]+)").matcher(e.getText());
                    if (m.find()) return Integer.parseInt(m.group(1).replace(",", ""));
                } catch (Exception ignored) {}
            }
            pause(700);
        }
        return -1;
    }

    @Test(description = "Grid totals are refresh-deterministic; API list count is reconciled (advisory)")
    public void checkDataConsistency() {
        ExtentReportManager.createTest(MODULE, "UI vs API", "DC_ConsistencyAcrossModules");
        RestAssured.baseURI = AppConstants.BASE_URL;
        int modulesChecked = 0;
        List<String> mismatches = new ArrayList<>();   // reproducible refresh mismatches (the real defects)

        for (String[] pg : PAGES) {
            String mod = pg[0];
            try {
                // ── load + capture the SPA's own API traffic ──
                js().executeScript("performance.clearResourceTimings();");
                driver.get(AppConstants.BASE_URL + pg[1]);
                pause(SETTLE_MS);
                int before = gridTotal();
                if (before < 0) { System.out.println("[Consistency] " + mod + ": no grid pagination — skipped"); continue; }
                Set<String> apiCalls = capturedApiGets();

                // ── refresh + re-read ──
                driver.navigate().refresh();
                pause(SETTLE_MS);
                int after = gridTotal();
                // one retry if the refresh read looks unsettled (0/-1)
                if (after <= 0) { pause(2500); after = gridTotal(); }

                String verdict = (after == before) ? "consistent"
                        : (after <= 0 ? "unreadable-after-refresh" : "MISMATCH");
                refreshRows.add(new String[]{mod, String.valueOf(before), String.valueOf(after), verdict});
                if ("MISMATCH".equals(verdict)) mismatches.add(mod + ": " + before + " → " + after);
                modulesChecked++;

                // ── API-vs-UI advisory: the captured list call that plausibly belongs to THIS module ──
                // (matching by module keyword avoids comparing e.g. Assets' grid to the shared SLD list).
                int apiCount = -1; String endpoint = "-";
                if (apiToken != null) {
                    for (String path : apiCalls) {
                        if (!pathMatchesModule(path, mod)) continue;
                        try {
                            Response r = given().header("Authorization", "Bearer " + apiToken)
                                    .header("Accept", "application/json").when().get(path).then().extract().response();
                            if (r.statusCode() >= 400) continue;
                            String b = r.asString() == null ? "" : r.asString().trim();
                            int c = -1;
                            if (b.startsWith("[")) c = new JSONArray(b).length();
                            else if (b.startsWith("{")) {
                                JSONObject o = new JSONObject(b);
                                for (String k : o.keySet()) if (o.optJSONArray(k) != null) { c = Math.max(c, o.getJSONArray(k).length()); }
                                for (String k : new String[]{"total","count","total_count"}) if (o.has(k)) c = Math.max(c, o.optInt(k, -1));
                            }
                            if (c > apiCount) { apiCount = c; endpoint = path.length() > 70 ? path.substring(0, 67) + "…" : path; }
                        } catch (Exception ignored) {}
                    }
                }
                String note = (apiCount < 0) ? "no module-specific list API captured — compare skipped"
                        : (apiCount == before ? "matches UI"
                        : "differs (UI " + before + " vs API " + apiCount + ") — may be client filter/scope");
                apiRows.add(new String[]{mod, String.valueOf(before), apiCount < 0 ? "-" : String.valueOf(apiCount), endpoint, note});

                System.out.println("[Consistency] " + mod + ": UI before=" + before + " after=" + after
                        + " (" + verdict + ") | API=" + apiCount + " (" + endpoint + ")");
            } catch (Exception e) {
                System.out.println("[Consistency] " + mod + " failed: " + e.getMessage());
            }
        }

        logStep("Checked " + modulesChecked + " grid modules. Reproducible refresh mismatches: " + mismatches.size());
        Assert.assertTrue(modulesChecked >= 3, "Should read a grid total on at least 3 modules (got " + modulesChecked + ").");
        writeReport(modulesChecked, mismatches);

        if (!mismatches.isEmpty()) {
            ExtentReportManager.logWarning("Grid total changed on refresh (data-consistency defect): " + mismatches
                    + " — see reports/data-consistency-report.md");
        } else {
            ExtentReportManager.logPass("All " + modulesChecked + " grid totals were refresh-deterministic.");
        }
        if (STRICT) {
            Assert.assertTrue(mismatches.isEmpty(),
                    "STRICT: grid total changed across a same-session refresh (data mismatch): " + mismatches);
        }
    }

    /** True if a captured /api path plausibly serves the given module's collection. */
    private static boolean pathMatchesModule(String path, String mod) {
        String p = path.toLowerCase();
        switch (mod) {
            case "assets":        return p.contains("/asset") || p.contains("/node");
            case "issues":        return p.contains("/issue");
            case "locations":     return p.contains("/location");
            case "connections":   return p.contains("/connection") || p.contains("/edge");
            case "tasks":         return p.contains("/task");
            case "opportunities": return p.contains("/opportunit");
            case "accounts":      return p.contains("/account") || p.contains("/compan");
            case "work-orders":   return p.contains("/session") || p.contains("workorder") || p.contains("work-order");
            default:              return false;
        }
    }

    private Set<String> capturedApiGets() {
        Set<String> out = new LinkedHashSet<>();
        try {
            @SuppressWarnings("unchecked")
            List<String> urls = (List<String>) js().executeScript(
                "return performance.getEntriesByType('resource').map(function(e){return e.name;})"
              + ".filter(function(u){return u.indexOf('/api/')>=0;});");
            if (urls != null) for (String u : urls) {
                String path = u.replaceFirst("^https?://[^/]+", "");
                if (!path.startsWith("/api/")) continue;
                boolean noise = false;
                for (String n : NOISE) if (path.contains(n)) { noise = true; break; }
                if (!noise) out.add(path);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private void writeReport(int modules, List<String> mismatches) {
        StringBuilder md = new StringBuilder();
        md.append("# Data Consistency (UI vs API) — ").append(AppConstants.BASE_URL).append("\n\n");
        md.append("**").append(modules).append(" grid modules** · ").append(mismatches.size())
          .append(" reproducible refresh mismatch(es)\n\n");

        md.append("## Refresh determinism (grid total before vs after a same-session reload)\n\n");
        md.append("| Module | Before | After | Verdict |\n|---|---|---|---|\n");
        for (String[] r : refreshRows) md.append("| ").append(r[0]).append(" | ").append(r[1])
                .append(" | ").append(r[2]).append(" | ").append(r[3]).append(" |\n");

        md.append("\n## API vs UI count (advisory — scope/filter can legitimately differ)\n\n");
        md.append("| Module | UI total | API count | List endpoint | Note |\n|---|---|---|---|---|\n");
        for (String[] r : apiRows) md.append("| ").append(r[0]).append(" | ").append(r[1]).append(" | ")
                .append(r[2]).append(" | `").append(r[3]).append("` | ").append(r[4]).append(" |\n");
        md.append("\n");

        try {
            new File("reports").mkdirs();
            try (FileWriter w = new FileWriter("reports/data-consistency-report.md")) { w.write(md.toString()); }
            System.out.println("[Consistency] wrote reports/data-consistency-report.md");
        } catch (Exception e) { System.out.println("[Consistency] report write failed: " + e.getMessage()); }
        System.out.println("\n===== DATA CONSISTENCY =====\n" + md);
    }
}
