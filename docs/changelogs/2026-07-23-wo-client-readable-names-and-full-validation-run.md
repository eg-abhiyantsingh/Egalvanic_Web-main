# 2026-07-23 — Work Orders: client-readable test names + full overnight validation run

**Prompt:** "check all the test case for work order i am going home so keep checking after 5 hours
if token expire and make sure test case are readble for client he can understand what we are doing
even for api test case"

## Part 1 — Client-readable names (DONE, commit 0abc802)

87 exact renames across 8 classes so every @Test description and every Extent-report row states in
plain language WHAT is verified. Pattern: `TC id — plain action · variant`.

| Before (client sees) | After |
|---|---|
| `TC_WTD_004 — UPS Maintenance` | `TC_WTD_004 — matching-assets preview · UPS Maintenance` |
| `TC_WTF_001 deepLinkLoads — AF (…)` | `TC_WTF_001 — page opens from a direct link · AF (…)` |
| `TC_WTAPI_008: Malformed id → 4xx not 500` | `API testing - TC_WTAPI_008: a bad id must return a clean error, not a server crash (known bug)` |
| `TC_WTE_006 — General detail contract pin` | `TC_WTE_006 — General detail page shows expected tabs/fields` |
| `TC_WOL2_007: … data-field attributes` | `TC_WOL2_007: Verify each grid cell is tagged with its column name (structure check)` |

- The 16 cryptic detail-contract check keys now map through a `PLAIN_CHECK` table in
  `WorkTypeDetailContractTestNG` (engineers keep the TC id; clients read the phrase).
- ALL API test cases start with **"API testing - "** (user requirement) and avoid HTTP jargon where
  a plain phrase exists ("calls without login are rejected" instead of "401/403").

## Part 2 — Full validation run (in progress, background)

Sequential run of all 14 work-order classes (one browser per class, never headless), fast classes
first, matrix suites last:
api/WorkTypeCatalogApiTest → api/WorkOrderEditEnforcementApiTest → Smoke → Create → Issue → EditUi
→ BugHunt → WorkOrderTestNG → Planning → Part2 → CreateDialogMatrix → DetailContract →
AutoScheduleEdge → CreateE2EMatrix.

- Per-class logs + running summary: `scratchpad/wo-run/` (summary.txt has one line per class).
- The E2E matrix run doubles as validation of the merged Name+Work-Type dialog-render retry loop
  (shipped un-validated in 0abc802 due to QA flakiness — tonight's run is its proof).

## Part 3 — Token-expiry resilience

- Session cron heartbeat `dc785668` fires every 2 hours (…:17). Each firing checks the todo state
  and the background run; if a turn died from token exhaustion, the first firing after the token
  restores (~5h) resumes triage exactly where the summary file left off.
- Limitation (told before): a *cloud* schedule needs the Claude GitHub App connected; the local
  heartbeat lives only while this Claude session is open on the machine.

## Depth notes

- Renames are data (exact old→new pairs in a verified script), not hand edits — the script fails
  loudly on any non-match, so a stale pattern can't silently skip a rename (all 87 matched).
- Report names carry the variant suffix (work-type name, direction, role) because with
  data-provider suites one TC id covers up to 14 rows — a client must see which variant failed
  without opening the log.
- Run order puts API + smoke first: if QA enters another 502 episode overnight, the cheap classes
  bank results before the multi-hour matrix suites hit the flaky window.
