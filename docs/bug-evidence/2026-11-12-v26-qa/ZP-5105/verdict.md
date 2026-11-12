# ZP-5105 — Services share EG forms instead of owning per-service copies

**Verdict:** PASS on QA for every web row. The alias-tenancy gate, the template audit and the AI service builder (#149) are backend
and NOT WEB-TESTABLE. Build `index-C1ZRIunF.js` (checked 16:02, 16:38 and 16:47 IST, 12 Nov 2026). Super Admin seat.

**Recommended status:** READY TO RELEASE.

Test data (QA-DEMO only): new form "QA-DEMO ZP-5105 12 Nov shared form" (id 8e17c04f-a17f-4448-ac15-26666ef664f2, renamed during the
test to "… (edited 12 Nov)"). It is attached to two QA-DEMO services:
"QA-DEMO builder-dialog test - delete me" (https://acme.qa.egalvanic.ai/services/85d67289-a267-4028-877c-1721c697712c, Circuit Breaker ›
Insulation Resistance PM (LV Breakers)) and "QA-DEMO hand-built (delete me)"
(https://acme.qa.egalvanic.ai/services/a6342955-a6e4-4269-becb-c706dd91a3a8, Transformer › QA IR Scan).

## Checked
| Check | How | Result |
|---|---|---|
| Attach references the shared form, not a copy | Method editor › Attach existing form › "Attach" | PASS: the dialog now reads "Attach shares the form — edits to it show up in every service that uses it." with "Attach a copy" / "Attach" (2 and 5 Nov: "A copy is added…" / "Attach copy"). Request `{"mode":"link","source_form_id":"8e17c04f…"}` → 201 `form_id 8e17c04f…`. Company form count 671 → 671 after two attaches; exactly 1 row with that title, `service_id: null` |
| Edit the shared form once → every referencing service sees it | `PUT /api/eg-forms/8e17c04f…` (title only, the endpoint the form builder saves to), then reopened both services with no re-edit | PASS: both services' Forms panels show "QA-DEMO ZP-5105 12 Nov shared form (edited 12 Nov)" (shot 4, video steps 5–6) |
| Used-by label | Attach dialog chip and Builder › Forms tooltips | PASS: second attach shows chip "Used by 1 of your service" (hover = first service's name). Builder › Forms: Edit tooltip "Edit Form (shared — used by A, B)", Delete disabled with "Used by A, B — remove it from those services first" (shots 2–3) |
| Failed attach shows the server's message | Real failures are all API-only (random form id → 400 "source form not found"; unknown procedure → 400 "procedure 'zz…' is not on this service"; bad mode → 400 "mode must be 'link', 'clone' or 'blank'"). A duplicate attach from the UI is accepted (201). UI check: the attach POST was answered with that real 400 body | PASS (SIMULATED response): the dialog shows "source form not found", not the generic "Failed to add the form" (shot 5) |
| Old per-service copies | Builder › Services | The pre-fix copies (e.g. "QA-DEMO ZP-4684 … form", service_id = hand-built) are still there. Expected: no migration was promised |
| Global form | Attach dialog | Bundle: a global form shows "Global forms are read-only — attach a copy to customize it." Not clicked today |
| Roles | `POST …/services/<id>/forms` with a non-existent form (nothing can be written) | SA/PM/EE get past the permission check (400 body error). Client Portal → 422 `permission_denied … procedures.manage`, and CP also gets 422 on the services list. Correct |
| Duplicate attach | Same form, same method, again | 201, the method still lists the form once (idempotent). It does create a new service version (see suggestions) |
| Alias-matched spec forms gated by tenancy / template audit excludes unreadable-template applies | Backend (#1632), no web surface found in the bundle | NOT WEB-TESTABLE |
| Service builder (AI pipeline #149) references shared forms | Needs an AI "Update service" build job | Not covered (long-running AI job on shared infra) |
| Performance | Attach POST | 201 in < 1 s each; the dialog search is debounced (250 ms) and paginated (25 per page) |

## Bugs found
None.

## Enhancement suggestions
- Chip copy: "Used by 1 of your service" reads wrong. Use "Used by 1 service" / "Used by 2 services" (or "1 of your services").
- Duplicate attach: say "already attached to this method" instead of returning 201 and minting a new service version with no change.
  Today it silently adds a version to the service history.
- Show the used-by names on the service page's Forms panel as well (for example "also used by: …"), so a builder knows that an edit
  affects other services before opening the form.

## Mobile impact
None directly (builder is web-only). Work orders created after a shared-form edit should carry the edited form on iOS/Android too;
worth one check on the next mobile round.

## Not covered
Alias tenancy gate, template audit, AI service builder #149 (backend). Attaching a global form with "Attach" (link) vs "Attach a copy"
was not clicked.

## Evidence
- `QA-12Nov-ZP-5105-video-steps.mp4` (137 s): attach to service A → attach to service B (used-by chip) → API read-back card →
  Builder › Forms tooltips → one edit → both services show it → duplicate attach accepted.
- `QA-12Nov-ZP-5105-1-attach-dialog-shares-not-copies.png` (PASS), `-2-used-by-label-in-attach-dialog.png` (PASS),
  `-3-builder-forms-used-by-delete-blocked.png` (PASS), `-4-second-service-shows-edited-shared-form.png` (PASS),
  `-5-SIMULATED-failed-attach-shows-server-message.png` (PASS, simulated server answer).
- Raw: `.playwright-mcp/v26/eric7/v03-out.json`, `r07-out.json`, `r09-out.json`.
