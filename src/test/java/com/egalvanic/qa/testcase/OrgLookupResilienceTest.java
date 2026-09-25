package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The sign-in page must not tell a real customer "Organization Not Found" because one request failed.
 *
 * <p>Found 25 Sep 2026 (Web v2.2, bundle index-B8jzC7QC): the login page loads the company settings
 * from {@code GET /api/company/alliance-config/{subdomain}.egalvanic}. On any non-OK status or network
 * error the app falls back to {@code companyError: o?.error || "COMPANY_NOT_FOUND"}, so a 503 or a
 * dropped request shows "Organization Not Found — please check the URL", with no retry. It happened
 * on its own on QA at 10:06 UTC during the regression run.
 *
 * <p>Two controls keep the check honest: the normal load must show the sign-in page, and a genuinely
 * unknown company must still show "Organization Not Found". The failure case is expected to FAIL
 * until the product is fixed. No sign-in is needed; nothing is written.
 */
public class OrgLookupResilienceTest {

    private static final Path OUT = Paths.get("test-output", "screenshots");
    private static final String NOT_FOUND = "Organization Not Found";

    private ChromeDriver chrome() {
        ChromeOptions o = new ChromeOptions();
        o.setAcceptInsecureCerts(true);
        o.addArguments("--window-size=1400,900");
        if (!AppConstants.CHROME_BINARY.isEmpty()) o.setBinary(AppConstants.CHROME_BINARY);
        if ("true".equals(System.getProperty("headless"))) o.addArguments("--headless=new");
        return new ChromeDriver(o);
    }

    /** Page text once the SPA has settled on either the sign-in form or an error card. */
    private String settledText(ChromeDriver d) {
        String text = "";
        for (int i = 0; i < 40; i++) {
            text = String.valueOf(d.executeScript("return (document.body && document.body.innerText) || '';"));
            if (text.contains(NOT_FOUND) || text.contains("Sign into your account") || text.contains("Email Address")) break;
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return text;
    }

    private void shot(ChromeDriver d, String name) {
        try {
            Files.createDirectories(OUT);
            Files.write(OUT.resolve(name + ".png"), d.getScreenshotAs(OutputType.BYTES));
        } catch (Exception ignored) { }
    }

    @Test(priority = 1, description = "Control: the ACME address shows the sign-in page when the company lookup works")
    public void normalLoadShowsSignIn() {
        ChromeDriver d = chrome();
        try {
            d.get(AppConstants.BASE_URL);
            String text = settledText(d);
            shot(d, "orgLookup_normal");
            Assert.assertFalse(text.contains(NOT_FOUND), "Normal load showed Organization Not Found:\n" + text);
            Assert.assertTrue(text.contains("Email Address") || text.contains("Sign into your account"),
                    "Normal load should show the sign-in page. Page text:\n" + text);
        } finally { d.quit(); }
    }

    @Test(priority = 2, description = "Control: a company that does not exist still shows Organization Not Found")
    public void unknownCompanyShowsNotFound() {
        ChromeDriver d = chrome();
        try {
            d.get("https://no-such-company-qa-check." + AppConstants.QA_DOMAIN);
            String text = settledText(d);
            shot(d, "orgLookup_unknownCompany");
            Assert.assertTrue(text.contains(NOT_FOUND), "Unknown company should show Organization Not Found. Page text:\n" + text);
        } finally { d.quit(); }
    }

    @Test(priority = 3, description = "A 503 on the company lookup must not tell ACME users their organization does not exist")
    public void serverBusyIsNotOrganizationNotFound() {
        ChromeDriver d = chrome();
        try {
            Map<String, Object> src = new HashMap<>();
            src.put("source", "(function(){var f=window.fetch;window.fetch=function(u,o){try{if(String(u).indexOf('company/alliance-config')>=0)"
                    + "{return Promise.resolve(new Response('<html>Service Unavailable</html>',{status:503,headers:{'Content-Type':'text/html'}}));}}catch(e){}"
                    + "return f.apply(this,arguments);};})();");
            d.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", src);
            d.get(AppConstants.BASE_URL);
            String text = settledText(d);
            shot(d, "orgLookup_503");
            Assert.assertFalse(text.contains(NOT_FOUND),
                    "A 503 from /api/company/alliance-config made the real ACME address say 'Organization Not Found' "
                    + "(the company exists). Expected a 'could not reach the server' message with Try again.");
        } finally { d.quit(); }
    }

    @Test(priority = 4, description = "A dropped company lookup must not tell ACME users their organization does not exist")
    public void networkErrorIsNotOrganizationNotFound() {
        ChromeDriver d = chrome();
        try {
            d.executeCdpCommand("Network.enable", new HashMap<>());
            Map<String, Object> block = new HashMap<>();
            block.put("urls", Collections.singletonList("*company/alliance-config*"));
            d.executeCdpCommand("Network.setBlockedURLs", block);
            d.get(AppConstants.BASE_URL);
            String text = settledText(d);
            shot(d, "orgLookup_networkError");
            Assert.assertFalse(text.contains(NOT_FOUND),
                    "A failed /api/company/alliance-config request made the real ACME address say 'Organization Not Found'.");
        } finally { d.quit(); }
    }
}
