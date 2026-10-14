# ZP-4750 — SKM export: option to omit the building from arc-flash label numbers

**Verdict:** PASS — build `index-D1IWZ6SI.js`.

## Checked
| Check | How | Result |
|---|---|---|
| Option appears next to the label-from-location option | Assets > SKM > Export, and the SLD ack dialog | "Omit building name (Floor/Room only)" sits indented under "Generate arc-flash label numbers from location". PASS |
| Omit disabled until location ON | default dialog | Omit checkbox is **disabled** until "from location" is ticked (code + UI). PASS |
| Option OFF (default) keeps the building | `?af_label_from_location=true` only | labels are `Building/Floor/Room` exactly as before. PASS |
| Option ON omits the building | `?af_label_from_location=true&af_label_omit_building=true` | `QA Bldg-North/Fl 1-2/Rm A-B` → `Fl 1-2/Rm A-B`; `Navratna Park/A-101/R-102` → `A-101/R-102`; DCBus `F2/R2`. Labels stay unique/ordered. PASS |
| Omit without location (no effect) | `?af_label_omit_building=true` alone | no labels at all; output byte-identical to the default export. PASS |
| Setting round-trips | toggle, close, reopen the dialog/ack | persisted in `localStorage` (`skmExport.labelOmitBuilding`); the SLD ack dialog reopens with the options remembered. PASS |
| Non-arc-flash exports unaffected | diff default vs omit-only | identical except AF label lines. PASS |
| API query contract | export-skm | `af_label_omit_building=true` honoured only when `af_label_from_location=true`; `=false`/`=yes` treated truthily as the backend expects ("true" required). PASS |
| Tenant/site scope | PM vs staff | export-skm leaks cross-tenant/cross-site — cross-cutting finding, not this ticket. |

## Bugs found
None introduced by this ticket. (The stale "-1, -2" tooltip belongs to ZP-4746; the cross-tenant export IDOR is filed separately.)

## Enhancement suggestions
- The two label options are the only things in the Assets export dialog besides scope; a one-line live preview of the resulting label for the first bus would remove all doubt about building-in/out.

## Mobile part for Nency
Backend + a frontend toggle; no mobile export surface. Nothing for Nency.

## Not covered
Did not open in PTW. Real PTW-authored file not exercised.

## Proposed Jira comment
h3. QA — ZP-4750 PASS (QA build index-D1IWZ6SI.js)

The SKM export dialog shows "Omit building name (Floor/Room only)" indented under the label-from-location option, disabled until that option is on. With it OFF the labels keep the building; with it ON the building segment is dropped ({{Building/Floor/Room}} → {{Floor/Room}}, DC buses too) while staying unique and ordered. Toggling it doesn't touch non-arc-flash exports, and the choice round-trips in the dialog.

!QA-12Oct-ZP-4750-4-PASS-xml-omit-building-richmond.png|width=720!
!QA-12Oct-ZP-4750-1-PASS-omit-disabled-until-location-on.png|width=720!

## Recommended status
**READY TO RELEASE.**
