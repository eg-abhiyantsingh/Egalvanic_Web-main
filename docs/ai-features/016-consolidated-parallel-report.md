# Consolidated Client Report for Parallel CI

> **Author:** Abhiyant Singh (with Claude AI assistance)  
> **Date:** April 9, 2026  
> **Prompt:** "Right now we have lots of email. I want a single client report even in parallel execution. Only single HTML file."

---

## Problem

In parallel CI (10 groups running simultaneously), each group runs in its own JVM. Each one triggers `@AfterSuite` -> `flushReports()` -> `EmailUtil.sendReportEmail()`, generating a separate `Client_Report_*.html`. If email was enabled, the client would receive **10 separate emails** — one per group.

Additionally, `SEND_EMAIL_ENABLED` in `AppConstants.java` was hardcoded as a `boolean` literal. The CI env var `SEND_EMAIL_ENABLED: "false"` was being **ignored** because `getEnv()` was never called for this field.

---

## Solution Architecture

### Phase 1 (Per-Group): Disable Email, Generate testng-results.xml

Each parallel group:
- Runs with `SEND_EMAIL_ENABLED: "false"` (env var override now works)
- Generates its own `testng-results.xml` in `target/surefire-reports/`
- Uploads reports as artifact (`reports-<group>`)

### Phase 2 (Summary Job): Merge and Send One Report

The summary job:
1. **Downloads** all `reports-*` artifacts (each contains `testng-results.xml`)
2. **Runs** `consolidated-report.py` which:
   - Finds all `testng-results.xml` files recursively
   - Parses test results (name, description, status, duration, class)
   - Maps class names to module names (e.g., `IssuePart2TestNG` -> "Issues")
   - Groups tests by module in a logical order
   - Generates a single, self-contained HTML report with:
     - Summary cards (total, passed, failed, skipped)
     - Progress bar with pass rate
     - Collapsible module sections (failed modules auto-expanded)
     - Test rows with status badges and duration
   - **Sends one email** with the report attached
3. **Uploads** the consolidated report as artifact (`consolidated-client-report`)

---

## Key Changes

### 1. AppConstants.java — `SEND_EMAIL_ENABLED` Now Configurable

```java
// BEFORE: Hardcoded — CI env var was IGNORED
public static final boolean SEND_EMAIL_ENABLED = true;

// AFTER: Reads from env var, defaults to "true" for local
public static final boolean SEND_EMAIL_ENABLED =
        Boolean.parseBoolean(getEnv("SEND_EMAIL_ENABLED", "true"));
```

Behavior:
- **Local**: defaults to `true` (sends email after test run)
- **CI per-group**: `SEND_EMAIL_ENABLED: "false"` -> no email
- **CI summary**: Python script handles email (not Java)

### 2. consolidated-report.py — Report Aggregator

Standalone Python script (no external dependencies). Uses:
- `xml.etree.ElementTree` for parsing TestNG XML
- `smtplib` + `email.mime` for SMTP email
- Class-to-module mapping for 35+ test classes

### 3. parallel-suite.yml — Summary Job Additions

New steps added to the summary job:
- `Checkout code` (to access the Python script)
- `Download all test reports` (pattern: `reports-*`)
- `Generate consolidated client report` (runs `consolidated-report.py`)
- `Upload consolidated report` (artifact: `consolidated-client-report`)

Email is sent only when `EMAIL_PASSWORD` secret is configured:
```yaml
SEND_EMAIL_ENABLED: ${{ secrets.EMAIL_PASSWORD != '' && 'true' || 'false' }}
```

---

## Email Flow — Before vs After

### Before (Broken)
```
Group 1 → @AfterSuite → sendEmail() → EMAIL #1
Group 2 → @AfterSuite → sendEmail() → EMAIL #2
...
Group 10 → @AfterSuite → sendEmail() → EMAIL #10
Summary → (no email)
```

### After (Fixed)
```
Group 1 → @AfterSuite → SEND_EMAIL_ENABLED=false → no email
Group 2 → @AfterSuite → SEND_EMAIL_ENABLED=false → no email
...
Group 10 → @AfterSuite → SEND_EMAIL_ENABLED=false → no email
Summary → consolidated-report.py → ONE EMAIL with single HTML report
```

---

## Files Modified

| File | Change |
|------|--------|
| `.github/scripts/consolidated-report.py` | New: report aggregator + email sender |
| `.github/workflows/parallel-suite.yml` | Summary job: checkout, download, generate, upload, email |
| `src/main/java/.../AppConstants.java` | `SEND_EMAIL_ENABLED` uses `getEnv()` |
| `docs/ai-features/016-consolidated-parallel-report.md` | This file |
