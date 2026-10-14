# ZP-4618 — Task #97 (R&D): Realtime SLD collaboration (presence + live change broadcast on the web editor)

**Verdict:** BLOCKED. The web code and the backend ticket/entities routes are on QA, but **no realtime WebSocket server answers on QA**, and `feature-sld-realtime` is OFF for Acme. Presence, cursors and live changes cannot be seen. What could be tested passes: flag OFF, the ticket API, the entities API, retry behaviour and tenant scope.
**Build:** QA `index-D1IWZ6SI.js` (SLD chunks identical to `index-BdXGlAq5.js` with hashes ignored).
**Tested:** 12 Oct 2026, 19:40–19:55 IST, acme.qa.egalvanic.ai. Two seats at once on the same SLD: Project Manager (`+project@`) and Super Admin (`+admin@`).
**Test data:** my site `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)` (`bb593be6-…`).
**Flags:** the bundle reads `_d("sld-realtime")`, i.e. LD key **`feature-sld-realtime`**. The live evalx value for Acme QA (company `d59d449b-…`) is **false**. A legacy key `sld-realtime` is also false.

## Checked

| Check | How | Result |
|---|---|---|
| Flag OFF (real, live) | Open my SLD and watch network and sockets for 8 s | PASS. No `/api/realtime/*` call and no realtime socket (only the Beamer and DevRev sockets). No presence avatars. The editor works normally (shot 1). |
| Flag ON, **SIMULATED** | LD evalx rewritten to `true` in this browser, and the LD stream blocked so it cannot restore `false`; PM and Super Admin open the same SLD | The editor starts realtime: `POST /api/realtime/ticket` returns 200 JSON `{ticket, ttl:60}`, then it opens `wss://acme.qa.egalvanic.ai/api/realtime/ws?ticket=…`. **Every handshake fails with "Unexpected response code: 200"**: the request falls through to CloudFront's `index.html`, so no realtime service sits behind the ALB rule on QA. No presence, cursors or live moves are possible (shot 5). With the stream left on, the rewrite is overwritten and nothing starts (first attempt). |
| `/api/realtime/ticket` returns JSON (be#1461 narrowed ALB rule) | `POST` from the page | PASS. 200 `application/json` `{ticket, ttl:60}`. `GET` → 405. A missing or malformed `sld_id` → 400 `sld_id required`. Unauthenticated → 401 JSON. |
| `/api/realtime/health` | `GET` | Returns the web app HTML, not the realtime service (shot 6). Per be#1461 the ALB should send `/api/realtime/ws` and `/health` to the realtime ECS service, so that service is **not provisioned on QA**. |
| Forged or expired ticket rejected | Open a socket with a made-up ticket | Not provable. A forged ticket and a valid ticket fail identically (1006, HTML handshake), because there is no server. |
| Ticket tenant/site gate | PM asks for a ticket for acme "Test site A" (not assigned to PM), and for another company's site | PASS. Both refused (404 page). Super Admin gets 200 JSON for the same acme site (positive control). Client Portal on my site: refused (no reach). |
| Retry behaviour while the server is missing (performance) | Count tickets and sockets with flag ON (simulated) | Reasonable backoff, no storm. P1 made 6 ticket calls and 11 socket attempts in about 100 s; 3 tickets in a 45 s window. No console errors apart from the blocked LD stream. The editor keeps working: a node drag still saves (`PUT /node/update` 200). |
| `POST /sld/{id}/entities` (be#1465/1467/1469) | My site, PM | PASS. `{node_ids:[1 id]}` returns exactly that node (v2 shape, 116 keys). `shape:"canvas"` adds `gone_node_ids`, `gone_edge_ids` and `reference_nodes`. `include:["touching","mappings","notes","sld"]` returns `comments`, `edge_sld_view`, `node_sld_view` and `sld.layout_owner_user_id/layout_owner_name`. 501 ids → 400 `node_ids must be a list of at most 500 ids`; 500 ids → 200; a non-UUID id → 400 `ids must be UUIDs`. An unknown include value is silently ignored (200). Site not assigned to the caller → 404; Super Admin → 200. |
| Live change between two editors | PM drags a node; Super Admin's editor is watched for 8 s | No live update, as expected without a server. Super Admin sees the move only after a refresh. |
| Activity indicator, avatars, inline update (fe#1646, fe#1650) | — | Cannot be seen without a server. |

## Bugs found
None in the code that could run. The blocker is environmental: the realtime service is not deployed on QA.

## Flags / environment needed (for the main session to ask Shubham in eg-internal-dev)
1. **`feature-sld-realtime` → ON**, environment **QA**, company **Acme** (`d59d449b-09d8-45d6-8f0a-ef70024b1293`), for ZP-4618. Then later **OFF** again to confirm the controlled switch.
2. **The realtime ECS service and ALB rule on QA** (`infra/bootstrap_realtime_service.sh`, routing `/api/realtime/ws` + `/health`) and **`REALTIME_NOTIFY=1`** on the QA API. Without these the flag alone shows nothing. If the R&D feature is meant to stay dev-only, the ticket should not be in the v2.4 release.

## Enhancement suggestions
1. Stop retrying after N failed handshakes and show a small "Live updates unavailable" note, instead of silently retrying forever.
2. Make `POST /realtime/ticket` refuse (or say "realtime disabled") when the company's flag is OFF. Today it mints tickets regardless of the flag.
3. Reject unknown `include` values on `/sld/{id}/entities` with 400 rather than ignoring them, so client typos show up.

## Mobile part for Nency
- **ZP-4653** ([iOS] realtime in the SLD viewer, online only; Ready for QA) and **ZP-4655** (Android, Backlog). They are blocked by the same missing QA realtime service and flag.
- Once both are on: change on web, then confirm the iPad applies the geometry live and re-fetches only the changed entities (`realtimeChanges`); going offline closes the socket; the iOS viewer shows cursors and selection outlines only (no avatars or toasts); an app build without the handler falls back to a full refresh after about 15 s.

## Not covered
- Every live-collaboration row (presence, cursors, selection outlines, live drag, group move carrying children, deferral while a dialog is open, own-echo skip, layout-lock banner update, removal from a view propagating). Blocked as above.
- `ehist_a3` migration and entity-history rows. That is backend; the related ZP-4611 entity_changes ticket is separate.

## Proposed Jira comment
```
h3. QA (12 Oct, QA build index-D1IWZ6SI.js) — BLOCKED (no realtime server on QA), rest passes

*Why blocked:* feature-sld-realtime is OFF for Acme on QA. With the flag forced ON in my browser only (simulated), the editor gets a ticket (POST /api/realtime/ticket 200 JSON) and opens wss://acme.qa.egalvanic.ai/api/realtime/ws, but every handshake fails: the request gets the web app's HTML (CloudFront index), and /api/realtime/health also returns the web app. So the realtime ECS service / ALB rule is not on QA, and presence, cursors and live changes cannot be tested.

||Check||Result||
|Flag OFF: no realtime calls, no socket, editor normal|PASS|
|/api/realtime/ticket returns JSON (not index.html); missing sld_id 400; 401 unauthenticated|PASS|
|Ticket for a site the user can't reach|PASS — refused (Super Admin 200 as control)|
|Retry while the server is missing|PASS — backoff, about 6 tickets / 100 s, editor keeps working|
|POST /sld/{id}/entities: exact ids, canvas shape, include sld/notes/mappings, >500 ids 400, non-UUID 400, scope|PASS|
|Presence / cursors / live moves / avatars / inline activity|BLOCKED|

Needed to finish: feature-sld-realtime ON for Acme on QA *and* the realtime service deployed on QA (REALTIME_NOTIFY on). If this R&D work is meant to stay on dev only, please take it out of the v2.4 release.

!QA-12Oct-ZP-4618-1-PASS-flag-OFF-no-realtime-layer.png|width=720!
!QA-12Oct-ZP-4618-5-BLOCKED-simulated-ON-socket-handshake-fails.png|width=720!
!QA-12Oct-ZP-4618-6-BLOCKED-realtime-health-is-the-spa.png|width=720!
```

## Recommended status
Stay In QA (BLOCKED). It needs the flag flip and the QA realtime service. Never READY TO RELEASE on a simulated state.
