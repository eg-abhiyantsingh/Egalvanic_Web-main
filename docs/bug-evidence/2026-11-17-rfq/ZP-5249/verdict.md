# ZP-5249 — Redesign the Getting Started user guide with step-by-step progress tracking

**Verdict: PARTLY**
The progress tracker works for staff seats: stages show in order, the overview names the current stage and next step, and a real saved step ticks within about 5 s. The tick also survives a reload and a sign-out/sign-in. Four new defects (below) stop a PASS:
- Another user's new site completes **your** Customer & Site Setup step.
- The Facility Manager (FM) is sent to screens it cannot open.
- The Client Portal (CP) seat gets an empty guide whose only button throws an error.
- Turning the company "Guided tours" switch OFF does not stop the welcome popping up.

- **Build:** `index-oFAZQ3rK.js` (QA, checked at 11:12 and again at 11:58 IST, 17 Nov). The live bundle contains both of the newest guide commits on `cicd/qa`: c5422a9c "Fix Getting Started guide role gating, wording and false ticks" and 1bac3e78 "Restore bulk permission…". PR: eg-pz-frontend #1948 (merged 5 Nov).
- **Company:** EG-ACME (acme.qa.egalvanic.ai). LaunchDarkly `feature-user-tour` = **true** (live evalx). Company "Guided tours" switch = ON (left ON).
- **Seats:** SA (multi-role: Super Admin, Admin, Portal Sales, AM, EE, PM), ADMIN, PM, AM, EE, TECH, FM, CP, PS.
- **Ticket comments:** none. QA Review rows are taken from the description.

## What was run (all in a visible Chrome, Playwright)
| # | Check | Result |
|---|---|---|
| 1 | First sign-in auto-opens "User Guide · End-to-end tour / Here's how EG-ACME works" | PASS. Seen for all 8 web seats; per user, per browser |
| 2 | "Maybe later" closes; reload → does not come back | PASS |
| 3 | Close (X) and Esc close it | PASS |
| 4 | Help → Getting started reopens it | PASS (every seat with workflows) |
| 5 | Stages in order, current stage + "Your next step" | PASS: "Workflow 1 of 5 · Start with the customer you work for"; after progress: "You are here", "In progress · Step 2 of 2 · To finish: Add a building." |
| 6 | Real step with saved data (SA): Customer C → Site C → building/floor/room | PASS. "+ New Customer" item ticked **5.7 s** after Create. Step 1 Complete **1 s** after Add Site, naming my own site. Building tick **6 s**, floor **4 s**, stage Complete **5 s** after the room. Line: "Verified: QA-DEMO ZP-5249 17 Nov Customer C · … Site C · building › floor › room mapped" |
| 7 | Negative: open New Customer, type a name, Cancel | PASS: nothing ticked after 12 s (card still "0 of 2 done") |
| 8 | Persistence: reload; sign out and back in (same browser) | PASS. Reload keeps the ticks. After sign-in the card stays closed (by design: sessionStorage). Help → Getting started shows Customer & Site Setup "In progress · Step 2 of 2" |
| 9 | Persistence in a NEW browser | Progress is gone: "0 of 5 workflows complete", stage 1 "Pending". Progress is localStorage only. See enhancements |
| 10 | Overview + progress card render; minimise / close ("Close the guide?") / "Pause · view all workflows" | PASS |
| 11 | "Go to …" links and stage buttons land on the right page | PASS for staff seats (/customers, /locations, /assets, /sessions, /opportunities, /slds). **FAIL for FM** (lands on Site Health) |
| 12 | Texts vs real menu/button names | PASS. Admin › Customers; Site Data › Locations / Assets; Operations › Work Orders; Sales › Quotes; Engineering › SLD. Also checked: + New Customer, Create, + Add Site, Add Site, Add Building, + Add Floor, + Add Room, Create Asset, Upload Anything → Extract assets, Bulk Edit › Download Template / Bulk Import, SKM › Import (Full), Create Work Order, WO Bulk Ops, Actions › Add New Asset / Add Existing by Service / Upload IR Photos, New Quote → Create Quote, Mark as Sent, Recipient, SLD lock toggle. Exception: FM, whose menu has no Site Data / Engineering |
| 13 | Per-role workflow lists (table below) | PASS for SA, ADMIN, PM, AM, EE, TECH. **FAIL for FM and CP** |
| 14 | Console | Staff seats: no guide errors. The only noise is unrelated: Beamer 429/CSP, DevRev CORS, realtime websocket. FM: 422 + "[mainAPIService] API call failed … permission_denied" every ~8 s. CP: TypeError on click |
| 15 | LD flag `feature-user-tour` OFF (SIMULATED: evalx answer patched AND the LD stream blocked) | PASS: no auto-open; Help links straight to /z-university. The 26 Oct question is answered: the web app now reads this flag. A real flip needs LD access: flag `feature-user-tour`, env `qa`, org EG-ACME |
| 16 | Company "Guided tours" switch OFF (REAL, flipped twice and restored ON; API confirmed `true` after) | Help → link to Z University: PASS. **But a fresh PM sign-in still got the guide auto-opened: FAIL (new bug).** PM cannot see the switch; PM PATCH → 403 "Admin access required": PASS |
| 17 | Reduced motion | PASS: stage-card hover transform/transition = none under `prefers-reduced-motion: reduce` (normal: translateY(-2px)) |

## Per-role workflow lists (fresh sign-in each)
| Seat | Menus the seat has | Workflows offered | OK? |
|---|---|---|---|
| SA | all | Customer & Site (2 steps), Asset Onboarding (5 ways), Work Order (3), Quote (2), One-Line (2) | yes |
| ADMIN (ADMINQA) | Builder, Maintenance Portal, Admin | Customer & Site (**"1 STEPS"**) only | yes. Minor: card first says "To finish: Add a building." although the map step is not offered to this seat |
| PM | Site Data, Operations, Maint. Portal, Admin › Customers | Customer & Site (2), Asset Onboarding (4 ways, no SLD way), Work Order (3) | yes (no Engineering/Sales in menu). Way 4 drops "Open Engineering › SLD" correctly |
| AM | Site Data (no Locations), Sales, Maint. Portal, Admin › Customers | Customer & Site (**"1 STEPS"**), Asset Onboarding (4), Quote (2) | yes |
| EE | Site Data, Engineering, Admin › Customers | Customer & Site (2), Asset Onboarding (5), One-Line (2) | yes |
| TECH | Site Data, Operations (WO), Engineering › SLD, Admin › Customers | Customer & Site (2), Asset Onboarding (2 ways: by hand, one-line), Work Order (3), One-Line (2) | yes. TECH really has New Customer, Add Site, Add Building, Create Asset, Create Work Order |
| FM | Maintenance Portal only | Asset Onboarding (5 ways), One-Line (2) | **NO**: /assets and /slds redirect FM to /maintenance-portal/overview (Site Health) |
| CP | Maintenance Portal (4 items) | **0 workflows**, but the guide still auto-opens and Help shows Getting started | **NO** |
| PS | — | "Web Access Restricted" (no web) | n/a |

## NEW defects (separate from the ticket's own scope); drafts in this folder
1. **BUG-colleague-site-false-tick.txt** (suggest High). User A starts Customer & Site Setup. User B (another seat, another browser) adds any site. Within ~2–8 s, A's card shows "Add the customer and site — Complete · Site: <B's site>". It also happened naturally at 11:25 IST: my tour adopted another tester's "QA-DEMO ZP-5285 17 Nov" site, which has **no customer at all**. Video + 2 screenshots.
2. **BUG-portal-fm-dead-workflows.txt** (suggest Medium). The Facility Manager is offered Asset Onboarding + One-Line Diagram. Every screen they name (/assets, /slds) redirects to Site Health, and "Go to Assets" lands on Site Health. "Open Site Data › Assets" shows ticked although the seat never saw it. The guide also POSTs /api/account/by-company/:id/v2 → **422 permission_denied every ~8 s** (9 times in ~50 s), and the console shows the error. Video + screenshot.
3. **BUG-cp-empty-guide-crash.txt** (suggest Medium). The Client Portal seat gets an auto-opened guide with 0 workflows ("…, step by step", "All 0 workflows", "Getting started is complete", "0 of 0 workflows complete"). Its only button, "Revisit workflow 1", throws `TypeError: Cannot read properties of undefined (reading 'key')` and nothing opens. Video + screenshot.
4. **BUG-tours-off-still-autoopens.txt** (suggest Low/Medium). With the company switch "Guided tours" OFF ("Turn this off and nobody here sees them"), a first sign-in still pops the guide; only the Help entry obeys the switch. Real switch, reproduced twice, restored ON. Video + screenshots.

Likely the same pattern as bug 1, NOT run in the browser: the code also ticks Work Order Setup and Quote Creation on any work order or quote made company-wide since the tour started (no creator filter). Worth a dev look while fixing bug 1.

## Enhancement ideas (not defects)
- Save progress per user on the server: today a new browser or device shows 0 of 5 even after a stage was verified, while the footer says "progress is saved for you". The "seen" flag is per browser too, so the welcome returns on every new browser.
- Stage 1 card shows "Site: <whatever site is selected>" before the reader has made a site. The "To finish" hint also flips between "Add a building." and "The customer and its site aren't saved yet." Show only the stage-1 gap until the new site exists.
- Grammar: "STAGE 1 · 1 STEPS" → "1 step" (ADMIN, AM).
- The expanded card covers the right ~22 % of the page (last table columns on Customers/Assets). When docked bottom-left during dialogs, the round "Z" button overlaps its title ("…omer & Site Setup").
- While the card is open the guide re-reads company-wide lists every 8 s and on each route change: 14 POSTs to /account/by-company in ~50 s for ADMIN; FM re-reads the whole-site node list. A lighter check would help large tenants.
- The company "Guided tours" switch (`input role=switch`) renders with no accessible name, although the code passes `aria-label`.
- Asset Onboarding is "Complete" only after all 5 ways. Most readers onboard one way, so this stage never completes for them (by design per code, but discouraging).
- The Help menu renders twice in the DOM (two "Getting started" + "Z University" pairs visible to scripts). It looks like one menu on screen; harmless, worth a look.

## Not covered
- Asset Onboarding ways 2–5 (Upload Anything, spreadsheet, SKM, SLD), Work Order Setup, Quote Creation and One-Line drawing were not completed with real data. The texts and buttons were checked; only Customer & Site Setup was done end to end.
- Real-step ticks were run on the SA seat only. The other roles were checked for lists, screens and buttons.
- LD `feature-user-tour` OFF was simulated, not flipped in LaunchDarkly.
- Demo company and French locale were not checked.
- Mobile: this guide is a web-only feature; mobile not checked.

## Test data (QA, EG-ACME)
- Customer C (full stage done): https://acme.qa.egalvanic.ai/accounts/b800e7c7-60bd-4dfc-9309-11f2cca8d27a. Site C id abb166c9-903b-4097-8359-6eb6195d42ed with building "QA-DEMO ZP-5249 B1" › "QA-DEMO F1" › "QA-DEMO R1".
- Customer D (video run): https://acme.qa.egalvanic.ai/accounts/4e76a5e9-64ca-49af-98e8-65544ecd6662. Site D id 13262644-576c-4fcf-8d59-b678d8e436e9.
- Customer B + "colleague site 1/2" (bug 1): https://acme.qa.egalvanic.ai/accounts/0120b301-34f3-47ad-ba08-49c9aa208e99
- Customer A (customer only, no site): name "QA-DEMO ZP-5249 17 Nov Customer A".
- Other tester's site adopted by my tour: "QA-DEMO ZP-5285 17 Nov" (id bf23e07f-8525-4378-be5a-bfda14fa3089, no customer).
- Guide: https://acme.qa.egalvanic.ai/dashboard → Help → Getting started. Company switch: https://acme.qa.egalvanic.ai/account → Company tab.

## Evidence
- Video (intended path): `QA-17Nov-ZP-5249-guide-progress-steps.mp4` (64 s, trimmed)
- Screenshots: `QA-17Nov-ZP-5249-1-overview-five-workflows.png`, `-2-customer-saved-item-ticked.png`, `-3-site-saved-step-complete.png`, `-4-after-sign-out-in-progress-kept.png`, `-5-stage-complete-building-floor-room.png`, `-role-PM-three-workflows.png`
- Bug videos/screens: `QA-17Nov-ZP-5249-BUG-*.mp4/png`
- Raw probe output per seat: `work/p3-<SEAT>.json` (lists + card texts), `work/p5-*.json` (FM/CP/ADMIN), `work/p6-*.json` (buttons per role), `work/p7-C.json` (real step timings), `work/p9.json` (labels), `work/p13.json` (company switch), `work/p16.json`.
