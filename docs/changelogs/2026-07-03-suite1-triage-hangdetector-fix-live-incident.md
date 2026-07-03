# 2026-07-03 — Suite 1 failure triage, HangDetector fix, and live backend incident

**Prompt:** Check everything from CI run 28458718306 (Parallel Suite 1 — Core
Regression, 848 TCs, 2026-06-30), especially flows. Only REAL bugs, no false
assertions — client- and manager-ready quality. Then continue with a
full-website crash sweep. All work done locally, step by step.

## Method (quality-first, 5 parts)

1. **Classify** — one deep-read analysis per failure signature (test source +
   page objects + exact locators + CI artifacts), adversarially cross-checked.
2. **API ground truth** — live QA API queried for every disputable claim.
3. **Fix** only what is provably automation's fault.
4. **Prove** — `VerifierSelfValidation` fixture harness (15/15 PASS).
5. **Re-run** the failing tests locally headed (in progress — see incident).

## The 34 persistent failures = 11 signatures, fully triaged

### Real app/backend findings (client-reportable) — 3

| Finding | Evidence |
|---|---|
| **BUG-012 regression** — "Company information not available" error banner on Ops Dashboard + Sales Overview | Genuine `.MuiAlert-*Error` in DOM (not keyword false-positive); tracked history: found 2026-04-21, fixed, regressed 2026-05-28; root = alliance-config/branding service |
| **Sales-dashboard latency** | `/api/company/{id}/sales-dashboard` measured **2.7–6.1s** over 4 calls today (ops-dashboard: 0.5s) |
| **Backend 502 instability** | Phase4's 2 "severe console errors" were real 502s on `/api/users/{id}/slds` — the health gate did its job |

### Automation faults (fixed this session) — 4 fixes

| Test failure | Root cause | Fix |
|---|---|---|
| Arc Flash / Maintenance "Page HUNG" | `HangDetector` spinner selector counts **determinate** MUI progress (readiness gauges, completion bars — `role=progressbar` + `aria-valuenow`) as loading spinners → deterministic false "hang" on healthy pages | `HangDetector.java`: only INDETERMINATE progress (no `aria-valuenow`, no `-determinate` class) counts as a spinner |
| WOP_020 "Edit should open editor" | `openEditForPlan` gave up immediately if the row wasn't rendered yet (async grid refilter race) | `PlanningPage.waitForExactRow()` poll (15s) before lookup in `openEditForPlan` + `openDeleteForPlan` |
| WOP_025 "Delete dialog should open" | Same race via `search()` fixed `pause(2500)` | Same fix |
| CWO_003 "Created WO should appear in grid" | **WO was created** (backend record `AutoTest_WO_44022`, `created_at 2026-06-30T23:50:01Z`, priority High — during that exact CI job). Grid check filtered a stale client-side dataset and could never see it; also `catch (Exception)` swallowed mid-flow crashes into silent green | `findWOInGrid` refreshes the page on attempts 4 & 7 to force refetch; create-flow crash now rethrows as AssertionError |

### Already fixed before this session — 2

WOP_003 (column headers) + WOP_018 (status cell): MUI DataGrid **column
virtualization** in the ~800×600 default headless window — commit `647561c`
(2026-07-02) added `--window-size=1920,1080`; 07-02 failed-list shows zero WOP
failures since.

### Verification of the fixes

- `mvn test-compile` clean.
- `VerifierSelfValidation` (real Chrome + HTML fixtures): **15/15 PASS**, including
  - existing "HangDetector flags infinite spinner" (fix did not blunt the verifier), and
  - **new negative fixture** "HangDetector ignores determinate gauges" (locks the regression out).
- UI-level rerun: launched but blocked by the live incident below; will re-run on recovery.

## 🔴 LIVE INCIDENT discovered during verification (2026-07-03 ~20:40 IST)

The company-config/branding backend is severely degraded on QA:

| Endpoint | Measured |
|---|---|
| `/api/company/alliance-config/acme` | **68.6s** (20:44) |
| `/api/branding/company/acme` | **90.6s**, then 21.8s |
| `/api/health` | **timeout at 100s** (20:46) |
| `/api/auth/login` (POST, Cognito) | 4.3s — works |
| ops-dashboard (control, earlier) | 0.5s |

Impact chain (verified in the shipped JS bundle `index-B_cyQD0f.js`):
the SPA fetches `/company/alliance-config/{subdomain}` at startup; on
failure it renders **"Company information not available"** (BUG-012's exact
string). While degraded, **the login form never renders** — users cannot log
in even though the auth API works. Local test run reproduced this live:
every class failed with "Login failed after 3 attempts" (12+ attempts,
timestamps correlate with the latency samples in
`latency-evidence.csv`).

This is the probable root cause of the 2026-06-30 BUG-012 banners AND of the
sales-dashboard slowness (2.7–6.1s) — one degraded service surfacing three ways.

## Files changed

- `src/main/java/com/egalvanic/qa/utils/verify/HangDetector.java` — determinate-progress exemption
- `src/main/java/com/egalvanic/qa/pageobjects/PlanningPage.java` — `waitForExactRow` + use in edit/delete
- `src/test/java/com/egalvanic/qa/testcase/WorkOrderTestNG.java` — grid refetch + no swallowed crash
- `src/test/java/com/egalvanic/qa/testcase/VerifierSelfValidation.java` — determinate-gauge negative fixture

## Pending on environment recovery (watcher running)

1. Local headed rerun of the 11 triaged tests (prove fixes at UI level).
2. Full-website crash sweep (`crash-sweep.xml`): AuthSmoke + Phase5 (all 15
   modules) + Phase4 + BugHuntPages/Global/Dashboard + DeepBugVerification +
   Monkey — ~61 executions, every one behind the health gates.

## Incident annex — measured statistics (20:44–22:23 IST, 99 minutes)

| Endpoint | Samples | >3s | 502/timeout | Median | Max |
|---|---|---|---|---|---|
| `company/alliance-config/acme` | 33 | 31 (94%) | 14 (42%) | 55.7s | 100s (timeout) |
| `/api/health` | 26 | 17 | 10 | 13.9s | 100s (timeout) |
| `branding/company/acme` | 8 | 6 | 0 | 34.6s | 75.1s |
| `company/config/acme` | 8 | 4 | 0 | 7.0s | 49.9s |
| `auth/login` POST (control) | 1 | 1 | 0 | 4.3s | 4.3s |

Brief healthy windows (e.g. 22:07: alliance-config 0.73s, health 0.85s) followed
by re-degradation — consistent with an overloaded/flapping upstream, not a hard
outage. Full raw data: latency-evidence.csv (session scratchpad; timestamps IST).

Impact confirmed live: login form unrenderable during degradation (12+ Selenium
login failures across 5 classes, timestamps correlating with slow samples);
`/api/health` itself unreliable, so upstream monitoring may be blind or alarming
already. Recommend backend/DevOps escalation for the QA config service.

## Post-recovery fix-verification rerun (22:42–22:57 IST)

20 tests ran. **5 previously-failing tests now PASS at UI level**, proving the fixes:
Arc Flash hang ✅, Maintenance hang ✅ (HangDetector determinate-gauge fix),
WOP_003 ✅, WOP_018 ✅ (window size), WOP_020 ✅ (waitForExactRow).

7 still failed — reconciled against the backend timeline (CSV):

- Backend RE-DEGRADED mid-run: alliance-config 0.7s (22:43) → 3.4s (22:50) →
  **60s timeouts (22:55–22:56)**. Incident is recurring, not a one-off.
- Phase4 ×2: REAL 502s again (`lookup/node-subtypes`, `shortcut/by-node-class`).
- Ops/Sales "Company information not available": REAL app bug, now precisely
  characterized — **one slow/failed config fetch permanently bricks the dashboard
  (no retry until page reload)**. Reproduced 06-30 (CI) and tonight (local).
- Task ET_003 / WOP_025 / CWO_003: ran 22:52–22:57 inside the timeout window —
  CONTAMINATED, queued for re-verification. Note: tonight's `AutoTest_WO_62934`
  is absent from the backend (create POST failed under outage) — the opposite
  failure mode from 06-30, consistent with env causation both times.

Next: stricter stability watcher (5×2.5min healthy samples), then the full
crash sweep + contaminated-test re-verification in one run.
