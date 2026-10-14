# Web v2.4 (Jira version 14280, release 23 Oct): deep QA pass started

- **Asked by owner, 12 Oct ~18:15 IST:** deep sanity test of the release (API, buttons, UI, enhancements, tenant, all roles,
  LD flags ON/OFF incl. new flags, performance), iOS/Android tickets to Nency, tickets for every new bug or suggestion.
  Follow-ups: "for enable disable flag message Shubham to group", "test all ready to qa ticket".
- **Release at 18:20 IST:** 100 tickets = 71 Ready for QA (all Eric's), 27 To Do, 2 Done.
- **QA build:** `index-BdXGlAq5.js` deployed 17:35 IST (dev promoted; before that QA was the prod re-baseline).

## Jira (from abhiyant.singh@, account checked)
- 65 web-testable tickets moved Ready for QA → In QA ("Start Testing"), 18:35 IST.
- 6 mobile-only tickets left in Ready for QA for Nency: ZP-4573, 4600, 4608, 4613, 4622, 4651.

## Nency (eg-internal-dev, tagged, ~18:45 IST)
Sent the 6 mobile-only keys + the 24 mixed web/iOS tickets (ZP-4481, 4482, 4514, 4532, 4557, 4570, 4579, 4583, 4591, 4593,
4594, 4611, 4614, 4617, 4618, 4621, 4645, 4683, 4688, 4717, 4751, 4753, 4799, 4800), noting the SLD viewer OTA republish need.
Promised per-ticket iOS checks after the web pass.

## Test groups (8 parallel testers, shared brief `docs/bug-evidence/2026-10-12-v24-qa/BRIEF.md`)
| Group | Tickets |
|---|---|
| SLD canvas/connections/classes | 4513, 4514, 4591, 4593, 4603, 4619, 4621, 4799, 4800, 4803 |
| SLD notes/realtime/groups/issues | 4481, 4570, 4594, 4597, 4601, 4617, 4618, 4645 |
| SKM / engineering export-import | 4482, 4484, 4582, 4746, 4750, 4760, 4762, 4805 |
| Journal walks / AI pipeline | 4480, 4508, 4511, 4512, 4535, 4561, 4564, 4571, 4579 |
| Asset records (FSA/torque/UT/IR) | 4532, 4557, 4560, 4586, 4683, 4688, 4710, 4801 |
| Work orders / Planned Work v2 | 4559, 4562, 4583, 4614, 4684, 4720, 4755 |
| Asset page / editor / forms | 4580, 4581, 4611, 4612, 4717, 4751, 4753 |
| Admin / tenancy / guest / infra | 4565, 4585, 4592, 4719, 4802, 4574, 4584, 4587 |
Verdicts land in `docs/bug-evidence/2026-10-12-v24-qa/<key>/verdict.md`; the main session posts Jira comments/status and
asks Shubham (eg-internal-dev) for LD flips.

## 13 Oct (just after midnight IST): first 10 tickets posted (owner: "update ticket status right now, cover 10 tickets first, then wait")
From abhiyant.singh@ via claude-in-chrome (account checked before every write), QA build index-D1IWZ6SI.js (same code as BdXGlAq5):
| Ticket | Comment | Status |
|---|---|---|
| ZP-4802 hide opportunities + soft-delete tenant gate | 45098 | READY TO RELEASE |
| ZP-4592 bulk-edit tenant gate | 45099 | READY TO RELEASE |
| ZP-4587 site-sync payload / account gates | 45100 | READY TO RELEASE |
| ZP-4482 engineering XML zip + README PDF | 45101 | READY TO RELEASE |
| ZP-4484 GF_* on Protection | 45102 | READY TO RELEASE |
| ZP-4750 omit building from AF labels | 45103 | READY TO RELEASE |
| ZP-4760 layout attrs / node-bus flags | 45104 | READY TO RELEASE |
| ZP-4762 node-bus Exclude flags | 45105 | READY TO RELEASE |
| ZP-4805 group eg_y midline | 45106 | READY TO RELEASE |
| ZP-4565 Maintenance Portal gate | 45107, 45108 | READY TO RELEASE (13 Oct, real flag OFF 12:57 → ON 13:06 IST, 9 seats PASS; QA EE role lacks maintenance_portal.manage, noted) |

Re-verified by me (PM seat, is_eg_admin false, 12 Oct ~23:55 IST) before filing — both HIGH cross-tenant leaks CONFIRMED:
- GET /api/quote/by-sld/f86052b7…/lightweight → 200, other company's quotes (random id → 0; GET /api/sld/f86052b7 → 422).
- POST /api/sld/f86052b7…/export-skm → 200, 375 components + presigned XML URL (random id → 400).
NOT yet filed (owner said wait). Pending bug list: quote/by-sld IDOR (High), SKM export/import IDOR (High), bulk-edit re-import phantom
updates (Medium), SLD-page SKM export not behind feature-skm-export (Medium), ZP-4582 import tiles/namespace (Med x2), ZP-4746 tooltip (Low),
/guests route not behind feature-guest-access (Low), malformed sld_id → 500 on quote/create-standard + bulk-edit (Low).
6 testers (SLD canvas, SLD notes, journal/AI, records, WO/planned, asset/forms) stopped at the session limit; partial work in
docs/bug-evidence/2026-10-12-v24-qa/. Remaining 55 In-QA tickets still to verdict.
