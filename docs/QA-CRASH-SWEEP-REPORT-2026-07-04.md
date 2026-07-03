# Full-Website Crash & Stability Sweep — 2026-07-03/04

**Scope:** every module of https://acme.qa.egalvanic.ai, crash/hang/error focused.
265 test executions across 8 suites (module interaction on all 15 thin modules,
boundary inputs, offline resilience, route health, mobile viewport, accessibility,
curated bug regression, AI monkey testing) + API-level verification of every
disputable claim. All runs local, headed Chrome, with a parallel backend-latency
monitor so every failure is attributed honestly (app bug vs environment).

---

## P1 — QA backend config service unstable for hours (CONFIRMED, live-measured)

`/api/company/alliance-config/acme` (+ branding/config/health endpoints) degraded
in three episodes: **20:40–22:37**, **22:50–22:56**, **23:23–01:03 IST** —
latencies 10–100s, hard 502s, `/api/health` timeouts. 99-minute sample set:
94% of alliance-config calls >3s, 42% outright failures (median 55.7s).

**User impact (all verified live):**
1. **Login page never renders** during degradation (the SPA blocks on this API
   before showing the form) — even though `POST /api/auth/login` works. Users
   locked out; reproduced by 12+ timestamped Selenium login failures locally
   AND today's CI run 44 (108 failures dominated by login/classSetup).
2. **Ops/Sales dashboards brick** with "Company information not available".
3. Cascading 502s on other services during episodes (`users/{id}/slds`,
   `lookup/node-subtypes`, `shortcut/by-node-class`) — captured as browser
   console errors by the health gates in three separate runs.

**Escalate to backend/DevOps.** Raw evidence: `latency-evidence.csv` (timestamped
curl samples, IST) + screenshots in test-output.

## P2 — The SPA has no resilience to backend failure (CONFIRMED, app-side)

The frontend's failure mode makes every backend hiccup look like a crash:

| Behavior | Evidence |
|---|---|
| **Dashboards: one failed config fetch → permanent error banner, no retry** (reload required) | "Company information not available" reproduced in 3 runs incl. minutes *after* backend recovery (BUG-012 regression, 3rd occurrence since 2026-04-21) |
| **Blank pages instead of error/loading states** | 18 routes rendered 0 visible text during degradation spikes (00:01–00:23) — Locations, Issues, Attachments, Jobs v2, AI Agent, Z-University, Reporting views, Admin/Settings views |
| **Offline = infinite spinner** | Forced-offline test on /planning hung on a spinner (no offline message/retry) |
| **Work-order creation fails silently under load** | 3 runs: create submitted, no user-visible error; 2× the WO never reached the backend, 1× created but grid never showed it |

One systemic recommendation covers all four: global fetch error boundary with
user-visible error + retry, instead of blank/spinner/permanent-banner.

## P3 — Confirmed functional bugs (env-independent, reproduced tonight on healthy backend)

1. **BUG-001**: invalid URLs render a blank page — no 404 page (regression, still open).
2. **BUG-003**: Create Issue form accepts submit with blank required Issue Class.
3. **BUG-005**: Issue Title accepts 2000+ chars — no maxlength.
4. **NEW — Task detail bounces back to list**: clicking the first task opens
   `/tasks/{id}` then returns to the list within seconds; detail unviewable.
   Reproduced 3/3 runs, last on a healthy backend (01:11). First row is a
   PM-type task — suspect PM-task detail routing. *(5-min manual confirm
   recommended before filing: click first task on /tasks.)*

## P4 — Accessibility audit (systematic, env-independent)

45+ routes carry 1–4 critical/serious WCAG violations each (axe-based
A11yVerifier; worst: /admin, /goals, /equipment-library, /reporting/legacy,
/analyzer with 3–4 each). Full per-route list in the sweep log / Extent report.

## What proved HEALTHY (crash-wise)

- **Zero JS crashes / uncaught exceptions** across all 15 modules' interaction
  tests and 5 AI monkey sessions (random clicking/typing with safety blocklist).
- All auth flows (6), all BugHunt page/global/dashboard checks (21) passed in
  healthy windows.
- No hangs on any module page with a healthy backend (previous "hangs" were a
  test-side false positive — fixed, see below).

## Quarantined (NOT reported as bugs — insufficient/contaminated evidence)

- 8 modules "blank @390px viewport": ran 23:30–23:31 inside a 53–60s
  degradation spike → retest when stable before claiming a responsive bug.
- EMP Detail 30s hang: single occurrence during degradation window.
- 06-30 CI's WOP/Task/CWO assertion failures: root-caused to test-side races /
  window-size / stale-grid issues — all fixed and re-proven green (below).

## Automation credibility work (same session)

- 34 persistent CI failures triaged to 11 signatures; **6 automation faults
  fixed** (HangDetector counting readiness gauges as spinners; grid-refilter
  races; stale-grid search; swallowed exceptions; diagnostic assert for the
  task-detail bounce), **5 already re-proven green at the UI**, and the verifier
  fixture harness extended (15/15 PASS) so the false-positive can't return.
- Every fix validated against the live app, not just compiled.

*Report generated from runs: CI 28458718306 (2026-06-30), local verify runs
20:36 & 22:42, full crash sweep 23:13–01:15 (+ latency monitors throughout).*
