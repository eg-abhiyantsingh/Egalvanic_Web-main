# Cable designation summaries omit the conductor configuration — QA verdict

**Tested:** 2026-08-21 · **Build:** QA V1.36 (`index-C98MwrA7.js`) · **Tenant:** `acme.qa.egalvanic.ai`
**PR:** eg-pz-frontend **#1200** (display-only: add `conductor_desc` to LIBRARY MATCHED card + Equipment Designations grid)

---

## Verdict — **PASS (code-diff confirmed).** The exact 2-line change from PR #1200 is present in the live QA build, in the shared summary function all three surfaces use. Visual pixel-capture couldn't be staged on QA (approval-gated grid) and the ticket says it shipped prod-only (no prod login here) — but the diff itself is verified, which is what a display-only fix comes down to.

This is a pure display change — `conductor_desc` was already in the data; the PR (+2 lines, 2 files) adds it to two more render surfaces so all three read identically (`Copper · 3-1/C+G · Magnetic · Conduit · THHN · PVC`).

## ✅ The exact diff, confirmed live

`formatSkmDesignation()` in `skmDesignation.js` is the **single shared** summary builder for cable designations — the search-match card, the LIBRARY MATCHED card, and the Equipment Designations grid all call it. Its cable-subtitle array:

**Before (pre-fix, from the May-30 reference clone):**
```js
[conductor_type, duct_material, installation, insulation_class, insulation_type]   // no conductor_desc
```
**After (live QA bundle `index-C98MwrA7.js`):**
```js
e.eqp_lib.conductor_type, e.eqp_lib.conductor_desc, e.eqp_lib.duct_material,
e.eqp_lib.installation, e.eqp_lib.insulation_class, e.eqp_lib.insulation_type
```
`conductor_desc` is inserted **immediately after `conductor_type`** — exactly the "after material, before Magnetic" slot the ticket specifies, producing `Copper · 3-1/C+G · Magnetic · Conduit · THHN · PVC`. Because it's the one shared function, all three surfaces get it identically → the cross-surface consistency the ticket asks for is structurally guaranteed, not just coincidental. This confirms the fix is on **QA too**, not only prod.

The four behavioral sub-checks follow directly from this single shared function:
- **LIBRARY MATCHED == search-match, char-for-char:** same function → identical string. ✅ (structural)
- **grid row carries it:** same function feeds the grid summary. ✅ (structural)
- **empty `conductor_desc` → no dangling separator:** the subtitle does `.filter(Boolean).join(' · ')`, so a missing value is dropped before the join — no empty slot, no trailing separator. ✅ (confirmed in code)
- **non-cable summaries unchanged:** the change is inside the `cat === 'cables-skm' || 'busway-skm'` branch only; transformer/device branches are untouched. ✅ (confirmed in code)

## Supporting evidence

**1. The data carries `conductor_desc`.** A bound cable designation on SLD `1e1d7a5a` (ZTest_28_07), e.g. `Cable47`, returns from `GET /api/sld/{sld}/library-designations` an `eqp_lib` object containing:
```json
{"conductor_type":"Copper","conductor_desc":"3/C+G","duct_material":"Magnetic",
 "installation":"Conduit","insulation_class":"THHN","insulation_type":"PVC", ...}
```
So the field the ticket is about exists and is populated on QA.

**2. A shared normalizer also exposes it.** Besides `formatSkmDesignation`, the bundle has a helper that flattens `eqp_lib` onto the designation object with `conductor_desc` in its copied-field list — so downstream summary consumers have the field available too. Belt-and-suspenders with the subtitle fix above.

## ⚠️ The one thing I could NOT do (honest limit)
**No live pixel screenshot of the rendered card.** The Equipment Designations grid on QA only surfaces AI-extracted/approved designations; the bound cables I found are unapproved (`eqp_engineering_approved:false`), so the grid stays empty and the LIBRARY MATCHED card couldn't be reached without fabricating an approved cable. And the ticket says the change shipped **prod-only**, where I don't have login (CI-only creds). Normally I attach a real card screenshot; here the deliverable is the code diff instead — which for a 2-line display-string change is the actual substance of the fix, and I verified it against the *live* build (not just the stale clone).

## Method
Live QA + PR #1200 (2 files, +2 lines, merged to cicd/prod). Confirmed `eqp_lib.conductor_desc` in the data via `GET /sld/{sld}/library-designations`. Located the pre-fix cable-subtitle array in the May-30 reference clone (`src/utils/skmDesignation.js` `formatSkmDesignation`) — **no `conductor_desc`** — then found the same function in the live QA bundle `index-C98MwrA7.js` **with `e.eqp_lib.conductor_desc` inserted right after `conductor_type`**. Confirmed the empty-case (`.filter(Boolean)`) and cable-only scope by reading the function. Grid on QA is approval-gated (couldn't stage a visible card). No prod access (CI-only creds, not used). No data mutated.
