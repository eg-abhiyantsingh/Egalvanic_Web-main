# 2026-11-12: Web v2.6 (Jira version 14299) deep sanity round

**Prompt:** "http://egalvanic.atlassian.net/projects/ZP/versions/14299/tab/release-report-all-issues we can start testing now ready to qa and in qa ticket in deepth sanity testing"

## Scope (JQL fixVersion = 14299, 16:00 IST)
34 tickets: 22 In QA, 8 READY TO RELEASE, 3 Ready for QA, 1 To Do.
- Ready for QA (Krunal): ZP-4430, ZP-5158, ZP-5223. Moved to In QA first (Start Testing).
- In QA (Eric, 22): ZP-4830..4835, 4885, 4886, 4940, 4942, 4943, 4947, 5026, 5031, 5100, 5105..5111.
  - On 2 Nov and 5 Nov QA commented "code not on QA yet".
  - These are the tickets I had moved from "Resolved (migrated)" on 2 Nov. The owner now asked for In QA to be tested, so they stay in scope.
- QA build **index-C1ZRIunF.js** (deployed 14:06 IST 12 Nov) carries the v2.6 code. The bundle has "Magic Layout All", "Select Entire Run", "Simplify Edge", "Approve & Next", "Topology Errors" and the collaborators UI; "Attach copy" is gone.

## How
- Tester brief: `docs/bug-evidence/2026-11-12-v26-qa/BRIEF.md`.
- 3 background testers (no Jira/Chat/git):
  - SLD/SKM: 4832, 4886, 4940, 4942, 4943, 4947, 5107, 4885, 5100.
  - Portal/collaborators/BCES: 4830, 4831, 4833, 5026, 5031, 5111.
  - Services/WO/engineering/backend: 5105, 5106, 5108, 5109, 5110, 4834, 4835.
- I test Krunal's 3 myself and post everything from Abhiyant's Jira (account 712020:8f78…).

## Results
| Ticket | Result | Comment | Status |
|---|---|---|---|
| ZP-5158 Subscription Activity pagination | PASS (tie order + new event mid-paging not covered: QA has 1 real event) | [46121](https://egalvanic.atlassian.net/browse/ZP-5158?focusedCommentId=46121) | READY TO RELEASE |
| ZP-5223 Generate Report picker search + service on rows | PASS (suggestion: identically named configs still look the same) | [46122](https://egalvanic.atlassian.net/browse/ZP-5223?focusedCommentId=46122) | READY TO RELEASE |
| ZP-4430 report preview 500 (Sentry reopened 6 Nov) | PASS: Performance sld EMP Lite 40 s / CA 35 s; all 134 configs 200; bad input 400/404 | [46124](https://egalvanic.atlassian.net/browse/ZP-4430?focusedCommentId=46124) | READY TO RELEASE |

### ZP-5158 details
- API `/api/subscription/activity?page&page_size` → `{events,page,page_size,total}`.
  - page 0/-1 → 1; page=abc → 400 "page and page_size must be integers"; page_size capped at 100.
  - The first page comes inside `/api/subscription` (`activity`, `activity_total`, `activity_page_size` 10).
- The UI pager was checked with 23 SIMULATED events (route interception): one request per page change; Previous/Next disabled at the ends; the header shows the total.
- The empty state was SIMULATED. T2 regression real: 1–10 → 11–20 of 140.
- PM/FM/CP → 403 forbidden on both endpoints.
- Activity is derived from subscription rows; QA acme has only 1 event, and seeding needs a dev (SQL).

### ZP-5223 details
- Search matches name, `work_type_names` and `work_type_types`.
  - "NETA" → QA Test; "Cleaning" → test; no match → 'No report configurations match "zzqq"'.
- 134 configs: 108 have no service, 0 have more than one ("+2" chip SIMULATED; the tooltip lists all).
- Default-selection logic is byte-identical to prod `reportConfigChoices`. Generate → POST /reporting/generate_simple 200 + PDF.
- Site picker regression: "Search sites…", 'No sites match "zzqq"'.
- Video: the unfinished step 10 segment was cut out.

## Trap
- Opening a report config editor in the owner's Chrome froze the tab (heavy auto-preview). Do config-editor checks in pwslot Playwright, not claude-in-chrome.
