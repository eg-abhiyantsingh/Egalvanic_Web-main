# ZP-4885 — Fuse class exports SKM ProtectionType "Low Voltage Fuse"

**Verdict:** PASS — QA, Web v2.6, build `index-C1ZRIunF.js` (12 Nov 2026, 16:35 IST), Super Admin seat.
(On 2 and 5 Nov the no-subtype fuse still exported "Fuse"; today the `fuselv_a1` data change is visible on QA.)

**Test data (own):** site "QA-DEMO v2.6 SLD editing 12 Nov" (SLD `c4438cd3-59b3-4639-9e60-2752dc5aa478`): QA26 FUSE nosub (class Fuse, no subtype),
QA26 FUSE LV sub (Fuse (<= 1000V)), QA26 FUSE HV sub (Fuse (> 1000V)), QA26 DC FUSE (class DC Fuse).
Export file kept here: `skm_export_c4438cd3_2026-11-12_110550.xml` (the zip also held "READ THIS BEFORE IMPORTING.pdf").

## Checked
| Check | How | Result |
|---|---|---|
| No-subtype fuse → "Low Voltage Fuse" | SLD › Export › Export Engineering XML › acknowledge › Export (zip downloaded, XML read) | PASS — line 104 `ProtectionType="Low Voltage Fuse"` |
| Fuse (<= 1000V) subtype still wins | same file | PASS — "Low Voltage Fuse" (line 132) |
| Fuse (> 1000V) | same file | PASS — "High Voltage Fuse" (line 139) |
| DC Fuse unchanged | same file | PASS — "DC Fuse" (line 146) |
| Fuse made before the migration | `POST /api/sld/363d5350…/export-skm` (QA-DEMO ZP-4800 SLD, same call as the UI; read-only) — QA4885 FUSE nosub created 2 Nov exported "Fuse" on 2 and 5 Nov | PASS — now "Low Voltage Fuse"; LV sub LV, HV sub HV, DC FUSE DC; 0 × "Fuse" in that file |
| No literal "Fuse" left | grep the whole XML | PASS — 0 occurrences of `ProtectionType` = "Fuse" |
| No class/subtype config holds exactly "Fuse" | `GET /api/lookup/node-classes` (64 classes incl. subtypes, EG-ACME) | PASS — Fuse class override = "Low Voltage Fuse"; none = "Fuse" (was "Fuse" on 5 Nov) |
| Export API | `POST /api/sld/{id}/export-skm` | 200 `{component_count, connection_count, filename, presigned_url, success, unconfigured_classes:[], warnings:[]}` |
| UI flow | Export menu: PDF / DXF / Engineering XML / Settings Verifier / JSON / Ground Fault JSON; acknowledgement dialog with 4 checkboxes | works; the dialog listed 2 topology errors from my own scratch data (not related) |

## Bugs found
None.

## Enhancement suggestions
None specific to this change.

## Mobile impact
None (backend export / class config).

## Not covered
- Importing the file into SKM PowerTools (no PTW on this Mac) — the "imports as a fuse, not a breaker" half needs PTW.
- Other companies' Fuse configs (democompany, eganco, espohio…) — only EG-ACME's classes are visible from this seat on QA.

## Proposed Jira comment
See `ZP-4885.txt`.

## Recommended status
READY TO RELEASE.
