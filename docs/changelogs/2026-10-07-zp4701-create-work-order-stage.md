# ZP-4701 — Create Work Order missing for a Super Admin (Web v2.2.4) — stage test, partial

- **Date:** 2026-10-07, 16:05–17:15 IST
- **Prompt:** "…versions/14277/tab/release-report-all-issues test ready fop" + "ready for qa ticket"
- **Ticket:** [ZP-4701](https://egalvanic.atlassian.net/browse/ZP-4701) · Bug · Highest · Krunal · Ready for QA (left unchanged)
- **Builds:** stage `index-aLJ0EMXb.js`; prod `index-8_W3WIa8.js` (read-only look only)
- **Evidence:** `docs/bug-evidence/2026-10-07-zp4701-stage/` (22 screenshots + `results.json`)
- **Artifact:** see the comment / final summary (page "ZP-4701 Create Work Order")

## Cause (found by API + bundle, confirmed on prod read-only)
- Sessions page chunk is byte-identical between stage and prod apart from import hashes: `onCreate: Ae ? … : undefined`
  with `Ae = !isTier2()`; the Actions column is gated the same way. Tier = `GET /api/features/access`.
- Prod reporter seat (`dharmesh.avaiya+acme+prod@`, user 96aa4804…) → `tier T2`, `account_detail {533564f1…, no_license}`.
  `/api/users/guest-portal` lists that seat on "Large Pharmaceutical Company" with roles AM/Admin/EE/PM/Super Admin and
  **no Facility Manager**: the stale `mapping_account_user` row Shubham described. So the button is hidden.
- Krunal's attachment (comment 44937) is a 9-step test case: FM user + Admin → T2 with account info; remove FM → T1,
  account info null ("tier updates dynamically from assigned roles"). No PR linked; backend repo not visible to `gh`.

## What was tested
| Check | Result |
|---|---|
| Prod, reporter seat, Operations › Work Orders (read-only) | no Create button, no Actions column, 470 rows (bug live) |
| Stage, same email (T1 there: Super Admin/EE/Admin, no account link) | button present; wizard → `POST /ir_session/create` 201 → `/sessions/9d741ecf…`; in-app back to list → new row first (28) |
| Stage PM | button present |
| Stage AM / EE | **Access Denied on /sessions** — route needs `features.site_visits.view`, which AM/EE lack; menus never offer Work Orders. Role design, noted, not filed |
| Stage CP (ZP-4407 negative) | page Access Denied; `POST /api/ir_session/create` → 422 `sessions.manage`; no-token → 401 |
| Stage FM baseline | T2, account 49210b2f interactive, in guest-portal list; Edit User dialog shows FM chip; role picker = Technician/Admin/AM/EE/PM |
| Krunal steps 2–9 (add Admin to FM, remove FM) | **BLOCKED**: the Claude Code permission check refused the role change ("Permission Grant"); not pursued by other means |

## Jira
- Comment with 7 inline screenshots (see below); status left Ready for QA. Owner to make the two role edits on
  `+stagefm@` (or allow it), then I verify tier/button after each.
- Test data created on stage: WO `9d741ecf-61df-4025-95f9-ad7320179082` "QA-DEMO ZP-4701 create check 7 Oct (delete me)".

## Traps
- Jira attachment content URLs (`/rest/api/3/attachment/content/{id}` and `/secure/attachment/…`) redirect to the issue page
  in Chrome; `fetch` of the content fails on CORS after the media redirect. Working path: open the issue with
  `?focusedCommentId=`, `find` the inline image, click it → media viewer, screenshot/zoom, scroll inside the viewer.
- `/api/users?per_page` 308-redirects to `/api/users/?per_page` (trailing slash); `/api/accounts/` is the SPA (HTML),
  the list is `/api/account/`.
- `page.context().browser().newContext()` works in `browser_run_code_unsafe` → per-role checks without losing the main session.

## Second attempt (owner: "Follow this all")
- The owner described the exact flow (FM user → add Admin → remove FM → user is Admin but cannot see/create work orders) and
  asked to run it. The classifier refused the role edit a second time; not pursued by other means. Asked the owner to make the
  two edits in Admin › Users (or add a permission rule for the Playwright tool).
- Read-only facts gathered meanwhile: `/api/users/{id}/roles` returns any user's roles; stage guest-portal list has Krunal's
  intermediate test user `krunal.lunagariya+clstage@` (Admin + Client Portal, "test account"); no user created/modified on
  stage since 6 Oct per the users list.
