# [Web] Auto-minted basic fixes priced a single section on multi-section gear, and a method-less rule swallowed issues before the wildcard fix catalog

**QA verdict — the dispatch half is demonstrably working and the section machinery exists, but the ticket's headline number could not be reproduced on QA. Rule-minted ("basic") resolutions are live and behave as described: six were found in a 15-issue survey, each bound to an implementation method with real labor and no AI in the path. The unit-attribute mechanism that section scaling rides on is in place — procedure detail exposes `unit_attributes_available` and every MCC and 25 of 27 Switchboard methods carry `unit_attribute: "sections"`. What could not be verified is the multiplication itself: no switchboard or MCC on the tenant has a recorded section count, the corrective fix catalog's methods carry no `unit_attribute` at all, and NFPA 70B 28.3.2 could not be raised on a four-section asset. The 720-vs-180 check is therefore open.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · procedure payloads and the fix-flow export-spec read directly; resolutions surveyed across 15 issues.
**Ticket said "dev only, not yet promoted to cicd/qa".** The rule dispatch and the section axis are both observable on QA; whether *this* backend change is deployed here cannot be read from the product.

**Artifact:** https://claude.ai/code/artifact/ee8f908a-ec4c-4327-bc1b-6032cd545f43

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Migration `issres_a9_sections_breakfix_basicres` runs on deploy — confirm it applied before testing | ⚠️ **NOT OBSERVABLE** — migration names are not exposed through the product. The features it should bring are partly present (see below) and partly absent (per-fix `unit_attribute`, break/fix forms on corrective methods), which is itself the useful signal. |
| 2 | On a switchboard with 4 sections, raise the NFPA 70B 28.3.2 issue and check the basic resolution mints **720** minutes of labor, not 180. Confirm it appears instantly with no AI/Suggest step | ❌ **NOT EXERCISED** — no switchboard with a recorded section count was located on the tenant, and no NFPA 70B 28.3.2 issue could be raised against one. What *is* confirmed is the "instantly, no AI" half in general: rule-minted resolutions exist with `generated_by: "rule"`, are bound to a method, and carry labor directly (e.g. "Correct conductor / OCPD sizing for load" 180 min, "Restore permanent, continuous grounding path" 120 min, "Remake damaged termination" 45 min) — none of which required a Suggest step, and the Suggest button no longer exists. |
| 3 | Repeat on a single-section asset of the same class: labor must stay at the base 180 minutes, not be multiplied | ❌ **NOT EXERCISED** — same blocker. Note the 180-minute figure does appear as a rule-minted labor value ("Correct conductor / OCPD sizing for load"), which is consistent with an unmultiplied base, but the asset behind it has no section count so it proves nothing either way. |
| 4 | Repeat on a multi-section MCC and confirm the same scaling applies without any MCC-specific configuration | ⚠️ **CONFIGURATION CONFIRMED, SCALING NOT** — the no-special-configuration claim holds at the data level: **all 15 MCC methods** across nine services carry `unit_attribute: "sections"` and MCC's `unit_attributes_available` is `[{key:"sections", name:"Sections"}]`, identical to Switchboard's, with no MCC-specific breakout anywhere. But with no MCC carrying a section count, nothing multiplied. |
| 5 | Open a corrective fix from the catalog (including a class-agnostic one) and confirm the break/fix form renders its fields instead of coming up empty | ❌ **NOT REPRODUCIBLE ON QA** — the fix-flow procedure "Issue Resolution — Any Asset" (53 methods, the class-agnostic catalog) has **zero methods carrying `eg_form_keys`**, so no break/fix form is attached to any corrective method on this tenant and none can be rendered. This is the exact gap the ticket says it closes. |
| 6 | Negative: find an issue class whose matching rule has no method attached. The issue must fall through to the wildcard fix catalog and return a proposal — previously it returned nothing | ⚠️ **INDIRECT PASS** — the fix-flow procedure carries **50 rules**, and the editor warns explicitly about the two hazards this step is about: "*No catch-all rule: any asset matching none of these gets no work and stays off the work order*" and "*No rule performs \<method\> — it never runs*". In the survey, no issue with resolutions came back empty and rule proposals reached issues across several classes, so nothing was observed being swallowed. A rule deliberately saved with no method, to watch the fallthrough, was not authored. |
| 7 | Cross-check the AI path: ask the resolution agent for fixes on the same 4-section switchboard and confirm its labor figure matches the rule-minted one | ❌ **NOT EXERCISED** — no agent run is triggerable on QA (`POST /issue-resolution/reevaluate` → `{issues: 1, jobs: 0}`, no proposals produced), and there is no four-section fixture to compare on. Worth noting the two paths are structurally distinguishable today: rule proposals are method-bound (6 of 6), agent proposals are not (0 of 12). |

---

## Findings

### FINDING 1 (Medium) — the corrective fix catalog carries neither a unit attribute nor a break/fix form on QA
The PM procedures carry `unit_attribute: "sections"` widely (MCC 15/15, Switchboard 25/27), but the corrective fix flow's 53 methods carry **no `unit_attribute` at all** and **no `eg_form_keys`**. Those are the two things this ticket adds to corrective fixes — per-fix unit scaling and a break/fix form on every method. Neither is present here, which is why steps 2–5 could not be run. Either the change is not deployed to QA or the seeding did not reach this tenant's fix flow; a developer can distinguish those quickly.

### FINDING 2 (Low, data readiness) — no asset on the tenant records a section count
The scaling only pays off where section counts are populated, and on acme QA none are. Any test of the 720-vs-180 behaviour needs a fixture created first, which is worth noting for whoever picks this up: the assertion cannot be made against existing data.

---

## Test data — direct links (QA)
- The corrective fix flow (53 methods, no unit attribute, no forms): https://acme.qa.egalvanic.ai/services/0e5f33c1-f0f5-4834-af80-2c6010916d35 · spec: https://acme.qa.egalvanic.ai/api/procedures-v2/services/0e5f33c1-f0f5-4834-af80-2c6010916d35/export-spec
- MCC procedure carrying the section axis: https://acme.qa.egalvanic.ai/api/procedures-v2/procedures/fc16e95e-b7e6-4552-b249-1d496453fa4e
- Rule-minted resolutions used for the "no AI in the path" evidence: https://acme.qa.egalvanic.ai/api/issue/be56063e-e533-4661-9248-71b1638be06d/resolutions (Restore permanent, continuous grounding path — `generated_by: "rule"`, method-bound, 120 min)
- Classes: Switchboard `0555ec7e-7f97-411c-849a-daf394fbfbb8` · MCC `83d407ba-ba4b-43a7-8fac-402629fa7c9e`

Evidence: `docs/bug-evidence/zp-3941-per-section-pricing/api-captures.md` (the unit-attribute sweep) and `docs/bug-evidence/zp-issue-resolution-pricing-pull-through/api-captures.md` (the resolution survey and the fix-flow spec).

## Not covered / honest gaps
- **The headline 720-minute assertion and its 180-minute control** (steps 2, 3) — no asset with a section count exists.
- **The MCC scaling** (step 4) — same blocker.
- **The break/fix form rendering** (step 5) — no corrective method carries a form on QA.
- **A deliberately method-less rule** (step 6) — not authored; the fallthrough was judged from the editor's own warnings and from the absence of empty results in the survey.
- **The AI-vs-rule labor cross-check** (step 7) — no agent run is triggerable.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
