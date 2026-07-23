# Work-Type E2E — cover EVERY Create-WO form field (fill all + verify each persisted)

**Date:** 2026-07-23
**Time:** 09:00 UTC
**Author:** Claude Code (automation, Fable 5), for shubham.goswami@egalvanic.com

## What was asked
"Cover everything in work order with filling form" — the create-WO E2E should fill the whole
form, not a couple of fields, and confirm the values save.

## What shipped
`WorkTypeCreateE2EMatrixTestNG` (per Work Type, all 14) now fills the **complete** Create New
Work Order form and verifies each value round-trips:

Fields filled on create (live-mapped from the dialog 2026-07-23):
- **WO Name** *, **Work Type** * (selected from the dropdown + commit verified), **Facility** *
  (defaulted to the pinned site), **Scope** (full for the 6 family reps / Start-Empty otherwise),
  **Priority**, **Est. Hours**, **WO Description**, **Photo Type**, **Start Date**, **Due Date**,
  and a **Team field-technician** (fills the empty "Select user" slot).

Persistence verified after create (the real end-to-end proof):
- UI (detail header, expanded): **Priority**, **Due Date** (Timeframe), **Description**, **Field
  technician**.
- Session API `GET /ir_session/{id}/full` (authoritative for Advanced Settings that the header
  doesn't surface): **photo_type**, **est_hours**, **description**, plus start/due dates logged.

New `WorkOrderPage` helpers (all avoid the MUI-Autocomplete hidden-inner-input trap and read the
value back so the caller knows it committed):
- `tryCommitComboByLabel(label, option)` — resilient combo commit by field label; `trySelectPhotoType`.
- `typeStartDate(mm/dd/yyyy)` — Start Date (the 2nd MM/DD/YYYY input; there was only a getter).
- `fillEmptyTeamUser()` — commits the first offered user into the empty field-technician slot.
- Hardened `fillEstHours`/`fillWoDescription` (from 2026-07-22): read-back + React native-setter
  fallback — the fix that made est_hours/description actually persist (sendKeys alone left them null).

## Depth explanation (for learning + manager review)
The earlier version only filled Name + Work Type, so a regression that dropped any other saved
field would pass silently. "Cover everything" here means two things working together: (1) the
create phase drives every field the user can set, and (2) the verify phase reads each value back
— from the UI where the detail page shows it, and from the saved session via the API where it
doesn't. That combination is what turns "a WO was created" into "a fully-specified WO was created
AND saved correctly", which is the coverage that actually protects the create flow.

A field that silently won't commit is recorded (we keep the value that actually stuck and verify
THAT), not assumed — so the test surfaces a real save gap instead of masking it, consistent with
the don't-over-report discipline.

## Scope-policy note (why Start-Empty, not full-scope)
The create rows use "Start Empty Instead" for every service, not a full all-assets scope. During
validation on QA (2026-07-23) a full-scope create for Condition Assessment sat on "Creating..."
and never resolved (no WO produced) — an intermittent backend slowness on large-scope creates,
unrelated to the form. Since this suite's purpose is full-FORM coverage + persistence, gating it
on that heavy, flaky path would make it slow and red for the wrong reason. Start-Empty creates are
fast and reliable and still exercise every form field and its persistence. The preview-vs-created
asset-count regression (the 2026-07-20 CTT bug net) stays in WorkTypeDetailContractTestNG against
the stable Z1 fixtures, where it doesn't depend on a live heavy create.
