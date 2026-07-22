# Work Type Matrix — all 14 dropdown options × 5 dimensions, 415 data-driven test cases

**Date:** 2026-07-21
**Time:** 16:30 UTC
**Author:** Claude Code (automation, Fable 5), for abhiyant.singh@egalvanic.com

## What was asked
The Create Work Order "Work Type" dropdown has 13 options — cover ALL of them, in depth, complex
flows included, with more than 400 test cases. Quality over quantity, no lazy approach, use
Playwright to understand the end-to-end flow first, never headless, iterate until it runs.

## What shipped
**415 data-driven test cases** across 5 new classes + 7 suite XMLs, all pinned to live-verified
facts (Playwright walkthrough of every one of the 14 dropdown options — the 13 services + "General"
— on acme.qa site Z1, 2026-07-21):

| Class | Tier | Rows | What it pins |
|---|---|---|---|
| `api/WorkTypeCatalogApiTest` | API | 84 | services catalog (13 pinned entries: name/key/id/family/de_energized), procedures list per service, scope-preview contract + `matching_count == assets.length` (CTT-mismatch regression), 6 Z1 fixture session endpoints, auth/malformed negatives, SQL-leak tripwires |
| `WorkTypeCreateDialogMatrixTestNG` | UI | 153 | per-type dialog contract: option catalog exact order, commit-per-type, scope preview vs 0-procedure notice vs General-neither, Auto-Schedule presence, Advanced-Settings defaults survive type switches, sections, required markers, create gating, type-switch retargeting, Start Empty |
| `WorkTypeDetailContractTestNG` | UI | 102 | 6 Z1 family fixtures × 16 read-only checks: exact tab strips, exact asset-grid columns, type chips, Data-Mask-only-on-Checklist, Actions-menu content (Upload IR Photos = IR-only), type-tab clickability, health |
| `WorkTypeCreateE2EMatrixTestNG` | UI | 42 | create → verify list → verify detail chip+tabs → API-delete, for every one of the 14 types (full scope for 6 family representatives, Start-Empty for the rest) |
| `WorkTypeAutoScheduleEdgeTestNG` | UI | 34 | Auto-Schedule enablement truth table per type, Shutdown 0-procedure create, General contract pin, cancel-discard, state reset, name edge cases (XSS/256-char/unicode), facility-switch preview refire, duplicate names, past due date |

Foundation shared by all five: `constants/WorkTypeCatalog.java` (the authoritative 14-profile
model: exact labels, API keys, service ids, 6 UX families with expected tabs/columns, Z1 fixture
map) + `WorkTypeUiBase` (site pinning to Z1, dialog lifecycle, committed-type selection, grid
search with indexing-lag retry, API-cleanup via `DELETE /ir_session/{id}`) + 20 new
`WorkOrderPage` readers (work-type options/value, matching-assets count parser, no-procedures
notice, Auto-Schedule state, detail tabs/chips/columns/Actions-menu).

**Validation evidence (live, headed Chrome):** API suite 81/81 GREEN in 3.5 min with the 3
`known-product-bug` tripwires excluded per repo convention — and proven RED when included (they
caught the real backend bugs). UI suites run next in this session; see the main changelog for
final tallies.

**Security finding (worth a Jira ticket):** `POST /api/ir_session/scope-preview` with a
non-uuid `sld_id` returns 500 **leaking raw psycopg2 error text + the full SQL SELECT and
`nodes` schema columns** to any authenticated client. Same verified 500-on-malformed-uuid class
as documented in project_api_testing; now permanently tripwired.

## Depth explanation (for learning + manager review)

**Why "13 options" became 14 profiles.** The dropdown shows 13 *service* work types plus
"General". The services API (`GET /api/procedures-v2/services`) is the authority: 13 entries,
each with a `type` field naming one of six UX families (AF, IR, COM, Checklist, Schedule, PM
Forms — 8 of the 13 are PM Forms). "General" attaches no service. Every downstream behavior —
which tabs the detail page gets, which asset-grid column appears, whether creating fires the
rules engine — hangs off that family. So the test architecture mirrors the product architecture:
one `WorkTypeProfile` per option, family contracts pinned once on the enum, and every test class
is a cross-product over profiles.

**Why data-driven beats hand-written here.** 400+ hand-written tests would be 400 copies of the
same skeleton, and the 15th work type would cost a week. With `@DataProvider` matrices each row
is a separately-reported ExtentReports test case, assertion messages carry the profile, and a
catalog change costs one file. This is the same pattern the repo already trusts in
`AssetEngineeringExhaustiveTestNG` — we scaled it, not invented it.

**Why Playwright discovery came first.** Three "facts" from our own 6-day-old docs were already
stale: the dialog now renders Schedule + Equipment sections, Auto-Schedule renders for ALL types
(docs said de-energized only), and the settled scope-preview text is "N matching assets" (never
recorded). Writing 415 assertions against stale docs would have produced hundreds of false reds.
One hour of live walkthrough (every option selected, all 6 family fixtures opened, API
request/response bodies captured) made the assertions correct on first run — the API suite went
81/81 on its first green-gate execution.

**Why the discovered oddities matter.** (1) NETA Testing's API key is `de-energized-testing` —
name≠key, so anything keyed by display name silently misses it; the catalog pins both. (2)
Shutdown (Composite) has 0 procedures and its own dialog notice — the only type that creates an
asset-less WO by design. (3) `DELETE /ir_session/{id}` returns 200 with an async mutation
receipt even for a nonexistent uuid — pinned as a contract observation (async-queue semantics,
debatable-by-design, not over-reported as CRITICAL per the owner's rule). (4) The
`matching_count == assets.length` assert per service is the regression tripwire for the verified
2026-07-20 CTT preview-vs-created mismatch.

**Runtime/CI thinking.** The dialog matrix REUSES one open dialog across 100+ rows (type
switching in place — itself the behavior under test), the detail matrix orders its 96 rows
fixture-major so 6 navigations serve 16 checks each, and the API class caches one scope-preview
response per service. Everything honours `-Dpause.scale` for CI speed-ups. E2E creations
self-clean via the API delete, so repeated CI runs don't pollute Z1.

## What the first validation runs taught us (the trial-and-error loop, for learning)

Every UI failure decoded into a real product contract nobody had documented — the exact payoff
of "run it, don't just compile it":

1. **Selenium and Playwright disagree about hidden text — by design.** The detail header's
   priority chip failed in Selenium but "existed" in my Playwright probes. Root cause: the chip
   is `visibility:hidden` inside a collapsed header accordion; Playwright's `textContent`
   includes hidden nodes, Selenium's `getText()` only returns rendered text. The debugging
   ladder: failure message → failure screenshot → cold-load repro → falsify width/height/profile
   hypotheses → computed-style dump (`visibility: hidden`, y=180) → interaction test (chevron
   click flips it visible). Lesson: pin behavior only after you can name the mechanism.
2. **Virtualized grids render nothing off-screen.** Expanding that header pushed the MUI
   DataGrid below the fold — zero column headers for 10 s of polling. Tests that mutate layout
   must restore it (`collapseWoDetailHeader()` in a `finally`).
3. **Grammar is part of the contract.** "1 matching asset" (singular, UPS on Z1) broke a
   plural-only regex; DGA's zero-match state adds an eligibility notice. Scope counts are
   site-data dependent — assertions now pin "settles + zero-state semantics", not magic numbers.
4. **The dialog keeps a sticky WO-Name draft across Cancel/reopen** (type resets, Create stays
   gated). Thirteen "fresh dialog" gating rows inherited the previous row's name — the fix is
   an explicit `clearWoName()`, and TC_WTE_010 now pins the draft contract itself.
5. **Auto-Schedule needs more than Est. Hours + Field Technician — and the "fix" taught a
   lesson in falsification.** Run 1's uniform all-disabled result looked like a due-date gate,
   and a quick probe seemed to confirm it. But a controlled full 14-type truth table (fresh
   dialog per type) showed the button stays disabled for EVERY type even with 1–173 matching
   assets, and that the masked date input silently ignores native value-setters (so the "due
   date enabled it" probe had never actually set a date — the enabling had come from residual
   dialog state). The honest, reproducible contract is the negative: those two inputs are not
   sufficient; enabling lives in the Schedule "+" block sub-flow. TC_WTE_001 pins the negative
   and records the truth table. Lesson: when a one-shot probe "confirms" a hypothesis, re-run it
   from a clean state before trusting it.
6. **NEW verified bug: whitespace-only WO names are accepted end-to-end** (Create enables,
   backend accepts, blank-named WO appears). Tripwired as `TC_WTE_011b` in the
   known-product-bug group — red on purpose until fixed, excluded from green gates. The
   verification probe WO was API-deleted.
