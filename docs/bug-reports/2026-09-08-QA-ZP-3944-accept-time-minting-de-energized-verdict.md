# [Web] Materials-library entries were minted during evaluation, and de-energized work had nowhere to be recorded

**QA verdict — the core regression PASSES cleanly: evaluating issues no longer touches the materials library. Two issues were evaluated with no acceptance and the library stayed at 17 entries with no write call, and evaluation-time materials carry `ml_id: null` — bound-only, never minted. De-energization is first-class: resolutions carry `de_energized`, the workbench shows a **De-Energized** chip, and the manual-resolution dialog offers "Requires de-energization (LOTO / outage window)". One important correction to the ticket: minting no longer happens at acceptance either. Accepting a resolution with an unpriced part minted nothing; the write happens at quote time, which is what the later ZP-3948 changed it to. So the checklist's accept-time bind/mint/dedupe steps describe behaviour this build no longer has.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · live UI with request capture, library counted before and after each step.

**Artifact:** https://claude.ai/code/artifact/82876c81-f854-4770-8536-df517a8fb445

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Migration `issres_a10_de_energized` runs on deploy — confirm it applied before testing | ✅ **PASS as far as the API shows** — `de_energized` is a first-class field on resolutions (`true` on both SCCR proposals, `null` on others) and on methods in the procedure payload. Whether the named migration is the one that put it there is not observable from the product. |
| 2 | Note the materials-library entry count, then evaluate several issues without accepting anything. **The count must not change — this is the core regression** | ✅ **PASS** — library at **17** entries before. Opened two issues (Repair Needed on Test asset, OSHA Violation on ATS Main) and ran **Re-evaluate** on each. Library after: **17** — unchanged, and **no non-GET call to `/api/materials-library` occurred at all**. Evaluation-time materials carry `ml_id: null` (e.g. "Grounding conductor / hardware"), i.e. unbound and unminted. |
| 3 | Accept a resolution whose part already exists in the library: it must bind to the existing entry (no new row) and pull that entry's price through onto the quote line | ⚠️ **SUPERSEDED — no minting or binding happens at accept on this build** — accepting "Restore permanent, continuous grounding path" (`POST /issue-resolution/{id}/accept` → 200, `accepted_at` set) left the library at **17** and the material still `ml_id: null, unit_cost: null`. The bind-and-price-through does happen, but **at quote time**: the Add-to-Quote dialog's Set-prices step collects the price and `POST /plans/from-issues {…, material_prices:{…}}` writes it onto the line and the library entry. Verified separately in the acceptance-to-quote run: a $45 unit cost entered there produced a $135 material line and the library entry now shows $45.00. |
| 4 | Accept a resolution whose part does not exist: exactly one new entry is minted, with the correct type/unit and typed catalog identity | ⚠️ **SUPERSEDED, same reason** — the accept minted zero entries. The equivalent at quote time was exercised in the acceptance-to-quote run. |
| 5 | Accept the same resolution twice, and accept a second resolution naming the same part with slightly different casing — dedupe on identity and name must prevent a duplicate | ⚠️ **NOT EXERCISED** — with nothing minted at accept, there was no duplicate to provoke on this path. |
| 6 | On a method the standard requires de-energized for, confirm the warning chip shows on the resolution card and the caption appears in the service overview | ⚠️ **PASS on resolutions; NOT OBSERVED on methods** — both SCCR proposals carry `de_energized: true` and render a **De-Energized** warning chip on the card, next to the title. But across all 39 MCC / Switchboard / Motor Controller / Panelboard procedures examined, **zero methods** carry `de_energized: true`, so the service-overview caption had nothing to display and the method-level seeding from the standards is not present on this tenant. |
| 7 | Tick and untick the de-energized checkbox in the manual-resolution dialog and in the procedure editor; reload and confirm the value persisted both ways | ⚠️ **CONTROLS CONFIRMED, PERSISTENCE NOT DRIVEN** — the manual-resolution dialog offers "**Requires de-energization (LOTO / outage window)**" and the method editor carries a `de_energized` field (it round-trips through the method save payload). A manual resolution was created with the box left unticked and came back `de_energized: null`. Ticking, saving and reloading in both places was not run. |
| 8 | Confirm all three set-paths land the flag: a rule-stamped resolution, an agent-set one, and a manually set one | ✅ **TWO OF THREE, and the third path exists** — **agent-set confirmed** (`de_energized: true` on the SCCR proposals). **Rule-stamped confirmed** — a 15-issue survey found six `generated_by: "rule"` proposals, and one of them, "Remake damaged termination" (45 min, method-bound), carries **`de_energized: true`**, so a rule does stamp the flag. **Manually set: not confirmed** — the manual resolution was created with the box unticked and came back `null`; ticking it was not driven. Distribution across the survey: `de_energized` true ×5, null ×14, false ×0. |
| 9 | Negative: a method with no de-energization requirement must show no chip and no caption | ✅ **PASS** — resolutions with `de_energized: null` (the Thermal Anomaly and OSHA proposals, and the manual one) render no chip, and no caption appears. |
| 10 | Precedent ranking: on an issue with both a documented service definition and a conflicting past job, confirm the agent follows the definition and cites the precedent narrative rather than adopting it | ⚠️ **NOT EXERCISED** — this needs an issue where a documented definition and a conflicting precedent both exist, and a fresh agent run to judge. No agent run could be triggered on QA in this session (`reevaluate` returns `jobs: 0`), so the doctrine could not be put to the test. |

---

## Findings

### FINDING 1 (Medium, documentation) — this ticket's accept-time minting has already been moved to quote time
The ticket's headline solution is "minting moved to acceptance", and steps 3–5 all test acceptance. On this build acceptance mints nothing: the write happens when the issue is quoted, which is exactly what the later ZP-3948 describes ("Minting moved from accept time to quote time"). Both cannot be current, and QA reflects the later one. Anyone working this checklist as written will conclude the feature is broken when it has in fact been superseded. The part that matters — **evaluation never writes to the library** — holds under both designs and is verified.

### FINDING 2 (Low) — method-level de-energization seeding is absent on this tenant
`de_energized` exists and works on resolutions, but no implementation method on any of the 39 procedures examined carries it. The ticket's carry-forward note already warns that the flag is seeded only where the standard is unambiguous and that "a missing chip is not proof that work is safe to perform energized" — on acme QA that caveat currently applies to every method.

---

## Test data — direct links (QA)
- Library counted before and after (17 → 17 through evaluation and through accept): https://acme.qa.egalvanic.ai/materials · API: https://acme.qa.egalvanic.ai/api/materials-library
- Evaluated without accepting: https://acme.qa.egalvanic.ai/api/issue/b83b932e-23d3-48d5-a3bb-48b90ce098e6/resolutions (Repair Needed) · https://acme.qa.egalvanic.ai/api/issue/be56063e-e533-4661-9248-71b1638be06d/resolutions (OSHA Violation — the one later accepted)
- Agent proposals carrying `de_energized: true` and the De-Energized chip: https://acme.qa.egalvanic.ai/api/issue/3449b323-6c48-4898-bf64-b536a95f2ffb/resolutions
- Manual resolution created with the checkbox unticked: https://acme.qa.egalvanic.ai/api/issue/b2a99a7f-e277-4a4a-a748-a9c038ffdc22/resolutions
- The quote-time minting path that replaced accept-time: `POST https://acme.qa.egalvanic.ai/api/plans/from-issues` (see the acceptance-to-quote verdict)

Evidence: `docs/bug-evidence/zp-3944-accept-time-minting-de-energized/` plus the shared session captures in `docs/bug-evidence/zp-issue-resolution-pricing-pull-through/api-captures.md`.

## Not covered / honest gaps
- **Accept-time bind / mint / dedupe (steps 3–5)** — not testable as written; the behaviour moved to quote time.
- **Ticking the de-energized box and reloading**, in either the manual dialog or the procedure editor.
- **The precedent-over-definition doctrine** — no agent run could be triggered.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
- Test data left in place and labelled: the OSHA Violation issue on ATS Main now carries an accepted resolution.
