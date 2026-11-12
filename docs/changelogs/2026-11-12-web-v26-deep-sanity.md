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
| ZP-4830 Collaborators | FAIL: Withdraw says "undefined's company" and fails; incoming requests have no Accept/Decline (Requests + Company Links grids read grid params as the row; confirmed in bundle). API accepts "not-an-email". Flag-off typed URL opens (SIMULATED) | [46130](https://egalvanic.atlassian.net/browse/ZP-4830?focusedCommentId=46130) | **To Do**, Eric → Avani |
| ZP-4831 Safety + Users | PARTLY: Users part passes; CM login blocked (no deliverable CM seat); /safety/sld renders for PM; Admin has no Safety menu but pages work by URL (question); lockout-options bad body 500 | [46131](https://egalvanic.atlassian.net/browse/ZP-4831?focusedCommentId=46131) | In QA |
| ZP-4833 Planned work | PARTLY: "Schedule locked for confirmation" passes; linked sub Team blocked by ZP-4830 | [46132](https://egalvanic.atlassian.net/browse/ZP-4833?focusedCommentId=46132) | In QA |
| ZP-5026 BCES-Viewer label | PARTLY: SIMULATED tiers only (QA acme offers no BCES tier) | [46133](https://egalvanic.atlassian.net/browse/ZP-5026?focusedCommentId=46133) | In QA |
| ZP-5031 BCES portal Lite/Viewer | PARTLY: SIMULATED BCES; non-BCES negatives real | [46134](https://egalvanic.atlassian.net/browse/ZP-5031?focusedCommentId=46134) | In QA |
| ZP-5111 Portal/BCES licensing | PARTLY: CP on No License real PASS; sync + DevRev 200 (403 on 5 Nov); BCES SIMULATED | [46135](https://egalvanic.atlassian.net/browse/ZP-5111?focusedCommentId=46135) | In QA |
| ZP-4832 SLD Photos overlay | PASS (non-eng-lib menu SIMULATED; badge 12 vs preview 9 question) | [46136](https://egalvanic.atlassian.net/browse/ZP-4832?focusedCommentId=46136) | READY TO RELEASE |
| ZP-4886 Magic Layout All | PASS | [46137](https://egalvanic.atlassian.net/browse/ZP-4886?focusedCommentId=46137) | READY TO RELEASE |
| ZP-4940 copy/paste | PASS | [46138](https://egalvanic.atlassian.net/browse/ZP-4940?focusedCommentId=46138) | READY TO RELEASE |
| ZP-4942 assemblies etc. | PARTLY: main flows pass; next-name bug → ZP-5264; PM library Access Denied (question); flag OFF only SIMULATED | [46139](https://egalvanic.atlassian.net/browse/ZP-4942?focusedCommentId=46139) | In QA (real flag flip asked) |
| ZP-4943 locked group + Edit with AI | PASS (stray 400 noted, no visible effect); flag OFF SIMULATED | [46140](https://egalvanic.atlassian.net/browse/ZP-4943?focusedCommentId=46140) | In QA (real flag flip asked) |
| ZP-4947 view overrides | PASS except Insert Cable row → ZP-5262 (Medium) + toolbar Remove from View → ZP-5263 | [46141](https://egalvanic.atlassian.net/browse/ZP-4947?focusedCommentId=46141) | READY TO RELEASE |
| ZP-5107 layout groups offline | PARTLY: online passes; offline = mobile bridge | [46142](https://egalvanic.atlassian.net/browse/ZP-5107?focusedCommentId=46142) | In QA |
| ZP-5100 SKM cables/import | PARTLY: cables + bus pass; transformer Type needs a real study | [46143](https://egalvanic.atlassian.net/browse/ZP-5100?focusedCommentId=46143) | In QA |
| ZP-4885 Fuse → Low Voltage Fuse | PASS (export XML attached) | [46145](https://egalvanic.atlassian.net/browse/ZP-4885?focusedCommentId=46145) | READY TO RELEASE |

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

## New bugs filed (owner rules: file without asking; area dev; To Do; sprint)
- [ZP-5262](https://egalvanic.atlassian.net/browse/ZP-5262) Medium: a cable inserted in one SLD view does not reach other views holding both ends. Avani, Z-26-11-S1, Web v2.6, relates ZP-4947.
- [ZP-5263](https://egalvanic.atlassian.net/browse/ZP-5263) Low: toolbar Delete › Remove from View leaves the cable. Avani, Z-26-11-S1, relates ZP-4947.
- [ZP-5264](https://egalvanic.atlassian.net/browse/ZP-5264) Low: the second assembly placement suggests "L1 CB-3" instead of "L2". Avani, Z-26-11-S1, relates ZP-4942.
- No future sprint exists other than "Triage", so the Low bugs went into the active sprint without a fix version.
- Kept as notes, not filed:
  - ZP-4943's stray PUT node/update 400: no visible effect.
  - ZP-4942: PM gets Access Denied on the Assemblies library. This is a design question.

## Asked Shubham in eg-internal-dev (~17:30 IST)
1. feature-sld-assemblies OFF → check → ON (4942/4943/5107).
2. feature-guest-access OFF → ON (4830).
3. Construction Manager seat with a deliverable email (4831).
4. A second QA company (not demo) with a non-staff admin (4830/4833).
5. A QA company offering BCES-Lite/Viewer, with a CP login per tier (5026/5031/5111).
6. A real SKM study (5100).

## Open for the owner
- The tester did not send invites to real/public mailboxes (the safety check stopped that); only `.invalid` addresses were used. The "public domain / egalvanic.com resolves to nobody" negative rows of ZP-4830 need the owner's OK to invite addresses he controls.
