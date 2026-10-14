# ZP-4511: Journal review. Accepting a walk ends interpretation, enclosures render as boxes, liberty rule claims unnamed feeders

**Verdict:** PASS with 1 low copy bug (the empty state still says "Run Interpret", but that button was removed). Part (1), "accepting ends the reading", was replaced on purpose by ZP-4535/#1413 ("accepted walks keep being read"), so I tested the current design: no Re-interpret button, Accept → "Accepted · kept up to date".
**Build:** QA `index-BdXGlAq5.js` → `index-D1IWZ6SI.js` (redeployed 18:48 IST; the source maps for the journal page, graph and services are identical).
**Test data (mine):** `QA-DEMO ZP-4561/4564 journal (delete me)` https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29 and the control `QA-DEMO ZP-4508 control notes-only journal (delete me)` https://acme.qa.egalvanic.ai/site-walks/88d2bb50-f04d-4e79-b696-d277119939ef. Both are on `QA-DEMO ZP-4561 site (delete me)`.
Notes spoken into the walk: "MSB … 2000 A three-pole main … three sections with four 400 amp breakers each. MSB feeds four dry-type transformers T1–T4 …", and in a second room "LP-1 … fed from transformer T1 in the main electrical room downstairs".

## Checked
| Check | How | Result |
|---|---|---|
| No Re-interpret button; `interpretSiteWalk` removed | Journal Review header in every state (empty, live, read, accepted, released); bundle search | PASS. The header only shows the hint, "Accept N", "Create N assets" or "Open one-line". |
| `POST /site-walk/<id>/interpret` removed | Same call on my walk | PASS. Answers the SPA index.html (no Flask route). Control: `GET /site-walk/<id>/release` answers Flask's own "405 Method Not Allowed", so a real route on that path does answer. |
| Accept → controls state | Clicked "Accept 4" | PASS (current design). The hint reads **"Accepted · kept up to date"**. "Create 19 assets" appears. Accept 200 `{accepted: 4}`. |
| Re-open the accepted walk: tree not duplicated | Reload plus `GET /site-walk/<id>` after Accept | PASS. 19 units, 0 proposed, no second copy. (After a later re-read, 2 units change id. That is a ZP-4535/ZP-4561 bug, filed there.) |
| Enclosure draws as a rounded box with breakers in a row inside; no board → own-breaker arrows | Graph view | PASS. MSB is a box with 13 breakers in a row. The 4 feeds drop from breakers T1–T4 to the transformers. The walk's `connections` hold only the 5 real feeds (`is_structural false`); there are no containment edges. |
| Liberty rule: a board with only unnamed breakers ("three sections, four 400A breakers each") gives its feeds to individual breakers, labelled by the load | Proposals on both walks | PASS. The 12 feeders are created; 4 are labelled T1–T4 and carry the feeds (`T1(breaker) → T1(transformer)` …). The other 8 stay "Circuit Breaker (n of 12)". One warning per board: "Four of them were labelled T1-T4 arbitrarily as the feeders for the transformers." |
| Negative: a feeder already named for something else is not reused | MSB main (2000 A) | PASS. "MSB main" was never given a transformer feed. |
| Three-phase main = 3-pole line-side node with the rating | MSB main unit | PASS. `pole_count 3`, `ampere_rating 2000`, inside MSB. |
| `/interpret/status` carries `accepted` (backend #1403) | `GET /site-walk/<id>/interpret/status` after Accept | MINOR GAP. The status payload has no `accepted` key (the keys are completed_at, content_digest, created_at, entry_count, error, id, is_live, needs_interpretation, photo_count, progress, proposed_*, runner, site_walk_id, status, warnings). The walk document (`GET /site-walk/<id>` → `interpretation.accepted`) has it, and the page re-reads the walk after every run, so the screen stays right. Logged as a contract note, not a bug. |
| Empty-state copy | New walk and a location not read yet | **FAIL (low)**. "Nothing has been read out of <location> yet. **Run Interpret** to turn its entries into assets…". There is no Interpret button any more. |
| FOLD_VERSION bump | n/a | NOT WEB-TESTABLE (only a one-time re-fold). |
| Roles / tenant / flag / perf | See the ZP-4561 verdict (shared walk endpoints). Flag `feature-site-walks` = true for Acme (live evalx). | See ZP-4561. |

## Bugs found
1. **Journal Review empty state still tells users to "Run Interpret", but the button no longer exists** (Low, frontend, suggested Krunal; owner Eric).
   Steps: 1) Sales › Site Walks › New Site Walk › Journal. 2) Open it, or open any location not read yet. 3) Read the right pane.
   Actual: "Run Interpret to turn its entries into assets, quantities and condition." Expected: copy that matches the automatic reading, for example "Entries are read automatically a few seconds after you add them."
   Evidence: `QA-12Oct-ZP-4511-6-empty-state-still-says-run-interpret-FAIL.png`, `QA-12Oct-ZP-4511-1-journal-review-empty-no-reinterpret-button-PASS.png`. Code: `SiteWalkJournal.jsx` InterpretedModel empty state. No existing Jira ticket covers it (searched "site walk / journal" tickets from the last 45 days).

## Enhancement suggestions
- Label claimed feeders "MSB › T1 feeder" instead of a bare "T1". Today the breaker and the transformer are both called "T1". The FEEDS list then reads "T1 → T1", and the engine's own warning gets confused ("'T1' was described as fed from more than one place; kept T1, dropped T2, T3, T4 …") even though the final feeds are right.
- The graph opens fit-to-pane with tiny unlabeled dots on a 19-unit walk. Open at label zoom, or show labels on hover.
- Merge the repeated rationale text on a grouped row ("Dry-type transformer fed from MSB.; Dry-type transformer fed from MSB.; …").

## Mobile part for Nency
The phone's journal review/summary should no longer offer a "Re-interpret" action, and it should show the same accepted state ("kept up to date"). Please check an accepted walk on iOS/Android for any leftover Re-interpret or "no longer read" copy.

## Not covered and why
The FOLD_VERSION one-time re-fold (backend cache, nothing to see). How the arbitrary breaker is chosen among unnamed feeders (by design, "deliberately arbitrary").

## Re-check 13 Oct on index-C-Cc1qD1.js

Re-tested 15:50–16:10 IST on QA build `index-C-Cc1qD1.js` (newer than `index-COeCBn0z.js`), Super Admin seat, same QA-DEMO walk A (https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29) and my no-site walk `QA-DEMO ZP-4561 no-site journal (delete me)` (https://acme.qa.egalvanic.ai/site-walks/edeb12e3-c4c1-4ad7-9c24-85f9059e502f). Live Jira ticket: no comments, still In QA. LD `feature-site-walks` = true for Acme (live evalx). AI cost today: 4 readings for all four journal tickets together (1 failed, see ZP-4535).

| Check | 13 Oct result |
|---|---|
| No Re-interpret button; header after Accept | STILL PASS. Header: "Accepted · kept up to date" + "Open one-line"; no button with "interpret" on the page. `QA-13Oct-ZP-4511-1-accepted-header-kept-up-to-date-no-reinterpret-PASS.png` |
| `POST /site-walk/<id>/interpret` removed | STILL PASS. Answers the SPA index.html (200 text/html); control `GET /site-walk/<id>/release` answers Flask "405 Method Not Allowed". |
| MSB draws as a box with its breakers in a row; feeds drop from breakers | STILL PASS before today's re-read: MSB box with 13 breakers, 5 feeds from breakers to T1–T5, LP-1 under T1. `QA-13Oct-ZP-4511-2-graph-msb-box-breakers-inside-feeds-PASS.png` |
| Empty-state copy (Low bug) | STILL FAILS. "Nothing has been read out of No location yet. Run Interpret to turn its entries into assets…" The Run Interpret button does not exist; the copy is still in the live chunk `SiteWalkDetail-Urf5ZaiJ.js`. `QA-13Oct-ZP-4511-3-empty-state-still-says-run-interpret-FAIL.png` |
| **NEW: liberty rule on a re-read of the accepted walk** | **FAIL.** I added one note ("There is also a sixth dry-type transformer T6 … fed from MSB.") to walk A. The engine re-read it (job d811187c, 16:04–16:05 IST). It kept only 4 of MSB's 12 feeder breakers (its own warning: "Only 4 of the 12 MSB 400 A feeder breakers were added"). T5 and T6 are now fed straight from the MSB board (`MSB → T5`, `MSB → T6`), not from a breaker. That is the exact problem (3) this ticket fixed ("feeds … stayed on the board"). On 12 Oct the first readings applied the rule correctly. `QA-13Oct-ZP-4511-5-reread-T5-T6-fed-from-board-not-a-breaker-FAIL.png` (before: `-2-`). The dropped breakers are the ZP-4535 re-read bug; filed there. |

**What changed since 12 Oct:** the copy bug still holds. New: the liberty rule is not kept when an accepted walk is re-read. Because of that, I no longer recommend READY TO RELEASE.

**Outside this ticket's scope (new, Low/Medium, frontend):** on a journal walk whose notes sit under "No location", the List view says "Nothing interpreted yet. Nothing has been read out of No location yet." But LP-9 was read: the Graph view shows LP-9 with its chips, and the header offers "Create 1 asset". Steps: 1) Sales › Site Walks › New Site Walk › Journal, no location. 2) Add note "Panelboard LP-9, Square D, 100 amp main lugs, 208Y/120 volt three phase." 3) Wait for the reading. 4) Click the List icon in Interpreted Model. Actual: "Nothing interpreted yet". Expected: the LP-9 row. Evidence: `QA-13Oct-ZP-4511-4-no-location-list-says-nothing-read-but-LP-9-read-FAIL.png`, positive control `../zp4535/QA-13Oct-ZP-4535-1-lp9-catalog-NQ130L1C-saved-as-130-FAIL.png` (Graph view, same walk, shows LP-9). No Jira ticket found for it (JQL: project ZP, created ≥ 11 Oct, text "site walk"/"journal"/"interpret" → only ZP-4836, unrelated).

## Proposed Jira comment
```
h3. QA re-check 13 Oct (QA env, build index-C-Cc1qD1.js): mostly PASS, 1 problem on re-read + 1 low copy bug

Test walk: https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29 (QA-DEMO, MSB with 3 sections of 4x400A unnamed breakers feeding T1-T4).
||Check||Result||
|No Re-interpret button; POST /site-walk/<id>/interpret gone|PASS|
|Accept -> "Accepted · kept up to date", then "Open one-line" after Create assets|PASS|
|MSB draws as a box with its breakers in a row; feeds drop from breakers to T1-T5|PASS|
|Liberty rule on the first reading (feeds claim unnamed feeders, labelled by load)|PASS (12 Oct)|
|Liberty rule when an accepted walk is re-read|FAIL (see 1)|
|Empty state text|FAIL, low (see 2)|

!QA-13Oct-ZP-4511-1-accepted-header-kept-up-to-date-no-reinterpret-PASS.png|width=720!
!QA-13Oct-ZP-4511-2-graph-msb-box-breakers-inside-feeds-PASS.png|width=720!

*1. After a re-read, new transformers hang from the board, not from a breaker*
# Open the accepted walk above (MSB box shows 13 breakers, feeds come from breakers).
# In "Main electrical room" click Add note: "There is also a sixth dry-type transformer T6, 30 kVA, 480 to 208Y/120, in this room, fed from MSB." Save.
# Wait about 1 minute for "Accepted · kept up to date", then open the Graph view.
*Actual:* the MSB box now has only 5 breakers. T5 and T6 are fed straight from the MSB board. The reader's own note says "Only 4 of the 12 MSB 400 A feeder breakers were added".
*Expected:* the 12 breakers stay; T6 (and T5) are fed from a free breaker labelled by the load, as on the first reading.
!QA-13Oct-ZP-4511-5-reread-T5-T6-fed-from-board-not-a-breaker-FAIL.png|width=720!
(The dropped breakers are reported on ZP-4535.)

*2. Low: the empty state still says "Run Interpret"*
# Open a journal walk, or a location, that has not been read yet.
*Actual:* "Run Interpret to turn its entries into assets, quantities and condition." That button no longer exists.
*Expected:* text that matches the automatic reading, for example "Entries are read automatically a few seconds after you add them."
!QA-13Oct-ZP-4511-3-empty-state-still-says-run-interpret-FAIL.png|width=720!

Recommended: stay In QA until problem 1 is looked at (it may be fixed together with ZP-4535).
```

## Recommended status
**stay In QA.** The core of the ticket passes (no Re-interpret, enclosure box, liberty rule on the first reading). But the liberty rule broke on today's re-read of an accepted walk (problem 3 of this ticket, seen once, on the current build). That is not "100% sure" enough for To Do, and it is not clean enough for READY TO RELEASE. File the "Run Interpret" copy bug (Low) and the "No location list says nothing read" bug (out of scope) as their own tickets.
