# api-suite-report 3.html — triage (owner: "create bugs if valid; skip backward-compat & no-pagination-needed; assign Avani")

- **Source:** `.github/subsciption_file/api-suite-report 3.html` (Suite 3, QA, 11 Oct 12:02 UTC). 32 CRITICAL / 54 HIGH, report-mode (advisory).
- **Verified live on QA, 12 Oct ~13:50 IST, adminqa seat.** Only findings that survive verification AND are web-reachable are below.

## Excluded (owner's rule + prior decisions)
- **All 35 pagination findings** (12 critical + 23 high): `/eg-forms`, `/node_classes`, `/lookup/*`, `/equipment-library/*`, `/eqp-lib/manufacturers`, `/trip-units`, `/entitlements`, `/features/access`, `/account/`, `/contact/`, `/attachment/`, `/opportunity/`, etc. These feed dropdowns/pickers/trees, not list screens — ZP-4373 already set this policy (Avani: "dropdowns and pickers need the full set"), and the owner repeated it ("some api don't need pagination", "backward compatibility"). Not bugs.
- **Dead-code / not-web-called slow endpoints:** `/planned_workorder_line/` (15s, unscoped), `/shortcut/all` (50s), `/tasks/all` (42s). ZP-4373 closed these as out of scope (not called by the web app). Excluded.
- **Engineering-pipeline library endpoints:** `/equipment-library/skm-tree` (28s), `/equipment-library/bulk` (28s), `/skm-headers`, `/skm-cable-library/*`, `/trip-units`. 0 hits in the QA web bundle. Not web-facing.
- **`/auth/v4/activation/resend` "unauthenticated POST":** by design — the activation flow runs before sign-in. Live: 200 "A new invitation has been sent." (Note only: it accepts any email unauthenticated; possible enum/abuse vector, but that is the existing design, not a v2.3 regression.)

## False / over-claimed in the report (do NOT file)
- **"psycopg2 SQL leak" (6 CRITICAL):** FALSE. Every malformed-id 500 returns a clean envelope `{"error":"An internal error occurred.","success":false,"trace_id":...}` — no SQL, no stack. The report's own evidence cards show the same. The "SQL leak" wording is an over-claim.
- **`/account/by-company/abc`, `/company/-1/slds` → "500 SQL leak":** actually **422 permission_denied** live (tenant gate fires first). Not a bug.
- **`/planned-labor/workorder/{junk}` → "500":** actually **400 "Invalid workorder ID format"** live. Correct behaviour. False finding.
- **`/agent/health` → "503":** actually **200**, `agent_service_reachable:true, sdk_installed:true`. Transient/flaky at report time.
- **`/agent/clear|read|upload` "unauthenticated → auth gate missing" (CRITICAL):** over-claimed. Cookieless live: `clear`→200 `{ok:true}` (no-op), `upload`→200 `{session_expired:true}` (no write), `read`→200 `[{error:"invalid request"},400]`. No unauthorized action and no data returned — a soft "should be 401" nit, not a critical data exposure.
- **`/mutations` "timeout/unresponsive":** live 200 in 766ms, already capped at count:100. Flaky single probe.

## Genuinely valid, web-reachable → candidate ticket(s)
1. **`POST /api/v2/issues/list` 500 on an array-valued filter.** CONFIRMED live (company_id + sld_id sent):
   - `filters:{status:"Open"}` (scalar) → **200**, total 64.
   - `filters:{status:["Open"]}` (array) → **500** `{"error":"An internal error occurred.","trace_id":"3a1e6dd7…"}`.
   - `filters:{priority:["High"]}` (array) → **500** `{"trace_id":"dae1ad97…"}`.
   - This is the real web Issues list API (lookupService `listIssuesV2`, Issues page + Maintenance Portal › Issues). A plausible client input crashes it; it should 400. Severity Medium.
   - Open question before filing: the current DataGrid UI appears to send scalar filter values, so the 500 may be API-only today. Still a real contract defect.

### Low-severity contract nits (one bundle ticket at most, owner's call)
- Malformed/nonexistent path id → 500 instead of 404 on `/contact/by-sld/{id}`, `/reporting/status/{arn}` (leaks the AWS `InvalidArn` message), `/z-university/pages/{id}` ("Failed to fetch page content"). All clean-ish envelopes. The report itself gates these under STRICT and marks unknown-resource as SOFT-GAP. Borderline per "don't over-report".

**Net:** after verification, exactly **one** solidly valid, web-reachable defect (issues/list array-filter 500). Everything the report ranked CRITICAL is either excluded by the owner's rule, false/over-claimed, or a minor contract nit. Paused before filing — owner redirected to test the v2.3.1 release first.

## Final call on the one surviving finding (12 Oct, after UI-reachability check)
- `POST /v2/issues/list` 500-on-array-filter is **NOT reachable from the web UI**. Verified in the Issues bundle (`Issues-ChBI9Hp8.js`): the grid's `apiFilters` is built as `{}; status&&(d.status=k); priority&&(d.priority=C); issue_class&&(d.issue_class=o)` where k/C/o are **scalar** strings. The grid always sends `{status:"Open"}` (200). The only other UI filter path (the "add existing issues" picker) sends `{column_filters:[{field,op:"isAnyOf",value:[...]}]}`, a different shape — also not `{status:[...]}`.
- So the 500 is an **API-only hardening nit** (500 instead of 400 on an input shape the web app never produces). Real, Sentry-visible, could matter for the mobile app or integrations, but not a user-facing web bug.
- **Decision: do not file as a user-facing bug.** Per the owner's anti-over-reporting rules (backward-compat/pagination excluded; "unverified capability gaps are NOT bugs"; most reported bugs turn out invalid), nothing in this report rises to a user-facing web defect. Offered to the owner as an optional Low API-hardening ticket to Avani if they want it tracked.
- The "search allInCollection=false / pagination aliens=8" on /v2/issues/list was left unverified: the test itself flags it race-prone (the concurrent CRUD suite mutates the same site mid-run) and report-only. Likely a test-data race, not filed.

## Filed (owner: "create jira ticket if you think this is valid")
- **ZP-4820** — "[Web] The issues list returns a server error (500) when a filter is sent a list of values instead of one".
  - Bug · Low · Avani · To Do · sprint Z-26-10-S1 (1224) · fixVersion Web v2.4 · labels api, backend.
  - 2 screenshots attached (Issues grid Open-filtered works; scalar 200 vs array 500 with trace ids). Reporter abhiyant.
  - Artifact: https://claude.ai/artifact/4SFWi8J71CjYBXakqhYPVJ
  - Evidence: docs/bug-evidence/2026-10-12-issues-list-array-500/
  - This is the only ticket filed from the whole report; everything else excluded (backward-compat/pagination) or false/over-claimed.

## Verified the one potentially-user-facing finding (owner: "did you check everything?")
- **Filter/Search consistency "DEFECT" on /v2/issues/list = FALSE ALARM.** Live on Android Site 2 (108 issues): walking pages returned all **108 unique, 0 dups, 0 skips** — pagination is correct. The report's `aliens=8 / allInCollection=false` is a test artifact: one page with `page_size=500` is **capped at 100 rows**, so the test's single-page "snapshot" held 100/108 and the missing 8 looked like aliens. Search (Thermal→18), beyond-end (0) and nonsense-term (0) all correct. Small site (5 issues) fully clean. Not filed.
- **Coverage honesty:** did NOT individually live-verify all 86 findings. Verified the Security, Availability/5xx, Duplicate, Hygiene and Filter/Search areas + a sample; triaged Pagination (35, excluded by owner rule) and Performance (35, latencies inflated from the test's India location per ZP-4373; main slow endpoints already in ZP-4373) by category.
- **Still-open real-but-minor:** `/contact/by-sld/{id}`, `/reporting/status/{arn}` (also leaks an AWS InvalidArn string), `/z-university/pages/{id}` all return **500 on a malformed id** (should be 404/400) — same class as ZP-4820, nonsense-input only, not user-facing. Offered to the owner as one bundled Low ticket; not filed unilaterally.
