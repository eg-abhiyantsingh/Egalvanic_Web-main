# [Web] Multi-section gear priced as one unit, and per-section pricing could not be set from the UI at all

**QA verdict — PASS on the contract and the migration. Procedure detail now returns `unit_attributes_available`, and it is class-aware: MCC and Switchboard both offer `sections`, while Motor Controller, Panelboard and every other class on the tenant return an empty list — so the new Pricing control is gated exactly as the negative case requires. Migration sections_a3 has landed: all 15 MCC methods across nine services carry `unit_attribute: "sections"`, including services that predate this work, and Switchboard's earlier sections_a1 flip is intact at 25 of 27 methods. Not verified end to end: the Pricing radio itself and the ×12 multiplication, because no MCC asset on the tenant has a populated section count.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · procedure-detail payloads read across every MCC / Switchboard / Motor Controller / Panelboard procedure (39 of the tenant's 215).
**Ticket said "dev only, not yet promoted to QA" — wrong:** `unit_attributes_available` is served on QA and the MCC methods already carry the section axis.

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Open a procedure method on a class that has a sections attribute and confirm the Pricing control appears with Per asset and Per section options | ⚠️ **PASS at the contract level; the control was not driven** — `GET /api/procedures-v2/procedures/{id}` returns **`unit_attributes_available: [{"key":"sections","name":"Sections"}]`** for every MCC and Switchboard procedure, which is precisely the list the ticket says gates the control. The radio itself was not opened in the method editor. |
| 2 | Set Pricing to Per section and confirm the labor field relabels to "minutes per section"; save, reopen, confirm it persisted | ⚠️ **NOT EXERCISED** — no method was switched through the UI. What is verified is the persisted end state: methods already on the section axis read back `unit_attribute: "sections"` with per-section labor figures (MCC Cleaning `est_mins 24`, Arc Flash Label Placement `est_mins 5`, Arc Flash Data Collection `est_mins 10` + `10`). |
| 3 | Switch the same method back to Per asset and confirm `unit_attribute` clears and the label reverts | ⚠️ **NOT EXERCISED** — clearing was not attempted (it would mutate shared QA procedure data). |
| 4 | Negative — open a method on a class with no count-like numeric attribute and confirm the Pricing control is hidden entirely rather than shown empty or defaulted | ✅ **PASS at the contract level** — `unit_attributes_available` is **`[]`** for Motor Controller, Panelboard, ATS, Battery, Busduct, Busway, Cable, Capacitor Bank, Circuit Breaker, Disconnect Switch, Fuse, Generator, Junction Box, Load, Loadcenter, Meter, Motor, Motor Starter, Other, PDU, Rectifier, Relay and Series Capacitor. An empty list is what hides the control, so no class without a section-like attribute can show it. |
| 5 | Confirm migration sections_a3 ran on dev, then check that MCC procedure methods across services carry `unit_attribute='sections'` — including services created before this deploy | ✅ **PASS** — **15 of 15 MCC methods** carry `sections`, none without, across Arc Flash Data Collection, Arc Flash Label Placement, Cleaning, Clean/Tighten/Torque, Condition Assessment, De-Energized Visual Inspection, Infrared Thermography, Insulation Resistance Testing and NETA Testing — all of which predate this work. Consistent with the migration having run and being applied service-wide. |
| 6 | Price a job against an MCC with `sections = 12` and confirm the labor multiplies by 12 against the per-section minutes; check the quote line reflects it | ❌ **NOT EXERCISED** — no MCC asset with a populated section count was located on the tenant, so the multiplication was never put to a real quote. This is the step that would prove the feature pays off, and it remains open. |
| 7 | Regression — price a job against an MCC with the sections attribute blank and confirm it still prices at 1× rather than zero or an error | ❌ **NOT EXERCISED** — same reason; the 1× fallback is unverified. |
| 8 | Regression — confirm Switchboard per-section pricing (sections_a1) is unchanged by this migration | ✅ **PASS, with a note** — **25 of 27** Switchboard methods carry `unit_attribute: "sections"` with the same per-section labor figures as their MCC counterparts. Two Switchboard methods carry no unit attribute; whether those two are deliberate exclusions or were missed by sections_a1 is not something the payload explains. |

---

## Findings

### FINDING 1 (Low) — two Switchboard methods sit off the section axis
Switchboard is 25 of 27 methods on `sections`; MCC is 15 of 15. The two exceptions may be intentional (work that genuinely does not scale with sections) but they are the kind of gap that reads as an incomplete migration. Worth a developer confirming which two and why.

### FINDING 2 (Low, data readiness) — the feature cannot pay off on this tenant yet
The ticket's own tradeoff note says correct pricing only materialises where section counts are populated. On acme QA no MCC asset carries a section count, so every MCC still prices at 1× today. That is the documented behaviour, not a defect, but it means the change is currently inert here and the section-count backfill is the thing that actually unlocks it.

---

## Test data — direct links (QA)
- MCC procedure carrying the axis: https://acme.qa.egalvanic.ai/api/procedures-v2/procedures/fc16e95e-b7e6-4552-b249-1d496453fa4e ("Arc Flash Data Collection — MCC", `unit_attributes_available [{sections}]`, method `unit_attribute: "sections"`)
- Its service: https://acme.qa.egalvanic.ai/services/d625cfa0-5447-52c5-858e-9ecd5c84d0fb
- A class with no axis, for the negative: https://acme.qa.egalvanic.ai/api/procedures-v2/procedures/8b3bb09a-1103-4c55-a38e-063242e7bb89 ("Arc Flash Data Collection — Motor Controller", `unit_attributes_available []`)
- Classes on the tenant: MCC `83d407ba-ba4b-43a7-8fac-402629fa7c9e` · Switchboard `0555ec7e-7f97-411c-849a-daf394fbfbb8` · Motor Controller `9cb62365-deb2-470a-a627-38fc49ae4ccb` · Panelboard `573cd257-7c51-4f29-a570-7f1d88491eb0`

Evidence: `docs/bug-evidence/zp-3941-per-section-pricing/`.

## Not covered / honest gaps
- **The Pricing control in the method editor** — the Per asset / Per section radio, the "minutes per section" relabel, and the save-reopen-clear cycle were all judged from the payload the control reads rather than driven, to avoid mutating shared QA procedure data.
- **The actual multiplication** (steps 6 and 7) — no MCC with a section count exists on the tenant.
- **Which two Switchboard methods lack the axis** — counted but not identified.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
