# ZP-4680: [Web] Report config preview: the Site picker shows only the first 50 sites A–Z (plus the default) and has no search, so most of a company's sites cannot be chosen as preview or AI-edit context

**Verdict:** PASS, with 2 Low follow-ups (one UI, one API). Build `index-COeCBn0z.js` (QA, checked at 13:15 and 13:52 IST, 13 Oct 2026).
Fix: frontend PR #1748 (picker becomes a server-searched Autocomplete) + backend PR #1552 (`has_more`). The fix is in the
QA bundle (`ReportingConfigEditor-BanMDYsu.js` carries `?q=`, `has_more` and both footer texts).

Test data: config "Annual Maintenance Report (Copy)" https://acme.qa.egalvanic.ai/reporting/config/0eee082a-af4d-4805-9ae0-c19d3996af9a
(read only, nothing saved), WO-level config "kroon-test-2" https://acme.qa.egalvanic.ai/reporting/config/c0fc4522-4957-4713-83dc-8bc4ef294105
(read only). Far sites used: "test site" `21bd10d4…`, "Z - Hospital" `d5d99deb-9554-4774-99d1-16eb1ded0735`.
No records were created or changed. The one AI edit request was blocked in the browser before it left (no AI call, config unchanged).

## Checked
| Check | How | Result |
|---|---|---|
| Picker opens on the default | Super Admin opens the config | Picker shows "Performance sld · 26 Feb 2026" (largest site, ZP-4398 rule kept); 1 `preview-entities` call + 1 `preview-html`. PASS (shot -1) |
| Full list + footer | Open the picker without typing | 51 rows (default + 50 A–Z) and footer "Showing only the first sites A–Z — type to search them all". PASS (shot -2) |
| Search finds "test site" | Type `test site` | 11 matches incl. "test site", "Test site", "Test site 4/12"… PASS (shot -3) |
| Picked far site re-renders preview | Pick "test site" | `POST preview-html {"primary_id":"21bd10d4…"}` 200, 5 pages, cover reads "MAINTENANCE RECORD · TEST SITE", Facility "test site". PASS (shot -4) |
| Site starting with Z | Type `z - h`, pick "Z - Hospital" | Found; preview-html with `d5d99deb…`, cover "Z - HOSPITAL". PASS (shot -9) |
| Partial / mid-word match | API `q=erform` | Returns "Performance sld". PASS |
| Case-insensitive | `test site` vs `TEST SITE` vs `tEsT` | Same 11 results for both spellings. PASS |
| No-match empty state | Type `qa-nomatch-zzqq` | **The dropdown shows the currently selected site ("test site") instead of "No sites match …"**. Same pin on every search ("z - " lists "test site" first). FAIL, Low, see B1 (shots -5, -6) |
| Reopen after a pick | Close and reopen | Full list again (52 rows: picked site + 51) and "test site" stays selected; no refetch of the base list. PASS |
| Escape / blur | Type `perf`, Escape, click away | Escape keeps the typed text; blur restores "Z - Hospital"; selection unchanged. PASS |
| AI-edit context uses the picked site | Pick "Z - Hospital", Edit with AI, Apply (request blocked in the browser) | Dialog says "Rendering against Z - Hospital"; the blocked `POST …/ai-edit` carried `primary_id=d5d99deb…` (Z - Hospital) and the message. The 503 in the shot is my block, not the app. PASS (shots -7, -10) |
| Every site reachable (admin) | API: search each of the company's 281 sites by its exact name | 281/281 found (count = `GET /api/company/{acme}/slds`; the ticket's 262 has grown to 281). PASS |
| PM scope (116 sites) | PM seat, same sweep | 116/116 own sites found; 150 of 165 unassigned acme site names searched → **0 returned**; base list 51 rows, all accessible. PASS |
| Client Portal scope (234 sites) | CP seat, same sweep | 234/234 found; 47/47 unassigned names → 0 returned. PASS |
| Other company's site | Search "Saveland" (company fdcadc86) from admin, PM, CP, AM | 0 results every time. PASS |
| preview-html scope | PM and CP: own site / unassigned acme site / other company's site `f86052b7…` / `not-a-uuid` | 200 + 5 pages / refused (masked 404) / refused (masked 404) / 400 "Invalid primary_id". Positive control = own site 200. PASS |
| Roles | AM (113 sites) | API 200, own scope. PASS |
| Roles | FM, Electrical Engineer | 422 `permission_denied` (no `reports.view`). PASS (expected) |
| Roles | PM opens the editor URL | "Access Denied" page (PM has no Report Builder), same as 6 Oct. PASS, expected (shot -8) |
| API: paging params | `limit=5`, `limit=500`, `offset=50`, `page=2`, `search=` | All ignored: always 51 rows, cap 50 + default, `has_more:true`. OK (cap is by design; search is the way to reach the rest) |
| API: garbage `q` | `%`, `_`, `'`, `' OR 1=1--`, `<script>`, `\`, unicode, 3 spaces, 300 chars | All 200; `%` → 0 rows and `_` → only names that really contain `_` (wildcards escaped); spaces = unfiltered list. PASS |
| API: 5,000-char `q` | URL too long | 400 "Bad Request" from the server front (clean). PASS |
| API: NUL byte `q=a%00b` | API only | 500 "A string literal cannot contain NUL (0x00) characters." Hardening note (not filed: a NUL cannot be typed into the box) |
| API: malformed config id | `GET /api/reporting/configs/not-a-uuid[/preview-entities]` | **500** "An internal error occurred." (random well-formed uuid → masked 404). The editor URL `/reporting/config/not-a-uuid` shows a **blank page** and fires Sentry events. FAIL, Low, see B2 (shot -12) |
| Other config types use the same picker | API: one config per type (11 types) + UI on the WO config "kroon-test-2" | All 11 types (session, plan_*, quote_*, issue, arc_flash_readiness, sld) return 50 + `has_more` and honour `q`. WO config: placeholder "Search work orders…", footer "Showing only the most recent work orders — type to search them all", search finds "QA-DEMO ZP-4532…". PASS (shot -11; the red boxes in that preview are that config's own missing S3 templates, unrelated) |
| Performance: typing | 9 keys at 110 ms, 5 keys at 60 ms, 4 keys at 450 ms | 1, 1 and 4 requests (300 ms debounce works; stale replies dropped). PASS |
| Performance: payload / latency | `preview-entities` | 0.24–0.73 s, 7.7 KB unfiltered, 1.7 KB for "test site"; no duplicate calls on load. PASS |
| Performance: preview render | `preview-html` | 6.4 s (test site), 10 s (Performance sld); first preview after page load 17.5 s, because the default is the largest site. Slow (> 2 s) but not caused by this ticket; see suggestions |
| Flags | `feature-reporting-v2` = ON (QA, Acme, live evalx) | The editor is behind it; this ticket adds no flag and changes nothing gated. No flip needed |
| Description's own steps | Steps 1–3 of the ticket | Picker searchable, "test site" and C–Z sites reachable, API no longer the only 51. PASS |

## Bugs found
**B1 (Low, frontend): the Site picker never shows "No sites match …"; the selected site is pinned on top of every search result.**
1. As an admin open https://acme.qa.egalvanic.ai/reporting/config/0eee082a-af4d-4805-9ae0-c19d3996af9a
2. Pick "test site" in the Site picker.
3. Click the picker again and type `qa-nomatch-zzqq` (or type `z - `).
- Actual: the list shows "test site · 14 Nov 2025" as the only row for a search that matches nothing; for `z - ` it shows "test site" first, then "Z - Hospital".
- Expected: "No sites match "qa-nomatch-zzqq"" (the PR already has this text); the current pick should not appear as a search result, or should be marked as the current choice.
- Cause (PR #1748): `options` always prepends the selection when the search doesn't return it, so `noOptionsText` can never show. The same happens on WO-level configs.
- Evidence: `QA-13Oct-ZP-4680-5-no-match-shows-selected-site-not-empty-state-FAIL.png`. Suggested assignee: Avani (wrote PR #1748) or Krunal (frontend).
- Jira search ("no match" / picker / preview-entities): nothing covers it.

**B2 (Low, backend + a frontend empty state): a report-config link with a bad id gives a blank page; the API answers 500.**
1. As an admin open https://acme.qa.egalvanic.ai/reporting/config/not-a-uuid (a truncated or mistyped link).
- Actual: the Reporting page stays blank with no message. `GET /api/reporting/configs/not-a-uuid` → 500 "An internal error occurred." (also `…/preview-entities`), and the page sends Sentry events. A well-formed unknown id also gives a blank page (API masked 404).
- Expected: 404/400 from the API and a "Report configuration not found" message with a link back to the Builder.
- Evidence: `QA-13Oct-ZP-4680-12-malformed-config-id-in-url-api-500-FAIL.png`. Suggested assignee: Avani (backend 500), Krunal (empty state).
- Same family as ZP-4824 / ZP-4820 (500 on bad input), different endpoint; no existing ticket covers `reporting/configs/{id}`.

## Enhancement suggestions
- Show who the site belongs to (account) or its asset count in each picker row: "test site" and "Test site" differ only by date today.
- Footer could say how many are hidden ("50 of 281 sites — type to search").
- Remember the last previewed site per config (it resets to the largest site on every open), and/or open on a small recent site: the largest site makes the first preview take 10–17 s.
- Mark the current choice in the open list (tick or "current" label) instead of mixing it into the results (fixes B1 too).

## Mobile part for Nency
None. Report Builder / config preview is web-only; nothing in PR #1748/#1552 reaches iOS or Android.

## Not covered
- A real (paid) AI edit run: by instruction I only proved the request carries the picked site (`primary_id`); the AI service itself was already proven on 6 Oct (ZP-4398).
- Saving a config (no save needed; no QA-DEMO copy created).
- Technician and Portal Sales seats (Technician has no web access; Portal Sales was not in this run).

## Proposed Jira comment
h3. QA: ZP-4680 PASS (QA build index-COeCBn0z.js, 13 Oct)

The preview Site picker is now searchable and reaches every site:
# Opens on the default site; the list shows the first 50 A–Z with the footer "type to search them all".
# Typing searches the server (one request per typing pause). "test site", "Z - Hospital" and mid-word matches ("erform") are found, case does not matter.
# Picking a far site re-renders the preview for that site, and *Edit with AI* sends that site as its context ({{primary_id}} = Z - Hospital).
# All 281 acme sites can be found. Project Manager (116 sites) and Client Portal (234) find all of their own sites and none of the others; another company's site is never returned.
# The same picker works on Work Order, quote and plan configs ("Search work orders…").

||Check||Result||
|Search for each site by name (admin / PM / Client Portal)|281/281, 116/116, 234/234 found|
|Unassigned or other company's sites in results|0|
|Preview of an unassigned or other company's site (PM, CP)|refused; own site works|

!QA-13Oct-ZP-4680-3-search-test-site-found-PASS.png|width=720!
!QA-13Oct-ZP-4680-9-far-site-z-hospital-picked-preview-rerendered-PASS.png|width=720!
!QA-13Oct-ZP-4680-10-ai-edit-request-carries-picked-site-PASS.png|width=720!
(The 503 in the last image is from QA blocking the AI request on purpose, so no AI edit ran.)

Two small follow-ups, to file separately (Low):
* A search with no match shows the current site instead of "No sites match …" (the current pick is always added to the results).
* {{/reporting/config/not-a-uuid}} shows a blank page; the API answers 500 instead of 404.

!QA-13Oct-ZP-4680-5-no-match-shows-selected-site-not-empty-state-FAIL.png|width=720!

## Recommended status
READY TO RELEASE (B1 and B2 are Low and separate; neither blocks reaching or using any site).
