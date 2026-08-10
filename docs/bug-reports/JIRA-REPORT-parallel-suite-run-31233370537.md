# Jira Report — Parallel Full Suite, run 31233370537 (2026-08-08)

**Run:** https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/actions/runs/31233370537
**Env:** QA · https://acme.qa.egalvanic.ai · build **V1.36** · Chrome (GitHub Actions Linux)
**Triaged:** 2026-08-10

## Executive summary

| | |
|---|---|
| Tests executed | **1449** |
| Passed | 1303 |
| **Failed** | **102** (+1 failed `@BeforeClass` = 103 events) |
| Skipped | 44 |
| Distinct failure signatures | 88 |
| **REAL product bugs to file** | **1** |
| Real but already tracked | 1 (ZP-2025) |
| Open product questions (not tickets) | 3 |
| Test-infra ticket | 1 |
| Test-side / environment / stale-vs-V1.36 | ~97 |

> **Note on the expected count:** this run had **103** failure events, not 200+. The two
> 2026-08-10 runs (31368731158, 31376211945) were Auth+Site-only verification runs with 1
> failure each. If a 200+ run exists it is older than 2026-08-08 — say the word and I will
> triage that one instead.

**Headline: 103 failures produce 1 new Jira bug.** That is the correct outcome, not an
under-count — the reasoning for every downgrade is recorded below so it can be challenged.

---

## 🔴 FILE THIS — 1 bug

### [Goals] `/goals` crashes to the application error boundary — module unusable

**Type:** Bug · **Severity:** High · **Priority:** P1
**Component:** Goals (Sales) · **Affects version:** V1.36 · **Environment:** QA

**Environment**
- Environment: QA (`https://acme.qa.egalvanic.ai`), build V1.36
- Platform: Web · Browser: Chrome (latest stable, GitHub Actions Linux runner)
- Site: `test site` · Role: Super Admin (also reproduced under internal Admin)

**Preconditions**
- User is logged in with access to the Sales area.

**Steps to Reproduce**
1. Log in to `https://acme.qa.egalvanic.ai`.
2. Navigate to **Goals** (`/goals`).
3. Observe the page.

**Actual Result**
The page renders the application's crash boundary — *"Something went wrong / We encountered an
unexpected error. Our team has been notified"* with **Refresh Page** / **Try Again** buttons and
a Sentry crash-report dialog. No app shell, no sidebar. Underlying error:
`TypeError: Cannot read properties of undefined (reading 'length')`.
**Sentry error ID: `5daf74a67017`.**

**Expected Result**
The Goals module renders its content (or a normal empty state).

**Evidence**
- `NewModulesSmokeTestNG#testTC_NM_03_Goals` — *"Goals module did not render"*
- Screenshot in run artifacts (`reports-dashboard-bughunt/test-output/screenshots/testTC_NM_03_Goals_FAIL_20260808_024031.png`)
- Independently reproduced live on 2026-08-06 and 2026-08-07 under two different roles
  (`docs/AI-FEATURES-CHANGELOG/2026-08-06-real-fails-only-deep-triage.md`, `2026-08-07-final-fullsuite-scorecard.md`)
- Knock-on: ~12 downstream Goals tests skip on the data precondition.

**Why this survived challenge:** it is the *product's* crash boundary plus a Sentry dialog — a
test cannot manufacture that. It is not Access Denied (the suite explicitly detects and retries
that under the internal Admin role). An unguarded `.length` on `undefined` is a product defect
regardless of which data triggered it.

---

## 🟠 ALREADY TRACKED — do not raise a duplicate

### [Auth] No rate limiting on `/api/auth/login` → **ZP-2025**

`AuthenticationTestNG#testTC_SEC_02_LoginRateLimitAfterFailures` — 10 consecutive failed logins
all returned 401; no 429/423/403 at any point. Keep the test red as the tripwire.

**Two caveats to add to ZP-2025 before anyone calls login brute-forceable:**
1. The test uses a **non-existent account**. A per-account lockout — the most common design —
   would never trip. This can only detect IP/global throttling.
2. Measured on the **QA host only**. Throttling is often terminated at a WAF/edge that QA runs
   without; absence here is not evidence of absence in production.

---

## 🟡 PRODUCT QUESTIONS — take to the PM, do not file yet

These are confirmed *behaviour changes* whose **intent** is unknown. One answer either
re-baselines the tests or converts into tickets.

### Q1. Did V1.36 intentionally change the per-class Engineering field catalog? (**29 tests**)

| Class | June 2026 (live-mapped, 39/39 green) | 2026-08-08 |
|---|---|---|
| **VFD** | System Voltage + Phase Configuration + Mains Type | System Voltage **only** |
| **Motor Starter** | System Voltage + Phase Configuration + Mains Type | System Voltage **only** |
| **Switch** | Pole Count + **Manufacturer** | Pole Count + **Ampere Rating**, no Manufacturer |
| **Rectifier** | read-only System Voltage | Primary/Secondary Voltage + kVA Rating + % Impedance |

Not a timing race: `MAINS_08` waited >15 s and **passed on MCC, Switchboard, PDU, Other and VFD
Panel in the same method and run**, failing only on VFD and Motor Starter.

Consequence if unintended: the NFDS "Create a Main Switch?" flow has no reachable entry point on
VFD, so `MAINS_09/10/11` cannot run at all. Rectifier showing *kVA Rating + % Impedance*
(transformer fields) is the hardest to justify.

**Independent corroboration (API, 2026-08-10):** every class checked — VFD, Motor Starter, MCC,
Panelboard, Switchboard — returns `allowed_mains_type_ids: []` while `requires_phase_config:
true`. Whether empty means "unrestricted" or "none allowed" is the question.

### Q2. Did library matching move behind the new **`Use library`** button? (**10 tests**)

Selecting a Manufacturer no longer reveals "N possible matches" cards within 12 s (9 observations,
2 classes, 2 manufacturers, 2 shards — not a flake). V1.36 added `Use library`, `Frame Amps`,
`AIC Rating`, `Trip Amps`, `Trip Settings` — the signature of a redesign the tests never clicked.
**If wrong, this is P1**: no library match → no Trip Configuration → arc-flash inputs unfillable.

### Q3. Does WO ▸ Add Issue validate silently? (**2 tests**)

**Create Issue** is enabled with Title + Proposed Resolution, but no issue appears in the grid.
V1.36 appears to have promoted **Issue Class** to submit-time enforcement (the grid now has an
Issue Class column). Enabled button + no POST + no visible error would be a REAL P2 silent
failure; validated-with-feedback is by-design.

---

## 🔧 TEST-INFRA TICKET (ours, not the product's)

### `ensureSite()` fails silently in 6 Arc Flash / Asset classes

`ArcFlashTestNG`, `ArcFlashPlatformTestNG`, `ArcFlashRoleMatrixTestNG`,
`ArcFlashConnectionsTestNG`, `ArcFlashAssetClassMatrixTestNG` all set
`SITE = "Android Qa Site1"`, but **every failure screenshot in this run shows `Site: test site`**.
`ensureSite()` returns `false` on failure and the caller only prints it — it never fails the test.

**Impact:** an entire cluster of "product regressions" was measured on the wrong site, against a
June baseline captured on a different one. Convert to `SkipException` so site drift can never
again masquerade as a product regression.

---

## ✅ DOWNGRADED FROM "REAL" — recorded so the reasoning can be challenged

**These were initially triaged as real product bugs and then refuted by artifacts inside the run
itself.** Filing them would have been the expensive mistake.

| Initial claim | Verdict | Refuting evidence |
|---|---|---|
| [Arc Flash] Per-class readiness collapsed to one "Unknown" bucket, `0/1950`, all class cards gone (2 fails + 15 skips) | **TEST-SIDE** | A screenshot from the *same site, same run, 7 min later* (`testAFP_02_SldViewerPresence_FAIL_…025256.png`) shows the **full breakdown**: ATS 11%, Cable 100%, Circuit Breaker 100% 335/335, ~18 cards. `getClassBreakdown()` is a single-shot scrape with **no poll**; it captured the pre-hydration placeholder. The gauges reading "100%" are the classic `0 of 0` empty default — they read 3% later. |
| [SLD] `/slds` renders two duplicate "Select View" dropdowns (BUG-026 regression) | **BY-DESIGN** | The test's own screenshot shows the two controls are **different**: an empty-state CTA at (1069,626) under the heading *"Select a View to Load Assets"*, and the persistent canvas-toolbar control at (339,893) next to Export. Positions match the assertion exactly. The product's own copy points at the other one. |
| [Dashboard] BUG-012 "Company information not available" regression | **TEST-SIDE — broken tripwire** | The assertion ORs `alertVisible` with *"was `alliance-config` requested at all"* — no status, no timing. The run reported `alertVisible=**false**`; it tripped purely on a healthy 200. The app always makes that call. **It has never passed since the 2026-05-18 polarity flip.** Ironically PR #1053 fixed the real banner by calling that endpoint *more* reliably. Fix the test at `BugHuntDashboardTestNG.java:137`. |
| [Arc Flash] AF_15 refresh icon missing | **TEST-SIDE** | The failure screenshot **shows the refresh icon**. `clickRefresh()` requires `40 ≤ rect.top ≤ 120`; the tab strip renders at `y ≈ 141`. Off-by-layout. |

**Also verified as not-a-bug (independent API check, 2026-08-10):** ~35 "Asset Subtype should
offer X" failures. `/api/node_classes/user/{id}` returns **exactly** the asserted values — all 11
Circuit Breaker subtypes, all 10 Disconnect Switch subtypes, Battery's "Lithium-Ion". The product
has these options; the tests snapshot the section before it populates.

---

## Test-side / environment work items (~97 failures, no Jira needed)

1. `getClassBreakdown()` — add a poll until ≥2 cards or a non-`Unknown` name.
2. `AssetEngineeringExhaustiveTestNG` — the per-class Engineering snapshot races the section's
   own population; wait for a control, not a fixed pause.
3. `BugHuntDashboardTestNG:137` — assert the banner, not the existence of an API call.
4. `BugHuntPagesTestNG` BUG-026 — scope the probe to the canvas toolbar, or skip while the empty
   state shows.
5. `clickRefresh()` — match `aria-label`/`title` instead of pixel geometry.
6. Six classes — make `ensureSite()` failure fatal.
7. Arc Flash role-matrix (6 fails) — role switching now redirects and reloads by design; the
   `SkipException` in those tests is unreachable.
8. Work Order search tests — debounce races; wait for the row count to settle.
9. `WorkOrderCreateTestNG` due-date — gate on the redesigned V1.35 dialog being settled.

## Caveat on evidence quality

One triage agent reported that FAIL screenshots can depict the wrong browser in parallel runs
(`ScreenshotUtil` falls back to `lastRegisteredDriver` across threads). The adversarial pass could
not reproduce that for **this** run — every screenshot it opened matched its own test's page and
data. Treat it as an open risk for parallel runs generally, not as a defect in this report's
evidence.
