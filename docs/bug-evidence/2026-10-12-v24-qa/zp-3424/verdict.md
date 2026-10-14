# ZP-3424 — Asset Table: Persist Sort Order (Ascending/Descending)/ Filter

**Verdict: PARTIAL.** All 8 acceptance criteria pass on QA build `index-COeCBn0z.js` (re-checked 13 Oct 13:53 IST). One Medium bug was found in the same "open an asset, then come back" flow: the **Search** text comes back in the search box, but the list is **not** filtered by it.
Frontend PR eg-pz-frontend #1753 and backend PR eg-pz-backend #1559 were both merged to QA on 13 Oct. The ticket has no dev notes beyond the two merge comments.

Test data: site **Richmond, CA** (`768ff683-4244-4fa0-aced-36ed471f0c33`, 466 assets, 408 top-level rows with nesting on) and **Performance sld** (`6271fdc2-…`, 8,481 assets) for performance. Shared view used in the tests:
`https://acme.qa.egalvanic.ai/assets?sort=label&dir=desc&page=2&class=453df10a-682f-46ca-a5ee-1ed94f40f8f3&room=5ce2d1b1-2713-4afe-9ba4-5541d75f1004`
(Asset Class = ATS, Location = Building-1212 > Floor-1212 > Room-1212, Asset Name Z→A, page 2 → 26–50 of 128).

Scripts and raw output are in `.playwright-mcp/v24/zp3424/` (r1–r7.js / .out). All runs used a headed Chrome through pwslot.sh.

## Checked

| # | Check | How | Result |
|---|---|---|---|
| 1 | URL encodes sort, filter and page state (ids/enums only) | PM, Richmond: picked Asset Class ATS and a Location in the filter panel, then sorted Asset Name Z→A and went to page 2 | URL = `?sort=label&dir=desc&page=2&class=<uuid>&room=<uuid>`. Only ids and enums are used, with no names. Default sort (created_at desc) and page 1 are left out of the URL. **PASS** (shot 1) |
| 2 | No PII in the query string | Looked at every param name and value. Also opened `?class=john.doe@example.com&room=Jane Smith` | The code whitelists the keys `sort, dir, page, size, class, subtype, room, parent, com, shutdown`. Filter values must be UUIDs; `com` and `shutdown` must be known enums. Free text is dropped and removed from the URL, and the "Some filters couldn't be applied." notice shows. Search text is never written to the URL. **PASS** |
| 3 | AC1: open an asset and come back | Clicked a row → `/assets/{id}` → browser Back | Same URL, sort, both filter chips and page 2 (26–50 of 128). One list call. **PASS** (shot 2). There is no "Back to Assets" button on the detail page, so browser Back and the sidebar are the only ways back. |
| 4 | AC2: navigate away and return | Clicked sidebar Issues, then sidebar Assets (in-app, no reload) | Restored `?sort=qr_code&dir=asc&class=ATS` with the same rows. **PASS** (shot 5). A typed URL / full page load of `/assets` with no params opens the default view, as the "URL state, not storage" DoD requires. |
| 5 | AC3: copied URL in a new tab | Same browser, new tab with the copied URL | Same chips, sort indicator and rows (26–50 of 128). **PASS** (shot 6). The QA seats show the mandatory-MFA "Set up later" screen in every new tab (sessionStorage). The deep link survives that screen. |
| 6 | AC3/AC8: deep link before sign-in | Fresh browser, FM and CP seats, opened the shared URL while signed out → signed in | Login keeps the deep link: the app lands on `/assets?...`, then clamps and drops whatever doesn't apply to that seat. **PASS** |
| 7 | AC4: shared URL with a filter the recipient can't see | FM (11 sites, no access to Richmond, CA) opened the PM's URL | FM sees only his own site (7 assets). The class and room filters are dropped with "Some filters couldn't be applied." and page 2 is clamped to 1. No Richmond data appears. **PASS** (shot 10) |
| 8 | AC4: same URL, Super Admin and CP | SA with Richmond selected; CP in Maintenance Portal › Assets | SA gets the identical view (shot 12). CP's portal Assets grid also hydrates `?sort=label&dir=desc&class=ATS` from the URL (68 ATS rows, shot 11). **PASS** |
| 9 | Server-side re-authorisation of the site | FM called `GET /api/lookup/v2/nodes/{Richmond}?…class&room…` and `/filter-options` directly | Both refused (no JSON; QA's CDN masks 403/404 as 200 HTML). `/api/sld/{Richmond}` → 422 permission_denied. Positive control: PM and SA get 200 JSON (466) through the same call. The older "lookup/v2 ignores assigned sites" leak no longer reproduces for this route and the FM seat. **PASS** |
| 10 | Server-side re-validation of filter ids | PM: list API with another company's class id (`4c6569dd…`, "Transistor"), a room and a parent asset from another site (Android Site 2), random UUID, non-UUID text | All return 200 with `total:0`. Nothing from another site or company is returned. Bad enums are rejected with clear 400s: `com=99` → "com must be 1, 2, 3, 4 …", `shutdown_restriction=bogus` → "must be one of …". **PASS** |
| 11 | Server-side sort is whitelisted | `sort_by=bogus`, `sort_by=label; DROP TABLE nodes--`, `sort_by=company_id`, `sort_dir=sideways` | All return 200 and quietly fall back to the default order. No 500, no SQL error. **PASS** |
| 12 | AC5: invalid sort column / direction | `?sort=bogus&dir=asc`, `?dir=desc`, `?sort=label&dir=sideways`, XSS payloads in sort/class | Default order plus the notice "Couldn't restore the saved sort — showing the default order." No error, no script runs (0 dialogs), and the URL is cleaned. `dir=sideways` keeps the Asset Name column and uses A→Z, which matches the ticket's "falls back to default sort direction" (but see the suggestion on the wording). **PASS** (shot 7) |
| 13 | Invalid / unauthorized filters in the URL | random UUID, another company's class, another site's room and parent, `com=7`, `shutdown=bogus` | Each one is dropped, with the notice "Some filters couldn't be applied." The URL is rewritten. **PASS** (shot 8). The legacy `?class=ATS` (a name) is resolved to the class id. |
| 14 | AC6: page beyond range | `?page=99999` (no filter → page 17, 401–408 of 408); `?…&page=99999&class=ATS` (→ page 7, 151–170 of 170); `?page=-1`, `?page=abc` → page 1; `?size=1000` → 25 | Clamped silently, as the ticket says. **PASS** (shot 9) |
| 15 | AC7: Clear all filters | Focused "Clear all filters", pressed Enter | Back to `/assets` with no query string, the default sort and all 408 rows. **PASS** (shot 4) |
| 16 | AC8: hard refresh | F5 on the shared URL (×3) | Fully hydrated: same chips, sort and page. 1 list call per reload in 2 of 3 runs; one run showed 2 identical calls (not repeatable). **PASS** (shot 3) |
| 17 | Sort is server-side now, and persists across pages | Header clicks, page 2, and comparison with the API truth | Every header click sends `sort_by/sort_dir`. Z→A row 1 = `wwwwwwwwwwwwwwwww` = the API's true last. Page 2 continues the order (page 1 ends `Transformer`, page 2 starts `testb`). On Android Site 2, Z→A row 1 = **`yu`**, the true last from the ZP-4338 proof. ZP-4338 (page-local sort) is fixed for Assets. **PASS** |
| 18 | a11y: aria-sort | Read `aria-sort` on every header | The sorted column shows ascending/descending; the other sortable columns show `none`. Keyboard: focus the QR Code header + Enter → sorts, URL updates, focus stays on the header. **PASS** |
| 19 | a11y: aria-live notice | DOM | `role=status aria-live=polite` region is always present (1 per page); the notice text is injected into it and has a close (X) button. **PASS** |
| 20 | a11y: filter chips | Tab to a chip + Delete key | The chip is a focusable button with aria-label "Remove filter: Asset Class: ATS". Delete removes it, the URL updates, and focus moves to the next chip. **PASS** |
| 21 | a11y: toolbar | Accessible names of the toolbar buttons | The **filter-panel toggle and the refresh button have no accessible name** (screen readers announce just "button"). This comes from the shared MainDataGrid toolbar, so it is minor and listed under suggestions. |
| 22 | Roles | PM (116 sites), Super Admin (staff), FM (11 sites), CP (234 sites, portal) | All behave as above. Admin/AM/EE were not run separately: they use the same component, and their only differences are permission-gated toolbar buttons. Technician has no web access. |
| 23 | LaunchDarkly | Bundle (`Assets-BIHkD_dh.js`) | URL-state parsing (`gn/ws`) runs unconditionally. No flag gates this feature, so no flip is needed. N/A |
| 24 | Performance | Request log per load | One list call per load (≈0.3–0.9 s, 4–14 KB) + filter-options (≈0.5–1.0 s). Invalid/legacy ids cost one extra list call (filter-options validates them after the first fetch). `page=99999` costs two calls (fetch, then clamp). These are acceptable. **Pre-existing, not caused by this ticket:** every Assets load also downloads the whole site through `GET /api/lookup/nodes/{sld}`: 114 KB / 1.9 s on Richmond, and **1.37 MB / 9–10 s on Performance sld (8,481 assets)**. This happens with and without URL state. Rows show in 3.8–5.2 s on that site. |
| 25 | Console | pageerror/console during all runs | No app errors. Only third-party noise (Beamer CSP fonts, DevRev SRI, Beamer 429) and my own deliberate 400/422 probes. |

## Bugs found

### B1 — After opening an asset and coming back, the Search box still shows the text but the list is not filtered (Medium)
1. Sign in as Project Manager, select site **Richmond, CA**, open **Assets**.
2. Type `Generator` in **Search Assets…**. The list shows 12 generators (1–12 of 12).
3. Click any asset row to open it.
4. Press the browser **Back** button (or go to Issues and back to Assets through the sidebar).

- **Actual:** the Search box still says **Generator**, but the list shows **all 408 assets**, and the list API call has no `search=` parameter. The user thinks they are looking at generators. Typing one more character applies the search again.
- **Expected:** the search comes back together with the sort and filters (the list shows the 12 generators), or the box is cleared. The box and the list must agree.
- **Why:** the shared grid restores the box text from sessionStorage (`mainDataGrid_assets_searchQuery`). The Assets page keeps the applied search in its own state, which starts empty again, and nothing passes the restored text back. Sort and filters are restored from the URL; search is restored nowhere.
- **Evidence:** `QA-13Oct-ZP-3424-14-PM-search-Generator-before-opening-asset-PASS.png` (before), `QA-13Oct-ZP-3424-15-PM-search-box-says-Generator-but-list-shows-all-assets-FAIL.png` (after Back).
- **Suggested assignee:** Krunal (frontend).
- **Existing-ticket check:** JQL `project = ZP AND (summary ~ "asset search" OR summary ~ "search box" OR summary ~ "search text" OR text ~ "search persist" OR (summary ~ "assets" AND summary ~ "search"))`. The closest match is ZP-3652 "Web: Form list remains filtered after navigating back, while search text is cleared" (Done). It covers the Forms list and the opposite symptom. **No existing ticket covers the Assets search.**

## Enhancement suggestions
- **Put search in the URL too** (e.g. `q=`). It is the most-used way to narrow the table, and a shared or bookmarked view is incomplete without it. A search term is user input, not PII, but the PO should confirm. This would also fix B1.
- **Give the toolbar filter and refresh icon buttons an accessible name and tooltip** ("Filters", "Refresh"). Today both are announced as "button", which fails WCAG 4.1.2.
- **Change the notice wording for a bad direction.** `dir=sideways` keeps the column and uses A→Z, but the notice says "showing the default order". Something like "Couldn't restore the sort direction — showing A→Z" would be accurate.
- **Nest subcomponents is stored in localStorage, not the URL.** A shared link can open flat for one person and nested for another (row counts 466 vs 408). Consider a `nest=` param.
- **Assets page performance:** stop downloading the whole site through `/api/lookup/nodes/{sld}` on every visit (1.37 MB / 9–10 s on an 8.5k-asset site). This is the same pattern as ZP-4671 (QR Labels). It is a separate ticket for Krunal + Avani and is not caused by ZP-3424.
- **Detail page "Back to Assets" link:** add one that returns to the exact list view (the saved URL). Browser Back works today, but there is no in-app control for it.

## Mobile part for Nency
Web-only story (URL state). Nothing is needed on iOS/Android unless the mobile Assets list is expected to remember sort and filter too; the ticket doesn't ask for that. FYI only: the same `GET /api/lookup/v2/nodes/{sld}` now accepts `sort_by/sort_dir` server-side (allowed columns: label, qr_code, com, node_class_name, node_subtype_name, building_name, floor_name, room_name, parent_asset_name, created_at; page_size is capped at 200). Mobile lists that call it are not affected.

## Not covered
- Admin, AM and EE seats were not run one by one. They use the same component and data path as PM/SA, and the permission differences only touch toolbar buttons.
- The URL is not tied to a site. A link opened while a different site is selected reapplies the filters to that site, keeping class ids (company-wide) and dropping room/parent ids with the notice. This matches the design (no site id in the URL) and was not counted as a bug.
- Phone layouts (owner rule: no phone-width checks).
- Maintenance Portal: hydration from the URL was checked (CP). Back-navigation from a portal asset detail was not run separately; it uses the same component and the same `navigate` call.

## Proposed Jira comment

```
h3. QA — ZP-3424 on QA build index-COeCBn0z.js (13 Oct): all 8 ACs pass, 1 bug found (Search)

*Tested on* Richmond, CA (466 assets) as Project Manager, Super Admin, Facility Manager (no access to that site) and Client Portal (Maintenance Portal > Assets).

||Check||Result||
|Sort + filters + page go into the URL, ids/enums only (no names, emails or search text)|PASS|
|Open an asset, press Back → same sort, filters, page|PASS|
|Go to Issues and back via the sidebar → same view|PASS|
|Copied URL in a new tab / after sign-in → same view|PASS|
|Facility Manager opens the PM's URL → only his own site's data + "Some filters couldn't be applied."|PASS|
|Bad sort/dir (bogus, sideways, script text) → default order + notice, no error|PASS|
|Other company's class id / other site's room or parent / random id → dropped + notice; list API returns 0 rows for them|PASS|
|page=99999 → last page; page=-1 / abc → page 1; size=1000 → 25|PASS|
|Clear all filters → /assets with no query string|PASS|
|Hard refresh → hydrates from the URL|PASS|
|Sort is server-side and continues across pages (Z→A row 1 = true last asset)|PASS|
|aria-sort on headers, aria-live notice, chips removable with the keyboard|PASS|

*Bug: Search text comes back in the box, but the list is not filtered*
# Assets, type "Generator" in Search (12 rows).
# Open any asset, press Back.
# The Search box still says "Generator" but the list shows all 408 assets. Typing another character re-applies it.
Expected: the search is restored with the sort/filters (or the box is cleared), so the box and the list agree.

!QA-13Oct-ZP-3424-2-PM-after-open-asset-and-back-same-view-PASS.png|width=720!
!QA-13Oct-ZP-3424-10-FM-opens-PM-shared-url-only-permitted-data-plus-notice-PASS.png|width=720!
!QA-13Oct-ZP-3424-7-PM-invalid-sort-url-default-order-plus-notice-PASS.png|width=720!
!QA-13Oct-ZP-3424-15-PM-search-box-says-Generator-but-list-shows-all-assets-FAIL.png|width=720!

*Suggestions:* put search in the URL as well (q=); give the filter and refresh icon buttons an accessible name; stop loading the whole site via /api/lookup/nodes/{sld} on every Assets visit (1.37 MB / 9–10 s on an 8.5k-asset site; existing behaviour, not from this ticket).
```

## Recommended status
**Stay In QA** (PARTIAL). The ticket's own ACs pass. File B1 (Medium, Krunal) as a separate bug, then move ZP-3424 to READY TO RELEASE; or keep it In QA if the owner decides that search is part of "filters" for this story. No LaunchDarkly flip is needed.
