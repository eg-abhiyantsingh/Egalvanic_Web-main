# [Web] SKM round trip shortens asset names to 25 characters: re-importing an unchanged export renames the asset

## Simple explanation
- **Export Engineering XML** writes each asset's name cut to its **first 25 characters**. PTW names are short, so the cut itself may be needed.
- Re-importing that same, unchanged file (Assets › SKM › Import (Full)) **overwrites the asset's name in eGalvanic with the cut 25-character name**.
- Example: "QA-DEMO BUG TX-1 long name round trip 13 Nov" (44 characters) becomes "QA-DEMO BUG TX-1 long nam".
- The review screen lists the row as a normal "Update". Both names appear side by side, but nothing warns that the eGalvanic name will be shortened.
- Names of 25 characters or fewer are not affected (control: "QA-DEMO BUG TX-2 short" stays as is). Any customer with long asset names silently loses the rest of each name on every round trip.

## Links (QA)
- Asset, now renamed: https://acme.qa.egalvanic.ai/assets/69c1dc30-da5a-4289-b638-6f585a5d5f92
- Control asset (short name, unchanged): https://acme.qa.egalvanic.ai/assets/f0d37de7-c1d1-409d-b01c-f2c2ada12c32
- SLD page for the export: https://acme.qa.egalvanic.ai/slds (site "QA-DEMO BUG SKM names 13 Nov")
- Assets page for the import: https://acme.qa.egalvanic.ai/assets

## Actual vs Expected
- **Actual:** after re-importing the unchanged native export, the asset's name is the 25-character SKM name. The rest of the eGalvanic name is gone, with no warning.
- **Expected:** re-importing an eGalvanic export keeps the full eGalvanic name. One option: skip the name update when the SKM name is just the cut-down version of the current name. At minimum, the review should warn "name will change from … to …" before committing.

## Test data
| Item | Value |
|---|---|
| Environment | QA, https://acme.qa.egalvanic.ai |
| Build | `index-C6DvLko9.js` (13 Nov 2026), re-proved 14:37 IST |
| Seat | Super Admin |
| Site | "QA-DEMO BUG SKM names 13 Nov", `98c231f6-96e7-4ddc-b138-cb3c65fb589c` (QA test data) |
| Asset TX-1 | `69c1dc30-da5a-4289-b638-6f585a5d5f92`, Transformer, 500 kVA, 5.75 %Z. Name before: "QA-DEMO BUG TX-1 long name round trip 13 Nov" (44 characters) |
| Control TX-2 | `f0d37de7-c1d1-409d-b01c-f2c2ada12c32`, Transformer, 75 kVA, 3.5 %Z. "QA-DEMO BUG TX-2 short" (22 characters) |
| Exported file | `skm_export_98c231f6_2026-11-13_090722.xml`, saved here as `QA-13Nov-BUG-skm-roundtrip-name-25-chars-exported.xml` |

## Steps (from a clean start, normal UI path)
1. Pick a site with an asset whose name is longer than 25 characters, for example a transformer named "QA-DEMO BUG TX-1 long name round trip 13 Nov". Open the asset and note its full name (screenshot 1).
2. Go to **SLD**, click **Export** › **Export Engineering XML**, then click **Export Engineering XML** in the "Before you export to SKM" dialog. The file downloads.
3. Open the file. The component header reads `s:name="QA-DEMO BUG TX-1 long nam"`: the first 25 characters only (screenshot 2).
4. Go to **Assets**, click **SKM** › **Import (Full)**, choose the same unchanged file, then click **Analyze**. The review shows "2 existing assets will be updated". The TX-1 row shows Name "QA-DEMO BUG TX-1 long nam", Action "Update QA-DEMO BUG TX-1 long name round trip 13 Nov", and no warning about the name (screenshot 3).
5. Click **Update 2 assets**.
6. Open the asset again. Its name is now **"QA-DEMO BUG TX-1 long nam"** (25 characters) (screenshot 4). The short-name control TX-2 is unchanged.

## Notes for the developer
- **Two halves.**
  - The export cuts `s:name` to 25 characters. That may be a PTW limit and fine on its own.
  - The importer treats the file's `s:name` as a rename for assets matched by Alternate UID (`s:altId`).
  - On a native eGalvanic round trip, the match is by `altId` (correct, no duplicates), but the label is overwritten.
- **Possible fix:** when an asset is matched by `altId` and the incoming name equals the current name cut to 25 characters, keep the current name. Otherwise show the rename in the review (old → new) so the user can see it.
- **Commit response** (`POST /api/skm/import-xml/commit`): `updated_nodes: 2`, plus the warning "assets matched an existing EG asset by Alternate UID and were updated in place". The name change is not mentioned.
- **Reproduced 3 times:**
  1. On "QA-DEMO ZP-5226 13 Nov transformer XR" (5 transformers, all long names cut) through the API.
  2. On the same site with a long/short pair through the API.
  3. This package, through the UI path above.
- **Found while testing ZP-5226** (transformer X/R in the SKM export). It is not caused by ZP-5226: the X/R part of that ticket passes.
- **Severity:** Medium (silent loss of user data on a documented round trip).
- **Suggested assignee:** Avani (backend import/export).

## JQL run (no existing ticket found)
- `project = ZP AND text ~ "SKM" AND (text ~ "truncat*" OR text ~ "25 char*" OR text ~ "label overwritten" OR text ~ "name length") ORDER BY created DESC`
- Related but different: ZP-3836, SKM round trip duplicates (Done; does not cover names); ZP-2120 / ZP-3595, "Export improvements (full asset names…)", which are about other exports.

## Evidence
- `QA-13Nov-BUG-skm-roundtrip-name-25-chars-video-steps.mp4` (58 s; contact sheet checked; no email shown).
- `…-1-asset-name-before.png`: orange = full name before.
- `…-2-exported-xml-name-cut-to-25.png`: red = TX-1 name cut to 25 characters; orange = TX-2 control.
- `…-3-import-review-no-warning.png`: red = review row with no warning about the name.
- `…-4-asset-name-after-reimport.png`: red = asset renamed to the 25-character name.
- `QA-13Nov-BUG-skm-roundtrip-name-25-chars-exported.xml`: the exported file.
