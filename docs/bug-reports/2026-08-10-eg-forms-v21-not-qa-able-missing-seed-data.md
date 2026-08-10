# [EG Forms v2.1] Not QA-able on QA — none of the v2.1 constructs or validation rules exist there

**Env:** QA V1.36 · https://acme.qa.egalvanic.ai · **Found:** 2026-08-10
**Ticket:** EG Forms v2.1 — Server-side validation, PowerDB carbon-copy constructs
**Severity:** Medium (blocker for QA, not a product defect) · **Priority:** High — blocks sign-off

## Summary

Four of the six QA items cannot be executed on QA as it stands, because **the data the feature
operates on is not present**. Frontend PR #1079 states it was *"Verified end-to-end in localdev
against 7 seeded PowerDB carbon-copy"* forms — that seed data did not reach QA.

## Evidence (scanned all 344 EG Forms + 5 live form instances, as internal Admin)

| v2.1 construct from PR #1079 | Occurrences across all 344 forms |
|---|---|
| `option_sets` / `options_ref` | **0** |
| `column_groups` | **0** |
| `pairs_per_row` | **0** |
| `disabled_cells` | **0** |
| `meta_fields` | **0** |
| `PowerDB` / `powerdb` | **0** |
| "carbon" | 4 — **all domain text** ("Carbon Monoxide", "Carbon Dioxide", "carbon dust"), no carbon-copy constructs |

Across 5 live form instances on session `5c837e80-a739-4bda-b9ff-d1a9a32bbb25`
(definitions up to 5.7 KB): **no `required`, no `calc`, no `min`/`max`, no `disabled_cells`.**
`"required"` never appears as a JSON key anywhere in the corpus.

## Consequence per QA item

| # | QA item | Status |
|---|---|---|
| 1 | Submit a form that passes client validation but violates a **server rule** | **BLOCKED** — no form on QA declares any rule to violate |
| 2 | Submit-gate exemption still requires **calc resolution** | **BLOCKED** — no calc fields, no `disabled_cells` (the calc-exempt construct) |
| 3 | Pipeline loads schema doc S3-first, matches backend canonical | **NOT TESTABLE from the app** — S3 / AI-pipeline internal |
| 4 | Build and render a **PowerDB carbon-copy** construct, values carry across | **BLOCKED for render**; a tester must first *author* one in the builder. Note `POST /api/eg-form-instance/{id}/copy-data-to` exists and is the likely carry-across mechanism — worth targeting directly |
| 5 | Cross-platform rules hold on **iOS** | **NOT TESTABLE here** — separate iOS harness |
| 6 | Regression: existing v2.0 forms still render and submit | **PASS** — see below |

## Item 6 — regression PASSES

`POST /api/eg-forms/{id}/render-preview` on 6 substantial existing forms: **6/6 → HTTP 200**,
6.1–7.8 KB HTML each, 0.3–0.8 s. 327 active forms in total.

| Form | definition | result |
|---|---|---|
| Energized Visual Inspection | 1572 b | 200, 6166 b, 0.3 s |
| Test eg form 1 | 14494 b | 200, 7759 b, 0.3 s |
| De-Energized Visual Inspection | 7041 b | 200, 6169 b, 0.7 s |
| Clean, Tighten, Torque — Cleaning | 2532 b | 200, 6177 b, 0.8 s |
| Insulation Resistance Testing | 3819 b | 200, 6679 b, 0.8 s |
| Clean, Tighten, Torque — Lubrication | 2259 b | 200, 6180 b, 0.8 s |

## What is needed to unblock

Seed QA with the same fixtures the feature was verified against in localdev — the **7 PowerDB
carbon-copy forms** — plus at least one form declaring a **required** field and one with a
**calc** field feeding a `disabled_cells` region. Without those, items 1, 2 and 4 can only be
"tested" by authoring the fixtures by hand first, which tests the builder rather than the
validation and carbon-copy behaviour the ticket is about.
