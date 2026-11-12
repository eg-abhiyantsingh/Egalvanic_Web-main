# ZP-4835 — Mutation worker: roll back a failed handler before recording failure; local backends never consume a deployed env's mutations

**Verdict:** PASS for part (1), seen end to end from the web. Part (2) (local backend vs dev queue, ECS/RDS gate,
`MUTATION_QUEUE_ENV`) is NOT WEB-TESTABLE. QA build `index-C1ZRIunF.js` (checked 16:02 / 16:26 IST, 12 Nov 2026). Super Admin seat.

**Recommended status:** READY TO RELEASE for (1). Part (2) needs a developer check on dev with server access.

Test data (created today, QA-DEMO): site "QA-DEMO ZP-4835 12 Nov lane probe" (b698cf98-150a-4316-a104-16b75b13cb5d) with node A
"QA-DEMO ZP-4835 12 Nov node A" (8d66f94a…) and node B (56ab4c68…).

## Checked
| Check | How | Result |
|---|---|---|
| A handler flush failure is counted, retried, then dead-lettered | Queued (no `X-Direct-Write`) `PUT /api/node/update/<A> {room_id:<random uuid>}` → FK violation on `nodes.room_id`, the ticket's own example | PASS, 3 runs (16:14, 16:21, 16:24 IST): `received` → attempt 2 at about 8 s → `dead_letter`, **attempt_count 4**, at about 11 s. No endless 10 s retry |
| The recorded error is the real one (session rolled back first) | Audit Log › View Details / `GET /api/mutation/status/<id>` | PASS: `psycopg2.errors.ForeignKeyViolation … nodes_room_id_fkey … Key (room_id)=(…) is not present in table "rooms"` (shot 2). Before the fix (2 Nov, #185049, same kind of failure) the log read "This Session's transaction has been rolled back due to a previous exception during flush… first issue Session.rollback()", the PendingRollbackError symptom |
| Later writes in the same SLD lane keep applying | Queued valid rename of node B on the same site, sent right after A | PASS: B `applied` within 4–8 s, while A was still retrying (shot 1, video) |
| The failed entity itself is not jammed | Queued rename of node A after its dead letter | PASS: `applied` (run 16:14) |
| Audit Log UI | Admin › Audit Log: rows, Status chips, Attempts badge, View Details | PASS: Dead Letter row with Attempts 4; Applied row for B; details show the error JSON and payload |
| iOS asset deletes / web unassignment that used to stick | Needs the dev repro data | Not re-run. The normal sync paths applied today (B and A renames) |
| Local backend vs dev SQS, ECS/RDS gate, `MUTATION_QUEUE_ENV` | Server-side | NOT WEB-TESTABLE |
| Performance | Worker latency | Valid change applied in ≤ 4–8 s; failing change dead-lettered in about 11 s |

## Bugs found (outside this ticket's scope, low)
1. **`GET /api/mutations?status=<unknown value>` returns 500 "An internal error occurred" instead of 400.**
   Steps: (1) sign in as Super Admin on QA; (2) open https://acme.qa.egalvanic.ai/api/mutations?status=pending&limit=1 (or
   `status=bogus`). Actual: HTTP 500 `{"error":"An internal error occurred"}`. Expected: 400 naming the allowed values
   (received, processing, applied, rejected, dead_letter, logged). Control: `status=dead_letter` → 200.
   Re-proved twice (16:05 and 16:28 IST, same SA seat, same URL). Severity: Low. The Audit Log UI only sends valid values, so users
   are not affected; it is an unhandled-input 500 that shows up in error monitoring. Suggested assignee: Avani (backend).
   JQL run: `project = ZP AND (text ~ "mutations status 500" OR text ~ "audit log status filter" OR summary ~ "mutations") AND created >= -120d`.
   No match (closest: ZP-4883, `since` echo, Done; ZP-3719, audit-log filters, Done).

## Enhancement suggestions
- Treat integrity errors (foreign key, not null) as **permanent**. Dead-letter them on the first attempt instead of three
  "transient" retries. Today they are marked `error_type: transient` and retried 3 times with no chance of success.
- Audit Log: fill **Entity Name** for update rows. Today it shows "-" for every update, so searching "ZP-4835" finds only the creates.
  The update rows (including the dead letter) can only be found by mutation id or by scrolling.
- Audit Log: show the dead-letter reason (first line of `error_message`) as a column tooltip, so support can triage without opening
  each row.

## Mobile impact
iOS/Android: an offline change that the server rejects (for example a room deleted on web while the device was offline) should now
dead-letter after 4 attempts, and the device's later changes for that site should still sync. Worth one offline repro on each app.

## Not covered
Part (2), local backend / dev SQS / ECS-RDS gate / `MUTATION_QUEUE_ENV`: server internals with no web surface.

## Evidence
- `QA-12Nov-ZP-4835-video-steps.mp4` (65 s): Audit Log → two queued changes sent from the browser → A dead-lettered after 4
  attempts, B applied → View Details with the real FK error → timeline card.
- `QA-12Nov-ZP-4835-1-audit-log-dead-letter-and-applied.png` (PASS): boxed rows, A Dead Letter (4) and B Applied, same site.
- `QA-12Nov-ZP-4835-2-dead-letter-details-real-error.png` (PASS): details with the ForeignKeyViolation text.
- Raw: `.playwright-mcp/v26/eric7/r04-out.json`, `v01-out.json`, `r06-out.json` (500 re-proof).
