# Audit-Log Rejected Rows → Steps Reconstruction (SLD View Race) — 2026-07-31, 18:40 IST

## What was asked
A screenshot of Admin → Mutation Audit Log filtered to Rejected showed three
`Mapping_node_sld_view` Update rows from minutes earlier — "can you find this steps".

## What was done (the investigation path, reusable as a technique)
1. **UI → API:** the audit-log page is backed by `GET /api/mutations` — querying
   `?status=rejected&limit=10` returned the exact rows (timestamps matched the screenshot to
   the second) WITH the fields the UI truncates: full entity_type `mapping_node_sld_view`,
   error `PERMANENT_NOT_FOUND: "Node not found in view"`, mutation/entity/user ids.
2. **Entity resolution:** `GET /api/graph/nodes/{id}` identified both entities as live nodes
   (a Switch and a Junction Box) on SLD "Chicago illinois".
3. **State cross-check:** `GET /api/sld/{sld}/view-mappings` showed both nodes DO have view
   rows now — in "View 1 - edit" — but their `created_at` values (12:48:30Z / 12:48:47Z) are
   **12–14 s AFTER the rejections** (12:48:18/:33/:34Z), and `modified_at=None`.
4. **Hypothesis → deterministic repro:** the position UPDATE races the membership CREATE.
   Confirmed by sending `PUT /sld-view/{view}/nodes/{node}/position` for a node not in the
   view: HTTP **200, mutation "received", coordinates echoed**, then the same Rejected row
   appeared ~25 s later — sitting directly above the user's rows in the same query.

## Result — the steps
Open SLD editor → switch to a saved view → **add a node to the view and move it in the same
gesture**. The editor's queued position write processes before the add-to-view row exists →
rejected asynchronously → audit-log row. No UI error; the dragged position is silently lost.

## Depth explanation (for learning / manager walkthrough)

**Why the audit log is the only witness:** this backend uses a dual write path — without
`x-direct-write:true` a write is queued and the HTTP response is `202-in-spirit`
(`{"_mutation":{"status":"received"}}` inside a 200). Validation happens later in the queue
processor. So a write can "succeed" at HTTP level and fail in reality; the ONLY user-visible
trace is the admin audit log the user screenshotted. When triaging anything on this platform,
"200" means "accepted for processing", not "done" — always check `/api/mutations` for the
mutation_id the response hands back.

**The `created_at` vs `client_timestamp` diff is the entire proof.** Three timestamps tell
the story: rejection at :18, row creation at :30, no `modified_at` ever. That's not
correlation — it's a causal ordering violation visible in the data itself. When hunting
async races, put the entity's lifecycle timestamps next to the failure's timestamps before
theorizing.

**Why reproduce when the timeline already convinces?** The timeline proves what happened
once; the repro proves it's deterministic and gives developers a one-line curl to verify the
fix (and it caught a second, separable defect: the endpoint echoes state it never applied —
an API-contract bug independent of the race).

**Relation to known clusters:** same defect family as the bulk-create 500
(reference-to-nonexistent-entity), but the queued path *rejects politely* while the
synchronous path *crashes with 500* — two different failure surfaces for one validation gap.
