# Work Order → Add Issue test suite

- **Date:** 2026-06-22
- **Prompt:** Follow-up workflow spec — "Navigate to /sessions … change site to All Facilities … click a
  work order … navigate tabs to the Issues tab … Actions → Add Issues … fill Title + Proposed
  Resolution … Create Issue."
- **Type:** New UI test class — 4 TestNG tests for the WO-scoped issue-creation flow.

---

## Flow (verified live with Playwright, then automated in Selenium)

Work Orders (`/sessions`) → site **All Facilities** → click a **Work Order** grid row ⇒
`/sessions/{id}` detail with a tab strip **Assets · Tasks · EG Forms · Issues · IR Photos ·
Attachments**. On the **Issues** tab the **Actions** button opens a menu with **Add Issues** →
**Add Issue** drawer: Priority (default **Medium**) · Issue Class* · Asset · **Title*** ("Enter issue
title") · Description · **Proposed Resolution*** ("Describe the proposed resolution") → **Create
Issue**. Creating with only Title + Proposed Resolution **succeeds** (Issue Class's `*` is not enforced
on submit) — the issue lands in the WO's Issues grid and the Issues tab count increments.

| # | Test | Asserts |
|---|------|---------|
| WOI_01 | WO detail tabs | opening a WO shows Tasks / EG Forms / Issues tabs |
| WOI_02 | Add Issue opens | Issues tab → Actions → Add Issues opens the drawer (Title, Proposed Resolution, Create Issue) |
| WOI_03 | Required fields | Priority defaults to Medium; Title + Proposed Resolution marked required (*) |
| WOI_04 | Create issue | filling Title + Proposed Resolution → Create Issue creates the issue (appears in the WO's Issues grid) |

## Key findings during automation

1. **WO list is a `div[role='row']` grid**, not an HTML table. The page also carries a *hidden* asset
   `tbody tr` table (25 rows) — so a `tbody tr` selector grabbed the wrong, invisible rows. Fixed by
   targeting `div[role='row']` rows that contain `[role='gridcell']` (skipping the columnheader row),
   preferring a row whose name contains "Work Order" (Job-type sessions have a different detail layout).
2. **The row click navigates via the row element** (SPA → `/sessions/{id}`), discovered by driving the
   flow in Playwright (the WO detail tablist + Actions → Add Issues + the modal field placeholders were
   all mapped live there first).
3. **The app is slow** — `navigateToWorkOrders` ≈ 24s and the WO-detail tab strip renders ~20s after the
   row click. The fix was generous waits (`waitForTabs(35)`) plus a fast-path that reuses an
   already-open WO detail across tests (skips the ~24s re-navigation), keeping the suite reliable.

## Validation
Driven end-to-end in Playwright first (created a real test issue to confirm the happy path), then the
Java suite validated **headed** against `acme.qa.egalvanic.ai`. WOI_04 creates one issue per run
(`QA-AUTO WO Issue <ts>`) on the first Work Order — consistent with the workflow being tested.

## Wiring
`suite-workorder-issue-add.xml` (dedicated, 4 TCs) + added to `suite-workorder-issue.xml` + a
`parallel-suite.yml` matrix row under the existing `WORK_ORDERS` toggle. Disabled DOM-discovery
diagnostic (`testWOI_00_DumpFlow`) retained.
