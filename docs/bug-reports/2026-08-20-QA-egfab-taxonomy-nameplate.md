# EG Forms acceptance-batch: taxonomy + nameplate — QA verdict

**Tested:** 2026-08-20 · **Build:** QA V1.36 · **Tenant:** `acme.qa.egalvanic.ai`
**PRs:** eg-pz-backend **#1018** (migration `egfab_a1`) · eg-pz-reporting-lambdas **#276** (nameplate name-resolution)

---

## Verdict — taxonomy migration is on QA and correct (contra the "dev only" note). Nameplate *render* (reporting #276) not exercised.

The ticket says the migration is "dev only, not yet on QA," but the live `/node_classes` response on QA shows it has already been applied here. Verified directly against that response (the authoritative source that populates every asset-class picker in the product).

## ✅ Verified on QA (live `/node_classes`, 591 classes)

**New classes exist** — MV Switch, Instrument Transformer, Lightning Arrester.

**Subtypes under the correct parents:**

| Parent class | Subtypes present |
|---|---|
| **Instrument Transformer** (global) | **Current Transformer**, **Potential Transformer** ✅ |
| **Transformer** (company override) | **Low Voltage Transformer** ✅ |
| **ATS** (global) | **Static Transfer Switch** ✅ (+ existing ATS subtypes) |
| **MV Switch** (company override) | **SF6 Padmount Switch** ✅ (the ticket's "SF6 Switch"; note the name), + the moved MV subtypes ↓ |

**MV shift done** — MV Switch now holds **Load-Interruptor Switch, Bypass-Isolation Switch (>1000V), Disconnect Switch (>1000V), Fused Disconnect Switch (>1000V)**; and **Disconnect Switch** now holds only the **≤1000V** subtypes (Bypass-Isolation ≤1000V, Disconnect ≤1000V, Fused Disconnect ≤1000V). So the >1000V gear moved off Disconnect Switch onto MV Switch. ✅ (#2)

**Nameplate core attributes present** — e.g. Lightning Arrester carries Model, Catalog Number, Style, Protection Class, MCOV, Duty Cycle, Serial Number (A/B/C phase) as reserved core attrs. ✅ (the 74-attr append)

**Multi-tenant overrides** — the override classes returned are parented per-company: MV Switch override on `d59d449b` (acme), Transformer override on `c9e4561b`, Lightning Arrester override on `05961117` — i.e. overrides landed for **≥3 different companies**, each on that company's own override class (not the global). ✅ (#6)

## ⚠️ Not exercised (honest)
- **#3 Nameplate RENDER fix** (reporting-lambdas **#276**) — export a canonical id-keyed nameplate asset (LVCB) to docx/PDF and confirm the nameplate block is populated (was blank before). This is a *separate* deploy from the migration; I did not export a docx and parse it, and can't confirm #276 is on QA from here. **The core-attrs it renders from do exist (above), but the render itself is unverified.**
- **#4 Negatives** (inline-name Breaker-1 still wins; id-not-in-definition stays off) — render-level, not exercised.
- **#5** Nameplate values read as context, not duplicated as editable inputs — render-level, not exercised.
- **#7 Idempotency** (run `alembic upgrade head` twice → no double-insert) — cannot run alembic / DB from QA; the ticket states a dev dry-run gave 94 statements all UPDATE 0 / INSERT 0 0.
- **#1 "pickable" via the create-asset UI** — the classes are provably in `/node_classes` (which feeds every picker); I did not capture a create-asset dropdown screenshot (the new classes have no assets yet, so they appear only in the all-classes create picker, not session-scoped views).

## Method
Live `GET /api/node_classes` on acme QA (591 classes); parsed each class's subtypes + `is_override`/`company_id`; checked the four new subtypes' parents, the MV shift (present on MV Switch, absent from Disconnect Switch), the nameplate core attrs, and per-company override parenting across three companies.
