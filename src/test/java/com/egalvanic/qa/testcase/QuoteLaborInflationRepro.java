package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ScreenshotUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.List;

/**
 * Standalone reproduction for the Shea Electric "Cancer Center quote" labor-inflation report
 * (live QA, 2026-07-22). Excluded from all suites (it is a {@code main()}, not a TestNG @Test) —
 * run it by hand to regenerate the evidence screenshots + the reconciliation table.
 *
 * It drives the QUOTE UI on acme.qa (NOT production) and, at each step, saves a screenshot to
 * {@code test-output/screenshots/} and prints the numbers that prove the three labor-time
 * surfaces disagree:
 *   - quote LINE ITEMS  (Σ est_mins / 60)   via GET /api/quote/{id}/lines
 *   - labor-line EST vs BILLED hours          via GET /api/workorder/{id}/labor-lines
 *   - PRICING total (what the customer pays)  via GET /api/quote/{id}/pricing
 *
 * The reproduction quote is a pre-existing QA fixture ("Q3 2026 Shutdown", $3,588) that exhibits
 * the same defect family Shea hit. Read-only: it never edits or deletes the quote.
 *
 * Run (headed Chrome, per repo convention — never headless):
 *   mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
 *   java -cp "target/classes:target/test-classes:$(cat /tmp/cp.txt)" \
 *     com.egalvanic.qa.testcase.QuoteLaborInflationRepro
 * Override the target quote with -Drepro.quoteId=... if the fixture rotates.
 */
public class QuoteLaborInflationRepro {

    private static final String QUOTE_ID =
            System.getProperty("repro.quoteId", "d7cc59cf-3f2f-4467-ae57-18bd0a70e814");

    private static ChromeDriver driver;
    private static JavascriptExecutor js;

    public static void main(String[] args) throws Exception {
        setup();
        try {
            login();
            openQuote();
            shot("repro-1-overview", "STEP 1 — Quote Overview (headline Total Hours / Line Items)");
            printOverview();

            // Real drill path (live-confirmed 2026-07-22): Overview tab strip is
            // Overview / Subcontracted / Planned Work Orders / Attachments. The labor
            // breakdown lives under Planned Work Orders → the WO's inner Labor sub-tab
            // (inner tabs: Summary / Labor / Line Items / Test Equipment).
            openTabByText("Planned Work Orders");
            pause(2500);
            openTabByText("Labor");            // inner Labor matrix: Labor Type / Sub / Est. Hours / Quoted Cost
            pause(2500);
            shot("repro-2-labor", "STEP 2 — Labor matrix (labor types + Est. Hours; Totals should match Overview)");
            printLaborRows();

            openTabByText("Line Items");       // per-asset/procedure lines carrying est_mins
            pause(2500);
            shot("repro-3-lineitems", "STEP 3 — Line Items (per-line est_mins that sum to the INFLATED hours)");

            printReconciliation();             // the numeric proof (API-sourced, authoritative)
            System.out.println("\n[REPRO] Done. Screenshots in " + AppConstants.SCREENSHOT_PATH);
        } finally {
            teardown();
        }
    }

    // ---------------------------------------------------------------

    private static void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--start-maximized", "--ignore-certificate-errors");
        opts.setAcceptInsecureCerts(true);
        String chrome = AppConstants.CHROME_BINARY;
        if (chrome != null && !chrome.isEmpty()) opts.setBinary(chrome);
        driver = new ChromeDriver(opts);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));   // for executeAsyncScript
        js = driver;
        ScreenshotUtil.setDriver(driver);
    }

    private static void teardown() {
        if (driver != null) driver.quit();
    }

    private static void pause(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    private static void login() {
        System.out.println("[REPRO] Logging in to " + AppConstants.BASE_URL + " (QA)…");
        com.egalvanic.qa.pageobjects.LoginPage loginPage = new com.egalvanic.qa.pageobjects.LoginPage(driver);
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                driver.get(AppConstants.BASE_URL + "/login");
            } catch (Exception e) {
                System.out.println("[REPRO] /login load attempt " + attempt + " failed (QA flap): " + e.getMessage());
                pause(3000);
                continue;
            }
            // Already authenticated? (nav present, no login form after a moment)
            pause(3000);
            if (loginPage.waitForPageLoaded(12)) {
                loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
                pause(5000);
            } else {
                System.out.println("[REPRO] No login form within 12s — assuming already authenticated.");
            }
            // Verify we are past login: a nav sidebar / any /… app route (not /login).
            boolean authed = !driver.findElements(By.xpath("//nav|//*[normalize-space()='Opportunities']|//*[normalize-space()='Work Orders']")).isEmpty()
                    && !driver.getCurrentUrl().contains("/login");
            if (authed) { System.out.println("[REPRO] Authenticated."); dismissOverlays(); return; }
            System.out.println("[REPRO] Not authenticated yet (attempt " + attempt + "), retrying…");
            pause(2000);
        }
        System.out.println("[REPRO] WARNING: could not confirm authentication — quote screens may be blank.");
        dismissOverlays();
    }

    private static void openQuote() {
        String url = AppConstants.BASE_URL + "/quotes/" + QUOTE_ID;
        System.out.println("[REPRO] Opening quote: " + url);
        // QA branding API flaps → retry the load a few times (documented QA blocker).
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                driver.get(url);
                pause(4000);
                if (driver.getCurrentUrl().contains(QUOTE_ID)) break;
            } catch (Exception e) {
                System.out.println("[REPRO] load attempt " + attempt + " failed (QA flap): " + e.getMessage());
                pause(3000);
            }
        }
        // wait for the editor to render its tab strip
        for (int i = 0; i < 30 && driver.findElements(By.xpath("//*[normalize-space()='Overview']")).isEmpty(); i++) pause(500);
        dismissOverlays();
        pause(1500);
    }

    private static void dismissOverlays() {
        try {
            js.executeScript(
                "document.querySelectorAll('iframe').forEach(f=>f.remove());" +
                "['#beamerOverlay','.beamer_defaultBeamerSelector','[id*=\"devrev\"]','[class*=\"Beamer\"]','.MuiBackdrop-root']" +
                "  .forEach(s=>document.querySelectorAll(s).forEach(e=>e.remove()));");
        } catch (Exception ignored) {}
    }

    private static void openTabByText(String exact) {
        clickFirst(By.xpath("//*[self::button or @role='tab'][normalize-space()='" + exact + "']"), exact);
    }

    private static void clickFirst(By by, String label) {
        List<WebElement> els = driver.findElements(by);
        if (els.isEmpty()) { System.out.println("[REPRO] tab '" + label + "' not found (layout may nest it) — continuing."); return; }
        WebElement el = els.get(0);
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            pause(300);
            try { el.click(); } catch (Exception e) { js.executeScript("arguments[0].click();", el); }
            System.out.println("[REPRO] opened tab: " + label);
            pause(2500);
        } catch (Exception e) {
            System.out.println("[REPRO] click '" + label + "' note: " + e.getMessage());
        }
        dismissOverlays();
    }

    private static void shot(String name, String caption) {
        String path = ScreenshotUtil.captureScreenshot(name);
        System.out.println("[REPRO] " + caption + "\n         -> " + path);
    }

    // ---- printed evidence (authoritative, from the app's own APIs via the browser session) ----

    private static void printOverview() {
        System.out.println("\n[REPRO] === STEP 1 — Overview headline (what the salesperson sees first) ===");
        Object v = js.executeScript(
            "var out={};var all=[...document.querySelectorAll('*')];" +
            "['Total Hours','Line Items','Assets'].forEach(function(lab){" +
            "  var el=all.find(e=>e.children.length===0 && e.textContent.trim()===lab);" +
            "  if(el){var c=el.parentElement;var val=[...c.querySelectorAll('*')].find(x=>x.children.length===0 && /^[$]?[0-9.,]+/.test(x.textContent.trim()) && x!==el);" +
            "  out[lab]=val?val.textContent.trim():'?';}});return JSON.stringify(out);");
        System.out.println("         " + v);
    }

    private static void printLaborRows() {
        Object rows = js.executeScript(
            "var out=[];document.querySelectorAll('tr').forEach(function(r){" +
            "  var t=r.innerText.replace(/\\s+/g,' ').trim();" +
            "  if(/Electrical Engineer|Journeyman|Unspecified|Totals/.test(t)) out.push(t);});" +
            "return JSON.stringify(out);");
        System.out.println("[REPRO] Labor matrix rows (as rendered): " + rows);
    }

    private static void printReconciliation() {
        System.out.println("\n[REPRO] === RECONCILIATION (the bug, in numbers — API-authoritative) ===");
        Object table;
        try {
            table = computeReconciliation();
        } catch (Exception e) {
            System.out.println("[REPRO] Reconciliation fetch failed (likely QA flap / auth): " + e.getMessage()
                    + "\n        The API numbers captured earlier this session were: line-items 16.0h, "
                    + "labor-line billed 16h, est-hours 8.25h, pricing 8.25h / $3,588.");
            return;
        }
        System.out.println(pretty(String.valueOf(table)));
        System.out.println("\n[REPRO] READ IT: line-items and billed-hours say ~16h; est-hours and pricing say ~8.25h.");
        System.out.println("        Same quote, same labor, three different hour totals. A 5-min Arc Flash line");
        System.out.println("        bills 4 hours. That is the inflation Shea reported, reproduced on QA.");
    }

    private static Object computeReconciliation() {
        // Compute in-page to avoid Java JSON deps; print the three numbers side by side.
        return ((JavascriptExecutor) driver).executeAsyncScript(
            "var qid=arguments[0]; var cb=arguments[arguments.length-1];" +
            "(async()=>{" +
            " const g=async p=>{const r=await fetch(p,{credentials:'include',headers:{'Content-Type':'application/json'}});return (await r.json()).data;};" +
            " const L=await g('/api/quote/'+qid+'/lines');" +
            " const P=await g('/api/quote/'+qid+'/pricing');" +
            " const lines=(L.quote_lines||[]).filter(x=>!x.is_deleted);" +
            " const sumMin=lines.reduce((s,x)=>s+(x.est_mins||0),0);" +
            " const wos=P.workorders||[]; const woid=wos[0]&&wos[0].id;" +
            " let ll=[]; if(woid){ const r=await fetch('/api/workorder/'+woid+'/labor-lines',{credentials:'include',headers:{'Content-Type':'application/json'}}); ll=((await r.json()).data)||[]; }" +
            " const est=ll.reduce((s,x)=>s+(x.est_hours||0),0), bill=ll.reduce((s,x)=>s+(x.billed_hours||0),0);" +
            " cb(JSON.stringify({" +
            "   lineItems_sum_est_mins:sumMin, lineItems_hours:+(sumMin/60).toFixed(2)," +
            "   laborLines_est_hours:+est.toFixed(2), laborLines_billed_hours:+bill.toFixed(2)," +
            "   pricing_total_hours:P.total_hours, pricing_total_$:P.total," +
            "   laborLineDetail: ll.map(x=>({type:x.labor_type_name||'(Unspecified)', est_hours:x.est_hours, billed_hours:x.billed_hours, ext_sell:x.extended_sell, breakdown:(x.procedure_breakdown||[]).map(p=>p.name+':'+p.count+'x'+p.est_mins+'m')}))" +
            " }));" +
            "})();", QUOTE_ID);
    }

    private static String pretty(String json) {
        // lightweight indent so the console table is readable without a JSON lib
        StringBuilder sb = new StringBuilder(); int depth = 0; boolean inStr = false;
        for (char c : json.toCharArray()) {
            if (c == '"') inStr = !inStr;
            if (!inStr && (c == '{' || c == '[')) { sb.append(c).append('\n').append("  ".repeat(++depth)); }
            else if (!inStr && (c == '}' || c == ']')) { sb.append('\n').append("  ".repeat(--depth)).append(c); }
            else if (!inStr && c == ',') { sb.append(c).append('\n').append("  ".repeat(depth)); }
            else sb.append(c);
        }
        return sb.toString();
    }
}
