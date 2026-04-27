package com.egalvanic.qa.testcase;

import com.egalvanic.qa.constants.AppConstants;
import com.egalvanic.qa.utils.ExtentReportManager;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OWASP A06 — Vulnerable and Outdated Components.
 *
 * Why this class exists:
 *   The eGalvanic QA framework's pom.xml pins specific Selenium / TestNG / Faker /
 *   ExtentReports versions. Each pinned version has a CVE history. This test class
 *   reads pom.xml and flags ANY dependency that is older than a known cutoff (most
 *   recent stable as of the file's last update) — so the suite has a built-in
 *   tripwire against shipping known-vulnerable libraries.
 *
 *   This is a STATIC test (no browser, no API). It runs against the local pom.xml.
 *   That makes it deterministic, fast (< 1 second), and CI-friendly.
 *
 * What we test:
 *   TC_DEP_01  pom.xml exists and parses
 *   TC_DEP_02  Selenium >= 4.20 (Selenium 4.x has had multiple CVEs in older patches)
 *   TC_DEP_03  TestNG >= 7.10 (older versions had log4j transitive issues)
 *   TC_DEP_04  No dependency uses a wildcard or RANGE version (e.g., [1.0,))
 *              — wildcards make CVE auditing impossible
 *   TC_DEP_05  Known-vulnerable libraries are absent (e.g., commons-collections 3.x,
 *              log4j-core < 2.17, jackson-databind < 2.13.5)
 *
 * Doesn't extend BaseTest — no browser needed. Standalone class with own lifecycle
 * to keep CI cost minimal (registering this with the suite adds ~1s, not 30s).
 */
public class OwaspKnownVulnsTestNG {

    private static final String MODULE = "OWASP Security";
    private static final String FEATURE = "Vulnerable & Outdated Components";

    private static final File POM = new File("pom.xml");

    /** Each entry: artifactId -> {minVersion, knownIssue}. */
    private static final Map<String, String[]> VERSION_FLOORS = new LinkedHashMap<>();
    static {
        // Format: { minVersion, "human-readable concern" }
        VERSION_FLOORS.put("selenium-java",
            new String[]{"4.20.0", "Pre-4.20 Selenium has CDP / netty CVEs (CVE-2024-*)"});
        VERSION_FLOORS.put("testng",
            new String[]{"7.10.0", "Older TestNG pulled vulnerable jcommander / yaml deps"});
    }

    /** Dependencies that are flat-out forbidden — known-bad libraries / versions. */
    private static final List<String[]> FORBIDDEN_DEPS = Arrays.asList(
        new String[]{"log4j-core", "<2.17.0", "Log4Shell CVE-2021-44228 / CVE-2021-45046"},
        new String[]{"commons-collections", "3.2.2-", "Java deserialization gadgets pre-3.2.2"},
        new String[]{"jackson-databind", "<2.13.5", "Multiple deserialization CVEs in older 2.x"}
    );

    private String pomContent = "";

    @BeforeClass
    public void readPom() {
        ExtentReportManager.initReports();
        if (!POM.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(POM))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            pomContent = sb.toString();
        } catch (Exception ignored) {}
    }

    @AfterClass
    public void teardown() {
        // No browser, no driver to clean up. Reports are flushed by the test-runner.
    }

    @Test(priority = 1, description = "TC_DEP_01: pom.xml is present and non-empty")
    public void testTC_DEP_01_PomExists() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_DEP_01_PomExists (OWASP A06)");
        Assert.assertTrue(POM.exists(),
            "pom.xml not found at " + POM.getAbsolutePath()
            + " — cannot audit dependencies");
        Assert.assertFalse(pomContent.isBlank(),
            "pom.xml is empty or unreadable");
        ExtentReportManager.logPass("pom.xml read OK (" + pomContent.length() + " bytes)");
    }

    @Test(priority = 2, description = "TC_DEP_02: Each version-floored library meets minimum (no known CVE versions)")
    public void testTC_DEP_02_VersionFloorsHonored() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_DEP_02_VersionFloorsHonored (OWASP A06)");
        if (pomContent.isBlank()) throw new org.testng.SkipException("pom not loaded");

        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : VERSION_FLOORS.entrySet()) {
            String artifact = entry.getKey();
            String minVer = entry.getValue()[0];
            String concern = entry.getValue()[1];
            String found = findArtifactVersion(artifact);
            if (found == null) continue;  // not in pom; skip
            if (compareSemver(found, minVer) < 0) {
                violations.add(artifact + " " + found + " < " + minVer + " (" + concern + ")");
            }
        }

        Assert.assertTrue(violations.isEmpty(),
            "OWASP A06 — Outdated dependencies found:\n  - "
            + String.join("\n  - ", violations)
            + "\nUpgrade in pom.xml and re-run.");
        ExtentReportManager.logPass("All version floors honored ("
            + VERSION_FLOORS.size() + " checked)");
    }

    @Test(priority = 3, description = "TC_DEP_03: No wildcard or open-range dependency versions")
    public void testTC_DEP_03_NoWildcardVersions() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_DEP_03_NoWildcardVersions (OWASP A06)");
        if (pomContent.isBlank()) throw new org.testng.SkipException("pom not loaded");

        // Match version tags whose value is a wildcard (* or LATEST) or open range like [1.0,)
        Pattern p = Pattern.compile(
            "<version>\\s*(\\[\\d|LATEST|RELEASE|\\*)\\s*[^<]*</version>",
            Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(pomContent);
        List<String> hits = new ArrayList<>();
        while (m.find()) hits.add(m.group(0));

        Assert.assertTrue(hits.isEmpty(),
            "OWASP A06 — pom.xml uses wildcard/RANGE versions, which break CVE auditing "
            + "(today's build differs from yesterday's silently). Pin every version. "
            + "Found: " + hits);
        ExtentReportManager.logPass("All dependency versions are pinned");
    }

    @Test(priority = 4, description = "TC_DEP_04: Forbidden vulnerable libraries are absent")
    public void testTC_DEP_04_NoKnownVulnerableLibraries() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_DEP_04_NoKnownVulnerableLibraries (OWASP A06)");
        if (pomContent.isBlank()) throw new org.testng.SkipException("pom not loaded");

        List<String> hits = new ArrayList<>();
        for (String[] forbidden : FORBIDDEN_DEPS) {
            String artifact = forbidden[0];
            String concern = forbidden[2];
            if (pomContent.contains("<artifactId>" + artifact + "</artifactId>")) {
                String version = findArtifactVersion(artifact);
                hits.add(artifact + " " + (version == null ? "(unknown ver)" : version)
                    + " — " + concern);
            }
        }

        Assert.assertTrue(hits.isEmpty(),
            "OWASP A06 — Known-vulnerable library detected in pom.xml:\n  - "
            + String.join("\n  - ", hits));
        ExtentReportManager.logPass("No forbidden libraries present ("
            + FORBIDDEN_DEPS.size() + " checked)");
    }

    @Test(priority = 5, description = "TC_DEP_05: pom.xml declares Maven Enforcer or dependency-check plugin")
    public void testTC_DEP_05_DepCheckPluginRecommended() {
        ExtentReportManager.createTest(MODULE, FEATURE,
            "TC_DEP_05_DepCheckPluginRecommended (OWASP A06)");
        if (pomContent.isBlank()) throw new org.testng.SkipException("pom not loaded");

        boolean hasEnforcer = pomContent.contains("maven-enforcer-plugin");
        boolean hasDepCheck = pomContent.contains("dependency-check-maven")
            || pomContent.contains("org.owasp");
        boolean hasVersions = pomContent.contains("versions-maven-plugin");

        // Soft warning, not hard fail — projects can use external SCA instead.
        // We log the recommendation but only fail if NONE of the three is present.
        if (!hasEnforcer && !hasDepCheck && !hasVersions) {
            // Log an audit warning, mark as soft-fail by skipping. AppSec teams
            // sometimes prefer an out-of-band scanner (Snyk, Dependabot), which is fine.
            throw new org.testng.SkipException(
                "OWASP A06 — recommend adding `dependency-check-maven` or "
                + "`versions-maven-plugin` to surface CVE drift in CI. "
                + "Skip rather than fail since SCA may be handled externally.");
        }
        ExtentReportManager.logPass(
            "Dependency hygiene plugin present (enforcer=" + hasEnforcer
            + ", depCheck=" + hasDepCheck + ", versions=" + hasVersions + ")");
    }

    /** Locate the version tag immediately following a given artifactId. */
    private String findArtifactVersion(String artifactId) {
        // Pattern: <artifactId>X</artifactId>...<version>Y</version> within the same dependency block
        Pattern p = Pattern.compile(
            "<artifactId>\\s*" + Pattern.quote(artifactId) + "\\s*</artifactId>\\s*"
            + "(?:<scope>[^<]*</scope>\\s*)?"
            + "<version>\\s*([^<]+?)\\s*</version>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(pomContent);
        if (m.find()) return m.group(1).trim();
        // Fallback — version sometimes precedes artifactId
        Pattern p2 = Pattern.compile(
            "<version>\\s*([^<]+?)\\s*</version>\\s*"
            + "<artifactId>\\s*" + Pattern.quote(artifactId) + "\\s*</artifactId>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m2 = p2.matcher(pomContent);
        if (m2.find()) return m2.group(1).trim();
        return null;
    }

    /** Returns negative if a < b, 0 if equal, positive if a > b. Handles X.Y.Z + qualifiers. */
    private int compareSemver(String a, String b) {
        // Strip qualifier (e.g., -SNAPSHOT, -RC1)
        a = a.replaceAll("-.*$", "");
        b = b.replaceAll("-.*$", "");
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        int len = Math.max(aParts.length, bParts.length);
        for (int i = 0; i < len; i++) {
            int aVal = i < aParts.length ? safeParse(aParts[i]) : 0;
            int bVal = i < bParts.length ? safeParse(bParts[i]) : 0;
            if (aVal != bVal) return aVal - bVal;
        }
        return 0;
    }

    private int safeParse(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    // Reference to AppConstants prevents an unused-import warning if we ever need it
    @SuppressWarnings("unused")
    private static final String UNUSED_KEEPALIVE = AppConstants.MODULE_BUG_HUNT;
}
