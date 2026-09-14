# ZP-4042 — /reporting/history 500 — raw evidence (2026-09-14, QA build index-DDSq5pRr.js)

## Deterministic, role-independent 500 (positive control passes)

GET /api/reporting/history?sld_id=aadcee4c-7dd0-45b3-81b9-309c5c166084

| Role (seat)                         | /reporting/history            | /reporting/configs (control) |
|-------------------------------------|-------------------------------|------------------------------|
| CP  (+clientportal@, Client Portal) | 500 internal_error            | 200 application/json         |
| PM  (+project@,      Project Mgr)   | 500 internal_error            | 200 application/json         |
| ADMIN (+admin@,      EG staff)      | 500 internal_error            | 200 application/json         |

Also 500 with NO params, with a bogus all-zero sld_id, and with limit=5 only.
Content-type of the 500 is application/json (a real handler error), not the ~2KB masked HTML shell.
An unknown sibling route (/api/reporting/history-does-not-exist) returns 200 text/html (masked) — so
/reporting/history is a REGISTERED route that always faults, not a soft-404.

## trace_ids handed to the developer
CP:    2dc69a18efe946079496ce01b3277526 · 7d22b7d8a2ec4c02b5002989e0c307e1
PM:    5e472461a55a4473ba58a10edea7b78e · 78e6a41631994527b312419fd84c32b3
ADMIN: 698b016371184008aae508d9805864f6 · ef657d356f634af19336b717067f0e55
earlier: 005f90ffb40b4f4dba15500dae2ff9da · 88e77208c54a47128c236900f7eefe55 · 2d5dd0cbec54421e9d8fd7db6a927e0d

## Deploy state on QA (why this is a pre-promotion blocker, not a live outage)
- The NEW endpoint /api/reporting/history IS on QA (returns a JSON 500, not a masked 404) but its backing
  table report_generations (migration rptgen_a1, "the portal's only query path") appears absent → every
  query 500s. Sibling /reporting/configs works, so the reporting blueprint itself is healthy.
- The NEW customer portal routes /maintenance-portal/* are NOT functional on QA: /maintenance-portal/reports
  renders "Access Denied" and no shipped page calls /reporting/history.
- The live /maintenance/reports is the OLD report-GENERATION catalog (Condition Assessment, EMP Lite, Arc
  Flash Readiness, Annual Maintenance Report, Asset Service History, Program Compliance). It calls
  /reporting/configs and works — it is NOT the ZP-4042 recorded-history view.
- Net: no customer hits the 500 today because the consumer UI is not live. But the endpoint must not ship
  broken — promoting the portal frontend before the migration lands breaks the Reports view immediately.

## Read-only X-EG-Portal guard (ZP-4042 negative case) — UNCONFIRMED, not reported
- node/create returned identical "200 received" with and without X-EG-Portal:true — but node writes are
  async (200 = queued, not persisted) and the read-only backend (#1192/#1193) may not be on QA, so this
  does NOT prove a guard bypass. Marked inconclusive; needs the guard confirmed present on QA first.

## Note
- The service-object site_walk_config PUT probe on d625cfa0 (global service "Arc Flash Data Collection")
  is a no-op on read-back (200 but field stays null); the service object is intact. Not a finding.
