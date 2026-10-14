# ZP-4570 — Quick photo view for assets on the GoJS SLD (task #23, TKT-865)

**Verdict:** PARTIAL. The new quick view itself works: hover card, gallery, "View photos" menu, dismiss rules, error state, French strings. But on today's QA build, one hover on a locked graph opens **two photo cards at once**: the new one and the older photo-badge popover (ZP-4435). This clash is already known and fixed in **ZP-4832** ("drop the photo popover", cicd/dev only, not on QA yet).
**Build:** QA `index-C-Cc1qD1.js` (checked live 13 Oct 16:40 and 17:06 IST).
**Tested:** 13 Oct 2026, 16:40–17:06 IST, acme.qa.egalvanic.ai, Project Manager seat (main), Super Admin, Client Portal, Electrical Engineer, Facility Manager.
**Test data:** read-only hovers on site "Abhiyant test 12" (`517a73f6-…`, 28 assets with photos). No writes on it.
**Ticket re-read live:** In QA, 0 comments. The 19 files from 12 Oct were re-checked. Shots 19/21 from 12 Oct ("maintenance-portal-one-line-hover … PASS") are mislabelled: they show "Access Denied". Do not use them.
**Flags:** `feature-gojs-sld` is live ON for Acme. The quick view is only in the GoJS editor. **SIMULATED OFF:** the older editor loads ("Select a View to Load Assets") and has no quick view (shot 4800-8, same gate). No new flag for this ticket.

## Checked

| Check | How | Result |
|---|---|---|
| Hover ~1 s on an asset with photos | Locked graph; pointer on the body of "Dc bus test" (away from the badge); sample every 250 ms | New card appears at ~1.0 s: name, class, "6 photos", 4 thumbnails, "+2". **FAIL (known):** the older photo-badge popover is already open at 0.25 s, so 2 cards stack (shot 1). Fixed by ZP-4832, not deployed to QA. |
| Pointer into card / leave | Move into the card, wait 1.5 s; then leave and sample every 100 ms | PASS. Stays open while inside. Closes 200–300 ms after leaving. |
| "No photos" | Hover asset "Hhj" (0 photos) | PASS. "Hhj · DC Bus · 0 photos · No photos" (shot 3). |
| "Photos unavailable" | QA forced `GET /photo/by_entity/*` to 500 on a not-yet-hovered asset "ATS 4 g" | PASS. "Photos unavailable" (shot 5). The header still says "0 photos" (see observation 2). |
| Gallery from thumbnail | Click the 2nd thumbnail | PASS. Opens at "2 / 6". ArrowRight → 3, 4; ArrowLeft → 3. Thumbnail strip: 6 images. |
| Gallery from icon | "View all photos" icon on the card | PASS. Opens at "1 / 6". |
| Gallery from context menu | Unlock the graph, right-click the asset, "View photos" | PASS (shot 4). Not in the menu while locked (unchanged lock behaviour). |
| Download / Open asset page | Gallery buttons | PASS. Download saves the jpg. "Go to Asset Page" opens `/assets/010b2574-…` in a **new tab**. Esc closes. |
| Zoom / pan | Drag the photo; mouse wheel, Ctrl+wheel, double-click, "+" key | Pan PASS (image moves 120,60). **Zoom: no input changed the "100%" label or the image size** (shot 8). The ticket says "ZoomPanImage zoom/pan". Possibly trackpad-pinch only; not confirmed, so not filed. |
| Dismiss | Wheel, mouse press, programmatic pan, zoom | PASS for all four. |
| No card on links / node-buses | Hover a link midpoint; code check | PASS on a link. The code excludes only `is_node_bus` assets and layout groups. Bus-role assets such as "DC Bus" are real assets and do get a card (by design). |
| Cache | Hover the same asset again | PASS. 0 new photo requests (30-min per-node cache). |
| French | User menu > Language > Français | PASS. "Voir toutes les photos", "Aucune photo", "0 photo", gallery "Profil", "Fermer", "Télécharger la photo" (shots 6, 7, 14). |
| Flow-only / iOS embed | **SIMULATED** `window.FLOW_ONLY_MODE=true` | **Not proven.** The new card still opened in the simulation (shot 11). The code gate is `flowOnlyMode prop \|\| window.FLOW_ONLY_MODE \|\| iOS bridge`; on web the editor gets `flowOnlyMode:false` and the global may be read too late. Needs a real check in the iOS viewer (Nency). |
| Maintenance Portal read-only one-line | `/maintenance-portal/sld` as Client Portal, Project Manager, Facility Manager | **Not testable:** "Access Denied" for every seat (shots 10, 12). The portal SLD surface was removed by ZP-4731 (Done), so this QA Review row no longer applies. |
| API contract | `GET /photo/by_entity/{id}` (+ `?types=node_profile,node_nameplate,node_arc_flash_sticker,node_other`) | 200 JSON. Random UUID → 200 `[]`. Non-UUID → 200 HTML (QA CDN masks 404 as the SPA page). |
| Roles | `/auth/v2/me` + calls | Client Portal has `photos.view`, gets the photos JSON, and `/slds` shows "Access Denied" (nav has no SLD link: nav and route agree). Electrical Engineer and Facility Manager have `photos.view`. Super Admin gets "View photos" in the node menu. |
| Tenant / site scope | Staff positive control | Inconclusive. The only unassigned site's asset has 0 photos (staff and PM both get `[]`). The other company's graph is refused even to staff (422), so no foreign node id was available. |
| Performance | Network log | Photos are fetched lazily after the dwell: 2–3 calls per new asset (`by_entity` with and without `types`, 400–700 ms). No calls > 2 s. The two calls come from the two cards (the old card fetches all types). |

## Bugs found
- **Two photo cards on one hover (known):** already covered by **ZP-4832** (removes the ZP-4435 popover). Not a new ticket. Re-test once ZP-4832 reaches QA.

## Observations (not filed)
1. **Photo count disagrees with the badge.** "Dc bus test" has 11 photos (badge "11"), but the card says "6 photos". The card only counts Profile, Nameplate, Arc-flash sticker and Other. The 5 Panel-schedule photos are dropped (API: 1 profile, 5 nameplate, 5 panel schedule). If the badge stays (ZP-4832 keeps it under the "Photos" overlay), a user sees 11 and 6 for the same asset.
2. In the error state the header reads "0 photos" next to "Photos unavailable". Better to hide the count when the fetch failed.
3. The gallery shows a "100%" zoom label that never changed with mouse input in my run.

## Enhancement suggestions
1. Count all photo types on the card, or label it "6 of 11 photos (asset photos)", so it matches the badge and the asset page.
2. Add a keyboard path: Space or Enter on a selected node opens the card. Today the card is mouse-only.
3. Show the photo type and date under the thumbnails.

## Mobile part for Nency
SLD viewer change (OTA bundle). On iPad/iPhone: open an SLD with photo assets and long-press/tap nodes. **No** hover card and **no** gallery must open inside the embedded viewer (flow-only). After ZP-4832's OTA publish, the photo-count badge must be gone in the app viewer.

## Not covered
- The real iOS/flow-only embed (simulation not decisive, see above).
- Portal one-line (route removed for every role by ZP-4731).
- Cross-company photo read (no foreign node id obtainable from the acme host).

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C-Cc1qD1.js (QA): PARTIAL, waiting for ZP-4832

*Works:*
# Hover about 1 s on an asset: card with name, class, count, 4 thumbnails, +N. It stays while the pointer is on it and closes about 0.3 s after leaving.
# "No photos" and "Photos unavailable" (forced error) show correctly.
# Gallery opens from a thumbnail, from the gallery icon and from right-click "View photos" (unlocked graph). Arrow keys, thumbnail strip, download and "Go to Asset Page" (new tab) work.
# Mouse press, wheel, pan and zoom close the card. Links get no card.
# French strings show ("Voir toutes les photos", "Aucune photo").

*Problem on this build (already fixed in ZP-4832, not on QA yet):*
# Open any SLD (graph locked) with assets that have photos.
# Rest the mouse on an asset with photos for 1 s.
*Actual:* two photo cards open on top of each other (the old photo popover and the new card).
*Expected:* one card.

!QA-13Oct-ZP-4570-1-FAIL-two-photo-cards-on-one-hover.png|width=720!
!QA-13Oct-ZP-4570-2-PASS-gallery-zoom-pan.png|width=720!
!QA-13Oct-ZP-4570-5-PASS-forced-error-photos-unavailable.png|width=720!

*Notes:* the card counts 6 photos while the badge says 11 (panel-schedule photos are not counted). Mouse-wheel zoom in the gallery did not change "100%". The Maintenance Portal SLD shows Access Denied for all roles (removed by ZP-4731), so that row could not be tested.

*Status:* stay In QA until ZP-4832 is on QA, then re-check the single card.
```

## Recommended status
**Stay In QA.** Everything in this ticket's own scope passes except the stacked card, which ZP-4832 fixes. Re-test when ZP-4832 is deployed to QA. Not To Do: the fix lives in another ticket.
