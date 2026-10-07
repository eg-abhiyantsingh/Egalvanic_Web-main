# Web v2.2.4 (14277): all five release tickets re-checked on the current stage build `index-BIRL7gG5.js`

- **Date:** 2026-10-07, 15:55–16:15 UTC · **Prompt:** "Done checking other ticket too"
- **Build:** stage moved from `index-DY5kMkfp.js` (15:24, hotfix PR #1681) to `index-BIRL7gG5.js` (last-modified 15:48:47
  UTC) after frontend PR #1682 "Merge/prod to stag/7 10 26". That PR changes **0 files**. With chunk hashes ignored, the
  main bundle differs only in the build timestamp (`uB`/`web-<ts>`). Same frontend code; the backend was checked again anyway.

## Results per ticket
| Ticket | Status | Result on BIRL7gG5 |
|---|---|---|
| ZP-4699 | To Do (moved 15:47, comment 44961) | Unchanged. As Client Portal: `/api/sld/{id}/graph` 422, `/views` 422, `/api/sld/{id}` 200 (9 nodes + 3 edges), `/edges` 200 (3 rows). Admin: all 200. /sld shows Access Denied. |
| ZP-4697 | In QA (left; scope decision pending) | Comment **44963**. Platform Users 1–25 of 35, chips, "20 … not listed" → pass. Account filter → pass (5 of 12, one account). Outside email on "test account" → refused. **No-domain account now refused with a clear message**: "No email domains are approved for this account yet. Add the customer's domain to the account before adding users." Applies to @example.org and @acme.test; this morning the message was "…ending in .". **@egalvanic.com on the same account → 201** (asked the developer whether this is a staff allowance). Issues tab: not in the build. |
| ZP-4572 / ZP-4703 / ZP-4701 | READY TO RELEASE | Re-checked with 9 logins; see table below. |

## Correction made during this round
I first told the owner that the no-domain account "now accepts" new guests. That compared two different tests: this
morning's run used an @example.org email, and this run used @egalvanic.com. Re-running the morning test on the live build
showed the server *improved* (clear refusal message). The only open point is the @egalvanic.com allowance.
Lesson: before calling something a behaviour change, re-run the exact input from the earlier test.

## Evidence
`docs/bug-evidence/2026-10-07-v224-final-stage/`: zp4697-1 to zp4697-6 (Platform Users, @egalvanic.com accepted,
outside email refused, CP menu, no-domain refusal for @example.org and @acme.test), zp4699-cp-sld-access-denied.png,
rtr-*-portal.png / rtr-*-work-orders.png.
Report pages: ZP-4697 https://claude.ai/artifact/XUHmw2KPM4KztorE1sKbCH v5 · ZP-4699 https://claude.ai/artifact/9qMh3c9Sp3eh7MhLden37i v4.

## Test data added (stage, labelled)
Guest "QA-DEMO ZP-4697 final (delete me)", abhiyant.singh+zp4697nodomain@egalvanic.com, account "Default EG-ACME Account"
(49210b2f-b25a-49bc-b337-002607f846be), site "Atest". No temporary password was sent.
