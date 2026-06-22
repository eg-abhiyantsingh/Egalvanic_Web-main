# Class-taxonomy conformance tests for all three templates (node / edge / issue)

- **Date:** 2026-06-22
- **Prompt:** "these 3 files are most important — update the test case according to that" (`node_classes_template (13).xlsx`, `edge_classes_template (3).xlsx`, `issue_classes_template.xlsx`)
- **Type:** New deterministic API conformance tests, one per taxonomy.

---

## Goal

For each of the three source-of-truth templates, a test that verifies the **live tenant taxonomy
matches the template** — so drift (a class disappearing/renamed in the app) is caught.

## Why API-level (not UI)

Verified live that the Create-Issue MUI dropdown is unreliable to open in Selenium, and the
asset/connection class lists are large. The class catalogs are served by stable endpoints that
directly feed the UI — `GET /api/{node,edge,issue}_classes` — so the conformance is deterministic
there (no browser flake). Same pattern across all three; **subset checks**, so each tenant's junk
classes (`DEVTOOL_TEST*`, `Test*`, `QANode`, `QA_ATS1`, `Node Bus`, `Cable - Kurt`, …) are ignored.

## Tests added

| Test | Template | Endpoint | Expected |
|------|----------|----------|----------|
| `IssueClassContractApiTest` (prior commit) | `issue_classes_template.xlsx` | `/api/issue_classes` | 7 classes + NEC/NFPA/OSHA Subcategory has options |
| `NodeClassContractApiTest` | `node_classes_template (13).xlsx` (via gold) | `/api/node_classes` | **data-driven** from `node_classes_gold.json` (39 classes) |
| `EdgeClassContractApiTest` | `edge_classes_template (3).xlsx` | `/api/edge_classes` | Busway, Cable, DC Cable |

`NodeClassContractApiTest` reads the class names from `testcase/node_classes_gold.json` (which is
regenerated from template 13) rather than hard-coding 39 names — so it **auto-tracks** future
template/gold changes. Edge analysed first (new file): 3 classes, 16 attrs, structurally clean.

Wired into `suite-load-api.xml` (the CI "Load + API" group) and `suite-api.xml`. Committed the
`edge_classes_template (3).xlsx` so the source the test references is tracked.

## Validation (live + red-proofed)

- All three pass live: node `1s` (logged "All 39 gold node classes present… 59 distinct live"),
  edge `0s`, issue `0s` — deterministic, no browser.
- **Red-proofed** each: injecting a non-existent class makes the test fail with a clear
  "MISSING template-defined classes […]" message; reverted. (Node confirmed it loads all 39 from
  gold, not a vacuous empty-set pass — `loadGoldClassNames` throws `SkipException` if empty.)
- `mvn test-compile` clean.

## Live-tenant cleanup still outstanding (data-side, not test bugs)

The QA tenant carries junk classes not in the clean templates — node: `QANode`/`QA_ATS1`/`Test`/
`Node Bus`; edge: `DEVTOOL_TEST EdgeClass Updated`/`TestClass`/`NewNewClass`/`Test5`/`Cable -
Kurt`/`Cable - Simple`/`Cable Egan`; issue: `DEVTOOL_TEST IssueClass Updated`×9/`Test*`/`Other`.
Deleting those in Settings → Classes would make the live tenant match the templates exactly.
