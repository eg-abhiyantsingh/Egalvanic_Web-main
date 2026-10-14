# ZP-4592 — Tenant-gate the bulk-edit export / validate / process endpoints

**Verdict:** PASS (tenant gate) — build `index-D1IWZ6SI.js`. Fix already live on prod + cherry-picked to QA/dev. Permission `bulk_operations.manage` required on all three.

## Checked
| Check | How | Result |
|---|---|---|
| EXPORT own site | PM seat (is_eg_admin:false), `GET /bulk-edit/export?sld_id={own}` | 200 JSON, presigned .xlsx URL + stats. PASS (positive control) |
| EXPORT another company's site | PM/AM/EE/FM/CP + staff, `sld_id=f86052b7…` (company fdcadc86, NOT acme) | all → masked-404 HTML (refused). Even the staff seat is refused. PASS (shot -1) |
| EXPORT acme site NOT assigned to PM | PM, an acme site outside the seat's 116 | refused (masked-404). PASS |
| EXPORT random uuid (control) | PM | masked-404. PASS |
| VALIDATE own site + own file | PM multipart `/bulk-edit/validate` | 200 with a changes/conflicts/warnings diff. PASS (positive control) |
| VALIDATE foreign / unassigned / random site | PM (+ AM/EE/FM/staff) | all masked-404 (refused). PASS |
| PROCESS foreign / unassigned site | PM multipart `/bulk-edit/process` (own file, so no foreign write even if gate missed) | masked-404 (refused). PASS |
| Permission gate | CP (no `bulk_operations.manage`) | VALIDATE own AND foreign → **422 permission_denied, Required: bulk_operations.manage**. PASS |
| UI | PM, Assets > Bulk Edit > Bulk Import | Export downloads own site; import wizard opens. PASS |
| Perf | export own sites | 0.6–1.7 s each; validate 0.8–2.2 s. No slow (>2 s on the big site only). |

Malformed `sld_id` on export/validate/process → 500 `internal_error` (minor, see B1).

## Bugs found
**B1 (Low) — bulk-edit export/validate/process 500 on a malformed `sld_id`.**
- `GET /bulk-edit/export?sld_id=not-a-uuid` and the validate/process equivalents → 500 `internal_error`, not 400. No data exposure. Assignee: Avani. No existing ticket.

**B2 (Medium, data-integrity — same feature, not the tenant gate) — re-importing an UNCHANGED bulk-edit export is not idempotent: the Review step proposes phantom changes.**
- Steps: (1) Assets > Bulk Edit > Bulk Export for a site. (2) Without editing the file, Bulk Edit > Bulk Import, upload that exact file. (3) Skip the ambiguous name-reference, reach Review.
- Actual: "20 assets to update, 4 connections to update" with diffs like `voltage: (empty) → 240.0`, `ampere_rating: (empty) → 800`, `Catalog Number: 56 → 56.0`, `Size: 67 → 67.0`, `Serial Number: 89 → 89.0`. Export writes integers, validate re-reads them as floats, and empty cells come back as populated values → spurious diffs. Clicking **Process Import** would overwrite real data with these normalisations.
- Expected: an unchanged export round-trips to "No changes".
- Severity: Medium (silent data mutation risk on a high-impact write path; user must click Process). Evidence: `QA-12Oct-ZP-4592-5-unchanged-reimport-review-phantom-changes.png`. Assignee: Krunal (export/import formatting) + Avani (validate diff). No existing ticket. I did NOT click Process Import — no data mutated.

## Enhancement suggestions
- Bulk Import Review has no "download the phantom-diff / changes as CSV" and no "select which rows to apply" — add per-row apply checkboxes so a user can reject spurious diffs.
- Export filename of QA records includes the "QA-DEMO… (delete me)" label — fine, but a bulk-import dry-run/"diff only" mode would de-risk the round-trip.

## Mobile part for Nency
Bulk edit is web-only; no mobile surface. Nothing for Nency on this ticket.

## Not covered
Did not click Process Import (would mutate real site data). B2's "voltage empty→240" source (rules-engine recompute vs export gap) not root-caused — left for the dev.

## Proposed Jira comment
h3. QA — ZP-4592 PASS: bulk-edit export/validate/process are tenant-gated (QA build index-D1IWZ6SI.js)

From a customer seat (PM, is_eg_admin:false):
* EXPORT own site → 200 .xlsx URL (positive control). EXPORT another company's site ({{f86052b7…}}, company fdcadc86), an unassigned acme site, and a random uuid → all refused (masked 404). Even the staff seat is refused on the foreign site.
* VALIDATE / PROCESS behave the same — own site works, foreign/unassigned/random refused.
* A seat without {{bulk_operations.manage}} (Client Portal) → 422 permission_denied on validate.

!QA-12Oct-ZP-4592-1-export-tenant-gate-proof-PM.png|width=720!

Found two separate issues in the same feature (not the tenant gate): (1) a malformed sld_id 500s instead of 400; (2) re-importing an *unchanged* export proposes 24 phantom changes (int→float, empty→populated) — a data-integrity risk on Process Import.

!QA-12Oct-ZP-4592-5-unchanged-reimport-review-phantom-changes.png|width=720!

## Recommended status
**READY TO RELEASE** for the tenant-gate scope. File B1 (Low) and B2 (Medium, data-integrity) as separate tickets.
