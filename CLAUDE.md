# CLAUDE.md — eGalvanic Web Automation

Project memory for Claude Code. Read automatically at session start.

## Working preferences (from the repo owner)
- **Act autonomously — do NOT ask for permission.** Complete the full task end to
  end (wire it in, run it, fix it) rather than stopping at "here's a plan / want me
  to continue?". Only ask when a decision is genuinely the user's to make.
- **Always validate, don't just compile.** After implementing, prove the work is
  actually useful: run it, execute the relevant tests, and show evidence it catches
  what it claims to. Compiling is necessary but not sufficient.
- The hardcoded credentials / Gmail app-password in `AppConstants` (lines 23, 274)
  are intentional for this QA project — **leave them as-is, do not move to secrets.**

## Project shape
- Java 11, Selenium 4.29, TestNG 7.8, REST Assured, ExtentReports. React + MUI SPA.
- App under test: `https://acme.qa.egalvanic.ai` (`BASE_URL`, overridable via env).
- `src/main/.../pageobjects` (POM, no shared BasePage), `utils`, `utils/ai`
  (self-healing, Claude client, monkey/visual/bug-detection), `utils/verify`
  (destructive-testing verifiers — see below). Tests in `src/test/.../testcase`.
- `BaseTest` = one browser session per **class**, auto login + site selection,
  recovery from the app's "Application Error" / update-alert overlays.

## Destructive-testing verifiers (`com.egalvanic.qa.utils.verify`)
Built + validated 2026-06-02 (see `docs/test-coverage/FRAMEWORK_AUDIT_AND_HARDENING.md`).
All hard-fail, no swallowed exceptions, reuse `ScreenshotUtil`/`ExtentReportManager`.
- `BrowserErrorCapture` — uncaught + promise + resource + console + native SEVERE JS errors.
- `HangDetector` — frozen page / infinite spinner / pending network → fail with evidence.
- `AssetLoadVerifier` — `naturalWidth` images, PDF render, 4xx/5xx request capture.
- `UIStateValidator` — blank page / error banner / clipped layout.
- `StateIntegrityChecker` — before/after snapshots (loss / duplication / corruption).

Regression harness: `VerifierSelfValidation` (a `main()`, excluded from all suites).
Run with HTML fixtures of known bugs to prove the verifiers still catch them:
```
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -cp "target/classes:target/test-classes:$(cat /tmp/cp.txt)" \
  [-Dchrome.binary=/path/to/chrome -Dwebdriver.chrome.driver=/path/to/chromedriver] \
  com.egalvanic.qa.testcase.VerifierSelfValidation
```

## Health gates wired into BaseTest
- `BaseTest.classSetup()` installs `BrowserErrorCapture` + failed-request capture.
- `BaseTest.verifyPageHealth(name)` — hard-asserts no JS errors / failed requests /
  broken UI on the current page. Call it after navigations in module tests.
- `@AfterMethod` monitors console + network on every PASS; set
  `STRICT_HEALTH_GATES=true` to make those global checks hard-fail the build.
- `BaseTest` honours `-Dchrome.binary` / `CHROME_BINARY` for non-managed Chrome.
