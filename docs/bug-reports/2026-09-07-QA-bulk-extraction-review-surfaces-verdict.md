# Bulk-extraction review surfaces: complex-settings finder and critical-warning treatment

**QA verdict — backend #1166 PASS · frontend #1338 PASS · frontend #1337 PARTIAL: no-match rows and Mark reviewed PASS, red treatment PASS in the asset editor and on the Equipment Designations grid, but **on the OCPD Settings grid a bound breaker with warnings gets NO chip at all (neither red nor amber)** · 2 NEW DEFECTS (that missing chip; asset editor says "LIBRARY MATCHED" for a no-match) · 1 PIPELINE FINDING (a real interrupting-kA conflict never becomes a critical warning; the red path is unreachable from the product on QA)**

**Artifact:** https://claude.ai/code/artifact/0c777ff4-e6a6-420a-affb-62461323d743
**Tested:** 2026-09-07 · `acme.qa.egalvanic.ai` build **V1.36** · tenant acme (`d59d449b`) · Super Admin seat · live UI (Playwright-driven, visible browser) + live API + bundle `index-BV5BwzBG.js`.
**Ticket said "dev only, not yet promoted to QA" — wrong again.** All three PRs are live on QA: the API returns `included_seg_count`, honours `complex=1` / `sort=segments`; the bundle carries the `Complex · N seg` chip, the `Complex (≥3 segments)` filter, `critical_warnings` red rendering, `No SKM match — …` and `Mark reviewed`.

---

## Where the feature actually lives (read this before re-testing)

The ticket says "Equipment Designations grid". On V1.36 the designations grid is split into **kind views** under Engineering → DESIGNATIONS: Short-Circuit Ratings (`/short-circuit-ratings`), Feeder Schedule (`/feeder-schedule`), **OCPD Settings (`/ocpd-settings`)**, Transformer Schedule. `/equipment-designations` (kind = all) has **no Settings column at all** — the Complex chip and the sortable header exist **only on `/ocpd-settings`**. The filter-tray "Trip settings" select is present on every kind view, but only OCPD Settings shows the chip that explains the count.

Constant in the bundle: `d2r = 3` (= `COMPLEX_SETTINGS_MIN`). Chip label is `Complex · N seg` (middle dot, not the ticket's "Complex - N seg"). Chip colours: bg `#fff3e0`, text `#e65100` (amber).

---

## QA-review checklist — results

| # | Ticket step | Result | Evidence |
|---|---|---|---|
| 1 | Apply **Trip settings: Complex (≥3 segments)**; only rows with ≥3 segments (primary + GF) remain | ✅ PASS — site "Abhiyant 21 may ios": 73 rows → **4** (6 · 5 · 5 · 4 seg). Request: `…/library-designations?page=1&per_page=25&kind=ocpd&complex=1`. Boundary proven by editing trip segments on asset **Test**: 4→**3** seg stays in (`1–4 of 4`, chip `Complex · 3 seg`); 3→**2** seg drops out (`1–3 of 3`, no chip); restored to 4. GF counted: Richmond **CT 450** = 4 primary + 2 GF = **6**, **CB 451** = 4 + 1 = **5**. | `14-filter-complex-only`, `17-boundary-3seg-in-filter`, `18-boundary-2seg-excluded` |
| 2 | Sort the Settings column → most-complex-first; amber chip shows the count | ✅ PASS — header click sends `sort=segments`; order 6 · 5 · 5 · 4 · then 1-seg fuses (no chip) · then 0. Page 2 request keeps `sort=segments` (sort applied before pagination — API probe on "test site": p1 `[4,4,4,4]`, p2 `[4,0,0,0]`). Second click clears the sort (no ascending mode; `order`/`dir`/`-segments` are ignored server-side). Tooltip: "6 pulled-in trip segments — the configs the AI extraction is most likely to get wrong; verify against the device photos." | `15-sort-segments-desc` |
| 3 | Trigger a critical interrupting-kA mismatch → red in asset editor, designation chip ("Mismatch"), bulk dialog | ⚠️ **Could not be triggered from the product.** 3 real bulk extractions, 6 breakers, 5 synthetic nameplates designed to conflict (100 kA on an E1.2 whose frames stop at 65; 50 kA with no matching tier; a manual node AIC of **65 kA** against a 25 kA plate; a realistic multi-tier plate) → the agent never emitted `eqp_lib.critical_warnings`. It either refused to bind (`no_match`) or bound the kA-matching frame with an **ordinary** warning. Rendering contract verified instead: (a) bundle branches (`Fe.length>0 ? "Critical mismatch — needs review"`, `#c62828`/`#fdeded`, chip label `Mismatch`, critical items listed first); (b) **crafted `critical_warnings` on CB5 via the same node-update write the apply uses** → see "Crafted critical" below. | `24-asset-editor-critical-red-well`, `26-equipment-designations-mismatch-red-chip`, `27-ocpd-settings-bound-row-no-chip`; amber control `08-asset-editor-warnings-well-cb2` |
| 4 | `no_match` rows print "No SKM match — <manufacturer type>" | ✅ PASS — dialog rows: `No SKM match — ZORVEX Electric ZORVEX ZX-400-3P · 400A frame / 400A`, `No SKM match — ABB SACE Emax 2, E1.2, Ekip DIP LI · 250A frame / 250A`; matched control row `SCHNEIDER/SQUARE D Powerpact HJ · 150A frame / 150A`. Grid shows `—` in Device for no-match rows (honest). **But the asset editor still says "LIBRARY MATCHED"** — Defect 1. | `04-bulk-dialog-done-nomatch-highlighted`, `07-grid-after-apply` |
| 5 | "Mark reviewed" clears both lists | ✅ PASS — asset editor (CB2): click → `POST /extraction/eqp-lib/mark-warnings-reviewed {node_id}` → `{warnings_reviewed_at}` → well disappears without Save; grid chip gone on reload while un-reviewed CB1 keeps its chip. The well/chip condition is `(warnings || critical_warnings) && !warnings_reviewed_at`, so one click hides **both** lists. Bulk dialog exposes the same action per row as **"Mark as reviewed"** (after Apply, in the expanded warning panel). | `09-mark-reviewed-before`, `10-mark-reviewed-after`, `11-grid-after-mark-reviewed`, `20-bulk-dialog-expanded-mark-reviewed` |

### Crafted critical (rendering contract) — what the UI does when `critical_warnings` IS present
After run 2 left CB5 bound to Powerpact HJ with one ordinary warning, `critical_warnings` was written onto its `eqp_lib` with the same write the apply path uses (`PUT /api/node/update/{id}` with `X-Direct-Write`, body `{eqp_lib: {…existing, critical_warnings: [one line], warnings_reviewed_at: null}}` → 200, readable back after ~10 s). Then, live:

| Surface | Result |
|---|---|
| **Asset editor** (SLD → Edit Asset → ENGINEERING) | ✅ red well `#fdeded` / border `#ef9a9a`, heading **"CRITICAL MISMATCH — NEEDS REVIEW"** (`#c62828`), the critical line first in red, the ordinary line below in grey, one **Mark reviewed** button (`24-asset-editor-critical-red-well`). |
| **Mark reviewed** | ✅ one click → `POST /extraction/eqp-lib/mark-warnings-reviewed {node_id}` → both lists disappear together, editor shows only LIBRARY MATCHED + TRIP CONFIGURATION (`25-asset-editor-critical-after-mark-reviewed`); grid chip on Equipment Designations gone. Un-review exists too: same endpoint with `{unreview: true}` (the bulk dialog's "Undo" link) → `warnings_reviewed_at: null`. |
| **Equipment Designations grid** (`/equipment-designations`, kind = all) | ✅ red chip **"Mismatch"** (`#fdeded` / `#c62828`), tooltip "Critical mismatch — needs review" listing the critical line first; CB1 beside it keeps the amber **"Incomplete"** (`26-equipment-designations-mismatch-red-chip`). |
| **OCPD Settings grid** (`/ocpd-settings`) | ❌ **no chip at all** for the same row — see Defect 2 (`27-ocpd-settings-bound-row-no-chip`). |
| **Bulk Extraction Job dialog** | not observable with crafted data (rows come from the job store); bundle branch verified: `Fe.length>0 → "Critical mismatch — click to review"`, icon `error.main`, critical items listed before ordinary ones. |

CB5 is left in this state (un-reviewed, crafted critical line clearly labelled) so the developers can open it.

---

## Defects and findings

### DEFECT 1 — Asset editor shows a green "LIBRARY MATCHED" card for a `no_match` result
**Where:** SLD → Edit → Edit Asset drawer → ENGINEERING, on any breaker whose extraction ended in `eqp_lib.no_match: true` (CB1, CB2, CB7 on Android Site 2).
**Repro:** 1) `https://acme.qa.egalvanic.ai/sld?focusNode=7ee495b9-7b81-4e59-a58c-26e5ccb01ba9&sldId=aadcee4c-7dd0-45b3-81b9-309c5c166084` → Edit → the CB2 drawer opens. 2) Scroll to ENGINEERING.
**Actual:** green check + **"LIBRARY MATCHED — ZORVEX Electric / ZORVEX ZX-400-3P — ZXB4003"**, directly above the amber "Incomplete information — needs review" well whose first line says "No SKM match…".
**Expected:** the same honesty #1337 gave the bulk dialog — a "No SKM match" card (no green check, no `skm_oid`), so a reviewer opening the asset does not read a fabricated manufacturer as a designation. The grid already agrees (Device = `—`), so the editor is the one surface still "looking designated".
**Severity:** Medium — this is exactly the confusion the ticket set out to remove, on the surface where the engineer edits the device.

### DEFECT 2 — OCPD Settings never shows the warning chip for a **bound** breaker (red or amber)
**Where:** `/ocpd-settings`, Device column. **Repro:** open OCPD Settings on Android Site 2 and look at **CB5** (bound to Powerpact HJ, un-reviewed, `critical_warnings` + `warnings` present) — no chip. Switch to `/equipment-designations`: the same row shows the red **Mismatch** chip. CB1 (a `no_match` row) shows amber **Incomplete** on both views.
**Why (bundle):** the `ocpd` column set overrides the Device render: `if (eqpLib.skm_oid) return Et(title, subtitle)` — the plain two-line renderer with **no** warnings chip; only the fallback `vt(tn)` (used for un-bound / no-match rows) carries the `Mismatch`/`Incomplete` chip. The base `ln` column used by kind = all and the feeder column both render the chip.
**Impact:** the OCPD Settings grid is the breaker review surface this ticket equips with the Complex chip, yet a designated breaker whose extraction raised warnings — including a critical interrupting-kA mismatch — is indistinguishable there from a clean one. Reviewers must leave the view to see the flag. Severity: **High** for the ticket's stated goal.

### FINDING 3 (pipeline, not UI) — a genuine interrupting-kA conflict never becomes a critical warning
CB5 was set by hand to **AIC Rating 65 kA** (manual OCPD entry, `ocpd.aic_rating = 65`), then extracted from a PowerPacT HJ plate reading **25 kA @ 480 V**. The agent bound the 600 V / 25 kA frame and produced only an **ordinary** warning about the 480 V/600 V tier; the node now carries `aic_rating 65` vs `frame.kaic 25` and renders **amber "Incomplete"**, not red. Across 3 runs / 6 assets, `critical_warnings` was never populated. The red treatment (#1337) is wired and correct, but on QA nothing upstream produces the signal it renders. Worth a question to the extraction-pipeline owners: what emits `eqp_lib.critical_warnings`, and is it deployed to the QA agent-runner?

### FINDING 4 (agent consistency, for the pipeline team)
Same class of plate conflict, two behaviours: E1.2 plates with a kA no E1.2 frame offers → **refused** (`no_match`, "identity contradiction"); HJ plate with a kA that exists only at another voltage tier → **bound** the kA-matching frame at the "wrong" tier + warning. Reviewers will see the first as "No SKM match" and the second as a designation with an amber chip.

### Notes (not defects)
- Agent warning strings are 300–900 characters and render verbatim in the grid tooltip and the well (readable, but the tooltip covers three rows).
- The Bulk Extraction Job dialog **starts a billable run the moment it opens** unless a previous run still has staged results; the "A previous extraction still has N unapplied results — Resume that run / Start new" prompt worked (run #2 resumed after the dialog had been closed mid-run).
- `complex=0` / `complex=true` are silently ignored (only the literal `1` filters) — consistent with the other filters on this endpoint.

---

## How the fixtures were built (all through the real UI)
QA had **no asset with a nameplate photo**, so six synthetic breaker nameplates were rendered (PIL) and attached through the only web path that exists: SLD → Edit → Edit Asset → **Asset Photos → Nameplate → upload → Save Changes** (`POST /photo/create`, type `node_nameplate`). Then Bulk Ops → tick rows → **AI Extraction (N)** → Bulk Extraction Job (`POST /extraction/bulk-job/submit`, Step Function `eg-pz-qa-ai-bulk-extraction-sfn-ohio`, model `claude-opus-5`, ~2 min for 3 assets) → Apply (`POST /extraction/asset-agent/apply`).

| Asset (Android Site 2) | Plate | Agent result |
|---|---|---|
| CB1 | ABB Emax 2 E1.2 N 250, Ekip DIP LI, **Icu 100 kA** | `no_match` + 2 warnings (no E1.2 frame offers 100 kA) |
| CB2 | fictitious **ZORVEX ZX-400-3P**, 35 kA | `no_match` + 1 warning — the honest-row case |
| CB4 | Square D PowerPacT **HJL36150**, 25 kA | **bound** Powerpact HJ, 150 A / 600 V / 25 kA, no warnings (positive control) |
| CB5 | same HJ plate, node pre-set to **AIC 65 kA** (manual) | bound HJ + ordinary warning; **no critical** |
| CB7 | ABB E1.2, **Icu 50 kA** | `no_match` + 2 warnings |
| CB8 | HJ plate with realistic 240/480/600 V table (65/25/18 kA) | **bound** Powerpact HJ 150 A / 600 V / 25 kA, confidence high, **no warnings at all** (agent noted the plate table is "offset one voltage column from SKM's HJ data" and still raised nothing) |

Job ids: run 1 `002070e5-beee-42f9-b6e6-6b50f2f970e8` (3 assets), run 2 `86c4e56e-b091-4ded-89b0-3c0633535a9c` (2), run 3 `bulk-job` for CB8 (1 asset, 150 s).

---

## Test data — direct links (QA)
- OCPD Settings grid (active site is picked in the rail): https://acme.qa.egalvanic.ai/ocpd-settings
- Android Site 2 designations API (complex + sort): https://acme.qa.egalvanic.ai/api/sld/aadcee4c-7dd0-45b3-81b9-309c5c166084/library-designations?kind=ocpd&complex=1&sort=segments&per_page=100
- "Abhiyant 21 may ios" designations API (4 complex rows): https://acme.qa.egalvanic.ai/api/sld/c041dfd1-b66a-41e8-8b14-f65bc6d195a5/library-designations?kind=ocpd&complex=1&sort=segments&per_page=100
- Richmond, CA (GF-counted rows CT 450 = 6, CB 451 = 5): https://acme.qa.egalvanic.ai/api/sld/768ff683-4244-4fa0-aced-36ed471f0c33/library-designations?complex=1&per_page=100
- CB1 (no_match, 100 kA plate): https://acme.qa.egalvanic.ai/assets/673505c4-3e7a-4bfd-b569-b831daced7ef · editor https://acme.qa.egalvanic.ai/sld?focusNode=673505c4-3e7a-4bfd-b569-b831daced7ef&sldId=aadcee4c-7dd0-45b3-81b9-309c5c166084
- CB2 (no_match ZORVEX, reviewed — Defect 1 repro): https://acme.qa.egalvanic.ai/assets/7ee495b9-7b81-4e59-a58c-26e5ccb01ba9 · editor https://acme.qa.egalvanic.ai/sld?focusNode=7ee495b9-7b81-4e59-a58c-26e5ccb01ba9&sldId=aadcee4c-7dd0-45b3-81b9-309c5c166084
- CB4 (bound HJ, control): https://acme.qa.egalvanic.ai/assets/e78230ed-b0c1-4b81-a8a1-f80ec46d4252
- CB5 (AIC 65 vs frame 25, bound HJ, **crafted critical left un-reviewed — Defect 2 repro**): https://acme.qa.egalvanic.ai/assets/9cdfce8d-97b4-488a-a0da-142fc026eda5 · editor https://acme.qa.egalvanic.ai/sld?focusNode=9cdfce8d-97b4-488a-a0da-142fc026eda5&sldId=aadcee4c-7dd0-45b3-81b9-309c5c166084
- CB7 (no_match 50 kA): https://acme.qa.egalvanic.ai/assets/5433d920-767f-4402-b4a4-1d73abdaba67
- CB8 (multi-tier plate): https://acme.qa.egalvanic.ai/assets/144f7944-b2fc-4e7a-b087-1d006b9cc02c
- Boundary asset "Test" (Abhiyant 21 may ios, restored to 4 seg): https://acme.qa.egalvanic.ai/sld?focusNode=a46cc279-c917-44a4-a067-30e556f3e8f8&sldId=c041dfd1-b66a-41e8-8b14-f65bc6d195a5
- Run status: https://acme.qa.egalvanic.ai/api/extraction/bulk-job/status?execution_arn=arn%3Aaws%3Astates%3Aus-east-2%3A165183897698%3Aexecution%3Aeg-pz-qa-ai-bulk-extraction-sfn-ohio%3A002070e5-beee-42f9-b6e6-6b50f2f970e8 (run 1) · …%3A86c4e56e-b091-4ded-89b0-3c0633535a9c (run 2)
- Photo check: https://acme.qa.egalvanic.ai/api/photo/by_entity/673505c4-3e7a-4bfd-b569-b831daced7ef (type `node_nameplate`; `?types=nameplate` returns `[]`)

Evidence folder: `docs/bug-evidence/zp-bulk-extraction-review-surfaces/` (26 screenshots, the six synthetic plates, run-1 status JSON).

---

## Not covered / honest gaps
- The **red "Critical mismatch" row inside the Bulk Extraction Job dialog** was not seen live: dialog rows come from the job's staged results, which cannot be crafted. Verified by bundle reading only (`Fe.length>0 → "Critical mismatch — click to review"`, `error.main`, critical items listed before ordinary ones).
- `critical_warnings` **production** (which pipeline rule emits it) — outside the web app; see Finding 2.
- Roles other than Super Admin were not exercised on these views (gate is `features.equipment_designations.view` + `eng-lib`, unchanged by this ticket).
- Feeder/SCCR/Transformer kind views: the Trip-settings filter select is present there too; chip/sort are OCPD-only by design (column defs per kind) — not tested beyond presence.

## Method notes
Fetch-shim request capture on the SPA (`library-designations` query strings, `/extraction/*` bodies and responses), API cross-checks on every UI claim, positive controls (matched device, un-reviewed row, unfiltered/sorted list), boundary edit at exactly the threshold, and the site survey of all 238 SLDs for designated breakers (`complex=1&per_page=1` per site: "test site" 5, "Abhiyant 21 may ios" 4, "15 June 2026" 4, Testoffline_25_05 3, Chicago 3, Richmond 2).
