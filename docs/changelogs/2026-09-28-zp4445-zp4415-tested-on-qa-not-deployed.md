# ZP-4445 + ZP-4415 tested in depth on QA — both fixes not on QA yet (2026-09-28)

**Prompts:** "test this ticket in depth" (ZP-4445), "test this ticket too" (ZP-4415), "share me artifact with proof screenshot".

| Ticket | Verdict on QA (index-BfTHwafh.js) | Proof page |
|---|---|---|
| ZP-4445 Suggestion Polling Off | FAIL — all three dashboards re-fetch badge counts, Needs Attention and the donut at exactly +5:00 while visible, and again on return after >5 min hidden. PR #1554 is on `cicd/stag` (acme.stage has it), not `cicd/qa`. New paged endpoints attention/{sales,ops,admin} ARE live on QA and page correctly (131/62/118 items, no repeats). | https://claude.ai/artifact/83gY5X84LrSW7PwsfSgUtz |
| ZP-4415 Report history 500 | FAIL — `/reporting/history` 500 on every call (3 sites, no sld_id, bad limits, unknown-id download) for admin and PM; FM gets 422 `reports.view` (role drift). The Reports page in this build never calls history at all. Backend fix eg-pz-backend#1368 not visible to the QA account. | https://claude.ai/artifact/BpUp6ZqM1ktgbU83WbN3yr |

Tests added: `ZP4445DashboardPollingTest` (3 dashboards in parallel, in-page request recorder, visibility sampling, pass = polling gone) and `ZP4415ReportHistoryTest` (UI). Evidence: docs/bug-evidence/2026-09-28-zp4445-zp4415/. No Jira changes made.
