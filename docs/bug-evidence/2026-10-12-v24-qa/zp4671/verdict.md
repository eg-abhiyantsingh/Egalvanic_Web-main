# ZP-4671: [Web] QR Labels: opening the page downloads every asset of the site before it can be used

**Verdict:** PASS. Build `index-C14cwR0U.js` (QA, checked 17:33 and 19:12 IST, 13 Oct 2026).
Fix: backend PR #1561 + frontend PR #1755 ("Load QR Labels assets on demand"), both merged to QA 13 Oct (Dharmesh's comments 45121/45122).
The QR chunk in the live bundle (`index-xNHeNBu3.js`) calls `filter-options`, `by-ids`, `qr_labeled`/`has_qr_code` and `qr-labels/codes/check`.

Test data: read only. Sites: "Performance sld" `6271fdc2…` (8,481 assets), "test site" `21bd10d4…` (1,983), "Test site A" `b99f9c84…` (94).
No codes were saved. The Edit codes drawer was opened and cancelled.

## Checked
| Check | How | Result |
|---|---|---|
| Open the page on an 8,481-asset site | Super Admin, Performance sld, full page load | First rows at 4.6 s, including the app start. QR calls: `filter-options` 13 KB, `location/sld` 7 KB, one `page=1&page_size=50` (7 KB), one `page_size=1`, `node-classes` 72 KB. **No 200-row paging.** The old loader fetched every 200-row page, which is 43 pages on this site (the same 43 calls the "All" option still makes on request, 1.16 MB). PASS (shot -1) |
| Same on 1,983 and 94 assets | test site / Test site A | 4.4 s and 5.0 s to first rows, and the same set of calls: the cost no longer grows with the site. Before the fix test site took 10 calls / 4.6 s. PASS (shots -2, -3) |
| Location tree counts | Compare with the API | test site: rooms 1,668 + "No location" 315 = 1,983. Performance sld: 4,686 + 3,795 = 8,481. Both match `total`. PASS |
| Tick a building | Performance sld "Northside Hospital Forsyth Main Building" | Banner "4609 selected · 3216 without a code", footer "1393 labels · 140 sheets". It took 25 calls (200 rows each, 609 KB, 12.7 s). Loading is on demand, only when the user asks. PASS (shot -4) |
| Tick "No location" | test site | `room_id=none`, 2 calls, 315 selected. PASS (shot -5) |
| Paging | Next page | One call `page=2&page_size=50`, "51–100 of 1983". PASS |
| Server search | Type "breaker" | One call `search=breaker` (debounced, 6 KB). PASS (shot -6) |
| Filters are server-side | "No QR code yet", "Hide already labeled" | Each toggle makes one call with `has_qr_code=false` / `qr_labeled=never`. PASS |
| Rows per page | 50 / 100 / 200 / All | "All" on 8,481 assets = 43 calls, 1.16 MB, 18.5 s (opt-in, no warning). Note, see suggestions (shot -7) |
| Scroll | Scroll the table to the bottom | 0 calls (paged table, not infinite scroll). PASS |
| Continue to step 2 | After ticking 1,476 assets | 0 extra calls. Step 2 says "1475 selected assets have no QR code and won't be printed". PASS |
| Edit codes drawer | Open on 315 assets, cancel | Opens; no write. PASS |
| Print history | Open drawer | `GET /qr-labels/prints?sld_id=…&limit=50`, empty for test site. PASS |
| API: bad input | `page_size=5000` / `page=-1` / `page=abc` / `qr_labeled=bogus` / `has_qr_code=maybe` / `room_id=not-a-uuid` / `by-ids` with 201 ids | capped at 200 / page 1 / page 1 / 400 / 400 / 200 empty / 400 "At most 200 ids". PASS |
| API: tenancy | PM (116 sites), FM (11), CP (234), EE (4). Own site → JSON; unassigned acme site ("Z - Hospital") → `lookup` 400, `filter-options` 400, `by-ids` refused, `codes/check` 422; other company's site (Saveland `f86052b7…`) → refused (CDN HTML) | Own site works for every seat; every unassigned or foreign call refused. PASS |
| Roles | nav + `/qr-labels` | PM and FM: page works (they hold `qr_labels.manage`). CP and EE: no menu item, typed URL shows "Access Denied", API 422 "Required: qr_labels.manage". PASS (shots -9, -10) |
| Flag `feature-qr-labels` | Live evalx: ON for Acme. SIMULATED OFF (evalx + feature endpoints rewritten in the browser) | Menu item greyed with a lock; `/qr-labels` shows "Feature Not Available". PASS, SIMULATED (shot -8). The fix does not add or change a flag. |
| Performance, other | Duplicates on open | `page=1&page_size=50` is fetched twice (first without, then with `qr_labeled=never`), plus a `page_size=1` count call; `/users/<me>/slds` twice on one site. Small (≈10 KB), see suggestions |

## Bugs found
None in the ticket's scope.

## Enhancement suggestions
- "Select all 50" now selects only the visible page. Earlier it said "Select all 1983". Rename it to "Select page (50)", or add "Select all 8,481 on this site" that uses `ids_only` (one call).
- Rows per page "All" on a big site makes 43 calls (18 s) with no warning. Show "This loads 8,481 rows" or cap it at 1,000.
- Drop the duplicate first-page call on open (fetch once with the default "Hide already labeled" filter).
- Step 2 text "Sheet counts assume 1 coded assets" needs singular/plural.

## Mobile part for Nency
None. QR Labels printing is web only.

## Not covered
- Saving codes on 1,000+ assets (that is ZP-4672, verified separately).
- A real print run (no change in print code).

## Proposed Jira comment
h3. QA: ZP-4671 PASS (QA build index-C14cwR0U.js, 13 Oct)

QR Labels no longer downloads the whole site when it opens.
# On "Performance sld" (8,481 assets) the page shows its first 50 rows in about 4.6 s with one page call, a room-count call and the tree. Before the fix it fetched all assets in 200-row pages.
# 1,983 and 94 assets: same calls, same time. The wait no longer grows with the site.
# Ticking a building, a floor or "No location" loads only that location (4,609 assets = 25 calls). The tree counts match the site total (1,668 + 315 = 1,983).
# Search, "No QR code yet" and "Hide already labeled" are one server call each.
# Project Manager and Facility Manager can use the page. Client Portal and Electrical Engineer get "Access Denied". Another site or another company's site is refused by every QR call.

||Site||Assets||Calls to first rows||Before||
|Performance sld|8,481|1 page + counts|43 pages|
|test site|1,983|1 page + counts|10 pages, 4.6 s|
|Test site A|94|1 page + counts|1 page|

!QA-13Oct-ZP-4671-1-8481-asset-site-opens-with-one-page-of-50-PASS.png|width=720!
!QA-13Oct-ZP-4671-4-tick-building-selects-4609-PASS.png|width=720!
!QA-13Oct-ZP-4671-9-client-portal-access-denied-PASS.png|width=720!

Small follow-ups (suggestions, not blockers): "Select all 50" now means "this page"; "All" rows per page loads 43 pages with no warning; the first page is fetched twice on open.

*Status:* moved to READY TO RELEASE

## Recommended status
READY TO RELEASE.
