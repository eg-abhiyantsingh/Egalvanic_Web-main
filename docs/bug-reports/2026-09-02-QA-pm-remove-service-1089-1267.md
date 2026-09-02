# PM standards remove-service (#1089 / #1267) — QA verdict: PASS, 1 discrepancy

**Tested:** 2026-09-02 · **Env:** acme.qa.egalvanic.ai V1.36 · driven live in the browser (Admin).
Ticket said "dev only"; per standing rule I tested QA directly — **it is live on QA.**
**Artifact:** https://claude.ai/code/artifact/bab6fcde-3447-4b20-9b45-e26581aa739a

## Verified
- Nav: Builder rail = Reports · Services · PM Plans · Forms; PM Plans highlights on /pm-plans; gone from Admin.
- Rail remove (small): ZP-1242 standard → dry run "4 cadences across 4 plans" (200 {plans:4,removed:4}),
  confirm → 200 {removed:4} → rail "Nothing prescribed yet." Server count, not client-recomputed.
- Rail remove (large): Custom standard / Clean Tighten Torque → dry run 200 {plans:219,removed:219},
  dialog "219 cadences across 219 plans"; CANCEL → 229 plans unchanged (nothing written).
- Matrix row remove: CTT on Battery/ESS → 3× DELETE /procedures-v2/pm-plans/{plan}/services/{svc} (200),
  row cleared, plan count 229 intact; dialog says "clearing N cadences" (not "criticalities"). Works via the
  existing per-mapping DELETE (no new backend surface).
- Negative global (UI): NFPA 70B 2026 (global) → "Customize" only; no Swap/Remove/trash/Add service.
- Negative malformed: non-UUID service_id → 400 {"error":"service_id must be a UUID"} (not 500).
- Negative tenant isolation: unscoped service_id on own standard → 200 {plans:0, removed:0} (nothing removed).

## DISCREPANCY (author confirm, not user-facing)
Ticket says a global standard must 404. OBSERVED: POST .../standards/{globalId}/remove-service → **200 + null
body** for both a real service on that global and a random UUID. Nothing is written and the UI never exposes
the path (no controls on a global), so no user harm — but an API client reads 200 as success for a silent
no-op. Confirm: is null-200 the intended no-op on a not-owned standard, or should the _own_pm_standard gate
404 as written?

## Not covered
- isAdminOnlySurface trap: a role lacking features.site_visits.view would lose PM Plans after the Builder move.
  Admin + Super Admin hold it (PM Plans reachable); did not enumerate every role for a stranded one.
- Did not read is_deleted=true at DB (confirmed via UI refresh + rowcount response).
- Rowcount-vs-preview race not forced (timing-dependent).

## Test data
Removals on QA sandbox standard ZP-1242 (drained empty as the test) + a labelled QA-DEMO standard; the
large-standard test was cancelled. No global/seeded standard mutated.
