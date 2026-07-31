# SLD View Mutation Race — "Rejected" rows in the Mutation Audit Log — 2026-07-31

**Question asked:** the Admin → Audit Log (`/admin/audit-log`, Status = Rejected, last 24h)
shows three rejected `Mapping_node_sld_view` **Update** mutations at ~18:18 IST — what steps
produce these?

**Answer: adding a node to a saved SLD view and moving it in the same gesture.** The editor's
queued position UPDATE races the view-membership CREATE; the update is processed while the
`mapping_node_sld_view` row doesn't exist yet → async rejection `PERMANENT_NOT_FOUND: "Node
not found in view"`. The UI never shows an error — the API answers `200` and even **echoes the
new coordinates back** — so the user's drag position is silently lost and the failure is only
visible in this audit log.

## The three audit rows, decoded (via `GET /api/mutations?status=rejected`)

| client_timestamp (UTC) | mutation_id | node | error |
|---|---|---|---|
| 2026-07-31T12:48:18Z | `dafcaa16…` | `33ff390b…` — **Switch** | PERMANENT_NOT_FOUND: Node not found in view |
| 2026-07-31T12:48:33Z | `077088d3…` | `e0c85828…` — **Junction Box** | same |
| 2026-07-31T12:48:34Z | `0ac5503f…` | `e0c85828…` — **Junction Box** | same |

Context resolved live: both nodes exist and are not deleted, both belong to SLD
**"Chicago illinois"** (`d42076e4-e851-4295-8165-b9b92ab8e64d`), and both NOW have
`mapping_node_sld_view` rows in view **"View 1 - edit"** (`92e1d825…`).

## The smoking gun — row `created_at` vs rejection time

| Time (UTC) | Event |
|---|---|
| 12:48:18 | Switch position UPDATE → **REJECTED** (no row yet) |
| **12:48:30** | Switch's view-membership row **created** (x=-1321.77, y=-1575.20) |
| 12:48:33 / :34 | Junction Box UPDATEs ×2 → **REJECTED** (no row yet) |
| **12:48:47** | Junction Box's row **created** (x=-857.74, y=-2387.20) |

Every rejection precedes its own node's row creation by 12–14 s (the async queue's processing
lag). Both rows have `modified_at = None` — **no successful update ever landed**; whatever
position the user dragged to after adding was dropped.

## Deterministic reproduction (done live, 2026-07-31 13:01Z)

```bash
# Any node of the SLD that is NOT a member of the view (3,694 of 3,759 qualify):
curl -sk -X PUT "$B/sld-view/92e1d825-dce6-4d87-8a3d-dc26979b9392/nodes/d7c84f6c-a942-4a31-8af0-9aeaaf9ad617/position" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"x": -100.5, "y": -200.5}'
```

Response — **HTTP 200**, mutation "received", **coordinates echoed as if applied**:

```json
{ "_mutation": { "mutation_id": "171f66fc-00c7-431f-bb68-10372ad3af17", "status": "received" },
  "id": "d7c84f6c-…", "node_id": "d7c84f6c-…", "view_id": "92e1d825-…", "x": -100.5, "y": -200.5 }
```

~25 s later, `GET /api/mutations?status=rejected&limit=3` shows the new rejection at the top,
directly above the user's three rows:

```
2026-07-31T13:01:16Z  mapping_node_sld_view  update  d7c84f6c…  PERMANENT_NOT_FOUND: Node not found in view   <-- repro
2026-07-31T12:48:34Z  mapping_node_sld_view  update  e0c85828…  PERMANENT_NOT_FOUND: Node not found in view
2026-07-31T12:48:33Z  mapping_node_sld_view  update  e0c85828…  PERMANENT_NOT_FOUND: Node not found in view
```

(The 13:01:16Z row is this reproduction — a rejected mutation writes nothing, so it left no
data behind, but it will be visible in the audit log filtered to Rejected.)

## User-level steps that generate these audit entries

1. Open the SLD editor for **"Chicago illinois"**.
2. Switch from "All" mode to a saved view (**"View 1 - edit"**).
3. Add a node that isn't in the view yet (pull / drag from unpulled nodes) — here a Switch,
   then a Junction Box.
4. Move the node right as it's added (or the editor emits a position write as part of the
   same gesture).
5. The position UPDATE is queued before the membership CREATE commits → rejected async.
   UI shows nothing; Admin → Audit Log shows `Rejected / Mapping_node_sld_view / Update`.

## Defects worth ticketing

1. **Ordering (root cause):** per-(view,node) mutation ordering is not enforced — an UPDATE
   for an entity can be processed before the CREATE that makes it exist. Fix: serialize queued
   mutations per entity key, or have the client defer position writes until the add-to-view
   mutation is confirmed.
2. **Contract (silent data loss):** `PUT /sld-view/{view}/nodes/{node}/position` answers
   `200` + echoes the new coordinates for a node that is NOT in the view. The client cannot
   distinguish success from a doomed write. Fix: validate membership at request time (404),
   or at minimum stop echoing state that was not applied.
3. **UX:** rejected mutations affecting the user's last action deserve a UI signal (toast /
   retry), not only an admin audit-log row.

## Related, previously-known context

- Same-day audit-log rows also decode cleanly with this technique, e.g.
  `node` UPDATE rejected `PERMANENT_VALIDATION: "Cannot enable Trust the Photos: the asset
  has no photos"` (30 Jul) and `mapping_node_session` UPDATE rejected
  `PERMANENT_NOT_FOUND: "Mapping not found"` (29 Jul).
- The `/mapping/node-session/bulk-create` 500 (Sentry cluster, see
  `docs/bug-evidence/sentry-500-cluster-jul22-23/`) is the synchronous cousin: same
  "referenced entity doesn't exist" class, but there it crashes the request instead of
  rejecting a queued mutation.

## Artifacts

- `mut.json` — raw `GET /api/mutations?status=rejected` capture (the user's 3 rows + older examples)
- `rep1.body` — the 200-with-echo response to the doomed position PUT
