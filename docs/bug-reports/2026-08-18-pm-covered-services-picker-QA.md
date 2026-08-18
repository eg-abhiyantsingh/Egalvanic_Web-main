# Covered-services picker empty for customized PM standards — QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**Fix:** eg-pz-backend `091b92489` (cicd/dev) + `41dd1275f` (cicd/prod hotfix) — `app/routes/procedures_v2_routes.py` (+18/-5). Follow-up to ZP-3747.
**Ticket's stated environment:** *"live on cicd/prod and cicd/dev. Deliberately NOT on cicd/qa … a QA-environment check will not show it until the next dev-to-qa lift."*

---

## Verdict — the note is ACCURATE: the fix is NOT on QA, and the bug is reproducible here

I did **not** take the deploy note at face value (several "not yet in QA" notes have been stale this session). I reproduced the bug directly on QA — and this time the note holds: the fix is genuinely absent from cicd/qa, and the original defect is live.

**The exact bug signature, live on QA today:**

| Standard | `GET /api/procedures-v2/pm-plans/services?pm_standard_id=…` |
|---|---|
| **Global** — NFPA 70B 2026 (`e9ac475e…`, `is_global=true`, 228 plans) | **HTTP 200, 6 services** ✅ (Clean/Tighten/Torque, De-Energized Visual Inspection, DGA / Fluid Sample, Infrared Thermography, …) |
| **Customized** — NETA MTS (`e55cb748…`, company-owned) | **HTTP 200, 0 services** ❌ |
| **Customized** — NFPA 70B 2023 (`c4788784…`) | **HTTP 200, 0 services** ❌ |
| **Customized** — NFPA 70B 2026 (`dd534d95…`) | **HTTP 200, 0 services** ❌ |
| **Customized** — RamTest_18_06_03 (`415ef2fd…`) | **HTTP 200, 0 services** ❌ |
| **Customized** — Test_07_09 (`ee867551…`) | **HTTP 200, 0 services** ❌ |

Every one of acme's **5 company-owned (customized) standards** returns an **empty** covered-services list — exactly the *"No services prescribed by this standard's plans"* the picker shows, which disables **Next** and blocks Generate/Edit EMP. The **global** standard lists its 6 services normally. That global-works / customized-empty split is the precise fingerprint of the unfixed `WHERE pp.is_global = true` filter this route still carries on QA.

**Tenancy gate also absent (post-fix behavior not present):** calling acme's endpoint with a **demo-owned** standard id (`6998bb7e…`, `company_id = 93611164` = Demo) returned **no `403`** — it fell through rather than returning *"pm_standard_id not available to this company"*. The fix's `pm_standard_accessible(...)` gate is not on QA either.

## What this means for QA

- **Cannot be marked verified-on-QA yet** — the code isn't there. This is the correct state per the ticket: the fix was a prod hotfix cherry-picked to dev, bypassing the normal dev→qa flow.
- **The reproduction above is the pre-fix baseline.** After the next dev→qa lift, re-run the same calls: every customized standard should then return its services with plan counts (not 0), and the demo-standard call should return **403**. I can re-verify in ~2 minutes once it's promoted.

## Mapping to the ticket's QA-review items (all pending the lift)

| QA item | On QA today |
|---|---|
| Customized standard → picker lists services (not "No services") | ❌ still empty (bug live) |
| Next enabled + EMP generates for a customized standard | ❌ blocked (picker empty → Next disabled) |
| Regression: global standard still lists its services | ✅ works (6 services) — the unaffected path |
| Picker matches the services the plan engine schedules | ⏳ n/a until fixed |
| Tenancy: foreign `pm_standard_id` → **403** | ❌ no 403 gate on QA |
| Chip colours (de_energized) unchanged | ⏳ n/a until fixed |

## Recommendation

Hold QA sign-off until the fix lifts from cicd/dev to cicd/qa. The moment it does, ping me — the verification is a single script (the same calls above) and the pass criteria are unambiguous: customized standards return services, and a foreign standard returns 403. Until then, this ticket is **correctly "not testable on QA"** — and now that's *confirmed by reproduction*, not just asserted.

## Method notes
- Endpoint: `GET /api/procedures-v2/pm-plans/services?pm_standard_id=<id>`; standards from `/api/pm-standards` (company-owned) and `/api/procedures-v2/pm-plans/standards` (global).
- Demo standard id (`6998bb7e…`) obtained from a demo-tenant session to construct the cross-company tenancy case.
- Read-only; nothing created or modified.
