# Five passing v2.2 tickets moved to READY TO RELEASE (2026-09-22)

**Prompt:** "the ticket that are passed can you update status and add comment too. just update pass ticket
only. ready to qa to ready to release if everything is working as expected."

## What was changed in Jira

For each ticket: **one comment** carrying the QA verdict, then the **status transition only**
(transition id 7, `Ready for QA` → `READY TO RELEASE`). Nothing else was touched — fix version,
assignee, priority, labels, sprint and description are all unchanged, verified by re-reading the five
issues after the transition.

| Ticket | Title | Comment | Status |
|---|---|---|---|
| ZP-3990 | Reactivate Work Order menu item is missing its icon | 44378 | READY TO RELEASE |
| ZP-4059 | In a connection, when we press X the screen should not hide | 44379 | READY TO RELEASE |
| ZP-4111 | Device-rule picker's quick-search dropped device rows | 44380 | READY TO RELEASE |
| ZP-4165 | Customers tree: paginate and move search server-side | 44381 | READY TO RELEASE |
| ZP-4212 | "Defer scheduled service" shows the raw API error | 44382 | READY TO RELEASE |

Each comment gives the build (`index-C9NJAR1x.js`), numbered steps in plain words, what happens now,
what was expected, and a *For the developer* line where there is an endpoint worth naming.

## Two passes deliberately NOT moved

**ZP-4159 — Facility Manager can open work orders for unassigned sites.** The access hole itself is
closed and was proved today with a positive control. But the refusal renders
`Unexpected token '<', "<!DOCTYPE "... is not valid JSON` instead of the app's own Access Denied card,
so "everything is working as expected" is not true of the screen yet. Left in Ready for QA pending a
decision: move it and raise the error copy as its own ticket, or hold it until the copy is fixed.

**ZP-4030 — deep linking email notifications.** Only the backend half is checkable from here: both
well-known files are served. The user-facing half — clicking a link in a notification email and landing
on the right work order with the right site — needs a real notification email, which this run did not
generate. Not a full pass, so not moved.

## Not moved for other reasons

ZP-4042 remains the promotion blocker. The eight open records from the morning re-check keep their
current statuses. The 18 tickets added to the release overnight are still untested and were not touched.
