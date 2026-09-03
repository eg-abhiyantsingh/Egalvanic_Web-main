# proposed_resolution not appearing until Issue refresh — root-caused (two defects, not one)

**Date:** 2026-09-03 · **Env:** QA only · **Prompt:** DevRev ticket paste (report builder / issue refresh)

## What was done
Full root-cause investigation of the DevRev ticket, live on QA: 1,041 issues, 106 report configs,
866 page templates, 50 issue classes interrogated through the app's own APIs (recorded from the UI,
never guessed — the two obvious GET paths are SPA-HTML 200 traps).

## Findings
1. **CONFIRMED — the field is not exposed.** `proposed_resolution` appears in the
   `available_fields` whitelist of 0 of 8 issue templates across all 106 configs (string absent from
   every config outright). The whitelist is hand-curated — the Quote Issue Report variant carries 5
   extra issue fields someone added — so this is a routine one-line extension that was never made.
2. **CONFIRMED — the reporter's template has a dead gate.** The resolution column sits inside
   `{% if "Recommendations" in details_by_key %}`; 0 of 84 issue-class property definitions on the
   tenant are named "Recommendations" (nothing matches /recommend/i), so the block can never render.
   Fixing #1 alone will not surface the column.
3. **REFUTED — the "until issue refresh" mechanism.** 530 never-edited issues hold a non-empty
   `proposed_resolution` (631/1041 total). The issue record is never stale; what can materialise on
   save is a `details_by_key` KEY (auto-fill / name re-join) — the gate, not the field.
4. **NEW DEFECT — the `details_by_key` namespace is polluted** (live): empty-string key,
   `"Problem Temp"` vs `"Problem Temp   "`, `"Reference Temp"` vs `"Reference Temp  "`,
   `"Current Draw (A)"` vs `"Current Draw (A) "`, case-dupe `replacement check` ×3; one class
   defines a property with no name. Templates keying on names silently miss rows.
5. "Proposed Resolution" is TWO fields (class property in `details[]` vs top-level column) with
   different values on the same issue — a naming collision the builder does not disambiguate.

## Limits (stated in the report)
Blank render explained but not witnessed: every issue-scoped HTML template on QA is a stub and all
substantive issue templates are DOCX (preview refuses non-HTML). Class definitions are per-tenant.

## Deliverables
- Artifact (H1 = verbatim ticket title, 2 real QA screenshots):
  https://claude.ai/code/artifact/867b7806-25d5-44a2-a4f4-07bda67860c9
- Verdict doc: docs/bug-reports/2026-09-03-proposed-resolution-field-gap.md
- **No PDF — owner directive mid-session ("no need for pdf, artifact is okay"), rule updated in memory.**
- Memory: project_reporting_engine_internals.md (endpoint contracts, two config generations, traps)
