# ZP-4684: [Web] Service apply keeps .docx form templates instead of regenerating HTML

**Verdict: PARTIAL.** The positive (.docx) path is NOT WEB-TESTABLE on QA today; the negative (HTML) path passes through the live API.
Build `index-D1IWZ6SI.js` (19:15 IST). The change is backend only (eg-pz-backend #1483, `service_spec_service.py`) and has no flag.

## Why the .docx path cannot be tested on QA
- None of acme's 649 EG forms has a .docx template: every `form_template.s3_key` ends in `.html` (`GET /api/eg-forms`, all pages).
- None of the 19 services' current spec carries a .docx form. Spec forms carry `template_html`, for example NETA Testing v2 has 92 forms, all HTML.
- The web cannot create one. The form/report template builder shows DOCX as **disabled** (`ConfigBuilderModal.jsx`: `{value:"docx", disabled:true}`). The service create/update dialogs accept .docx only as AI *input* documents. The ticket names "Console task #129", so the .docx templates come from the internal console, which QA does not use.
- I did NOT point a QA-DEMO form at one of the 19 shared .docx report page templates (cover pages, RFQ details). Before the fix, an apply could overwrite that shared template for every report that uses it.

## Checked (negative path: HTML forms behave as before)
Service: **QA-DEMO hand-built (delete me)** (mine). Form: **QA-DEMO ZP-4684 html form (delete me)** `0db85564-cd22-4ccf-8742-26562aa230b7`.

| Check | How | Result |
|---|---|---|
| Add a form to a service (applies a version) | `POST /procedures-v2/services/{id}/forms {mode:blank}` → 201, version 10 | PASS: form created with an HTML template (`…html`) |
| Re-apply the same version (spec has no `template_html`) | `POST …/versions/{v10}/apply` → 200 `forms_updated:1` | PASS: same template id `ee95138c…`, HTML regenerated to a new file (`aca9c9652243.html`) |
| Round-trip export-spec (with HTML) → new version + apply | `GET export-spec` (form now carries `template_html`, 7,617 chars) → `POST …/versions {spec, apply:true}` → 201 | PASS: same template id, HTML file rewritten (`d8e646ff200a.html`) |
| Template stays linked across applies | `form_template_id` identical after all three applies | PASS |
| UI | Service page shows the form; Version history lists the applies (screens 1–2) | PASS |
| Roles | Services are Admin/Super Admin only (route gate); tested as Super Admin | n/a for other roles |
| Performance | apply 0.4–1.0 s | OK |

API log: `QA-12Oct-ZP-4684-api-log.txt`.

## Bugs found
None proven.

## Observation for the developer (not a defect claim)
Every apply rewrites the form's HTML file to a new S3 key, even when the spec carries the same `template_html`. If hand-tuned HTML layouts are stored only in that file, re-activating an older version whose spec has no `template_html` (versions saved by "Add form" store none) will regenerate the HTML. Worth one unit test next to the .docx one.

## What is needed to finish
A dev-seeded EG form with a .docx template, wired to a method of a QA-DEMO service on acme QA. Or the console steps to attach one (task #129). Then: apply that service's version and confirm `form_template.s3_key` still ends in `.docx`; add the service to an asset in a work order and confirm the minted form instance has `template_format: docx` (the Export Forms dialog then offers DOCX).

## Enhancement suggestions
1. Show the template type (HTML / DOCX) next to each form in the service's Forms panel and in Version history, so admins can see which forms are Word-templated before they apply a version.
2. Version history: show "forms updated: N" from the apply result, so a template rewrite is visible.

## Mobile part for Nency
None directly. If a .docx-templated form reaches a work order, the iOS/Android form fill and submit must still work; the template affects only export.

## Not covered
The .docx keep path (see above).

## Proposed Jira comment
```
h3. QA result: PARTIAL, positive path not testable on QA (build index-D1IWZ6SI.js, 12 Oct)

*Why:* acme QA has no EG form with a .docx template (all 649 forms use HTML templates), and the web cannot attach one (the DOCX option is disabled in the template builder; .docx files on a service are AI input only). The .docx templates come from the console (task #129).

*Checked (HTML path, behaves as before):* on QA service QA-DEMO hand-built (delete me) I added a form, re-applied the version, and posted the exported spec back as a new version. The form kept the same template each time and its HTML was regenerated normally.

!QA-12Oct-ZP-4684-1-service-html-form-after-applies-PASS.png|width=720!
!QA-12Oct-ZP-4684-2-service-version-history-applies-PASS.png|width=720!

*Needed to finish:* please seed one .docx-templated EG form on a QA-DEMO service on acme QA (or share the console steps). QA will then apply the service and confirm the .docx template is kept and the work-order form instance reports template_format docx.
```

## Recommended status
**stay In QA** (needs a .docx-templated form seeded on QA)
