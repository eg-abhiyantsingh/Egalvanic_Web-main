# "If a ticket passed, move it": ZP-4346 and ZP-4360 to READY TO RELEASE, ZP-4045 taken to its two server faults (2026-09-24, evening)

**Prompt:** "if ticket are passed then move ready to qa to ready to release" — a general rule, applied to the Ready-for-QA set on
`index-CC4S9HsJ.js` (bundle unchanged since the afternoon pass).

## Rule applied
A ticket moves when every acceptance criterion a web user can exercise passed on the shipping build and nothing observed
contradicts it; un-exercisable parts are named in the QA comment. Any observed failure, or a core scenario that could not
be run at all, keeps the ticket in Ready for QA.

## Moved (transition 7, QA comment each)
| Ticket | Why it passes | Comment |
|---|---|---|
| ZP-4346 | Save service pinned to the panel's bottom edge, on screen while 164 assets scroll; the ticket's purpose (primary action visible without scrolling) is met, its wording ("at the top") is not — written on the ticket, reopen if the top placement is wanted | 44498 |
| ZP-4360 | WO list 310–376 ms, asset list 285–735 ms, site dashboard 470–944 ms end-to-end from India against a 272 ms network floor; results correct (374 assets, WO rows, site card); RDS AAS left to the dev | 44499 |

## Ready for QA now: 20 (was 28 at 16:30)
Five moved by QA today (4338, 4353, 4280, 4346, 4360); six moved by others this evening: ZP-4292, 4291, 4351, 4352 → In QA;
ZP-4082, 4067 → READY TO RELEASE. Web v2.2 = 83 tickets: 20 RFQ · 47 RTR · 7 In QA · 1 In Progress · 7 To Do · 1 Backlog.

## ZP-4045 / ZP-4060 — what was proven this evening
- Nav panel 264 px, "Maintenance Program" not truncated ✔ (DOM measurement).
- Future date refused by the server at three entry points: Record completion (morning), PM-plan apply `service_dates` (400),
  asset Add Service History `last_serviced_on` (400) ✔. Pickers the ticket names stop at today (Performed on, Last performed) ✔.
  The Add Service History "Last serviced" picker (portal and staff) has no upper bound — not in the ticket's list; server holds.
- Created `QA-DEMO ZP-4045 shutdown rule - delete me` (Panelboard, 8b54aac9-…) on Android Site 2, set Shutdown Windows =
  "Can never shut down" (PUT …/shutdown → 200). Compliance then shows its Clean, Tighten, Torque / De-Energized Visual
  Inspection / NETA Testing deviations **acknowledged automatically by "Shutdown rule"** with the reason stated; IR stays open ✔.
  Score 0.3 % (3 of 865), legend "Acknowledged 3" — ZP-4060's acknowledged segment finally visible.
- **Not passing:** human acknowledge on the same pair → 400 "1 key(s) do not match a current deviation" in three request shapes
  (body, body + pm_standard_id, ?pm_standard_id=) with a key read seconds earlier; withdraw of the automatic ack (row icon)
  → **500** internal_error trace f418ae516fbc18a7d5ba4a70baf7b2dd, no toast. Acknowledgements tab lists none of the automatic acks.
- Bulk stated-history dialog not found in this build; demo-site CTT-2028 case has no acme counterpart.

## Other
- ZP-4315: Krunal's 24 Sep image is a WEB capture (ring 56 % on the Sep 8, 12:44 PM WO), not iOS — still no iOS reading.
- ZP-4390 / ZP-4391 screenshots attached through the Jira page (attachments 37559 / 37560) — the connector cannot upload.
- Boards republished at the same URLs: Deep Pass (v9) and Readiness (v12); ledger 20 rows, tiles, moves band, four-defect
  decision item, ZP-4045/4060 walkthrough rewritten with captures 32–35, ZP-4346/4360 in "Evidence for the moves".

## Defects still without a ticket (owner's go-ahead pending)
1. Compliance Acknowledge → 400 raw toast (every request shape).
2. Withdraw automatic "Shutdown rule" acknowledgement → 500, silent.
3. Customer-portal Work Orders list refused (company_data.view) + Create Work Order offered to a view-only role.
4. Minor: asset Add Service History picker accepts a future date; server refuses with a raw JSON toast.

Evidence: `docs/bug-evidence/2026-09-24-rfq-retest/` captures 31–35 + NOTES.md.
