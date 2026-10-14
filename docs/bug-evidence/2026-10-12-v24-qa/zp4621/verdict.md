# ZP-4621 — Task #100: Batch node position moves and All-Nodes collapse apply as one queued mutation

**Verdict:** PARTIAL / FAIL on the collapse row. Batch **positions** pass on both paths (direct and queued), including bounds, roles and fail-closed checks. Batch **collapse** through the queue (the iOS / offline path) never applies. The node route refuses it with 400 "Could not determine entity_id". The view route accepts it, then it ends as **dead_letter** ("Working outside of request context"). On the web (direct writes), collapse works as one request.
**Build:** QA `index-C14cwR0U.js` (live 17:32 and 18:11 IST, 13 Oct).
**Tested:** 13 Oct 2026, 17:45–18:08 IST. Project Manager (main), Super Admin (staff, to read mutation status and as a control), Electrical Engineer (not assigned to my site), Client Portal.
**Test data (mine):** site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-…`) and view "QA-DEMO ZP-4800 view (delete me)" (`21281f97-…`). I moved only my `QA G-*` / `QA SWBD1` assets and set them back afterwards.
**Ticket re-read live:** In QA, 0 comments. Labels api / backend / migration (no web change).
**Flags:** `feature-sld-realtime` is **OFF** live for Acme. SIMULATED ON in a second browser: no SLD realtime socket opened (only Beamer/DevRev sockets, plus one 502 in the console), so the "one broadcast" row could not be seen.

## Checked

| Check | How | Result |
|---|---|---|
| `PUT /node/batch-positions` direct (what the web sends, `X-Direct-Write`) | 3 of my assets +10 px | PASS. 200 `{"success":true,"updated_count":3}`, all 3 moved. |
| Same, **queued** (iOS path, no `X-Direct-Write`) | 3 assets +20 px; mutation status read by Super Admin | PASS. 200 `received` → **applied** (op `batch_positions`, entity = sld_id when it is sent, otherwise the first node). **No enum 500.** One mutation for 3 assets. |
| Bounds | Direct | PASS. 2001 items → 400 "positions must be a list of at most 2000"; 2000 → 200. Non-UUID → 400. Missing x / null y → 400. `x:true`, `x:"5"`, `1e309`, `NaN`, `1000001` → 400 "x out of range"; `1000000` → 200. `width:false` / `5e6` → 400. Not a list → 400. Empty list → 200 (0 updated). Body `{}` → 200 (0 updated; minor, see suggestions). |
| Bounds on the queued path | 2001 items; `x:true` | PASS. Accepted as `received`, then **rejected PERMANENT_VALIDATION** by the worker (400 → permanent, as the ticket says). Nothing moved. Non-UUID is refused at once (400). |
| Fail closed: unknown node | Random UUID | PASS. Refused (QA CDN masks it as HTML), nothing written. |
| Fail closed: **another company's** node, no partial write | — | NOT TESTED with a real foreign node. On this build even the staff seat is refused for the other company's site (`/sld`, `/nodes/sld`, `/lookup` → 422/HTML), and the mutation log is company-scoped, so no foreign node id can be found without the demo tenant (not allowed). **Dev to confirm with the unit test** (403 + no partial write). |
| Roles | Client Portal; Electrical Engineer **not assigned** to my site | Client Portal: 422 `nodes.manage` for both batch routes (correct). Electrical Engineer: `GET /sld/{id}/graph` → 422, but `PUT /node/batch-positions` on my asset → **200, and the asset moved** (shots x0, x1, x2). That is the same site-scope gap as **ZP-4865**, now also on this new route (see out of scope). |
| Collapse, web (All Nodes) | Right-click QA SWBD1 › Collapse Downstream; then Expand | PASS. **One** `PUT /node/batch-collapse-states` with all 6 states, 200; 5 assets fold away (shot 3). Expand = one request. |
| **Collapse, queued** (iOS / offline) — row 5 | `PUT /node/batch-collapse-states` without `X-Direct-Write` (with and without `sld_id`, with `id`) | **FAIL.** 400 `{"error":"Could not determine entity_id"}` every time, so it is never queued. |
| **Collapse, queued, view route** (the route the log shows as `batch_collapse`) | `PUT /sld-view/{view}/collapse-states` without `X-Direct-Write` | **FAIL.** 200 `received`, then **dead_letter** after 4 tries: `TRANSIENT_ERROR … "Working outside of request context"`. The collapse never applies. Admin › Audit Log shows "Batch_collapse · Dead Letter · 4" (shot 4). Repeated twice (mutations `2f9edafa…`, `303fd496…`). Control: the per-node queued `PUT /sld-view/{view}/nodes/{node}/collapse` and queued `PUT /node/update` `{is_collapsed}` → **applied**. |
| Web multi-asset drag | Select 3 assets in All Nodes, drag | Works, but the web still sends **one `PUT /node/update` per asset** (3 requests), not `batch-positions` (shot 2). Layout ops with several moved assets do use `batch-positions`. Not in this ticket (no web change); see suggestions. |
| One commit / one realtime broadcast | Second browser, realtime SIMULATED ON | NOT WEB-TESTABLE on QA: realtime is OFF and no realtime socket opened. The queued batch did land in the database (one mutation, 3 assets). |
| SQS: one `delete_message_batch` per receive | — | NOT WEB-TESTABLE (backend). |
| Performance | Network | Direct batch of 3: 280–320 ms. Queued: applied within ~10 s. Collapse of 6: one request, ~300 ms. 2000-item batch: 1.2 s. |

## Bugs found (in scope)
### 1 (High for iOS/offline): A queued "collapse" never applies
Steps (QA, build `index-C14cwR0U.js`, 13 Oct; seat with `mutations.view`, e.g. Super Admin; my site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)"):
1. All Nodes collapse, queued like iOS: send `PUT /api/node/batch-collapse-states` **without** the `X-Direct-Write` header, body `{"states":[{"node_id":"<asset id>","is_collapsed":true}]}`.
   - **What happens:** 400 `{"error":"Could not determine entity_id"}`. Adding `"sld_id"` or `"id"` to the body does not help.
2. View collapse, queued: send `PUT /api/sld-view/21281f97-167c-464c-ba50-7f60f777220e/collapse-states` without `X-Direct-Write`, body `{"states":[{"node_id":"<QA CB1 id>","is_collapsed":false}]}`.
   - **What happens:** 200 `received`. Then open **Admin › Audit Log**: the row "Batch_collapse · Mapping_node_sld_view" shows **Dead Letter** after 4 attempts (Screenshot: `QA-13Oct-ZP-4621-4-FAIL-audit-log-batch-collapse-dead-letter.png`). The error (`GET /api/mutation/status/{id}`) is "Exceeded max retries (3): Working outside of request context."
3. Control: queued `batch_positions` in the same minute → **Applied** (the row just below in the same screenshot).
- **What should happen:** a queued collapse applies like a queued position batch (one mutation, applied), with no enum error and no dead letter.
- **Why it matters:** a collapse/expand made on iOS (or offline) is lost; the web never sees it.
- **For the developer:** the node route has no entity-id rule for the queue (positions got "sld_id or first node"). The view-route worker handler reads the Flask request context.

## Out of scope (already filed — add this route to it)
### ZP-4865: a user not assigned to a site can still change its SLD — `PUT /node/batch-positions` is affected too
1. Sign in as the Electrical Engineer. In SLD, search "QA-DEMO ZP-4800": "No sites available" (Screenshot `QA-13Oct-ZP-4621-x1-PASS-EE-cannot-pick-the-site.png`). `GET /api/sld/{id}/graph` → 422.
2. Send `PUT /api/node/batch-positions` `{"positions":[{"node_id":"<QA G-Meter id>","x":3100,"y":1700}]}` with `X-Direct-Write: true`.
3. Sign in as the Project Manager (site owner) and open the SLD. QA G-Meter moved from its place (Screenshot `QA-13Oct-ZP-4621-x0-PASS-meter-in-normal-place-before.png`) to 3100,1700 (Screenshot `QA-13Oct-ZP-4621-x2-FAIL-asset-moved-by-unassigned-EE.png`).
- **Expected:** 422, like the read. The new tenant check is company-level only; site assignment is not checked. I set the asset back.

## Enhancement suggestions
1. Web: when several selected assets are dragged in All Nodes, save them with one `PUT /node/batch-positions` (today it is one `PUT /node/update` per asset; 3 requests for 3 assets).
2. `PUT /node/batch-positions` with `{}` (no `positions`) should be 400, not 200 "0 updated".
3. Admin › Audit Log: let the user open a row to read `error_details` (today it shows "Dead Letter" with no reason). The "Received" time shows "01:35:31" with no AM/PM.

## Flags / flips needed
None for this ticket. To see the "one broadcast" row on QA, `feature-sld-realtime` would need to be ON for Acme (QA). That belongs to ZP-4618; I did not ask for it.

## Mobile part for Nency
This ticket is mainly for iOS. On iOS, with site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)":
1. Run a magic layout / straighten / area move on 3+ assets. Then check in web Admin › Audit Log that it is **one** "Batch_positions · Applied" row (not ~12 "Update" rows), and that the web SLD shows the new places after a refresh.
2. Collapse and expand a branch in All Nodes and in a view on iOS. Check that the web shows the same collapse after a refresh, and that the Audit Log has no "Batch_collapse · Dead Letter" row. Expected to fail today (bug 1).

## Not covered
- Real other-company node (no id obtainable; dev unit test).
- Realtime broadcast count (flag OFF, no socket).
- SQS batch delete (backend).

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C14cwR0U.js (QA): positions PASS, queued collapse FAIL

*Works:*
# PUT /node/batch-positions moves 3 assets in one call, both direct and queued. The queued one is a single "Batch_positions" mutation, Applied, with no enum error.
# Limits work: 2001 items, a non-UUID id, missing x, true / "5" / NaN / 1e309 / 1000001 are all refused (400 on the direct call, Rejected PERMANENT_VALIDATION on the queued one). An unknown asset is refused. Client Portal gets 422.
# Web: right-click > Collapse Downstream sends one batch-collapse-states request for all 6 assets.

!QA-13Oct-ZP-4621-3-PASS-collapse-downstream-one-batch-request.png|width=720!

*Fails (row 5, queued collapse, the iOS / offline path):*
# Send PUT /api/node/batch-collapse-states without the X-Direct-Write header. Result: 400 "Could not determine entity_id". It is never queued.
# Send PUT /api/sld-view/<view>/collapse-states without X-Direct-Write. Result: 200 received, then Admin > Audit Log shows Batch_collapse = Dead Letter after 4 tries. The error is "Working outside of request context". The queued Batch_positions in the same minute is Applied.
Expected: a queued collapse applies like a queued position batch.

!QA-13Oct-ZP-4621-4-FAIL-audit-log-batch-collapse-dead-letter.png|width=720!

*Also:* a user not assigned to the site (Electrical Engineer) can move its assets with batch-positions (200, saved). This is the same gap as ZP-4865; please cover this new route there too.
!QA-13Oct-ZP-4621-x2-FAIL-asset-moved-by-unassigned-EE.png|width=720!

*Not checked from the web:* another company's node (no id reachable on QA; please confirm with the unit test), one realtime broadcast (feature-sld-realtime is OFF on QA), SQS batch delete.

*Status:* moved to To Do
```

## Recommended status
**To Do.** The ticket's own row 5 (batch collapse applies through the queue) fails on both collapse routes. This is shown in the Audit Log with a staff seat and repeated twice. The positions part passes. If the owner prefers to split the collapse part out, "stay In QA" is the alternative.
