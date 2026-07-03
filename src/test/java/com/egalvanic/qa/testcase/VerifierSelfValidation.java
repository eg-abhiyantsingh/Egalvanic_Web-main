package com.egalvanic.qa.testcase;

import com.egalvanic.qa.utils.verify.AssetLoadVerifier;
import com.egalvanic.qa.utils.verify.BrowserErrorCapture;
import com.egalvanic.qa.utils.verify.HangDetector;
import com.egalvanic.qa.utils.verify.StateIntegrityChecker;
import com.egalvanic.qa.utils.verify.UIStateValidator;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;

/**
 * VerifierSelfValidation — proves the Phase-2 verifiers actually CATCH bugs
 * and do NOT raise false positives on healthy pages.
 *
 * It is deliberately NOT a TestNG @Test (no login, no live app) and is excluded
 * from every suite XML, so it never runs in CI by accident. Run it manually:
 *
 *   mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
 *   java -cp "target/classes:target/test-classes:$(cat /tmp/cp.txt)" \
 *        -Dchrome.binary=/path/to/chrome \
 *        -Dwebdriver.chrome.driver=/path/to/chromedriver \
 *        com.egalvanic.qa.testcase.VerifierSelfValidation
 *
 * If 'chrome.binary' / 'webdriver.chrome.driver' are unset, Selenium Manager
 * resolves them automatically. Exit code 0 = all verifiers behave correctly.
 *
 * Each check loads an HTML fixture with ONE planted bug, asserts the matching
 * verifier reports it, then loads a clean fixture and asserts it stays silent.
 */
public class VerifierSelfValidation {

    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        ChromeOptions opts = new ChromeOptions();
        String bin = System.getProperty("chrome.binary");
        if (bin != null && !bin.isEmpty()) opts.setBinary(bin);
        opts.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--window-size=1280,900");

        WebDriver driver = new ChromeDriver(opts);
        com.egalvanic.qa.utils.ScreenshotUtil.setDriver(driver);
        try {
            checkConsoleCapture(driver);
            checkBrokenImage(driver);
            checkFailedRequest(driver);
            checkBlankPage(driver);
            checkErrorBanner(driver);
            checkHang(driver);
            checkPdfRender(driver);
            checkStateIntegrity(driver);
        } finally {
            driver.quit();
        }

        System.out.println("\n================ VERIFIER VALIDATION ================");
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        System.out.println("====================================================");
        if (failed > 0) System.exit(1);
    }

    // ---- 1. BrowserErrorCapture: uncaught + promise + console ----
    private static void checkConsoleCapture(WebDriver d) {
        load(d, "<html><body><h1>err</h1></body></html>");
        BrowserErrorCapture.install(d);
        ((JavascriptExecutor) d).executeScript(
            "setTimeout(function(){ throw new Error('boom-uncaught'); }, 0);" +
            "Promise.reject(new Error('boom-promise'));" +
            "console.error('boom-console');");
        sleep(700);
        List<BrowserErrorCapture.JsError> errs = BrowserErrorCapture.getErrors(d);
        String all = errs.toString();
        expect("BrowserErrorCapture catches uncaught+promise+console",
            all.contains("boom-uncaught") && all.contains("boom-promise") && all.contains("boom-console"),
            "captured=" + all);

        // clean page -> no false positive
        load(d, "<html><body><h1>clean</h1></body></html>");
        BrowserErrorCapture.install(d);
        sleep(200);
        expect("BrowserErrorCapture clean on healthy page",
            BrowserErrorCapture.getErrors(d).isEmpty(), "got=" + BrowserErrorCapture.getErrors(d));
    }

    // ---- 2. AssetLoadVerifier.findBrokenImages ----
    private static void checkBrokenImage(WebDriver d) {
        // 1x1 valid gif as data URI = healthy; bad host = broken
        String good = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
        load(d, "<html><body>" +
            "<img id='good' src='" + good + "' style='width:30px;height:30px'>" +
            "<img id='bad' src='http://127.0.0.1:1/nope.png' style='width:30px;height:30px'>" +
            "</body></html>");
        sleep(500);
        List<AssetLoadVerifier.BrokenAsset> broken = AssetLoadVerifier.findBrokenImages(d);
        expect("AssetLoadVerifier flags the broken image",
            broken.size() == 1 && broken.get(0).url.contains("nope.png"),
            "broken=" + broken);

        load(d, "<html><body><img src='" + good + "' style='width:30px;height:30px'></body></html>");
        sleep(300);
        expect("AssetLoadVerifier clean when all images load",
            AssetLoadVerifier.findBrokenImages(d).isEmpty(), "broken=" + AssetLoadVerifier.findBrokenImages(d));
    }

    // ---- 3. AssetLoadVerifier failed-request capture ----
    private static void checkFailedRequest(WebDriver d) {
        load(d, "<html><body><h1>net</h1></body></html>");
        AssetLoadVerifier.installFailedRequestCapture(d);
        ((JavascriptExecutor) d).executeScript(
            "fetch('http://127.0.0.1:1/dead').catch(function(){});");
        sleep(700);
        expect("AssetLoadVerifier captures failed fetch",
            !AssetLoadVerifier.getFailedRequests(d).isEmpty(),
            "failed=" + AssetLoadVerifier.getFailedRequests(d));
    }

    // ---- 4. UIStateValidator blank page ----
    private static void checkBlankPage(WebDriver d) {
        load(d, "<html><body></body></html>");
        UIStateValidator.UIStateReport blank = UIStateValidator.validate(d, "blank-fixture");
        expect("UIStateValidator detects blank page",
            !blank.ok() && blank.toString().contains("BLANK"), "report=" + blank);

        load(d, "<html><body><main><div style='width:400px;height:200px'>" +
            "<p>Real content here with plenty of text and structure.</p>" +
            "<button>Act</button><table><tr><td>cell</td></tr></table></div></main></body></html>");
        UIStateValidator.UIStateReport healthy = UIStateValidator.validate(d, "healthy-fixture");
        expect("UIStateValidator clean on populated page", healthy.ok(), "report=" + healthy);
    }

    // ---- 5. UIStateValidator error banner ----
    private static void checkErrorBanner(WebDriver d) {
        load(d, "<html><body><main><div style='width:400px;height:200px'>" +
            "<p>page with lots of content and structure so it is not blank</p>" +
            "<div role='alert' class='MuiAlert-standardError'>Something went wrong loading data</div>" +
            "</div></main></body></html>");
        UIStateValidator.UIStateReport rep = UIStateValidator.validate(d, "error-banner-fixture");
        expect("UIStateValidator detects error banner",
            !rep.ok() && rep.toString().contains("ERROR"), "report=" + rep);
    }

    // ---- 6. HangDetector ----
    private static void checkHang(WebDriver d) {
        // permanent visible spinner = hung
        load(d, "<html><body><div class='MuiCircularProgress-root' " +
            "style='width:40px;height:40px'>spinner</div></body></html>");
        HangDetector.HangReport hung = HangDetector.check(d, "spinner-fixture", 2);
        expect("HangDetector flags infinite spinner", hung.hung, "reason=" + (hung.hung ? hung.reason : "not hung"));

        load(d, "<html><body><h1>settled content</h1><p>done</p></body></html>");
        HangDetector.HangReport ok = HangDetector.check(d, "settled-fixture", 5);
        expect("HangDetector clean on settled page", !ok.hung, "reason=" + ok.reason);

        // DETERMINATE progress (readiness gauges, completion bars) shares MUI classes
        // and role=progressbar with real spinners but is a permanent data visualization
        // — it must NOT read as a hang. Regression fixture for the 2026-06-30 Arc Flash
        // false "Page HUNG" (three determinate gauges flagged as spinners for 30s).
        load(d, "<html><body><h1>Arc Flash readiness</h1>" +
            "<span class='MuiCircularProgress-root MuiCircularProgress-determinate' " +
            "role='progressbar' aria-valuenow='37' style='width:40px;height:40px'>37%</span>" +
            "<span class='MuiLinearProgress-root MuiLinearProgress-determinate' " +
            "role='progressbar' aria-valuenow='50' style='width:200px;height:4px'></span>" +
            "</body></html>");
        HangDetector.HangReport gauge = HangDetector.check(d, "determinate-gauge-fixture", 5);
        expect("HangDetector ignores determinate gauges", !gauge.hung, "reason=" + gauge.reason);
    }

    // ---- 7. AssetLoadVerifier.isPdfRendered ----
    private static void checkPdfRender(WebDriver d) {
        load(d, "<html><body><div id='viewer'>" +
            "<embed type='application/pdf' src='report.pdf' style='width:600px;height:800px'>" +
            "</div></body></html>");
        expect("AssetLoadVerifier confirms PDF rendered",
            AssetLoadVerifier.isPdfRendered(d, By.id("viewer")), "expected true");

        load(d, "<html><body><div id='viewer' style='width:600px;height:800px'>" +
            "<p>PDF failed to load</p></div></body></html>");
        expect("AssetLoadVerifier flags missing PDF",
            !AssetLoadVerifier.isPdfRendered(d, By.id("viewer")), "expected false");
    }

    // ---- 8. StateIntegrityChecker ----
    private static void checkStateIntegrity(WebDriver d) {
        load(d, "<html><body><div id='grid'>" +
            "<div class='row'>Asset A</div><div class='row'>Asset B</div></div></body></html>");
        StateIntegrityChecker.Snapshot before = StateIntegrityChecker.snapshotRows(d, By.cssSelector(".row"));

        // legit create -> +1 unique
        ((JavascriptExecutor) d).executeScript(
            "var r=document.createElement('div');r.className='row';r.textContent='Asset C';" +
            "document.getElementById('grid').appendChild(r);");
        StateIntegrityChecker.Snapshot after = StateIntegrityChecker.snapshotRows(d, By.cssSelector(".row"));
        boolean grewOk;
        try { StateIntegrityChecker.assertGrew(before, after, 1, "create"); grewOk = true; }
        catch (AssertionError e) { grewOk = false; }
        expect("StateIntegrityChecker accepts a clean +1 create", grewOk, "after=" + after.values);

        // duplication bug -> must throw
        ((JavascriptExecutor) d).executeScript(
            "var r=document.createElement('div');r.className='row';r.textContent='Asset C';" +
            "document.getElementById('grid').appendChild(r);");
        StateIntegrityChecker.Snapshot dup = StateIntegrityChecker.snapshotRows(d, By.cssSelector(".row"));
        boolean caughtDup;
        try { StateIntegrityChecker.assertNoDuplicates(dup, "create-dup"); caughtDup = false; }
        catch (AssertionError e) { caughtDup = true; }
        expect("StateIntegrityChecker detects duplicated row", caughtDup, "rows=" + dup.values);
    }

    // -------------------------------------------------------------- helpers
    private static void load(WebDriver d, String html) {
        try {
            File f = File.createTempFile("fixture", ".html");
            f.deleteOnExit();
            try (FileWriter w = new FileWriter(f)) { w.write(html); }
            d.get(f.toURI().toString());
        } catch (Exception e) {
            // fallback to data URL
            d.get("data:text/html;charset=utf-8," + html.replace("#", "%23"));
        }
    }

    private static void expect(String name, boolean cond, String detail) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name + "   [" + detail + "]"); }
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}
