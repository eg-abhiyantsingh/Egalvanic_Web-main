# Parallel Workflow — Dashboard UI Refactor

> **Author:** Abhiyant Singh (with Claude AI assistance)  
> **Date:** April 9, 2026  
> **Prompt:** "I want yml format same as full-suite.yml — clean UI, follow same format"

---

## The Problem

The initial `parallel-suite.yml` (created earlier the same day) used raw Maven output:

```yaml
# BEFORE — raw Maven output dumps everything
run: |
  mvn test -B \
    -DsuiteXmlFile="${SUITE_XML}" \
    -Dheadless=true \
    2>&1 | tee /tmp/maven-output.log
```

This produced **hundreds of lines** of noisy output per group:
- Maven compilation messages
- Surefire plugin initialization
- Full stack traces for failures
- TestNG framework internals
- XML report generation messages

The sequential `full-suite.yml` had none of this noise because it used the dashboard script.

## The Solution

Replaced the raw `mvn test | tee` approach with the **same dashboard script** used by `full-suite.yml`:

```yaml
# AFTER — clean dashboard UI (identical to full-suite.yml)
- name: Run tests with dashboard
  continue-on-error: true
  env:
    FULL_SUITE_GROUP: ${{ matrix.group }}
  run: |
    bash .github/scripts/full-suite-dashboard.sh
```

### How it works

The `full-suite-dashboard.sh` script already supports a `FULL_SUITE_GROUP` environment variable:

| Value | Behavior |
|-------|----------|
| `all` or unset | Runs all 10 groups sequentially (full-suite.yml mode) |
| `auth-site-connection` | Runs only that group (parallel-suite.yml mode) |
| `workorder-issue` | Runs only that group |
| etc. | Any valid group key |

When running a single group, the dashboard script:
1. Runs Maven in the **background** with `-B -q` (batch + quiet)
2. Redirects output to a temp log file
3. **Monitors** the log for `PASSED:/FAILED:/SKIPPED:` lines from TestNG's ConsoleProgressListener
4. Prints **one clean line per test**: icon + test name + duration
5. Collapses raw Maven output in `::group::` blocks (hidden by default in GitHub Actions)
6. Writes `SUITE_PASSED`, `SUITE_FAILED`, `SUITE_SKIPPED`, `SUITE_DURATION`, `SUITE_RESULT` to `GITHUB_ENV`

### Before vs After in GitHub Actions UI

**Before (raw output):**
```
[INFO] Scanning for projects...
[INFO] Building eGalvanic-Web 1.0-SNAPSHOT
[INFO] --- maven-surefire-plugin:3.2.5:test ---
[INFO] Using configured provider org.apache.maven.surefire.testng.TestNGProvider
[INFO] Running TestSuite
... (200+ lines of noise)
PASSED: LoginTest.testValidLogin (3s)
FAILED: LoginTest.testInvalidLogin (5s)
org.openqa.selenium.NoSuchElementException: ...
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(...)
... (50+ lines of stack trace)
```

**After (dashboard output):**
```
  Running single group: Auth + Site + Connection

  -- Group 1/1: Auth + Site + Connection (130 tests) --

    ✅  ValidLogin                                           3s
    4/130 completed   ⏱️ 12s

    ❌  InvalidLogin
         Reason: NoSuchElementException: element not found
    5/130 completed   ⏱️ 17s
```

## Other Changes in This Prompt

### Simplified result parsing

The old workflow had a separate "Parse test results" step that read `testng-results.xml` directly. Now the dashboard script handles all result parsing and writes to `GITHUB_ENV`, so the "Write result JSON" step just reads those env vars.

**Removed step** (no longer needed):
- "Parse test results" — dashboard script does this internally

**Simplified steps:**
- "Write result JSON" — reads `SUITE_PASSED` etc. from `GITHUB_ENV` instead of `steps.results.outputs.*`
- "Group summary" — same simplification

### Documentation restructure

Moved from a monolithic `AI-FEATURES-CHANGELOG.md` to **one file per prompt**:
- Extracted Section 13 into `013-parallel-ci-workflow.md`
- This file is `014-parallel-dashboard-refactor.md`
- Going forward, each prompt creates its own numbered `.md` file

## Files Modified

| File | Change |
|------|--------|
| `.github/workflows/parallel-suite.yml` | Replaced raw `mvn test \| tee` with `bash full-suite-dashboard.sh`, simplified result parsing |
| `docs/ai-features/AI-FEATURES-CHANGELOG.md` | Replaced Section 13 inline content with pointer to standalone file |
| `docs/ai-features/013-parallel-ci-workflow.md` | NEW — Extracted Section 13 content |
| `docs/ai-features/014-parallel-dashboard-refactor.md` | NEW — This file |
