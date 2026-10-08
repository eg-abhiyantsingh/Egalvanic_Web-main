# Web v2.3 round 5 (QA) on release eve

- **Date:** 2026-10-08, 12:35–14:15 UTC (18:05–19:45 IST)
- **Prompt (owner):** "https://egalvanic.atlassian.net/projects/ZP/versions/14236/tab/release-report-all-issues we need to test this ticket too. tommmarow is our release"
- **Build:** QA `index-ChnGy-li.js` (new since round 4's `index-DlU497fE.js`)
- **Version 14236 (Web v2.3, 89 tickets) at start:** 15 Ready for QA and 2 In QA. A background agent read every ticket's description and all comments first (checklist point 17).

## Results
| Ticket | Result | Jira |
|---|---|---|
| ZP-4730 Create User outside email shows raw "API call failed: 400 {…}" | **PASS**: plain sentence; POST /api/users/ still 400; @example.com same | 45006 → READY TO RELEASE |
| ZP-4527 Customer tree click account/site | **PASS**: account → its account page; site → Site Data › Assets with that site current; chevron only toggles; account total = sum of sites (1,816 / 451); 0-site account OK. Question: "Android Site 2" shows 451 in the tree, 445 on Site Overview, 349 rows in Assets | 45007 → READY TO RELEASE |
| ZP-4713 Update Service: hidden "AI is revising" dialog loses the running state | **PASS**: after Hide or Escape the button turns into "Updating…"; clicking it while `running` reopens the progress view (no blank form); survives a reload; back to "Update service" when `applied` | 45008 → READY TO RELEASE |
| ZP-4723 CP SLD data via /api/sld/{id}, /edges | Working as designed (Avani 8 Oct, owner agreed) | 45010 → READY TO RELEASE |
| ZP-4398 AI editor on SLD configs | **PASS**: re-checked on the new build ("Site · Performance sld", Edit with AI); the 6 Oct move to RTR had never gone through | 45011 → READY TO RELEASE |
| ZP-4428 500 on node-session bulk-create | **PASS**: all 3 gaps from Avani's note, on QA. Quick Count with photo; Copy Quick Count (1 asset New room Z → gygy › gh › io); missing node/session → 400 naming the id (single + bulk); already linked → count 0; 3 identical requests at once → 201 ×3; another company's WO refused. Every write carried X-Direct-Write | 45016 → READY TO RELEASE (Sentry 2PP check after prod release) |
| ZP-4189 Dashboard LCP | **Improved, not at target**: warm reload median 3.40 s PM / 3.49 s Admin (was 5.77 s); cold 4.21 / 3.95 s; TTFB about 1.3 s. Render delay 4.5 → 2.1 s. Branding cached per host (`eg.branding.v1:acme.qa.egalvanic.ai`) | 45009, kept In QA (owner decides) |
| ZP-4080 Print QR Labels | Flag `feature-qr-labels`: real ON OK. Simulated OFF (test browser only): menu entry locked, /qr-labels "Feature Not Available". Real OFF not done: Shubham said "OFF for acme QA" but the app still reads ON (v13) | 45017, kept In QA |
| ZP-4530 Collapsed node won't expand | Set up on QA (QA-DEMO chain, custom view, MCC1 collapsed, renders purple). The Expand + delete step is unknown: the canvas is CSS-zoomed, so automated clicks miss. Asked Krunal what PR #1663 changed | 45020, In QA |

## Not tested this round (blocked, owner to decide)
- **ZP-4590:** waiting for Avani (which company/env).
- **ZP-4148:** duplicate of ZP-4138; its rule was replaced by ZP-4703.
- **ZP-4394:** needs class-scoped test equipment from Eric.
- **ZP-4372:** the DB checks need Dharmesh.
- **ZP-4529:** the dev's answer on the limit-reached reply is missing.
- **ZP-4423:** pipeline only, not testable on web.
- **ZP-4439:** infra; the dev says the approach did not work.
- **ZP-4305:** 6 Oct PASS with a header-lag open point. No new PR; the QA-DEMO issue is Open, so the lag needs a resolve-with-photo first.

## Side events
- **Maintenance Portal disappeared for Acme QA** between 12:40 and 13:00 UTC: `/api/features/access` shows maintenance-portal `is_entitled: false`, while the LD flag is still ON (v21). This happened around Shubham's "Disabled for Acme QA" reply. Asked him in eg-internal-dev to restore it, and to switch only `feature-qr-labels` (QA env client id `6a34e78f93e3e00a6eef8fda`, org key `d59d449b-…`).
- The owner forwarded my v2.2.5 production summary to Krunal ("Please check this. (Urgent)").

## Test data created (QA-DEMO)
- **WO `51d54087-…`:** 2 Circuit Breakers from Quick Count (one with a photo); 1 copied asset in room "gygy › gh › io".
- **Site `fdb3f473-…` ("QA-DEMO ZP-4675 site keep as entered (delete me)"):**
  - nodes QA-4530 T1, BUS1, CB1, MCC1, CB2, CB3 (5 connections);
  - view "QA-4530 view (delete me)" `468b1eea-…`, with MCC1 collapsed.
- **Service "QA-DEMO builder-dialog test - delete me":** 4 short AI updates (ZP-4713).

## Evidence
- `docs/bug-evidence/2026-10-08-v23-qa-r5/zp4730|zp4527|zp4713|zp4189|zp4398|zp4428|zp4080|zp4530/`
- Scripts: `.playwright-mcp/v23/r5*.js` (gitignored)
