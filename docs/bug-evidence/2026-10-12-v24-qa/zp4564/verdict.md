# ZP-4564: Issues on journal site walks. Engine proposes deficiencies, backend mints them at release

**Verdict:** FAIL (1 high bug: re-reading an accepted walk duplicates its issues and brings back a dismissed one, and release then creates them on the site). Everything else in the QA Review passed.
**Build:** QA `index-BdXGlAq5.js` → `index-D1IWZ6SI.js` (redeployed 18:48 IST; the journal/issue source maps are identical). Runner `sfn`.
**Test data (mine):** walk A https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29, control walk B https://acme.qa.egalvanic.ai/site-walks/88d2bb50-f04d-4e79-b696-d277119939ef, site `QA-DEMO ZP-4561 site (delete me)` 863c83ab-3aac-42a9-9cb1-29b86ad92659. Site issues created: 13bb6b75… (room level), 343ff287… (LP-1), then the duplicates 609448d2…, 2a2c2920…, 3b501cf1….

## Checked
| Check | How | Result |
|---|---|---|
| Blocked clearance in front of the MSB → OSHA Violation on the MSB | Note in walk A and walk B | PASS (both). "Blocked clearance in front of MSB", OSHA Violation, on the MSB, subcategory "Clearance - Insufficient Access". |
| Missing dead front on LP-1 → NEC Violation, immediate hazard, on LP-1 | Note in room 2 | PASS (both). Priority High, Immediate hazard, subcategory "Missing dead front, door, cover, etc. (NEC 110.12(A))". |
| Missing arc-flash labels for a whole room → ONE room-level issue | "Arc flash labels are missing on all the equipment in this room" | PASS (both). One issue on "the location itself", not one per asset. |
| Wear the tech calls sound → no issue | "light surface rust on the T2 enclosure but it is solid" | PASS (both). No issue raised. |
| Issue editor dialog | Issues tab › click a card | PASS. Class, Priority, Title, Found on, Description, Proposed resolution, Immediate hazard, Details (class properties), "Photos that show it 0/12", evidence quote, buttons Cancel / Not an issue / Save. |
| Edit → "Edited" chip; dismiss → "Dismissed" | Priority Medium → High › Save; then "Not an issue" | PASS. PUT 200 in about 2.5 s; the card shows Edited, then Dismissed (struck through, moved to the end). |
| PUT negatives | `PUT /site-walk/<id>/issues/<issue>` | PASS. Bad priority → 400 `bad_priority`; a random unit or a unit from another walk → 400 `bad_asset`; another company's class (fdcadc86 "OSHA Violation") → 400 `bad_issue_class`; a **global** class → 400 `bad_issue_class`; a photo from another walk → 400 `bad_photo`; empty title → 400 `title_required`. An unknown issue id, or another walk's issue on this walk's URL → no JSON (QA's CDN serves index.html for 404). |
| Release mints one issue per accepted, undismissed walk issue: on its node, site level if no unit | Accept › Create assets | PASS. "2 issues created": dead front on node LP-1 (High, immediate hazard, Open); arc-flash labels with `node_id null` on the site. The dismissed OSHA issue was **not** created. `issue_id` is stamped back and the card shows "On the site". |
| Photos linked to a walk issue are copied as issue photos | Linked the LP-1 nameplate photo to an issue (the same PUT the dialog sends), then Create | PASS. The site issue has 1 photo. |
| Release again → 0 issues | `POST /release` again | PASS (before any re-read): `issues_created 0`. |
| **A person's edit or dismissal survives a re-read** | Accepted + released walk A, then added one note (new transformer T5) and let it re-read | **FAIL.** The 3 old walk issues stayed, and the re-read **added 3 new walk issues** with re-worded titles: "Blocked clearance — MSB" (the issue I had **dismissed** is back as a new, undismissed Medium issue), "Missing arc flash labels — main electrical room", "Missing dead front — LP-1". Header: "Create 3 assets" (3 issues pending). Create made 3 more site issues. LP-1 now has 2 identical "Missing dead front" issues, and the dismissed clearance issue is now Open on the MSB. |
| A walk with issues but no units refuses release (`nothing_to_release`) | Fresh walk, release before any reading | PASS for the empty walk: 409 `nothing_to_release` "This walk has no assets yet — there is nothing to create." (I could not make an "issues but no units" walk on purpose; the engine always made units.) |
| Roles / tenant / flag / perf | See the ZP-4561 role table. Issue PUT tenant checks are above. `feature-site-walks` = true. Issue PUT about 0.3 s API, about 2.5 s UI round trip. | See ZP-4561. |

## Bugs found
1. **Re-reading an accepted journal walk duplicates its issues and brings back dismissed ones; Create assets then creates the duplicates on the site** (High; backend #1427 + pipeline #138, suggested Avani; owner Eric).
   Steps: 1) Journal walk, note: "…The clearance in front of the MSB is blocked by stored boxes. Arc flash labels are missing on all the equipment in this room…"; room 2: "…The dead front is missing on LP-1…". 2) Wait for the reading; open the Issues tab (3 issues). 3) Open "Blocked clearance…", change Priority to High, Save; open it again and click "Not an issue". 4) Accept › Create assets (2 issues created). 5) Add any new note to the walk and wait for "Accepted · kept up to date". 6) Open the Issues tab, then Create.
   Actual: 6 walk issues. The dismissed one is back as a new undismissed issue, and the 2 already created on the site appear again. Create makes 3 more site issues (duplicates, including the dismissed one).
   Expected (ticket): "A person's edit or dismissal always wins over a re-read", and release stays idempotent. The re-read should match the existing walk issues (engine uid), not add re-worded copies.
   Evidence: `QA-12Oct-ZP-4564-5-issue-dismissed-PASS.png` (before), `QA-12Oct-ZP-4564-6-after-reread-issues-duplicated-FAIL.png` (after), `../zp4561/QA-12Oct-ZP-4561-16-after-reread-release-result-FAIL.png` ("3 issues created on their assets"). Site issue ids are listed above. No existing ticket covers it.
   Note: before acceptance, re-reads kept issue ids fine (walk A's 2nd read kept `2d2574a3`/`418cd18b` and added only the new dead-front issue). The duplication shows up on an **accepted** walk.

## Enhancement suggestions
- Issues tab: add filters (live / dismissed / on the site) and a count per state. After a few re-reads the list gets long and mixed.
- When a walk issue is already "On the site", give a link to the real issue (`/issues/<id>`).
- In the issue editor, allow "Found on" to pick a location (room), not only a unit. Today a room-level issue can only stay room-level through the engine.

## Mobile part for Nency
Mobile journal walks feed the same engine. On iOS/Android: dictate a deficiency ("dead front missing on LP-1"), let the web read it, and check the phone shows the issue (if the app lists walk issues). Then check that after Create assets the issue appears under the asset's Issues on mobile with its photo.

## Not covered and why
- "Issues but no units" refusal: the engine always proposed units for my notes, so I could not build that state from the UI.
- `site_walk_issues` migration (DB only).
- `validate_issue_details` parity between engine and backend (code-level; the engine wrote valid class details in every case).

## Re-check 13 Oct on index-C-Cc1qD1.js

Re-tested 15:50–16:10 IST on QA build `index-C-Cc1qD1.js`, Super Admin seat, walk A https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29. Live Jira ticket: no comments, still In QA. LD `feature-site-walks` = true. Re-used walk A (no new walk). The readings are shared with ZP-4535 (job list there).

| Claim | 13 Oct result |
|---|---|
| State before re-reading | 6 walk issues: the original dismissed "Blocked clearance in front of MSB" (High, Dismissed, Edited) plus 5 already "On the site". `QA-13Oct-ZP-4564-1-issues-before-reread-one-dismissed-PASS.png` |
| Re-read with nothing new | PASS (positive control). Job 9625c519 kept the same 6 issues (same ids); the dismissed one stayed dismissed; nothing pending. `QA-13Oct-ZP-4564-4-nothing-new-reread-issues-unchanged-PASS.png` |
| **Bug: a re-read that changes the model re-proposes issues, and brings back a dismissed one** | **STILL FAILS, reproduced with the same kind of input as 12 Oct** (one note adding transformer T6; job d811187c, 16:04–16:05 IST). The walk went from 6 to 9 issues. 3 new re-worded copies: **"Working clearance in front of MSB blocked"** (the problem a person dismissed, back as a new, undismissed issue), "Arc flash labels missing on all equipment in room", "Dead front missing on LP-1, live parts exposed". The Issues tab shows 8. Create assets offers "1 asset, 2 connections and **3 issues**", so release would create 3 duplicate issues on the site. On 12 Oct it did (LP-1 has 2 "Missing dead front" issues). Evidence: `QA-13Oct-ZP-4564-2-after-reread-8-issues-3-new-copies-FAIL.png`, `QA-13Oct-ZP-4564-3-dismissed-clearance-back-as-new-issue-FAIL.png`, `../zp4561/QA-13Oct-ZP-4561-3-after-reread-create-offers-3-duplicate-issues-FAIL.png`. I did not click Create today. |
| **NEW: issue class on re-read** | **FAIL (same re-read).** The 3 new copies have class **"Default"** with a "Pick a class" chip, not NEC/OSHA Violation as in every 12 Oct reading. The engine's warning: "Issue class 'Default' was used as a placeholder because no exact issue class list was given." The ticket says the capture menu carries the company's issue_classes. This re-read apparently ran without them. Visible in `QA-13Oct-ZP-4564-2-…` and `-3-…`. |
| Editor, PUT negatives, release on its node / site level, photo copy, release idempotent (before any re-read) | Not re-run today (12 Oct: PASS). |

**What changed since 12 Oct:** the High bug holds, reproduced on today's build. New: the re-proposed issues lose their class ("Default"). No new dev comment.

## Proposed Jira comment
```
h3. QA re-check 13 Oct (QA env, build index-C-Cc1qD1.js): FAIL (1 high bug, seen again today)

*Still passing:* a re-read with nothing new keeps the same issues and keeps the dismissed one dismissed. (12 Oct: classes, hazard, room-level labels issue, editor, Edited/Dismissed, PUT refusals, release on its asset / site level, photo copy, release again -> 0: all PASS.)
!QA-13Oct-ZP-4564-1-issues-before-reread-one-dismissed-PASS.png|width=720!

*Bug: re-reading an accepted walk brings back a dismissed issue and copies issues that are already on the site*
# Open the accepted, already-created walk https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29 . In the Issues tab, "Blocked clearance in front of MSB" is Dismissed and the other issues say "On the site".
# In "Main electrical room" click Add note: "There is also a sixth dry-type transformer T6, 30 kVA, 480 to 208Y/120, in this room, fed from MSB." Save.
# Wait for "Accepted · kept up to date". Open the Issues tab, then click "Create 1 asset" (do not confirm).
*Actual:* 3 new copies appear: "Working clearance in front of MSB blocked" (the dismissed problem, now not dismissed), "Arc flash labels missing on all equipment in room", "Dead front missing on LP-1, live parts exposed". The dialog says "1 asset, 2 connections and 3 issues will be created", so they would be created again on the site (on 12 Oct they were: LP-1 has 2 "Missing dead front" issues). The copies also have class "Default" / "Pick a class" instead of NEC or OSHA Violation.
*Expected:* "A person's edit or dismissal always wins over a re-read". Issues already on the site are matched, not copied. Only real new problems are proposed, with a real class.
!QA-13Oct-ZP-4564-2-after-reread-8-issues-3-new-copies-FAIL.png|width=720!
!QA-13Oct-ZP-4564-3-dismissed-clearance-back-as-new-issue-FAIL.png|width=720!

Recommended: To Do.
```

## Recommended status
**To Do.** The ticket's own test row ("Edit or dismiss a proposed issue, then trigger a re-read -> the edit/dismissal survives") fails. It was reproduced on 12 and 13 Oct, and Create assets would put duplicate safety issues on the customer's site.
