# 2026-07-15 — Web v1.35 Work Order coverage: create-flow redesign fixes + coverage plan

## Prompt
Update and extend QA suites to cover Work Order changes in Web v1.35 (ZP-3000, ZP-3027,
ZP-3130, ZP-3129, ZP-3056, ZP-3043, ZP-3041, ZP-3120, ZP-3158) + iOS counterpart. Also
ran the full parallel suite locally and fixed issues found.

## Headline discovery (ZP-3000)
The **Create Work Order** dialog on `/sessions` was redesigned. Verified live via Playwright:
- New **required "Work Type"** combobox — Create stays disabled until it's set.
- New **Scope** (asset-filter builder + "Start Empty Instead") and **Team** sections.
- **Priority, Est. Hours, WO Description, Photo Type, Start Date** moved into a **collapsed
  "Advanced Settings" accordion** — their inputs are unmounted until it's expanded.

This is the real root cause of the `WorkOrderTestNG.testTC_CWO_003` failure in the
2026-07-14 full run (looked like a flaky Priority locator; was a form redesign).

## Changes made (compiled; validation gated on QA env recovery — see below)
**`WorkOrderPage.java`**
- Added `selectWorkType(String)` + tenant-agnostic `selectFirstWorkType()` fallback.
- Added idempotent `expandAdvancedSettings()` (no-op if already open/absent).
- `selectPriority()` now auto-expands Advanced Settings first.
- Dialog-scoped `PRIORITY_INPUT`; added `WORK_TYPE_INPUT`, `ADVANCED_SETTINGS_TOGGLE`.

**`WorkOrderTestNG.testTC_CWO_003_CreateWOAllFields`** — v1.35 flow: select Facility →
select Work Type (required) → Priority (auto-expands accordion) → Description → submit.

**`WorkOrderCreateTestNG`** (breaking fixes — these tests were stale/broken under v1.35):
- `testWOC_01_FormStructureAndDefaults` — expand Advanced Settings before reading the
  Priority/Photo Type/Start Date defaults (inputs unmounted while collapsed).
- `testWOC_02_CreateGatedOnName` — now asserts Create stays DISABLED with WO Name set but
  Work Type empty, then ENABLES once both required fields are set (v1.35 gating).

**`IssuePage.searchIssues`** — stopped swallowing its exception (now rethrows). The swallow
made `IL_003`/`IL_005` pass *vacuously* against an unfiltered list and `IL_004` fail on a
misleading row-count assertion. NB: the search *locators* already match live DOM
("Search Issues..." / "Search Assets...") — not a locator bug.

## Full parallel suite run (local, 2 worktree lanes)
Ran the CI "Parallel Full Suite" groups locally. Completed: auth-site (58/59), workorder-issue
(167/245), asset-1-2 (45/77), location-task (121/135), asset-4 (63/65). Triage verdicts:
- **auth-site**: 1 fail = `TC_SEC_02_LoginRateLimitAfterFailures` — **known product bug ZP-2025**
  (no login rate limit; 10×401, no 429/423/403). Left red intentionally.
- **workorder-issue / asset-1-2 / TaskTestNG**: the dominant cause was an **environment
  meltdown** — the `alliance-config/branding` API (documented login-form blocker) flapped
  (502/timeout) through the run, cascading into session-expiry re-login loops, the missing
  facility selector, and 72 `@BeforeClass` skips (IssuePart2TestNG, WorkOrderCreateTestNG).
  Not test bugs. Affected tests to be re-run on a stable env.
- **Genuine test issues found & fixed**: TC_CWO_003 (v1.35 form), IssuePage search swallow.

## Environment caveat (blocks full validation right now)
QA `alliance-config/branding` is **intermittently down** (recovered ~23:50, regressed, recovering
again). Live validation of the create-flow fixes is gated on 3 consecutive fast 200s before
running solo. **No fixes are committed until validated live** (repo directive: validate, don't
just compile). Also: QA sidebar version badge reads **V1.34** while these tickets target v1.35 —
some features below are likely not-yet-deployed.

## Coverage plan for the remaining tickets (from 9-ticket gap analysis)
No new test classes / suite XMLs needed — every item extends an existing file; all new POM logic
goes in `WorkOrderPage.java`. Shared helper `toggleShowPlanned()`/`isPlannedShown()` is reused by
ZP-3000/3130/3043/3041 (build once).

| Ticket | Plan | Testable now? |
|---|---|---|
| ZP-3000 | WOC_08 Work-Type gating, WOC_09 Scope section, WOC_10 Advanced-Settings-collapsed | Create dialog: yes; Services module + native Panel Editor: **undeployed (guard)** |
| ZP-3027 | Regression sentinels: Services nav distinct from /sessions; create flow intact | yes |
| ZP-3130 | Harden green-washing `SLC2_004/005/006` (assert + fail); Show-planned toggle tests | yes |
| ZP-3129 (**HIGH**) | WO-detail EG Forms tab shows human name not UUID/id; Edit-Form name repro | needs a WO with linked EG Form |
| ZP-3056 | In-session SLD: unmapped assets greyed/disabled + "Bring into Session" promotes | **likely undeployed (guard/skip)** |
| ZP-3043 | Sessions pagination disjoint/rows-per-page; API deep-pagination | 8656-scale not reproducible on tenant (adapts to real total) |
| ZP-3041 | Add `/planned-workorder-lines/` dataProvider row to `PaginationBehaviorApiTest` | API: yes (one-liner) |
| ZP-3120 | Turn SLD v3-vs-v2 delta into STRICT improvement guard; `X-DB-Query-Count` if present | perf: yes; DB-query criterion: not black-box observable |
| ZP-3158 | RBAC: reports.generate/manage gate per role; reporting exclusive to privileged roles | needs WO report-generate endpoint confirmed live |

Known green-washing to fix under ZP-3130: `WorkOrderPart2TestNG.testTC_SLC2_004/005/006` log
"PASS" unconditionally with no assertion on the filtered result — deferred until the filter
interaction can be validated live (env).

## Live validation attempts (QA env flapping — partial signal)
Two solo validation runs were gated behind sustained env-health windows. The QA branding API
flapped on a ~1-2 min cycle throughout, so each ~8-min run partly overlapped an outage. Findings:

- **CORE FIX PROVEN WORKING:** in `WOC_01`, after `expandAdvancedSettings()`, `getPriorityValue()`
  returned **"Medium"** and `getPhotoTypeValue()` returned **"FLIR-SEP"** — i.e. the accordion
  expanded and the label-anchored locators read the Advanced-Settings fields correctly. Work Type
  selection also logged success in both runs (`Inspection` and first-option `General`).
- **`WOC_01` remaining failure — pre-existing locator bug (not from these changes):**
  `Start Date should be pre-filled` → got empty. `WO_START_DATE_INPUT` = *first* `MM/DD/YYYY`
  input in the dialog, but in the v1.35 layout that first date input is **Due Date** (Start Date
  moved into Advanced Settings, later in DOM). The `WO_START_DATE_INPUT`/`WO_DUE_DATE_INPUT`
  `[1]`/`[2]` indices are effectively swapped now. **Needs one live DOM confirmation of date-field
  order before swapping** (not done blindly).
- **`TC_CWO_003` remaining failure — likely env-timing flake:** Priority input not found within 25s,
  yet `WOC_01` found the same field fine in the same run → consistent with the marginal env
  (one health probe was 4.9s mid-run), not a locator defect.
- **`WOC_02` OPEN QUESTION:** with WO Name set and Work Type=`General` (via `selectFirstWorkType`,
  which clicked the option), Create stayed **disabled**. Two hypotheses to check live: (a) a third
  gating requirement beyond Name+Work Type, or (b) the option-`<li>` click didn't commit the MUI
  Autocomplete value (`selectFirstWorkType` may need a firmer commit). `selectWorkType("Inspection")`
  via type+select got past Work Type in TC_CWO_003, favoring hypothesis (b).

**Decision:** NOT committing the test changes — they are compiled and the POM core is proven, but
not green end-to-end, and the repo rule is validate-don't-just-compile. Do not push non-green tests
to `main`. All work preserved in the working tree.

**#1 blocker to escalate:** QA `alliance-config/branding` is flapping (502/timeout on a ~1-2 min
cycle) — the documented login-form blocker. This intermittently breaks login across the whole suite
and is the dominant cause of both the full-run failures and the stalled validation. Worth raising
with the backend team as an environment-stability issue independent of these tests.

## Next
1. On sustained env recovery: one focused live DOM inspection to resolve the WOC_02 gating cause +
   confirm Start/Due date field order, fix those, then re-validate TC_CWO_003 + WOC_01 + WOC_02 and commit.
2. Implement testable-now batches (ZP-3130 filters/toggle, ZP-3041 API one-liner, ZP-3027 sentinels).
3. Guarded/skip stubs for undeployed slices (ZP-3056, ZP-3000 Services/Panel Editor, ZP-3158 report endpoint) that auto-activate on the v1.35 deploy.
4. iOS counterpart WO coverage (separate follow-on).
