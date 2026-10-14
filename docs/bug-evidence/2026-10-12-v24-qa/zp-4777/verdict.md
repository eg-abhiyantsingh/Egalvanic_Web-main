# ZP-4777: [Web] Invalid UUID format: badly formed hexadecimal UUID string

**Verdict:** PASS. Build `index-COeCBn0z.js` (QA, checked 13:15 and 13:52 IST, 13 Oct 2026). One adjacent Low API finding (B1).

**What the frontend was sending (from PR #1754, frontend only):** dragging a *view-link node* on the SLD (category `sldLink`, GoJS
key `sld-link-<uuid>`) inside a saved view fired two calls: the real save `PUT /api/sld-link/<id>/position` (200) **and** a redundant
`PUT /api/sld-view/<viewId>/positions` with `node_id:"sld-link-<uuid>"` → **400 "Invalid UUID format: badly formed hexadecimal UUID
string"** (84 Sentry events, one prod user). The fix skips link nodes in the drag-end save, keeps them out of the batch save, and saves
undo/redo of a link through the sld-link API. The QA GoJS chunk carries the new `sldLink` branch; the prod chunk (`index-zDSqDPx4.js`)
does not.

Test data (all mine, labelled): site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" `363d5350-b42d-4e48-850f-b9f72d27ea08` (team fixture), my
views "QA-DEMO ZP-4777 view A (delete me)" `f3ed98b1-adf2-45f7-a6f4-3d098c74f4dd` (QA DC-PV1, QA U2, QA DC-BUS2) and
"QA-DEMO ZP-4777 view B (delete me)" `0903017f-5197-4c90-a27d-5d91796ee545` (QA DC-CB1, QA DC-BUS1), and view link
`4ec6ed72-b496-48f8-a386-e1085b5f367e` (view A QA DC-PV1 → view B QA DC-CB1). Open: https://acme.qa.egalvanic.ai/slds → site above →
view menu → "QA-DEMO ZP-4777 view A (delete me)". Seat: Project Manager (`is_eg_admin:false`). Nothing outside my two views was moved.

## Checked
| Check | How | Result |
|---|---|---|
| Fix present in build | QA vs prod GoJS chunk | QA has the `category==="sldLink"` skip in the undo/redo save; prod does not. PASS |
| Link node exists in the view | `GET /sld-view/{A}/graph` → `sld_links[1]`; canvas shows the link circle under QA DC-PV1 | PASS |
| PR test 1: drag the link node (> 10 px) | Real mouse drag in view A | Only `PUT /api/sld-link/4ec6ed72/position` → 200 (`active_view_id` = view A). **No `/sld-view/…/positions` call.** PASS (shot -1) |
| PR test 1b: refresh | Reload, reopen view A | Link at the saved spot (1923, 310) = API `source_x/source_y`. PASS (shot -3) |
| PR test 2: same in the target view | View B, drag the reverse link node | Only `PUT /api/sld-link/4ec6ed72/position` → 200 with `active_view_id` = view B (stored as `target_x/y`). PASS (shot -4) |
| PR test 3: link + asset together | Select link + QA DC-PV1, drag | `PUT /sld-link/…/position` 200 + `PUT /sld-view/{A}/positions {positions:[{node_id:"e882a1dc…"}]}` 200: only the real asset id in the batch. PASS |
| PR test 4: undo a link drag, refresh | Drag link, toolbar Undo; again with Ctrl+Z; also Redo | Undo → `PUT /sld-link/…/position` back to (1923, 310) 200; Redo → 200 to the new spot; Ctrl+Z → 200. After reload the link stays at the undone spot. No 400. PASS (shot -5) |
| PR test 4b: undo of a combined move | Link + asset moved together, Undo | Both restored: sld-link 200 + view positions 200 (asset id only). PASS |
| PR test 5: normal asset drag | Drag QA U2 in view A | `PUT /sld-view/{A}/positions` with the asset uuid → 200. PASS |
| No bad ids in any request | All API calls across 4 browser runs (~8 drags, 4 undos, 1 redo, 3 reloads) | 0 requests with `sld-link-…`, `undefined` or `null` where an id belongs; **0 responses ≥ 400**. PASS |
| Console | Same runs | No app errors (only the known DevRev plug.js integrity and 429 third-party noise). PASS |
| Create View Link (UI) | Right-click QA DC-PV1 in view A → "Create View Link" | Dialog opens ("From: QA DC-PV1"). The link itself was created with the dialog's exact payload via API (`POST /api/sld-link/` 201) after the first UI attempt ran in All Nodes. PASS (shot -0) |
| Backend: the old bad payload | `PUT /sld-view/{A}/positions` with `node_id:"sld-link-<id>"`, `"undefined"`, and a batch of 1 real + 1 bad id (my view only) | All **400 "Invalid UUID format: badly formed hexadecimal UUID string"** (clean 400, no 500). Note: one bad id rejects the whole batch, which is why the frontend filter matters. PASS |
| Backend: other touched routes | `PUT /sld-view/<bad>/positions`, `PUT /sld-view/{A}/nodes/sld-link-…/position`, `PUT /sld-link/not-a-uuid/position`, `PUT /sld-link/<random>/position`, `PUT /node/update/sld-link-…` | All refused (masked 404, no data). PASS |
| Backend: malformed ids on common GET routes | 26 routes × `sld-link-<uuid>`, `undefined`, `null`, `123`, non-hex, random uuid (PM seat) | 24 routes refuse cleanly (masked 404 / 400 / 422). **`/api/opportunity/{bad}` → 500** (B1) and `/api/reporting/configs/{bad}` → 500 (filed under ZP-4680 B2). Positive controls (`/sld/{id}/views`, `/sld-view/{A}/graph`, `/node/{id}`, `/lookup/nodes/{sld}`) → 200 |
| Tenant | Link position API with unknown / malformed link id | Refused. A foreign company's link id was not available (none exist on QA). Partial |
| Roles | PM (customer) did every drag | Admin/other roles not repeated: the change is client-side only, same code path for any seat that can edit an SLD |
| Flags | `feature-gojs-sld` = ON (QA, Acme, live evalx). SIMULATED OFF via evalx rewrite | The SLD page still mounts the GoJS editor with the flag OFF (no React Flow fallback on this build), so there is no second path to test. The old React Flow code already skipped link nodes in prod too. No flip needed |
| Performance | Network per drag | 1 request per link drag, 2 per link + asset drag, 1 per undo (before the fix: 2 per link drag, 1 always failing). PASS |

## Bugs found
**B1 (Low, backend, API-only): `GET /api/opportunity/{id}` answers 500 for a malformed id instead of 400/404.**
1. Signed in as any seat (PM used), call `GET https://acme.qa.egalvanic.ai/api/opportunity/not-a-uuid` (also `undefined`, `null`, `123`, `sld-link-…`).
- Actual: 500 `{"error":"An internal error occurred.","success":false,"trace_id":…}`. A well-formed unknown uuid → masked 404 (correct).
- Expected: 400 "invalid id" or 404, like the other 24 routes probed.
- Not user-reachable today: `/opportunities/not-a-uuid` in the browser redirects to the Quotes list and never calls this API (ZP-4802). It only adds Sentry/log noise if any caller sends a bad id.
- Suggested assignee: Avani. Same family as ZP-4824 (Z University 500) and ZP-4820; no ticket covers `/opportunity/{id}`. File only if the owner wants API-only 500s ticketed (as was done for ZP-4824).
- No screenshot of the UI is a defect here (the page behaves); evidence is the API result in `.playwright-mcp/v24/zp4680-4777/res_c4777.json` (`probe`).

## Enhancement suggestions
- Backend: `PUT /sld-view/{id}/positions` rejects the whole batch when one `node_id` is malformed, so one bad key drops every real move in that save. Validate per item (skip bad ids, return them in the response) so a client slip can't lose layout work.
- A single click on a view-link node jumps straight to the other view. When a user only wants to select or nudge the link, that is easy to trigger by mistake; consider double-click (or a small "Go to view" button) to navigate.
- The Create View Link dialog says "All connected nodes are already in this view" when the only connected node (QA DC-CB1) is in another view but already linked. Say "already linked" instead.

## Mobile part for Nency
The SLD viewer on iOS/Android embeds this editor, and the PR keeps the iOS `nodePositionChanged` bridge message for link nodes. Please
check on mobile: open a saved view that has a view-link node, drag the link node and an asset, then reopen the view. The positions
should stay, with no error toast. Also tap a link node and check it still jumps to the other view.

## Not covered
- Production Sentry trend (needs Sentry access; I could not confirm the 84-event issue has stopped. Check after the release).
- A real React Flow (non-GoJS) renderer path: the flag override did not switch renderers on this build.
- Cross-company link id (no other company has view links on QA).

## Proposed Jira comment
h3. QA: ZP-4777 PASS (QA build index-COeCBn0z.js, 13 Oct)

*What was wrong:* dragging a view-link node on the SLD also sent {{PUT /sld-view/<view>/positions}} with {{node_id: "sld-link-<id>"}}. That is not a UUID, so the server answered 400 "Invalid UUID format" (the Sentry error).

*Tested in my own views* ("QA-DEMO ZP-4777 view A/B (delete me)" on the QA-DEMO ZP-4800 site, Project Manager seat):
# Drag the link node: only {{PUT /sld-link/<id>/position}} → 200; no view-positions call. After refresh the link stays where it was dropped.
# Same in the target view (view B): 200, saved for that view.
# Link + asset dragged together: link → sld-link 200; asset → view positions 200 with only the asset's id.
# Undo (toolbar and Ctrl+Z) and Redo of a link drag: saved through sld-link (200); after refresh the link stays at the undone spot.
# Normal asset drag: 200 as before.

||Check||Result||
|Requests with "sld-link-…", "undefined" or "null" as an id|0|
|Responses 400 or higher during all drags, undos and reloads|0|
|Old payload sent straight to the API|still 400 "Invalid UUID format" (clean, no 500)|

!QA-13Oct-ZP-4777-1-view-link-drag-only-sld-link-position-200-PASS.png|width=720!
!QA-13Oct-ZP-4777-5-link-drag-then-undo-persists-via-sld-link-PASS.png|width=720!
!QA-13Oct-ZP-4777-4-target-view-link-drag-PASS.png|width=720!

Side note (API only, Low): {{GET /api/opportunity/not-a-uuid}} returns 500 instead of 404; the web page for that link redirects to Quotes, so users don't see it.
Mobile SLD (link-node drag in a view) is with Nency.

## Recommended status
READY TO RELEASE.
