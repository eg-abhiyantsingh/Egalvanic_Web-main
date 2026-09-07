# 2026-09-07 — QA: Bulk-extraction review surfaces (backend #1166, frontend #1337/#1338)

**Prompt:** the merge-monitor ticket "Bulk-extraction review surfaces: complex-settings finder and critical-warning treatment" with its 5-step QA review; owner reminder mid-flow: "artifact".

## What was done
1. **Deployment fingerprint first** (the ticket said dev-only): API returned `included_seg_count` and honoured `complex=1` / `sort=segments`; the QA bundle contained the `Complex · N seg` chip, the `Complex (≥3 segments)` filter, `critical_warnings` red branch, `No SKM match — …`, `Mark reviewed`. All three PRs live.
2. **Found the real surface.** The Settings column, chip and sortable header live only on **`/ocpd-settings`** (Engineering → DESIGNATIONS → OCPD Settings); `/equipment-designations` (kind=all) has no Settings column. Column defs are per kind in the bundle; `d2r = 3`.
3. **Site survey** of all 238 SLDs (one `complex=1&per_page=1` call each) to find designated breakers with trip segments → tested filter/sort on "Abhiyant 21 may ios" (73 rows, 4 complex: 6/5/5/4 seg) and GF counting on Richmond (CT 450 = 4+2, CB 451 = 4+1).
4. **Filter + sort verified live** with a fetch shim recording the query strings the UI sends (`kind=ocpd&complex=1`, `sort=segments`, page 2 keeps the sort; second click clears it). **Boundary test:** edited the trip-settings checkboxes of asset "Test" 4→3→2 segments through the Edit Asset drawer; 3 stays in the filter with `Complex · 3 seg`, 2 drops out; restored to 4.
5. **Built nameplate fixtures** (QA had none): six synthetic breaker plates (PIL) attached via the real web path — SLD → Edit → Edit Asset → Asset Photos → Nameplate → upload → Save (`POST /photo/create`, type `node_nameplate`).
6. **Ran three real bulk extractions** (Bulk Ops → AI Extraction (N) → Bulk Extraction Job dialog; Step Function, claude-opus-5): honest `No SKM match — <mfr type>` rows confirmed (CB1/CB2/CB7), positive control CB4 bound to Powerpact HJ, CB5 pre-set to manual AIC 65 kA vs a 25 kA plate to force an interrupting-kA conflict.
7. **Mark reviewed** verified in the asset editor (endpoint `POST /extraction/eqp-lib/mark-warnings-reviewed` → `warnings_reviewed_at`; well + grid chip disappear, control row keeps its chip) and located in the bulk dialog ("Mark as reviewed" after Apply). Resume-previous-run prompt exercised.
8. **Critical path:** the pipeline never emitted `critical_warnings` (3 runs, 6 assets) → rendering verified by crafting the field on CB5 with the same node-update write the apply uses, plus bundle reading. Result: asset editor red well + one-click clear ✅; Equipment Designations grid red "Mismatch" chip ✅; **OCPD Settings grid shows no chip at all for a bound breaker with warnings** (the ocpd Device render returns a plain two-line cell for `skm_oid` rows) → Defect 2.
9. Verdict doc, evidence folder, artifact page, memory notes.

## Results (short)
- #1166 PASS · #1338 PASS · #1337 PARTIAL (no-match rows + Mark reviewed + red well/red chip on kind=all PASS; red critical path unreachable from the product on QA).
- **DEFECT 2 (High for the ticket's goal):** `/ocpd-settings` Device column renders bound breakers without any warning chip — CB5 with crafted `critical_warnings` shows red "Mismatch" on `/equipment-designations` but nothing on OCPD Settings.
- **DEFECT 1:** asset editor shows green **"LIBRARY MATCHED"** for a `no_match` eqp_lib (ZORVEX) — the surface #1337 forgot.
- **FINDING 3 (pipeline):** node AIC 65 kA vs bound frame 25 kA raised only an amber ordinary warning — the red treatment has no producer on QA.
- **FINDING 4:** agent inconsistency — same kA conflict is `no_match` on ABB E1.2 plates but a bound-with-warning on the Square D HJ plate.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-07-QA-bulk-extraction-review-surfaces-verdict.md`
- Evidence: `docs/bug-evidence/zp-bulk-extraction-review-surfaces/` (26 screenshots, 6 plates, run-1 JSON)
- Artifact: https://claude.ai/code/artifact/0c777ff4-e6a6-420a-affb-62461323d743
- Memory: `project_designation_views_and_bulk_extraction.md`

## Depth notes (learning — how to explain this)
- **Fingerprint before you believe the deploy note.** One API call (`?complex=1`) and one bundle grep settle "is it on QA" in a minute; the environment field was wrong for the ninth ticket running.
- **Test the rule at its boundary, not in the middle.** "≥3 segments" was proven with rows at exactly 3 (in) and 2 (out), produced by editing real trip segments — a 6-seg row passing the filter proves almost nothing about the threshold.
- **Count what the backend counts.** `included_seg_count` = primary + ground-fault segments; Richmond's CT 450 (6) and CB 451 (5) show the GF term is real, the ticket's "(primary + ground fault)" is honoured.
- **A fixture path is part of the test.** No nameplate photos existed on QA; discovering that the SLD Edit Asset drawer has a hidden `input[type=file]` per photo type unblocked a ticket family (ZP-3855/3856, #1058/#1246, #1077) that had been "not testable" since August.
- **When the trigger can't be reached, separate producer from consumer.** The UI's red treatment (consumer) was verified with a crafted `critical_warnings`; the missing part is the producer, and that is reported as a pipeline finding rather than a UI defect or a false PASS.
- **Positive controls everywhere:** CB4 (clean bind) beside CB1/CB2 (no match); un-reviewed CB1 beside reviewed CB2; unfiltered list beside the filtered one. Without them "chip absent" or "row gone" is unfalsifiable.
- **Cost awareness:** the Bulk Extraction dialog submits a billable job on open; three small runs (6 assets) were spent deliberately and the resume path was used instead of a fourth.
