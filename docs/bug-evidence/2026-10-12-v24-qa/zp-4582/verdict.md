# ZP-4582 — SKM import: namespace SKM ids per project so cross-project studies import correctly

**Verdict:** PARTIAL — build `index-D1IWZ6SI.js`. The core namespace model works; two real issues (a NULL-namespace wildcard reachable via the API, and the preview count/button ignoring the chosen namespace), plus a cross-cutting import-scope IDOR. All import tests ran ONLY on my own `QA-DEMO ZP-4582 SKM namespace (delete me)` site (cfc07c0d…), created for this.

## Checked
| Check | How | Result |
|---|---|---|
| Review dialog requires an SKM project | Import (Full) on QA-DEMO-PROJ-A | "SKM project" is a required field; commit button disabled while empty. PASS |
| Prefilled from study header (falls back to filename) | 3 header probes | all three suggested the upload **filename**; my synthetic PTW project-folder comment was not parsed (fallback path only — real PTW header not available, so folder-extraction unverified). |
| Lists existing SLD namespaces with overlap counts | second import (project B) | dropdown shows "QA-DEMO-PROJ-A — 4 assets"; helper "4 SKM ids are also used by another project here and will be created as separate assets". PASS |
| Two projects, same skm ids, one SLD → no false merge | imported A then B (both ids 1-4) | 8 separate assets, connections wired within each project; export renumbers the clashes and warns ("6 assets shared an SKM id … exported under a new id"). PASS |
| Second import, SAME project → merges | re-import A (rev2, ids 1-4, project A) | 0 created, "4 components already on this SLD from this project will be linked, not duplicated"; existing nodes kept. PASS (but see B1) |
| Legacy NULL-namespace rows adopted on re-import | committed C via API without a namespace, then re-imported C as project C | the NULL rows matched and were adopted into project C. PASS (back-compat path) — but the same mechanism is abusable, see B2 |
| SKM ID · namespace caption on imported assets | asset Engineering tab + asset-form Engineering header | "SKM ID 2 · QA-DEMO-PROJ-A" shown. PASS |
| Export renumbers duplicate ids + warns | export the SLD holding A/B/C/D collisions | 18 components, 18 unique s:id, warning present. PASS |
| Roles | SA/Admin/PM/AM/EE/FM have the SKM menu; CP absent; Tech restricted | import gated by `feature-skm-import` + `nodes.manage`. PASS |
| Flags OFF (simulated) | `feature-skm-import`/`-legacy` false | SKM dropdown drops the import items (fails closed). PASS |
| Tenant / site scope | PM (customer) seat | **import preview+commit succeeds on a site the PM is not assigned to** (created a node on my QA-DEMO site, PM not in its access list) — cross-cutting IDOR, see B3. |

## Bugs found
**B1 (Medium, frontend) — the preview count tiles and the commit button ignore the chosen namespace.**
- Steps: with project A already imported, Import (Full) the same study and type the existing name "QA-DEMO-PROJ-A".
- Actual: tiles read "4 New assets", button reads "Import 4 assets"; the commit then creates 0 and only links (helper text is correct: "4 components already on this SLD from this project will be linked"). The counts/button are computed at preview time before a namespace is chosen, so they over-count.
- Expected: once an existing project is selected, the tiles/button reflect update-vs-create ("Update 4" / "Import 0 new, update 4").
- Severity: Medium (a user clicks "Import 4 assets" expecting new rows). Evidence: `QA-12Oct-ZP-4582-5-FAIL-...png` + `-6-PASS-...` (result). Assignee: Krunal.

**B2 (Medium, backend) — a commit WITHOUT `skm_namespace` still creates NULL-namespace rows, which then act as a wildcard any later project adopts.**
- Steps (API; UI blocks the empty field, the API does not): `POST /skm/import-xml/commit` with no `skm_namespace` → created QDE-* with namespace NULL. Then imported a DIFFERENT study "QA-DEMO-PROJ-F" (same ids, different equipment): created_nodes 0, skipped_existing_nodes 4 — the unrelated QDE-* rows were silently taken over by project F.
- Actual vs expected: NULL-as-wildcard is the documented back-compat for *pre-#76* rows, but a NEW namespaceless commit re-creates the exact ambiguity #76 set out to fix. The backend should require a namespace on new commits (the UI already does).
- Severity: Medium (silent cross-project merge). Evidence: `QA-12Oct-ZP-4582-10-FAIL-...png`. Assignee: Avani.

**B3 (High, backend, cross-cutting) — `/skm/import-xml/preview` and `/commit` ignore site assignment (and company).** See the shared finding below; demonstrated here by a PM writing a node to a site it is not assigned to.

## Enhancement suggestions
- Dropdown should mark the auto-suggested project name as "(suggested from file)" so users don't assume it's an existing project.
- A "this will MERGE into project X" confirmation when an existing name is picked would de-risk accidental merges (the heuristic caveat the ticket calls out).

## Mobile part for Nency
Import is web-only; the `skm_namespace` column and "SKM ID · namespace" caption are new on assets — Nency: confirm the iOS/Android asset view shows (or at least doesn't break on) the namespaced skm_id.

## Not covered
Did not verify project-folder extraction from a real PTW header (only the filename fallback). Did not import anyone else's site (only my QA-DEMO site).

## Proposed Jira comment
h3. QA — ZP-4582 PARTIAL (QA build index-D1IWZ6SI.js). All imports on a throwaway QA-DEMO site.

Core namespace model works: the review dialog requires an SKM project (prefilled, existing names selectable with overlap counts); two studies from different SKM projects that reuse ids 1-4 import as separate assets with connections wired within each project; re-importing the SAME project merges in place; legacy NULL-namespace rows are still adopted; assets show "SKM ID n · <project>"; export renumbers duplicate ids and warns.

!QA-12Oct-ZP-4582-1-PASS-import-review-project-prefilled.png|width=720!
!QA-12Oct-ZP-4582-9-PASS-xml-namespaces-and-renumbered-ids.png|width=720!

Two issues:
# Picking an existing project still shows "4 new assets / Import 4 assets" though the commit links 0 (helper text is right). Counts/button are computed before the namespace is chosen.
# {{POST /skm/import-xml/commit}} without {{skm_namespace}} (the UI blocks this; the API doesn't) creates NULL-namespace rows that a later, unrelated project silently adopts — re-introducing the per-project ambiguity #76 fixed.

!QA-12Oct-ZP-4582-5-FAIL-same-project-button-says-import-4-assets.png|width=720!
!QA-12Oct-ZP-4582-10-FAIL-null-namespace-commit-swallows-other-project.png|width=720!

## Recommended status
**Stay In QA** — fix B1 (count/button) and B2 (require namespace on new commits). The import-scope IDOR (B3) is filed on its own.
