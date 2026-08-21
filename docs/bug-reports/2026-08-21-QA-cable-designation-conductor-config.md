# Cable designation summaries omit the conductor configuration — QA verdict

**Tested:** 2026-08-21 · **Build:** QA V1.36 (`index-C98MwrA7.js`) · **Tenant:** `acme.qa.egalvanic.ai`
**PR:** eg-pz-frontend **#1200** (display-only: add `conductor_desc` to LIBRARY MATCHED card + Equipment Designations grid)

---

## Verdict — the fix's code path is PRESENT in the QA build; I could not stage a rendered cable card on QA to photograph it, and the ticket says the change shipped to **prod only** (which I can't log into). Net: **strong code-level PASS, visual confirmation deferred to prod.**

This is a pure display change — `conductor_desc` was already in the data; the PR adds it to two more render surfaces so all three read identically (`Copper - 3-1/C+G - Magnetic - Conduit - THHN - PVC`). There is no API contract to test; it can only be confirmed by (a) the summary-composition code and (b) a rendered card.

## ✅ What I confirmed on QA

**1. The data carries `conductor_desc`.** A bound cable designation on SLD `1e1d7a5a` (ZTest_28_07), e.g. `Cable47`, returns from `GET /api/sld/{sld}/library-designations` an `eqp_lib` object containing:
```json
{"conductor_type":"Copper","conductor_desc":"3/C+G","duct_material":"Magnetic",
 "installation":"Conduit","insulation_class":"THHN","insulation_type":"PVC", ...}
```
So the field the ticket is about exists and is populated on QA.

**2. The QA frontend build composes the summary WITH `conductor_desc`.** The live bundle `index-C98MwrA7.js` contains the shared helper that flattens `eqp_lib` onto the designation object every summary surface reads from. Its copied-field list is:
```js
const w = ["matched_cable_size","matched_cable_size_unit","conductor_type",
           "conductor_desc","insulation_class","insulation_type","voltage_rating",
           "installation","duct_material","connection_type","qty_per_phase"];
```
`conductor_desc` is in that list, positioned right after `conductor_type` — exactly the "after material, before the rest" slot the ticket specifies. Because this is the **single shared normalizer**, the search match card, the LIBRARY MATCHED card, and the grid all read `conductor_desc` from the same source — which is precisely the consistency the fix is meant to guarantee. This strongly indicates the fix (or its mechanism) is already in the QA build, not only prod.

## ⚠️ What I could NOT confirm (honest limits)

- **A rendered LIBRARY MATCHED card / grid row with the conductor config visible.** The Equipment Designations grid on QA only surfaces AI-extracted/approved designations; the bound cables I found are unapproved (`eqp_engineering_approved:false`), so the grid stays empty and the LIBRARY MATCHED card (which lives in the designation-match flow) couldn't be reached without fabricating an approved, AI-extracted cable — a heavy multi-step setup that still wouldn't be the prod build. **So I have no live pixel screenshot of the card**, which is normally my standard.
- **Prod itself.** The ticket says the change is on **cicd/prod only**. Prod credentials are CI-only and I do not log into prod, so I cannot verify the three prod surfaces directly.
- **The four behavioral sub-checks** (character-for-character match between LIBRARY MATCHED and search-match cards; grid row carries it; empty-`conductor_desc` renders no dangling separator; non-cable summaries unchanged) — these need the rendered cards, so they are **unverified visually**. The code does guard against the empty case: the normalizer copies a field only when `M !== "" ` and the value is a string/number, so a missing `conductor_desc` is simply omitted (no empty slot / dangling separator) — consistent with the ticket's negative case, confirmed by reading the helper, not by seeing it.

## Recommendation
Low-risk, display-only change; the QA build already contains the shared `conductor_desc` composition. To fully close, do the visual confirmation **on prod** (where it shipped) per the ticket: bind a cable, open the LIBRARY MATCHED card, and compare its string char-for-char against the search-match card, then check the grid row and a no-`conductor_desc` cable. I can't do that from here (no prod login). Nothing on QA contradicts the fix.

## Method
Live QA. Located a bound cable via `GET /sld/{sld}/library-designations` (confirmed `eqp_lib.conductor_desc`); drove the Equipment Designations grid + SLD views in-browser (grid empty — approval-gated). Read the live SPA bundle `index-C98MwrA7.js` and located the shared eqp_lib→summary normalizer with `conductor_desc` in its field list. No prod access (CI-only creds, not used). No data mutated.
