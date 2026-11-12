# ZP-5110 — Auto-complete EG form sections when every visible input is filled (web)

**Verdict:** PASS on QA, build `index-C1ZRIunF.js` (checked 16:02 and 16:38 IST, 12 Nov 2026). Super Admin seat.

**Recommended status:** READY TO RELEASE.

Test data (all QA-DEMO, created today): work order "QA-DEMO ZP-5135 FLIR-SEP iOS 1.74 2 Nov"
(https://acme.qa.egalvanic.ai/sessions/96fdd76a-542f-45cf-bc92-569a00871bc4), new form instances on the QA-DEMO assets
"QA-DEMO 4 Nov ZP-5171 Main Distribution Panel" (Test form validation) and "QA-DEMO ZP-5204 5 Nov DP-1 copy". On that second asset is a
new QA-DEMO form "QA-DEMO ZP-5110 12 Nov conditional section v2" (id b9e5ab18…). Its section holds one always-hidden field and
one field that shows only when "Any visible damage?" = Yes.

## Checked
| Check | How | Result |
|---|---|---|
| Fill every visible input → section completes by itself | Test form validation › Inspection Information. Filled Name, Email, Date, Temperature and Condition | PASS: the footer turns into a disabled "Done — all fields filled" and the navigator shows a green check (shot 2) |
| One visible input blank → stays incomplete | Same, with Condition left empty | PASS: "Section Done" (not done) is still offered (shot 1) |
| Un-filling reverts | Cleared Temperature after the auto-done | PASS: back to "Section Done"; refilling it auto-completes again |
| Persists | Save 1 as Draft (PUT /eg-form-instance/bulk-patch 200), then closed and reopened | PASS: all 5 values kept, still "Done — all fields filled" (shot 3) |
| Hidden inputs do not block | QA-DEMO conditional form: `visible:false` field left empty | PASS: done with Inspector + "No" filled (shot 4) |
| Conditional input does not block while hidden, and blocks once shown | "Any visible damage?" = No → done; = Yes → "Describe the damage" appears empty → "Section Done" (not done); filled → done | PASS (shots 4–5, video steps 7–9) |
| Manual control still there | "Section Done" button and "Mark Not Done" are unchanged for manual use | Seen (manual path not re-clicked today; it was verified 2 Nov) |
| Performance | Network during the run | 197 API calls over 3 work-order loads and 3 form opens. No app call over 2 s; the only slow call was the third-party DevRev log ingest (2.5 s) |
| Flags | Bundle: auto-done lives in the EG form editor. No ticket-specific flag; the whole editor sits behind `feature-eg-forms` (ON for Acme) | No new flag to flip |
| Roles | Client-side logic, the same for every role that can edit forms | Not re-run per role (pure UI computation) |

Note on how I built the test form: a field's `visible` rule inside a section must use the full path
(`checks.has_damage === 'yes'`). My first draft used the bare key (`has_damage === …`), and that field never showed. That was my
form-definition mistake, not a product bug. The old QA form "QA-DEMO ZP-4717 visible normalization" only has conditional rules at
top level.

## Bugs found
None.

## Enhancement suggestions
- Give the green check next to an auto-completed section a tooltip ("All fields filled — done automatically"). The string already
  exists in the bundle. Right now the navigator check looks the same as a manual "Section Done".
- Form builder: when a `visible` rule names a sibling key inside a section without its section prefix, warn the author. That field
  silently never shows.

## Mobile impact
iOS viewer is ZP-5103: check the same three cases (all filled → done, one blank → not done, a hidden or conditional field does not
block) on the same QA-DEMO instances, so web and iOS agree on the section state of a draft saved on web.

## Not covered
- Whether blue "ghost" values (previous-visit preload) alone count as filled for auto-done. This needs a node with a prior
  submitted instance of the same form.
- Multi-edit (Bulk Ops › Edit with pinned forms): auto-done on each pinned card.
- Submit with every section auto-done: the "Submit with incomplete sections?" prompt should not appear. Not submitted, to keep
  the QA-DEMO drafts reusable.

## Evidence
- `QA-12Nov-ZP-5110-video-steps.mp4` (124 s): steps 1–9 with captions (contact sheet checked).
- `QA-12Nov-ZP-5110-1-one-input-blank-not-done.png` (PASS), `-2-all-filled-done-automatically.png` (PASS),
  `-3-after-save-draft-reopen-still-done.png` (PASS), `-4-hidden-and-conditional-do-not-block.png` (PASS),
  `-5-conditional-shown-empty-not-done.png` (PASS).
