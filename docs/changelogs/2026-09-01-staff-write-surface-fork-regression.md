# Staff write surface (ZP-3874 follow-on) — fork-helper regression on QA

**Date:** 2026-09-01 · **Env:** acme.qa.egalvanic.ai (V1.36) · **Outcome:** PASS, no defects filed

## What was asked

QA the "Staff tooling could only read" ticket (eg-pz-backend #1073/#1074, promoted by
#1080/#1081/#1082) now that it is In QA.

## How the scope was decided

The ticket's own QA Review section draws the line: the eight new `/staff/*` write routes stay
**inert** outside dev while `EG_STAFF_ALLOWLIST` is unset, so their positive paths are not
reachable on QA. The **exception it names explicitly** is the fork-helper regression —
`fork_config_into_company` and `fork_global_form` were extracted out of two *live customer*
endpoints (`/reporting/configs/<id>/fork`, `/eg-forms/<id>/fork`) that had zero test coverage, and
those now run the shared code on every branch regardless of the allowlist.

So the testable, valuable work on QA was the regression, plus confirming the staff gate holds.

## What was done

1. Report-config fork of the heaviest **global** config (10 templated pages) — global→company is the
   dangerous direction, since a leaked template ref on a global reaches every tenant.
2. EG Forms fork of a global carrying **76 node-class pairs** — chosen because node-class mappings
   decide where a form is offered.
3. Isolation proved by before/after hashing the source, each paired with a **positive control** that
   the write to the copy actually landed (otherwise "source unchanged" proves nothing).
4. Reachability proved the way the ticket means it: `available-for-node` on a real Transformer asset
   offers the fork and correctly supersedes the global.
5. Probed all nine `/staff/*` routes for the inert state.

## Result

Every isolation property holds. The specific failure the ticket warns about — a fork whose pages
still name the source's template keys — does not occur: **0 of 10** keys shared, **0 of 10**
`page_template_id`s shared, template rows created once each in the destination, and the copy renders.
Form fork relinks its own template, carries 76/76 node classes, is reachable, and re-forks
idempotently (201 create → 200 no-op, same id, no duplicate).

Staff routes all return `401 eg_staff_denied` as **JSON** — which confirms both that #1081 actually
deployed them to QA (a JSON refusal, not this host's `200 + SPA HTML` for unmatched paths) and that
`require_eg_staff` holds with the allowlist unset.

## Three false leads, each killed by a control

Worth recording, because each would have been a wrong bug report:

- Fork has `service_id: null` → **277/277** pre-existing overrides are too. By design.
- Fork missing from the EG Forms library grid → the grid shows **330** of **647**, excluding *all*
  overrides including the 277 that predate this branch. Pre-existing scoping.
- Global source shows "No pages yet" in the builder → an **untouched** control global resolves 0/9 of
  its templates in the catalog, same as the forked source's 0/10. Pre-existing, unrelated.

Plus a near-miss: the first template lookup keyed on `template` when the field is `s3_key`, so both
copy *and* source read as "MISSING". Only including the source as a control revealed the bug was in
the query, not the data.

## Deliverables

- `docs/bug-reports/2026-09-01-QA-staff-write-surface-fork-helper-regression.md`
- `docs/jira-export/…​.pdf` (7 pages, 2 real screenshots) for drag-drop into Jira
- `docs/bug-evidence/staff-write-fork-regression/` — 4 live captures
- QA Review Board rebuilt (56 reports)

## Test-data footprint

EG Forms fork removed (`PUT {is_deleted:true}`; `DELETE` is 405 and `/delete` hits the SPA shell),
which restored the global to the library and the asset's offered list — it had been superseding a
global for the whole shared tenant. Report-config fork left in place, clearly labelled
"QA-DEMO … (delete me)".
