# Fix: testInvalidCredentialsRejected flake + summary-on-skipped-test-group regression

- **Date:** 2026-06-18
- **Prompt:** "[run 27759933386] Still some test case is failing … debug properly, use Playwright to understand the E2E flow, go deeper, iterate."
- **Run:** Parallel Suite 2 (RBAC-only dispatch) — `RBAC — All` failed; `summary` failed.
- **Type:** Test-robustness fix + CI regression fix (no product bug).

---

## Two failures, two root causes (both diagnosed, not guessed)

### 1. `RoleLoginE2ETest.testInvalidCredentialsRejected` — flaky wait (the failing test)

**Symptom (CI):** `Invalid credentials should keep the user on the login page. URL:
https://acme.qa.egalvanic.ai/ expected [true] but found [false]`.

**Diagnosed live with Playwright** (logged out to a clean session, submitted wrong creds, recorded a
16s URL/DOM timeline):
- The app renders the login form at the **root path `/`** — `navigateToLogin()` loads `BASE_URL`
  (not `/login`), and the path **stays `/`** through a failed login. So `onLoginPage()`'s
  `url.contains("/login")` check is **always false** here; it relied entirely on `#email` being
  visible.
- On a clean/fast session the "**Invalid credentials**" error + form re-render settled at **~2s**,
  but the QA app cold-loads are very slow (I measured **25–35s** page loads). The test did a fixed
  `sleep(5000)` then a **single** check — in slow CI that lands mid-re-render (spinner → error),
  when `#email` is briefly detached → `onLoginPage()` false → fail. Pure race; the product is fine
  (invalid creds never reach the app).

**Fix** (`RoleLoginE2ETest`):
- Replaced `sleep(5000)` + single check with a **poll-until-settled** wait (`WebDriverWait` up to
  30s for `onLoginPage() || reachedApp()`), then assert. More robust **and** usually faster (returns
  as soon as the error renders).
- Broadened `onLoginPage()` to detect the root-path login form by multiple signals (`#email`,
  `input[type=password]`, or the "Sign into your account" / "Invalid credentials" text) so a briefly
  detached `#email` during the error re-render no longer reads as "not on login page".
- **Validated headed (non-headless) against the live app: PASS in 31s** (the slow load that would
  have flaked the old code).

### 2. `summary` job — regression from the new dynamic matrix

**Symptom:** `summary` failed with "No test results collected" → `overall_status=error`.

**Cause:** the multi-select change made `test-group` skippable (when no functional modules are
ticked — this dispatch selected only RBAC). `summary` was `needs: test-group` + `if: always()`, so
it still ran, found **no result artifacts**, and errored.

**Fix** (both `parallel-suite.yml` and `parallel-suite-2.yml`): gate summary
`if: ${{ always() && needs.test-group.result != 'skipped' }}` — it still runs on test-group
success **or** failure (to aggregate), but is **skipped** when test-group didn't run, so an
RBAC-only (or empty) dispatch no longer fails spuriously.

## Validation

- Both workflows parse; Java compiles.
- The login test passed headed against the live (slow) app.
- CI re-validation to follow: `rbac.yml which=login-e2e` (login suite headless) for the test fix,
  and a Suite-2 RBAC-only dispatch to confirm `summary` now **skips** instead of failing.
