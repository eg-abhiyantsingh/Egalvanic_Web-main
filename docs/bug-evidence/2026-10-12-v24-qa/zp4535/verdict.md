# ZP-4535: Journal capture menu. Per-class fields validated in the engine, review chips read the menu keys, accepted walks keep being read (#1413)

**Verdict:** FAIL (2 bugs, both reproduced, both with a control).
**Build:** QA `index-BdXGlAq5.js` → `index-D1IWZ6SI.js` (redeployed 18:48 IST; journal source maps identical). Interpretation `runner: "sfn"`.
**Test data (mine):** walk A `QA-DEMO ZP-4561/4564 journal (delete me)` https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29, control walk B `QA-DEMO ZP-4508 control notes-only journal (delete me)` https://acme.qa.egalvanic.ai/site-walks/88d2bb50-f04d-4e79-b696-d277119939ef, site `QA-DEMO ZP-4561 site (delete me)` (863c83ab-3aac-42a9-9cb1-29b86ad92659). Released asset LP-1: https://acme.qa.egalvanic.ai/assets/7091758a-3beb-409e-b96f-6a42d3ac615d

## Checked
| Check | How | Result |
|---|---|---|
| Panelboard gets only fields its class has; options match the menu | LP-1 proposal on walk A and walk B | PASS for the menu-backed fields: `node.mains_type MLO`, `node.ampere_rating 225`, `node.nameplate_voltage 208`, `node.phase_configuration 3P4W`, `node.aic_rating 22`, `attr.manufacturer Square D`, `attr.model NQ`, `attr.serialNumber`. Switchboard: `node.mains_type MCB`. Transformers: `node.kva_rating 75.0`, `node.secondary_voltage`, `attr.primaryVoltage`. |
| Negative: a value that does not fit is dropped or repaired, not captured | Spoke and photographed catalog number **NQ430L2C**; the Acme Panelboard `catalogNumber` attribute is numeric (the engine's own warning says so) | **FAIL.** The value is neither dropped nor kept as read. It is coerced to **`attr.catalogNumber: "430.0"`** on both walks. Create assets wrote `Catalog Number = 430.0` into LP-1's `core_attributes` on the real asset. The same job's warnings contradict each other: "…recorded it in notes instead" and "It was recorded as read". |
| Review chips use readable labels (prefix stripped, camelCase split) | Graph › click a unit | PASS. "catalog number", "serial number", "aic rating", "ampere rating", "mains type", "nameplate voltage", "phase configuration", "kva rating", "primary voltage". |
| Accepted hint | Accept 4 → add a note → wait | PASS. "Accepted · kept up to date" → "Accepted · reading changes" while the re-read runs → back to "kept up to date". "no longer read" is never shown. |
| #1413: an accepted walk is re-read automatically | Added a note to accepted walk A | PASS. Job `4f549f74` started by itself about 3 s after the save and succeeded in about 108 s; the new T5 landed accepted (0 proposed). |
| #1413: each unit keeps one id across re-reads; releasing again adopts, never duplicates | Released 19 units, added one note (a new transformer T5), let it re-read, then compared unit ids and released again | **FAIL.** 17 of 19 unit ids survived. **"MSB main" (2000 A) came back as a new unit "Circuit Breaker"**, and feeder "Circuit Breaker (5 of 12)" came back as a new unit "T5". The header offered **"Create 3 assets"** when only 1 was new. Create made 3 nodes: T5 (correct), a **second 2000 A main breaker inside MSB**, and a **13th feeder** (the old "(5 of 12)" node is still there). Release response: `nodes_created 3, nodes_reused 17`. |
| Migration swjrnl_a7 (`proposal_uid` column) | n/a | NOT WEB-TESTABLE (the column is not in any API payload). The id instability above is the visible symptom. |
| FOLD_VERSION / menu hash re-fold | n/a | NOT WEB-TESTABLE. |
| Tenant / roles / flags / perf | Shared walk endpoints: see the ZP-4561 role table. `feature-site-walks` = true (live). Re-read ≈ 108 s on sfn. | See ZP-4561. |

## Bugs found
1. **Journal interpreter turns an alphanumeric catalog number into a wrong number ("NQ430L2C" → 430.0) and Create assets saves it on the asset** (Medium; backend + pipeline, suggested Avani; owner Eric).
   Steps: 1) Sales › Site Walks › New Site Walk › Journal, on any site. 2) Add a location and a note: "Panelboard LP-1, Square D NQ, catalog number NQ430L2C, main lugs only, 225 amp…". 3) Wait for the reading (about 1–2 min). 4) Graph › click LP-1. 5) Accept › Create assets › open the LP-1 asset.
   Actual: the chip reads "catalog number: 430.0"; the asset's core attribute Catalog Number = 430.0.
   Expected: the ticket says a value outside the field's menu or type is removed and repaired, never invented. Either keep "NQ430L2C" (catalog numbers are alphanumeric, so the Acme Panelboard attribute type looks wrong too) or drop it with a warning. Never store a number made from part of the text.
   Reproduced on 2 walks (A with a photo, B notes only). Evidence: `QA-12Oct-ZP-4535-1-lp1-card-catalog-number-430-FAIL.png`, asset API `core_attributes catalogNumber "430.0"`. No existing ticket covers it.
2. **Re-reading an accepted journal walk gives some units new ids, so Create assets duplicates assets already on the site** (High; backend #1413, suggested Avani; owner Eric).
   Steps: 1) Journal walk with "MSB … 2000 amp three-pole main breaker … three sections with four 400 amp breakers each … feeds four transformers T1–T4". 2) Accept › Create assets (19 assets). 3) Add a note "There is also a fifth dry-type transformer T5 … fed from MSB." 4) Wait for "Accepted · kept up to date". 5) The header says "Create 3 assets"; click it.
   Actual: 3 assets created, including a duplicate 2000 A main breaker in MSB ("Circuit Breaker", next to the existing "MSB main") and an extra feeder "T5" (the "Circuit Breaker (5 of 12)" node stays).
   Expected: units keep their id across re-reads (proposal_uid). Only T5 is new, so the button reads "Create 1 asset" and the old breaker is re-labelled or adopted, not created again.
   Evidence: `QA-12Oct-ZP-4535-4-after-reread-create-3-assets-expected-1-FAIL.png`, `../zp4561/QA-12Oct-ZP-4561-15-after-reread-create-3-assets-confirm-FAIL.png`, `../zp4561/QA-12Oct-ZP-4561-16-after-reread-release-result-FAIL.png`, site node list (25 nodes; two 2000 A breakers inside MSB). No existing ticket covers it.

## Enhancement suggestions
- When a value is dropped by the menu check, show it on the unit card as "not saved: <field> <value> (reason)". Today it is only in the job warnings, which no screen shows.
- Show the job warnings in Journal Review (a collapsible "Notes from the reader" panel). They hold useful review items ("pole count assumed", "labels assigned arbitrarily") that the reviewer never sees.

## Mobile part for Nency
The phone's journal summary shows the same attribute chips. Check an interpreted LP-1 with a catalog number on iOS/Android: the value shown must match what was said, never "430.0". The phone also adds entries to accepted walks, so check that a phone entry after Accept triggers the re-read (web header shows "Accepted · reading changes").

## Not covered and why
`proposal_uid` and the migration head (DB only). The per-class prompt contents and the ~3K-token menu (pipeline only). Developer evidence: the `menu.json` staged for job `157a5117` and the validator log for `catalogNumber`.

## Re-check 13 Oct on index-C-Cc1qD1.js

Re-tested 15:50–16:10 IST on QA build `index-C-Cc1qD1.js`, Super Admin seat. Live Jira ticket: no comments, still In QA. LD `feature-site-walks` = true (live evalx). To keep AI cost low I re-used my QA-DEMO walks and ran 4 readings in total (shared with ZP-4511/4561/4564):

| Job (IST) | Walk | Input | Result |
|---|---|---|---|
| 99be3e56 (15:56–15:57) | A | note "nothing new in this room" | **failed**: "the run produced no assembly.json" |
| 9625c519 (16:01–16:02) | A | same note, edited (retry) | succeeded |
| b8a40a5d (16:01–16:02) | no-site walk (LP-9) | note "LP-9 catalog number NQ130L1C, 18 circuits." | succeeded |
| d811187c (16:04–16:05) | A | note "There is also a sixth dry-type transformer T6 … fed from MSB." (same kind of input as 12 Oct's T5) | succeeded |

| Claim | 13 Oct result |
|---|---|
| **Bug 1: alphanumeric catalog number becomes a number** | **STILL FAILS, reproduced on fresh input.** LP-9 "NQ130L1C" → chip **"catalog number: 130.0"**. The same job's warning says "I recorded it as stated; a person should check that it saves." `QA-13Oct-ZP-4535-1-lp9-catalog-NQ130L1C-saved-as-130-FAIL.png`. It is inconsistent: on walk A the same day's re-read kept LP-1's "NQ430L2C" in notes, which is right (`QA-13Oct-ZP-4535-5-control-lp1-catalog-kept-in-notes-PASS.png`). The asset released on 12 Oct still shows **Catalog Number 430.0** (Engineering tab, `core_attributes catalogNumber "430.0"`), and re-release never corrects it: `QA-13Oct-ZP-4535-2-lp1-asset-still-catalog-number-430-FAIL.png` (https://acme.qa.egalvanic.ai/assets/7091758a-3beb-409e-b96f-6a42d3ac615d). |
| Readable chips | STILL PASS ("catalog number", "ampere rating", "mains type", "phase configuration", prefixes stripped). |
| Hint "kept up to date" / "reading changes"; accepted walk re-read by itself | STILL PASS. A new note started a run about 3 s later ("Read 0 of 6 · 6 to go · Interpreting"), then "Accepted · kept up to date". |
| Re-read with nothing new keeps ids | PASS (new positive control). Job 9625c519: 20 of 20 unit ids kept, nothing pending, header back to "Open one-line". `QA-13Oct-ZP-4535-7-nothing-new-reread-kept-up-to-date-PASS.png` |
| **Bug 2: a re-read of an accepted walk does not keep its units** | **STILL FAILS, but the symptom changed.** With the T6 note (job d811187c): the duplicate-asset part did **not** repeat. The header offers "Create 1 asset" (only T6), which is right: `QA-13Oct-ZP-4535-6-after-T6-note-create-1-asset-count-right-PASS.png`. But the re-read **dropped 8 accepted units**: the walk went from 20 units to 13 (T6 included). 7 unnamed "Circuit Breaker (n of 12)" feeders and the "T5" breaker are gone. The engine's warning says "Only 4 of the 12 MSB 400 A feeder breakers were added". The walk now shows 5 breakers in MSB, while the site has 15 breaker nodes. `QA-13Oct-ZP-4535-3-reread-dropped-8-accepted-breakers-FAIL.png` (before: `../zp4511/QA-13Oct-ZP-4511-2-graph-msb-box-breakers-inside-feeds-PASS.png`, 13 breakers). The ticket says each run "reuses the interpreter's rows in place" and that accepted units keep one id. On 12 Oct this produced 2 re-labelled units and duplicate assets; on 13 Oct it dropped 8 accepted units. |
| Failed run shows "reading changes" for ever | Seen again: after job 99be3e56 failed, the header kept saying "Accepted · reading changes", with no error and no retry, until I edited the note. `QA-13Oct-ZP-4535-4-failed-reread-header-still-says-reading-changes.png`. **Already reported on ZP-4508** (12 Oct, "Queued forever"). Not counted again here. |

**What changed since 12 Oct:** bug 1 is confirmed again on a new walk with a new value. Bug 2 is still a bug, but today it showed up as dropped units, not duplicate assets. The title should say "re-read of an accepted walk changes its accepted units (re-labels or drops them)". No new dev comment.

**Outside this ticket's scope (seen in the same re-read):** the 3 new walk issues got the placeholder class "Default" ("Pick a class"). The engine's warning: "Issue class 'Default' was used as a placeholder because no exact issue class list was given". The re-read seems to have run without the company's issue-class list. Reported on ZP-4564.

## Proposed Jira comment
```
h3. QA re-check 13 Oct (QA env, build index-C-Cc1qD1.js): FAIL (2 bugs, both seen again today)

*Still passing:* readable chips ("catalog number", "ampere rating", prefixes stripped); an accepted walk is re-read on its own about 3 s after a new note ("Accepted · reading changes" -> "Accepted · kept up to date"); a re-read with nothing new keeps all 20 units and offers nothing to create.

*Bug 1: a catalog number with letters is saved as a wrong number*
# Sales > Site Walks > New Site Walk > Journal.
# Add note: "Panelboard LP-9, Square D, 100 amp main lugs, 208Y/120 volt three phase." Then add note: "LP-9 catalog number NQ130L1C, 18 circuits."
# Wait about 1 minute. Open the Graph view and click LP-9.
*Actual:* the chip reads "catalog number: 130.0". On 12 Oct "NQ430L2C" became 430.0 and Create assets saved Catalog Number = 430.0 on the real asset (still there today).
*Expected:* keep "NQ130L1C" as said, or drop it with a note. Never save a number made from part of the text.
!QA-13Oct-ZP-4535-1-lp9-catalog-NQ130L1C-saved-as-130-FAIL.png|width=720!
!QA-13Oct-ZP-4535-2-lp1-asset-still-catalog-number-430-FAIL.png|width=720!

*Bug 2: re-reading an accepted walk does not keep its accepted units*
# Open the accepted, already-created walk https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29 (MSB box has 13 breakers).
# In "Main electrical room" click Add note: "There is also a sixth dry-type transformer T6, 30 kVA, 480 to 208Y/120, in this room, fed from MSB." Save.
# Wait for "Accepted · kept up to date" and open the Graph view.
*Actual:* the walk dropped 8 accepted units (20 -> 13, T6 included). The MSB box now shows 5 breakers, while the site still has all of them. The reader's note: "Only 4 of the 12 MSB 400 A feeder breakers were added". On 12 Oct the same kind of note changed the id of 2 units instead, and Create assets made a duplicate main breaker and an extra feeder.
*Expected:* accepted units keep their id across re-reads; only T6 is added.
!QA-13Oct-ZP-4535-3-reread-dropped-8-accepted-breakers-FAIL.png|width=720!

Recommended: To Do.
```

## Recommended status
**To Do.** Bug 1 fails the ticket's own negative test ("a value outside its menu is dropped/repaired, not captured") and was reproduced again today on fresh input. Bug 2 fails the #1413 test ("each unit keeps one id across re-reads"), seen on both days with two different symptoms.
