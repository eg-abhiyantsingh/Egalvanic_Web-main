# Covered-services picker empty for customized PM standards (blocked Generate/Edit EMP) — QA verdict

**Tested:** 2026-08-21 · **Build:** QA V1.36 · **Tenant:** `acme.qa.egalvanic.ai` (2nd tenant: `demo`)
**Fix:** eg-pz-backend `091b92489` (dev) / `41dd1275f` (prod) — `procedures_v2_routes.py`, scope `pm-plans/services` by standard+tenant instead of `is_global`

---

## Verdict — **PASS.** The fix is live on QA (contra the "NOT on cicd/qa" note): the covered-services picker now returns services for a customized/forked standard, and a foreign company's standard is rejected. One spec deviation: the tenancy rejection surfaces as the platform's masked-404, not the clean `403` the ticket describes.

## ✅ The bug is fixed — forked standard now returns services
`GET /procedures-v2/pm-plans/services?pm_standard_id=<id>` (the picker's backing route):

| Standard | Result |
|---|---|
| **Global** (`NFPA 70B 2026`, control) | ✅ 200 → **6 services** with plan counts (Clean/Tighten/Torque 219, De-Energized Visual 219, NETA 218, Infrared 208, DGA 3, UPS 9) |
| **Customized / forked** (acme-owned, the bug case) | ✅ 200 → **6 services** with plan counts — **NOT** the old empty "No services prescribed…" |

Before the fix, the forked standard's `WHERE pp.is_global = true` matched zero of the company's own plan rows → empty list → **Next disabled, EMP blocked**. It now returns a full list, so **Generate/Edit EMP is unblocked** for customized standards. The query is scoped by `pm_standard_id` and accepts global-or-company-owned plans, matching what the plan engine schedules.

## ✅ Picker matches the plan engine
The forked standard's picker returns the same 6 services (with the same plan counts) that `service-usage` / the swap engine report for that standard — i.e. both now resolve by standard, not by `is_global`, so the picker reflects what will actually be scheduled.

## ✅ Chip-color driver intact
Each service carries `de_energized` (mixed True/False across the 6 — Clean/Tighten/Torque, De-Energized Visual, NETA = de-energized; DGA, Infrared, UPS = energized), so energized and de-energized chips still render distinctly.

## ⚠️ Tenancy gate — effective, but returns masked-404 not the spec'd 403
The ticket's negative case: a standard belonging to a **different company** must return **403 "pm_standard_id not available to this company"**, not an empty success.

I forked a genuinely **demo-owned** standard (`0e5f60f1`, created + owned by demo company `93611164`, confirmed absent from acme's standards list — not the shared global), then called the picker as **acme**:
- **acme-owned standard → 200, 6 services** (positive control)
- **demo-owned standard → masked-404** (200 + SPA HTML) — **rejected, no services, no leak**

So the gate **is effective** — a foreign standard yields nothing, which is the security substance (no empty-success payload exposing another tenant's plan shape). **But the mechanism is the platform's masked-404, not the clean `403` + message the ticket promises.** Same pattern I flagged on NETA-3 / EG-pin: the tenancy block works but surfaces as a masked-404 rather than a typed 403. Worth the dev confirming `pm_standard_accessible` actually raises the 403 here (vs the route 404'ing for another reason) — the outcome is safe either way, but a clean 403 is the intended, more debuggable contract. (Random/bogus id also → masked-404.)

## Method
Live QA. Reused acme forked standards (`ddec757b`) + the global `NFPA 70B 2026` from prior PM-standard testing; hit `GET /procedures-v2/pm-plans/services?pm_standard_id=` for global (control), forked (bug case), a demo-owned standard (tenancy), and a random id. Forked a demo-owned standard from the demo tenant specifically for the cross-tenant test (distinct company confirmed via standards-list membership), then deleted it. Masked-404 (200 + SPA HTML) treated as rejection, never success. Did not drive the Generate-EMP UI end-to-end (the picker's backing route is the fix's substance and is verified directly). No acme data mutated.
