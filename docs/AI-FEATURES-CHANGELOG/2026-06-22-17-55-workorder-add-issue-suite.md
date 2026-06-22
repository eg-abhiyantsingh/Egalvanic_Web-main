# Work Order → Add Issue — automating a cross-module workflow (WO-scoped issue creation)

- **Date:** 2026-06-22
- **Time:** ~17:55 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** Follow-up workflow spec: open a work order from `/sessions`, navigate to its Issues tab,
  Actions → Add Issues, fill Title + Proposed Resolution, Create Issue.

## What was built

`WorkOrderIssueTestNG` — 4 tests covering the work-order-scoped issue-creation flow (distinct from the
standalone Issues module). Validated headed against the live QA app.

## Depth explanation (for learning + manager review)

This one was a clinic in **"discover live, then automate"** and **reading the timing, not just the
DOM** — the same two lessons from the engineering suites, applied to a much slower, more deceptive page.

1. **Playwright as a microscope.** My first two blind Selenium attempts failed because I was guessing
   how the WO list and detail were structured. Switching to Playwright (already authenticated) let me
   *watch* the real flow: the WO list is a `role=grid`, clicking a **row** navigates to `/sessions/{id}`,
   the detail has a `role=tablist` (Assets/Tasks/EG Forms/Issues/…), Actions opens a menu with "Add
   Issues", and the drawer's fields are `Enter issue title` + `Describe the proposed resolution`. I even
   created a real issue there to confirm the happy path. Ten minutes of looking beat an hour of guessing.

2. **The hidden-table trap.** The Selenium DOM had a *hidden asset table* (`tbody tr` × 25) layered
   behind the WO `div[role='row']` grid. My `tbody tr` selector matched those invisible rows
   (`getText()` returned empty), so I "found rows" but clicked nothing useful. Lesson: when a selector
   matches but behaves oddly, dump what it actually matched — the element you think you have may not be
   the one on screen.

3. **Slow is a correctness problem, not just a speed one.** `navigateToWorkOrders` took **24 seconds**
   and the WO-detail tab strip rendered ~20s *after* the row click. My `waitForTabs(15)` expired before
   the tabs existed, so every test failed and burned ~268s in retries. The instrumented diagnostic
   (timestamps per step) made the 24s nav and the late-rendering tabs obvious. Fix: `waitForTabs(35)` +
   a fast-path that reuses an already-open WO detail so tests 2–4 skip the 24s re-navigation. Lesson:
   when a UI test flakes on a slow app, *measure each step's latency* before touching selectors.

4. **Required-but-not-enforced.** Issue Class is marked `*` in the modal, yet Create Issue succeeds
   without it — verified live. The test reflects reality (fills only Title + Proposed Resolution and
   expects success) rather than the label's implied contract.

## Wiring & hygiene
- `suite-workorder-issue-add.xml` (dedicated) + added to `suite-workorder-issue.xml` + a
  `parallel-suite.yml` matrix row under the existing `WORK_ORDERS` toggle.
- WOI_04 creates one issue per run (the workflow under test); disabled step-diagnostic retained.
