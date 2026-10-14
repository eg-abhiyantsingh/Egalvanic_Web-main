# ZP-4561: Site walks release to assets. POST /site-walk/<id>/release replaces Export to Quote

**Verdict:** FAIL (2 bugs). The core flow works for both walk types: Create assets, the counts, idempotent re-release, Open one-line, no pricing, role and tenant gates. But (1) created assets never get the walk's location/room, although the dialog promises "each asset carries where it was found"; and (2) after an accepted journal walk is re-read, Create assets duplicates assets already on the site (the same root cause as ZP-4535 bug 2).
**Build:** QA `index-BdXGlAq5.js` → `index-D1IWZ6SI.js` (redeployed 18:48 IST; SiteWalkRelease/useWalkRelease/SiteWalkDetail/SiteWalkJournal/siteWalkService source maps identical).
**Test data (mine, all on site `QA-DEMO ZP-4561 site (delete me)` 863c83ab-3aac-42a9-9cb1-29b86ad92659, customer `QA-DEMO ZP-4561 customer (delete me)`, both created through the walk's own "New customer" dialog):**
- Count walk `QA-DEMO ZP-4561 count (delete me)` https://acme.qa.egalvanic.ai/site-walks/c6d7a3ad-5df2-4ce3-8ebe-0c46699e968e
- Count walk by location `QA-DEMO ZP-4561 count by-location (delete me)` https://acme.qa.egalvanic.ai/site-walks/8b97625a-0451-4505-96a3-bfe9f9137710
- Journal walk `QA-DEMO ZP-4561/4564 journal (delete me)` https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29
- Released assets, for example LP-1: https://acme.qa.egalvanic.ai/assets/7091758a-3beb-409e-b96f-6a42d3ac615d

## Checked
| Check | How | Result |
|---|---|---|
| Count walk: no Export to Quote, no pricing | Count page header and right rail | PASS. No "Export to Quote", "Estimated service value" or "Show calculation". Header: "Complete Site Walk", ⋮ (Rename walk / Reopen walk). |
| Count walk: "Create N assets" appears after Complete | Counted Panelboard ×2 + Transformer ×1 › Complete Site Walk | PASS. The button reads "Create 3 assets"; before Complete there is no button (by design). |
| Count walk release: one node per counted slot | Create › dialog › Create | PASS. Confirm text "3 assets will be created…"; result "3 assets created." in 0.8 s; nodes "Panelboard 1", "Panelboard 2", "Transformer". |
| Re-release (count) → 0 created | `POST /release` again | PASS. `nodes_created 0, nodes_reused 3`. |
| Journal: "Create assets" appears only once nothing is proposed (after Accept) | Journal header before and after Accept | PASS. Before Accept: "Accept 4" only, and the release preview says `blocked: proposals_pending`. After Accept: "Create 19 assets". |
| Journal release: nodes, nesting, edges, photos, issues | Create › Create | PASS. "19 assets and 18 connections created, with 3 photos. 2 issues created on their assets." in 1.3 s. Breakers have `parent_id` = MSB. Edges: MSB main → MSB, MSB → each feeder, feeder → transformer, T1 → LP-1. Photos copied (LP-1 nameplate, MSB torque label). LP-1 nameplate values: 225 A, MLO, 208 V, 22 kA, manufacturer/model/serial in core attributes. |
| Dialog names unwired edges / skipped custom rows | Result dialog | Not triggered: `unwired []`, `skipped_custom []` (no custom rows; every edge had a free terminal). |
| Re-release (journal, before any re-read) → 0 created | `POST /release` again, and the Admin seat's call in the role sweep | PASS. `nodes_created 0, nodes_reused 19 (later 20), edges_existing 18–19, issues_created 0`. |
| **Location from the walk's room** | Asset page and node JSON of LP-1 (journal room "Second floor electrical closet") and the by-location count walk (room "Room A count test", Level 3) | **FAIL.** Every created asset has `location: null`, `room_id: null`, and the asset page shows "LOCATION: No Location". Seen on both walk types. |
| **Re-release after an accepted walk was re-read** | Added 1 note (new T5) to the accepted, released journal walk › "Create 3 assets" | **FAIL.** It created 3 nodes for 1 new unit: T5 plus a duplicate 2000 A main breaker and a 13th feeder (see ZP-4535 bug 2). The 3 issues it also created are duplicates (ZP-4564 bug). |
| "Open one-line" | After release, header button and dialog button | PASS. The header changes to "Open one-line"; it makes the walk's site active and opens `/sld`. |
| Negative: release refusals | API on my walks | PASS: empty walk → 409 `nothing_to_release` "This walk has no assets yet — there is nothing to create."; completed count walk with 0 counts → 409 `nothing_to_release`. `proposals_pending` and `walk_unassigned`: see the no-site journal row. |
| Journal walk with no site | See "No-site journal walk" below | See below. |
| Count walk previously exported to a quote | Read-only view of an exported count walk (not mine; nothing clicked) | See "Exported walk" below. I did not release someone else's walk, because that would write to their site. |
| Unit-id collision (#1423) | n/a | NOT COVERED. It needs a quote with colliding positional ids; Export to Quote no longer exists in the UI. |
| Migration swjrnl_a8 (`proposal_side`) | Indirect | The line-side/load-side edges above (main → board, board → feeder) show the sides are used. The column itself is not in any payload. |
| **Roles** (typed URL + API on my walks) | 9 seats | See the table below. PASS: only Super Admin, Admin, PM and AM can see walks; only they can accept or release; EE/FM/CP/Technician/Portal Sales get 422 `permission_denied`. |
| **Tenant / site scope** | PM and AM seats on walk A (site not assigned to them). Positive control: the Admin seat gets the same id as JSON; PM gets JSON for a walk on its own site. | PASS. PM/AM get no JSON for walk A (`GET`, accept, release, issue PUT); the UI says the walk is not available. Every seat's list holds only Acme walks (`company_id d59d449b…`). I could not get a walk id from another company on QA. |
| **LD flag `feature-site-walks`** | Live evalx: true for Acme. SIMULATED OFF (evalx rewritten) and SIMULATED LaunchDarkly unreachable | PASS. OFF: nav "Site Walks" greyed with a lock; `/site-walks` and `/site-walks/<id>` show "Feature Not Available". LD blocked: same (fails closed). Note: the API ignores the flag (`GET /site-walk/list` still 200 with 68 walks), the same pattern as other company flags. |
| **Performance** | Network timing | `GET /site-walk/list`: 64–68 walks, 71 KB, 0.33–0.88 s, **no pagination**. `GET /site-walk/<id>`: about 30 KB, 0.36–0.53 s. Release: 0.8 s (3 nodes), 1.3 s (19 nodes, 18 edges, 2 issues, 3 photos). Journal page: 1 walk GET + 1 photo GET + 1 S3-sign batch per entry of the visible location. No duplicate calls. |

### Roles (12 Oct, QA)
| Seat | /site-walks URL | My walk A (site not assigned to the seat) | list API | accept / release / issue PUT on my walks |
|---|---|---|---|---|
| Super Admin (staff) | list + New Site Walk | review + all actions | 200 | 200 (did the whole test) |
| Admin (staff, +admin@) | list + New Site Walk | review + actions | 200 (65) | accept 200 `{accepted:0}`; release 200 (0 created / 20 reused); PUT 200 |
| Project Manager | list + New Site Walk | "not available" (not their site) | 200 (6, all on their sites) | no JSON (refused; CDN serves HTML for 403/404). Control: GET a walk on PM's own site → 200 JSON |
| Account Manager | list + New Site Walk | "not available" | 200 (4) | no JSON (refused) |
| Electrical Engineer | Access Denied | Access Denied | 422 `site_walks.view` | 422 `site_walks.manage` |
| Facility Manager | Access Denied | Access Denied | 422 | 422 |
| Client Portal | Access Denied | Access Denied | 422 | 422 |
| Technician | Web Access Restricted | – | 422 | 422 |
| Portal Sales | Web Access Restricted | – | 422 | 422 |

## Bugs found
1. **Create assets drops the walk's location: every created asset shows "No Location"** (Medium; backend #1414, suggested Avani; owner Eric).
   Steps: 1) Sales › Site Walks › New Site Walk › Count, pick a site. 2) Count settings › By location › Add location "Room A count test" (floor Level 3). 3) Count 1 Panelboard › Complete Site Walk › Create 1 asset › Create. 4) Open the new asset.
   Actual: Location "No Location"; node `location null`, `room_id null`. The same happens on a journal walk (LP-1 captured in "Second floor electrical closet").
   Expected (ticket + dialog text "Each asset carries where it was found"): the asset's location is the walk's room (plus the typed position).
   Evidence: `QA-12Oct-ZP-4561-18-count-by-location-asset-no-location-FAIL.png`, `QA-12Oct-ZP-4561-14-released-lp1-photo-source-ok-but-no-location-FAIL.png`, `QA-12Oct-ZP-4561-17-count-walk-by-location-room-a-PASS.png` (the room on the walk). No existing ticket covers it.
2. **After an accepted journal walk is re-read, Create assets creates duplicate assets and duplicate issues** (High). Same root cause as ZP-4535 bug 2 (unit ids not kept across re-reads) and ZP-4564's bug (issues re-proposed). File it once and link it from all three tickets. Evidence: `QA-12Oct-ZP-4561-15-after-reread-create-3-assets-confirm-FAIL.png`, `QA-12Oct-ZP-4561-16-after-reread-release-result-FAIL.png`.

## Enhancement suggestions
- Paginate `GET /site-walk/list` (it returns every walk, 71 KB for 68 walks) and add Type (Count/Journal), Status and Created-by filters to the Site Walks grid.
- The confirm dialog could list what will be created (names/classes, collapsible), not only counts. A reviewer creating 19 assets cannot see which ones.
- After Create assets, add "View these assets" (Assets filtered to this walk) and a "Created from site walk <name>" line on each asset (provenance).
- Grey out (with a reason) instead of hiding the header action on a completed count walk with 0 counts. Today nothing shows, which looks like a broken page.

## Mobile part for Nency
iOS/Android start most journal walks and assign sites on the phone. Please check: (1) a walk released on the web shows its assets on the phone's site/SLD; (2) the phone no longer offers Export to Quote; (3) whether the phone can assign a site to a journal walk started without one (the web journal page has no site picker; see the no-site check).

## Not covered and why
- Unit-id collision across quote lines (#1423): it needs a quote built from a count walk, and Export to Quote is gone from the UI.
- Releasing a count walk that was exported earlier: only other people's exported walks exist, and releasing them would write to their sites. Read-only view below.
- A true cross-company walk id: QA lists no other company's walks to any seat.

## Re-check 13 Oct on index-C-Cc1qD1.js

Re-tested 15:50–16:10 IST on QA build `index-C-Cc1qD1.js`, Super Admin seat. Live Jira ticket: no comments, still In QA. LD `feature-site-walks` = true (live evalx). Re-used my QA-DEMO walks; no new walk created. The roles, tenant and flag rows from 12 Oct were not re-run (same frontend chunks and API; nothing in them was a bug).

| Claim | 13 Oct result |
|---|---|
| **Bug 1: created assets get no location** | **STILL FAILS, reproduced with a fresh release today.** I reopened my count walk `QA-DEMO ZP-4561 count by-location (delete me)` (By location, room "Room A count test", Level 3), counted one more Panelboard, then Complete › "Create 1 asset" › Create. Result: "1 asset created. 1 asset already existed." The new asset "Panelboard 2" (https://acme.qa.egalvanic.ai/assets/4b565b40-4717-4e71-8b73-6ddf6fdce898) shows **LOCATION: No Location**; node `location null`, `room_id null`. The confirm dialog still promises "Each asset carries where it was found". The ticket says release writes "the walk's room plus the typed position as location". Evidence: `QA-13Oct-ZP-4561-1-count-walk-room-a-level-3-PASS.png` (the room is on the walk), `QA-13Oct-ZP-4561-2-new-asset-from-room-a-shows-no-location-FAIL.png`. LP-1 from the journal walk is still "No Location" too. |
| Count walk re-release is idempotent; only new counts are created | STILL PASS ("1 asset created. 1 asset already existed."). |
| No Export to Quote / no pricing; "Open one-line" after release | STILL PASS (header "Complete · Open one-line"). |
| **Bug 2: Create assets after a re-read of an accepted journal walk** | **CHANGED.** Same kind of input as 12 Oct (one note adding transformer T6). Assets: **not duplicated today**; the confirm dialog offers "1 asset" (T6 only). Issues: **still duplicated**. The same dialog says "1 asset, 2 connections and **3 issues** will be created". The 3 issues are re-worded copies of issues already on the site, one of them the issue a person dismissed (ZP-4564). I did NOT click Create today, so no new duplicates were made. `QA-13Oct-ZP-4561-3-after-reread-create-offers-3-duplicate-issues-FAIL.png`. Also, T6 would be wired straight from the MSB board, not from a breaker (ZP-4511/ZP-4535). |
| Release refusals / roles / tenant / flag OFF (simulated) | Not re-run today (12 Oct: PASS). |

**What changed since 12 Oct:** bug 1 holds, re-proved on today's build with a new release. Bug 2's asset duplication did not repeat today; the issue duplication did. Bug 2 now belongs mainly to ZP-4564 (issues) and ZP-4535 (unit ids), so here it is a linked note, not a separate bug.

**Out of scope, already noted on 12 Oct:** a journal walk started without a site cannot be given a site on the web (Create stays disabled with "Assign this walk to a site first", and there is no site picker on the journal page). See `QA-12Oct-ZP-4561-22-journal-no-site-create-disabled-no-way-to-assign-FAIL.png`; it is still the same today on walk edeb12e3.

## Proposed Jira comment
```
h3. QA re-check 13 Oct (QA env, build index-C-Cc1qD1.js): FAIL (1 bug, plus duplicate issues from ZP-4564)

*Still passing:* count walk has no Export to Quote and no pricing; Complete -> "Create N assets"; running it again only adds what is new ("1 asset created. 1 asset already existed."); "Open one-line" after release. (Roles, tenant and flag OFF passed on 12 Oct; nothing changed.)

*Bug: created assets have no location*
# Sales > Site Walks > New Site Walk > Count, pick a site.
# Count settings > By location > Add location "Room A count test", floor "Level 3".
# Count 1 Panelboard > Complete Site Walk > Create 1 asset > Create.
# Open the new asset.
*Actual:* LOCATION shows "No Location" (the asset has no room and no location). The same happens for assets made from a journal walk's rooms.
*Expected:* the asset's location is the walk's room (ticket: "the walk's room plus the typed position as location"; the dialog says "Each asset carries where it was found").
!QA-13Oct-ZP-4561-1-count-walk-room-a-level-3-PASS.png|width=720!
!QA-13Oct-ZP-4561-2-new-asset-from-room-a-shows-no-location-FAIL.png|width=720!

*Also (see ZP-4564):* after an accepted journal walk is re-read (I added one new transformer), Create assets offers "1 asset, 2 connections and 3 issues". The asset count is right today, but the 3 issues are copies of issues already on the site (one of them was dismissed). I did not click Create.
!QA-13Oct-ZP-4561-3-after-reread-create-offers-3-duplicate-issues-FAIL.png|width=720!

Test data: count walk https://acme.qa.egalvanic.ai/site-walks/8b97625a-0451-4505-96a3-bfe9f9137710 , journal walk https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29
Recommended: To Do.
```

## Recommended status
**To Do.** The location bug is inside the ticket's own release contract ("the walk's room plus the typed position as location"). It was reproduced on both days, on both walk types, with a fresh release today.
