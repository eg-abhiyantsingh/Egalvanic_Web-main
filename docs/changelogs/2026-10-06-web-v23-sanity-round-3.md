# Web v2.3 — sanity round on the new QA build (6 Oct 2026)

**Prompt:** "https://egalvanic.atlassian.net/projects/ZP/versions/14236/tab/release-report-all-issues test all ready to qa ticket sanity testing"

**Where:** acme.qa.egalvanic.ai, web build `index-Dyz0HxpC.js` (deployed overnight; 5 Oct was `index-CdsUTPRH.js`, then `index-C913tyjW.js`).
**Page (same link, now version 8):** https://claude.ai/artifact/3e3VwLmEk5fDa7pQEyb24y
**Evidence:** `docs/bug-evidence/2026-10-06-v23-qa-r3/zp<number>/` (`results.txt` + screenshots), posted comment texts in `jira-comments/`.

## Scope
Jira version 14236 had **9 tickets in Ready for QA** at 13:20 IST: the 7 held on 5 Oct plus ZP-4421 (back from To Do with backend PR #1476) and
ZP-4590 (back from In Progress, no comment).
**ZP-4435** ([Web] Easier photo access from SLD) was moved to Ready for QA by Avani at 13:38 IST, after this list was taken; it is handled
as an addendum at the end of this file. Each was re-run on the new build the way a user would use it; every ticket got a Jira comment with
screenshots (the owner's standing rule), posted through the same render-checked path as on 5 Oct.

## Results
| Ticket | 6 Oct result | Jira |
|---|---|---|
| ZP-4421 | **PASS** — search now matches the "Applies to" text ("automatic" keeps only the Automatic Transfer Switch forms; "Any subtype" forms hidden; "transfer", "ATS", "bolt" behave) | **READY TO RELEASE** (moved) |
| ZP-4590 | **FAIL on QA** — Similar 95/90 °F → Delta T 5 °F → "Nominal"; the class expression on QA acme is still the old one, no "Minor" in any Thermal Anomaly class | Ready for QA, environment question asked |
| ZP-4148 | **FAIL as written, rule changed** — the new build drops the "Portal Sales" role-name gate; the portal follows features/access, so the Super Admin seat without Portal Sales now sees the whole portal (hidden on 5 Oct); PM unchanged (menu hidden, /condition still opens) | Ready for QA, duplicate of ZP-4138 per Dharmesh |
| ZP-4301 | PARTLY TESTED — server still names the view (re-proven with Fuse 2 in a new QA-DEMO view, single + bulk delete 400); web still sends allow_collapsed | Ready for QA |
| ZP-4305 | PARTLY TESTED — /pull-through-work still a blank page with the old heading, no route in the build | Ready for QA |
| ZP-4372 | PASS web part — 10 pages, 242 API calls, no 5xx; slowest 3.1 s (assets lookup, cold) | Ready for QA (DB checks with Dharmesh) |
| ZP-4394 | PARTLY TESTED — sections still done (section_states), Forms tab lists the Torque Record | Ready for QA |
| ZP-4423 | NOT TESTABLE from the web (pipeline fix) | Ready for QA |
| ZP-4529 | NOT CONFIRMED — simulated blank 200 reply still shows "HTTP 200"; no limit text in the new editor chunk | Ready for QA |

Also seen: ZP-4428 went READY TO RELEASE → Backlog at 21:19 IST on 5 Oct, moved by the Sentry integration (production error 2PP fired again;
the fix ships with ZP-4181). ZP-4665/4666 are new in the version (Backlog). ZP-4398 is still To Do.

## How it was done (fast path)
- One signed-in admin session (`QaDriverDaemon`, visible Chrome) ran scripted batches: Add Procedure dialog + real key presses for the
  four search terms; Create Issue with Thermal Anomaly (MUI selects open on mouse-down, not click); page sweep reading
  `performance` entries for 5xx; forms API; AI-edit reply simulation in the page.
- `V23RoleProbe` (own Chrome) re-ran the PM and Super Admin seats for ZP-4148 — that is what exposed the gate change; the old and new
  main bundles were diffed for the "Portal Sales" string to explain it (code, not data).
- ZP-4301 used an existing asset in a fresh view instead of creating a node: a hand-built `/node/create` body got a 500 (my payload, not a
  user flow; noted, not filed).
- Two independent reviewers checked every draft against the notes and the screenshot pixels before posting.

## Jira changes
- ZP-4421 → READY TO RELEASE (transition 7, status only).
- One QA comment per ticket with the screenshots attached (ids 44882–44891 in `docs/bug-evidence/2026-10-06-v23-qa-r3/jira-comments/POSTED.md`).
- **One wart:** ZP-4421's comment went out twice (44882 and 44883, identical, 1 ms apart). The first attempt uploaded the screenshots
  through Jira's own hidden file input, which froze the page renderer after the comment request had already left; the retry posted it
  again before the duplicate guard could see the first. Left in place for the owner to delete (the assistant does not delete comments).
  Lesson: never touch Jira's native `input[type=file]` from the extension; inject an own `<input type=file multiple>`, upload with
  `POST /rest/api/3/issue/<key>/attachments`, and run the duplicate guard before `POST …/comment`.
- Nothing else changed in Jira.

## Pending from earlier prompts
- ZP-3927 / ZP-3928 (iOS 1.57 / 1.56 automation): v1.69 of the QA app is in Downloads; the suite from 2 Oct needs a run on it. Not started
  in this session beyond reading the state.
- git push of the 5 commits (blocked for the assistant; owner runs `git push origin main`).

## Addendum — ZP-4435 (arrived in Ready for QA at 13:38 IST)
**QA was redeployed again at 14:07 IST to `index-CfC0Y-hD.js`** (last-modified 08:37 GMT). That bundle carries frontend PR #1661 (the
GoJS editor chunk has `openNodePhotos`, `photo_count → photoCount`, the `.npqv` quick-view) and backend PR #1474 is live (`GET
/api/sld/<id>/graph` returns `photo_count` per node). Tested 14:40–16:20 IST in the signed-in claude-in-chrome Chrome (admin seat) on
Android Site 2 (456 graph nodes, 443 drawn, 41 with photos). Evidence `docs/bug-evidence/2026-10-06-v23-qa-r3/zp4435/`.

| Check (ticket "Done means") | Result |
|---|---|
| Badge absent at zero photos, present with the count | **PASS** — camera + count on 41 nodes; "child 1" (0) no badge, hover opens nothing |
| No photo request on graph load; one per node on first hover; none on re-hover | **PASS** — first hover = `GET /api/photo/by_entity/<node>` + `POST /api/s3/urls/batch`; re-hover 0 |
| Popover never overlaps its node, incl. near the edges | **PASS** — right of the node (gap 0 px); at the right edge it flips left; at the bottom edge it stays inside the canvas |
| Keyboard path works end to end without a mouse | **FAIL** — Enter opens the edit path ("Editing is locked" toast), Space does nothing, Tab leaves the canvas. The handler compares `lastInput.key === "Space"` but GoJS reports the space bar as `" "`, so the keyboard open never runs |
| Pan/zoom unchanged after opening and closing a photo | **PASS** — same canvas element, same scale/position |
| Expired-URL and failed-thumbnail paths | **PASS** (simulated in the browser by rewriting the signed URL) — one silent re-sign then the photo; failing every time → "Unavailable" + Retry after exactly one re-request; Retry recovers |
| Checked on a diagram with more than 100 nodes | **PASS** — 443 |
| Locked canvas only | **PASS** — Lock Graph off → no badges, no popover; on → back |

Observations (not filed): 2 of 7 real thumbnail clicks only focused the tile (not reproduced in 3 instrumented trials); Panelboard
*groups* with photos get no badge; "Open asset" opens a new tab; many Android Site 2 nodes sit stacked on identical coordinates (test
data). Jira: comment **44892** with 7 screenshots; **moved Ready for QA → To Do** (transition 2) on the keyboard failure — certain on the
current build, control = the mouse path works on the same node.

## API-calling audit for the next sprint (owner, 6 Oct: "check api testing too for qr code and other module issue … multiple api calling")
Measured on QA (`index-CfC0Y-hD.js`) from the page's own network log; evidence `docs/bug-evidence/2026-10-06-v23-qa-r3/api-audit/`
(`results.txt` + 11 screenshots). Jira has no next sprint yet (active Z-26-09-S3 ends 10 Oct; the rest is the Triage bucket), so the
audit took the QR Labels tickets (ZP-4080 Ready for QA, ZP-4667 To Do, ZP-4668 Backlog) and ZP-4666 (asset linking). Nothing was posted
on those tickets.

| Where | Calls | Reading |
|---|---|---|
| QR Labels page load, test site (1,983 assets) | `GET /api/lookup/v2/nodes/<site>?page=1..10&page_size=200` — 10 calls in 4 waves, 4.6 s first→last | loads every asset up front; grows with site size |
| QR Labels steps 2–4 (stock, designer, print) | 0 | client-side |
| Add code, 1 asset → Save | 1 × `POST /api/node/update/<asset>` | fine |
| Add codes, 3 assets → Save | 3 × `POST /api/node/update/<asset>` in parallel (same ms), 0.8 s each | per-asset writes; 50+ (ZP-4668) = 50+ requests |
| Change an existing code → pop-up → Change codes | 1 × `node/update`; the pop-up **stays**, re-rendered as "0 of these assets…" | **ZP-4667 reproduced** on the Change-codes path (Cancel closed it); the stale pop-up makes no second write |
| Print history | 1 × `GET /api/qr-labels/prints` | fine |
| Work order page load | 36 calls (17 per-WO sections; `devrev/session-token` ×2) | the ZP-4444/4458 fan-out; duplicate boot calls |
| Actions › Add Existing by Service, 3 assets | 1 × `scope-preview`, **1 × `POST /api/ir_session/<wo>/add-assets`** (1.3 s) + 12 section refreshes | already one batched call |
| same, 308 assets | **1 × add-assets (4.1 s)** + 12 refreshes, ~10 s on screen | ZP-4666's "one write per asset" is not this path; its step-1 measurement must name the path |
| App shell on every page | `/api/auth/v2/me` ×2, `/api/devrev/session-token` ×2 | the plainest "multiple API calling" |

### Filed from the audit (owner, 6 Oct: "if the api issue is critical take in current sprint otherwise next sprint")
Verdict: not critical (nothing fails for the user, no data loss, no double writes) → **next sprint Z-26-10-S1** (id 1224, 13–25 Oct).
Created through the Jira REST API from the signed-in page, each with two real screenshots attached and inline, render-checked, then
Backlog → To Do. No fix version (Jira has no next web version yet); the owner sets it when Web v2.4 exists.
| Ticket | Type · priority | Summary |
|---|---|---|
| ZP-4671 | Bug · Medium | [Web] QR Labels: opening the page downloads every asset of the site before it can be used (10 calls, 4.6 s on a 1,983-asset site) |
| ZP-4672 | Story · Medium | [Web] QR Labels: "Save codes" sends one request per asset; no batch endpoint, so a 50-asset batch is 50 requests |
| ZP-4673 | Bug · Low | [Web] Every page load calls /api/auth/v2/me and /api/devrev/session-token twice |
Bodies in `docs/bug-evidence/2026-10-06-v23-qa-r3/api-audit/tickets/`. Not filed: ZP-4667 (already in the current sprint, High; my
reproduction path is in the audit table) and ZP-4666 (exists; the add-assets measurement answers its step 1). Nothing posted on those two.

