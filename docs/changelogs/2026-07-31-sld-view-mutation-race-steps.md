# 2026-07-31 — Audit-log "Rejected" rows decoded: SLD view add+move race

**Prompt:** screenshot of `/admin/audit-log` (Status=Rejected) with three
`Mapping_node_sld_view` Update rows — "can you find this steps".

## Steps found (and proven by live reproduction)

1. Open SLD **"Chicago illinois"** in the editor, switch to saved view **"View 1 - edit"**.
2. Add a node not yet in the view (Switch `33ff390b…`, Junction Box `e0c85828…`) and move it
   in the same gesture.
3. The queued position UPDATE processes before the view-membership CREATE commits →
   async rejection `PERMANENT_NOT_FOUND: "Node not found in view"` → the audit rows.

Proof: rejections at 12:48:18/:33/:34Z; the same nodes' membership rows were created at
12:48:30/:47Z (12–14 s later, the queue lag); both rows have `modified_at=None` → the user's
drag position was silently dropped. Reproduced deterministically at 13:01Z: position PUT for
any non-member node answers **200 + echoes the coordinates**, then lands as the same Rejected
row ~25 s later.

## Defects to ticket

- No per-entity ordering in the mutation queue (UPDATE processed before its CREATE).
- `PUT /sld-view/{view}/nodes/{node}/position` returns 200 + echo for a doomed write —
  silent data loss, only discoverable in the admin audit log.

Evidence: `docs/bug-evidence/sld-view-mutation-race/EVIDENCE.md` (timeline, raw captures,
repro curl). Note: the reproduction added one more Rejected row (13:01:16Z, Circuit Breaker
`d7c84f6c…`) — expected, rejected mutations write nothing.
