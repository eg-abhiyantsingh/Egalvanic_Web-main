# Deep review C: 7 rewritten bugs, checked as a skeptical developer and a non-technical manager (14 Oct 2026)

Tickets: ZP-4869, ZP-4870, ZP-4871, ZP-4876, ZP-4880, ZP-4882, ZP-4893. **Nothing was written to Jira.**

- **QA builds:** most checks ran on `index-G6uKkjPx.js`. QA redeployed during the review (around 10:00 UTC) to `index-D-HsTlcR.js`. All 7 bugs were checked again on the new build, and all 7 still reproduce (runs R1 and R2).
- **Method per ticket:**
  - Read the live ticket (no developer comments on any of the 7).
  - Looked at every referenced image and both videos (frames extracted).
  - Opened every Test data URL as the stated role (run H1).
  - Followed the Steps exactly as written, as the stated role, with a positive control.
  - Checked for duplicates with JQL. None found. ZP-4803's "PDF parity" covers dual offsets and merged terminals, not hidden-connection tags.
- **Headed Chrome runs:** `.playwright-mcp/v24/deep-review-C/` (scripts `a1`–`h1`, `r1`, `r2`; JSON results in `out/*.out`).
- **Roles used:** Project Manager `abhiyant.singh+project@`, Super Admin `abhiyant.singh+admin@`, Client Portal (one probe only). QA only.
- **Data safety:**
  - ZP-4880: the future-date Save is refused (400), so nothing was stored.
  - ZP-4876: the SKM imports were review only, closed with Cancel (`POST /skm/import-xml/preview`, no commit). The asset classes were unchanged afterwards.
  - Exports (PDF, SKM) and reads only otherwise.
  - The form builder sends an automatic `PUT /api/eg-forms/<id>` when *Edit Form* opens. No field was changed.
- **Outputs:**
  - Corrected payload for the FIX tickets: `docs/bug-evidence/2026-10-14-deep-review/payload-C.json`. It is an array of {key, summary, desc, files_to_attach}; files_to_attach holds absolute paths.
  - New and replacement images: `docs/bug-evidence/2026-10-14-deep-review/ZP-48xx-*.png`. Each was drawn with PIL or in the browser and checked by eye.
  - Every `!image!` in a payload description is either already attached to that ticket (exact filename checked against the live attachment list) or listed in files_to_attach.
  - Video links `[^future_date_infrared.mov]` and `[^email_show.mov]` are kept. No `{` inside `{{}}`.

| Ticket | Verdict | One line |
|---|---|---|
| [ZP-4869](https://egalvanic.atlassian.net/browse/ZP-4869) | **FIX** | Still reproduces (fresh PDFs on both builds, pixel-identical). Problems: the screen tag was called "fully readable" but is shortened with "…"; the step said "Site Data › SLD" (no such menu item for a PM); the text and image screenshot numbers did not match (3b/4/5 vs 4/5/6); and the tag starts mid-symbol, not "at the left terminal". New key image, mock-up header, canvas shot and 2 PDF crops. |
| [ZP-4870](https://egalvanic.atlassian.net/browse/ZP-4870) | **FIX** | Still reproduces (S1 only "Select...", M1 0 options, M3 no boxes; S2 lists 10 items). Problems: screenshot labels did not match the steps ("Screenshot 4" shown at step 5; "AUDIT …" captions); the chip said "11 option(s)" while the text said "10 items"; the "Settings > EG Forms" caption should be Builder › Forms. Real reach was not stated: only 1 of 660 forms (our API-built test form) uses a users list. |
| [ZP-4871](https://egalvanic.atlassian.net/browse/ZP-4871) | **FIX** (text only) | Still reproduces. Step 4 cannot be done as the stated Project Manager (no Builder menu, "Access Denied" on /eg-forms); it now names the Super Admin. Census numbers refreshed: 42 of 660 forms have no key, 35 real (was 40/658/33). Added the "asset" row control. |
| [ZP-4876](https://egalvanic.atlassian.net/browse/ZP-4876) | **FIX** | Still reproduces: the same 12 class changes. The steps no longer work as written, because Export Engineering XML now downloads a **.zip** and Import (Full) refuses it. Other fixes: "confirm" is really two "I understand…" ticks; "4 assets changed" was really 3; the "Class / subtype" column does show the new class (off-screen) and the ticket now says so; 6 of the 12 are flagged as "generic loads" in the export PDF (a dev would raise it). Before-classes screenshot added; "Out of scope" captions replaced. |
| [ZP-4880](https://egalvanic.atlassian.net/browse/ZP-4880) | **FIX** | Still reproduces. The Actual-result quote showed "—" while the screen shows the literal `\u2014`. Step 3 missed that the Maintenance tab opens a menu (Maintenance / Inspections / Infrared). The step screenshots were unhighlighted video frames; they are replaced by 4 fresh highlighted Project Manager shots on QA PNL1. |
| [ZP-4882](https://egalvanic.atlassian.net/browse/ZP-4882) | **FIX** (one link) | Wording bug still reproduces (18 issues, same 4 warnings). Everything else checks out. Only real defect: the step 3 link is dead. Jira linked `…/assets/f556e269-…dae):` with the "):" included, which opens "This asset link doesn't look right". |
| [ZP-4893](https://egalvanic.atlassian.net/browse/ZP-4893) | **FIX** | Still reproduces for a customer seat (PM): 246 users plus "None", 57 shown as emails. The steps named no role, and the Switchboard-1 link does not open for a PM; now PM on QA PNL1 with 4 clean highlighted shots (the old shot 3 had a pop-up over the emails). Panelboard-1 had no URL (dropped). "customer addresses" became "outside email domains". Never says "other companies". |

No ticket is INVALID-NOW.

---

## ZP-4869: SLD PDF export, hidden-connection tag under the symbol

**Reproduction.** As the Project Manager on site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)":
- The connection `30fcae76…` is hidden (`edge_manips {"hidden":true}`). It runs from the SRC CB bottom terminal to the JBOX left terminal.
- UI path Export › Export PDF › Export Entire SLD › Generate PDF. Request body: arch-d, omit_text false, engineering_mode true. `sld-export-2026-10-14.pdf` downloaded in 21 s.
- At 300 dpi the PDF shows "-DEMO 4591 SRC CB (delete me)" at the JB symbol.
- Measured: the black square spans x 323–426 px, and the visible text starts at 430 px at about 9.6 px per character. So the hidden "TO QA" (about 48 px) starts near the symbol's centre (375 px), not at the left terminal.
- Control at the SRC CB end, "TO QA-DEMO 4799 JBOX (delete me)", is readable. It is centred on its terminal.
- The re-export on `index-D-HsTlcR.js` gives a pixel-identical crop.

**Images.**
- Key image: the box is OK. Its caption said "starts at the LEFT terminal", which the measurement above does not support.
- Screenshot 1 caption and chip said the canvas tag is "fully readable". On screen it reads "QA-DEMO 4591 SRC CB (delet…": GoJS `isOverflowed=true`. The canvas tag also has an arrow and no "TO" prefix.
- The step text said Screenshots 4/5/6 while the images say 3b/4/5.
- The Screenshot 5 control drew the JBOX defect in green.
- The "Export PDF" chip in Screenshot 2 covers the next menu item. This is cosmetic, so the image is kept.

**URLs.** /slds and the JBOX asset (`525225bf…`, "QA-DEMO 4799 JBOX (delete me)", class Junction Box) open as PM.

**Steps.** The Project Manager's menu has no SLD item: Site Data has no SLD link, and only the staff Engineering rail has SLD. "Site Data › SLD" is therefore wrong, and the link works.

**Wording.** "so the name cannot be read" in the title overclaims: most of the name is readable. The dev note "fixed right-of-terminal offset" did not fit the control (centred tag) and has been rewritten as observed fact.

**Payload changes:**
- Title: "[Web] SLD (one-line diagram) PDF export: at a left-side terminal, the "TO …" name tag of a hidden connection is printed under the asset symbol, so the start of the name is hidden".
- Simple explanation, Actual and Expected corrected.
- The SLD page now opens by link.
- Steps 3 and 3b merged, so the image numbers match: 1, 2, 3/3b, 4, 5.
- Re-check line covers both builds.

**Attach:**
- `ZP-4869-key-highlighted-v2.png`
- `ZP-4869-expected-mockup-v2.png` (header no longer claims "same as on the screen")
- `ZP-4869-1-canvas-tag-left-of-symbol.png`
- `ZP-4869-4-pdf-jbox-tag-under-symbol.png`
- `ZP-4869-5-pdf-control-other-end-readable.png`

The PDF shots come from the 14 Oct export. Existing `5-2`, `5-3` and `5-3b` are reused.

**Now unreferenced (optional removal):** `5-1-…`, `5-4-…`, `5-5-…`, `ZP-4869-key-highlighted.png`, `ZP-4869-expected-mockup.png`.

## ZP-4870: form builder Web View, users-list fields empty

**Reproduction.** As the Super Admin: Builder › Forms (`/eg-forms`), test form `dc24bb98…`, *Edit Form*, *Web View*.
- S1 shows ["Select..."].
- M1 has 0 options.
- M3 renders 0 inputs.
- S2 has 11 options: "Select..." plus 10 items.
- Control: the same S1 inside WO `22a22b9c…` › More › Forms › 13N-H1-1 has placeholder "Type to search..." and 246 options.
- Same result on `index-D-HsTlcR.js`.

**Skeptical-developer check (reach).**
- On 14 Oct only **1 of 660** forms on Acme QA uses `options_source: "users"`: this API-built QA-DEMO form.
- The 13 Oct builder bundle's source picker offers only Test Equipment and Linked Asset.
- The builder code already handles users-source select fields (a "Default to the signed-in user" option), and the renderer supports `users`. So it is a valid but low-reach gap.
- The ticket was already Low. The Simple explanation now states the reach plainly.

**Images.**
- `6-4` is captioned "Screenshot 4" but was referenced as Screenshot 5.
- `4870-1` and `4870-2` carry "AUDIT …" captions with no screenshot number.
- `6-1` says "Settings > EG Forms" (the UI shows Builder › Forms).
- The key image and `4870-2` chips said "11 option(s)" next to text saying "10 items".
- All are fixed in the replacements. Screenshot 3 is a fresh 14 Oct capture. The key image has its two chips corrected.

**URLs.** `/eg-forms` and WO `22a22b9c…` open as Super Admin. `/eg-forms` gives "Access Denied" for a Project Manager, now noted in Test data.

**Attach:**
- `ZP-4870-key-highlighted-v2.png`
- `ZP-4870-1-forms-list-test-form.png`
- `ZP-4870-3-web-view-users-field-empty.png`
- `ZP-4870-4-control-test-equipment-field-lists-items.png`
- `ZP-4870-5-control-work-order-form-lists-users.png`

Existing `6-2` and `ZP-4870-expected-mockup.png` are reused.

**Now unreferenced:** `4870-1-…`, `4870-2-…`, `6-1-…`, `6-3-…`, `6-4-…`, `ZP-4870-key-highlighted.png`.

**Side note (not part of the ticket):** opening *Edit Form* sends an automatic `PUT /api/eg-forms/<id>` (175 B) even with no change.

## ZP-4871: WO Forms tab merges keyless forms

**Reproduction.** As the Project Manager, WO "QA-DEMO ZP-4801 Tuned column (delete me)" › More (Tasks, "Forms 3") › Forms:
- "Form Types" shows only "QA-DEMO ZP-4717 visible normalization (delete me)", with 2 classes and 3 rows.
- The first 13N-H1-1 row opens "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)". The "asset" row opens the ZP-4717 form.
- Same result on `index-D-HsTlcR.js`.

**Problems.**
- Step 4 (the Builder › Forms control) cannot be done as the stated PM: `/eg-forms` gives "Access Denied" and there is no Builder rail. Screenshot 3 was taken as Super Admin.
- The numbers in the Simple explanation and Notes were 13 Oct values (40/658/33). On 14 Oct they are 42 of 660 forms, 35 of them real.

**Images.** OK (the key image matches the pixels: "3", "2 classes").

**URLs.** OK.

**Payload changes.** Text only: role for step 4, the "asset"-row control sentence, refreshed counts and the re-check line. Nothing to attach.

## ZP-4876: SKM round trip changes classes

**Reproduction.** Project Manager, site QA-DEMO ZP-4800.

1. Before the import, Assets › Asset Class (UI, one search each): QA G-PDU = PDU, QA G-SeriesCap = Capacitor, QA G-MotorStarter = Motor Starter, QA G-Arrester = Lightning Arrester.
2. SLD › Export › Export Engineering XML opens "Before you export to SKM": 3 errors, 2 required "I understand…" boxes and 2 options. I ticked only the two "I understand" boxes. The download is **`skm_export_363d5350_2026-10-14_095009.zip`**, holding the `.xml` and "READ THIS BEFORE IMPORTING.pdf".
3. Assets › SKM › Import (Full) with the zip: file `accept=".xml"`, "Please select a valid SKM XML file (.xml)", Analyze disabled. With the unzipped `.xml`:
   - The review shows 0 New, 94 Updated, 47 Connections, 40 Unmatched.
   - Chips include "Panelboard: 12" and "Load: 9".
   - Columns are Name, Action, SKM type, **Class / subtype**, Library match. The class column is visible only after scrolling the table sideways and shows only the new class.
   - There is no class-change warning.
4. Comparing the preview `eg_class` with the current class gives exactly the ticket's 12 changes (82 unchanged).
5. Cancel. The classes are unchanged afterwards, and only `export-skm` and `preview` were called.

Same 12 on `index-D-HsTlcR.js`.

**Skeptical-developer points now covered in the ticket:**
- The export PDF's "Exported as generic loads" list (Motor Controller ×3, Instrument Transformer, Lightning Arrester, Shunt Reactor) explains 6 of the 12. It says nothing about re-import overwriting the matched existing assets.
- The other 6 (PDU, Other, MV Switch, Junction Box, Capacitor, Motor Starter) are flagged nowhere.
- The review does show the new class, but never the current one, and marks no change.

**Overclaim fixed.** "On 4 of these assets we confirmed…" is now 3: the 4-asset cut contained QA G-CapBank, which was already Capacitor Bank.

**Images.**
- Screenshots 1 and 2 were ZP-4603 PASS SLD shots that show symbols, not classes. They are replaced by an Assets-list "before" composite.
- Screenshots 4 and 5 carried "Out of scope · Step 2/3" captions. The captions are replaced, and the shots renumbered 5 and 6.
- The key image is still accurate and kept.

**URLs.** All 4 example assets open as PM with the stated names and classes.

**Payload changes:**
- Title now says "without any warning" instead of "silently".
- Steps 2 and 3 rewritten for the zip.
- New "generic loads" column in the step 5 table.
- Notes add ZP-3836 and the preview fields `eg_class` and `alt_node_id`.

**Attach:**
- `ZP-4876-1-assets-class-before-import.png`
- `ZP-4876-2-export-engineering-xml-dialog.png`
- `ZP-4876-3-review-update-94-assets.png`
- `ZP-4876-4-class-column-shows-only-new-class.png`
- `ZP-4876-5-13oct-4-asset-file-review.png`
- `ZP-4876-6-13oct-after-update-classes-changed.png`

**Now unreferenced:** `4876-1-…`, `QA-13Oct-ZP-4603-2-…`, `-7-…`, `-x1-…`, `-x2-…`.

## ZP-4880: future "Last serviced" shows raw error

**Reproduction.** Project Manager, QA PNL1 (`4b84bf4a…`):
- Clicking the *Maintenance* tab opens a menu: Maintenance, Inspections, Infrared.
- Maintenance › SERVICE HISTORY › + Add record.
- Cleaning, Last serviced 15/10/2026. The date input has no `max` and is valid, and Save is enabled.
- Save gives `PUT /api/asset-maintenance/node/4b84bf4a…/history` 400. The dialog shows `API call failed: 400 - { "error": "last_serviced_on cannot be in the future \u2014 it records work …", "success": false }` with the literal `—`.
- FDR-Circuit Breaker-3 opens for the Super Admin (class Cable). The Project Manager gets "This asset link doesn't look right", as the ticket says.
- Same result on `index-D-HsTlcR.js`.

**Problems.**
- The Actual-result quote showed an em dash "—", which contradicts both the pixels and the Simple explanation ("a code instead of a dash"). It now shows `\u2014`.
- Step 3 skipped the menu choice.
- Step screenshots 1, 2, 4 and 5 were unhighlighted video frames. The "close-up" was not a close-up.
- The video name says "infrared" but shows the Maintenance flow on FDR-Circuit Breaker-3; the link is kept with a label.

**Payload changes.** Corrected quote; step 3 split into menu → Add record; Notes cite ZP-4045 (future service dates refused by design).

**Attach.** 4 fresh highlighted Project Manager shots:
- `ZP-4880-1-maintenance-tab-menu.png`
- `-2-service-history-add-record.png`
- `-3-future-date-accepted.png`
- `-4-raw-error-after-save.png`

**Now unreferenced:** `1-asset-maintenance-tab-add-record.png`, `2-add-service-history-dialog.png`, `4880-3-…`, `3-future-date-shows-raw-api-error.png`, `4-raw-api-error-close-up.png`.

## ZP-4882: Inverter / Rectifier called "Transformer"

**Reproduction.** Project Manager, SLD issues box "18 issues": "Transformer without secondary voltage on" QA G-Inverter, QA G-Rectifier, QA INV1 and QA REC1, plus three real transformers. The detail says "This transformer has no secondary winding voltage set…".
- QA G-Inverter has Asset Class Inverter.
- ⋮ › Edit Asset (menu: Edit Asset, Show Tasks, Change history, Delete Asset) has Primary and Secondary Voltage.
- All four have `secondary_voltage_id` null. The check in the bundle is `F(t) && t.secondary_voltage_id == null`, where F means the class has primary/secondary voltage. So "setting Secondary Voltage clears the warning" holds.
- Same result on `index-D-HsTlcR.js`.

**Problem found.** Step 3 contains `(https://…/assets/f556e269-…dae): Asset Class` and Jira auto-linked `…dae):`, which is a **dead link**. Everything else (title, Simple explanation, images, other URLs, role) checks out.

**Optional polish, not done:** screenshot 2 is an unhighlighted Super Admin shot whose filename contains "FAIL".

**Payload.** Same text with step 3 rewritten as `…: https://…/assets/f556e269-32c7-4ae2-a8bb-7ec880304dae . Its *Asset Class* is *Inverter*.` plus the re-check line. Nothing to attach.

## ZP-4893: Technician list shows every account, many as emails

**Reproduction.**
- **Project Manager (customer seat)**, QA PNL1 › Maintenance menu › Infrared › + Add Reading. The Technician defaults to "Abhiyant Singh (you)".
- The list has 247 options: "None" plus 246 users. 57 are full email addresses, and all 13 addresses named in the ticket are present.
- `GET /api/eg-form-sources/users` returns 246 users, 57 email names, keys id/is_current/name.
- Same result on `index-D-HsTlcR.js`.
- The Super Admin sees the same list on Switchboard-1.

**Problems.**
- No role was named. The Switchboard-1 URL does not open for a PM ("This asset link doesn't look right"); it opens for the Super Admin.
- Panelboard-1 had no URL.
- Screenshot 3 had a pop-up covering the emails.
- Screenshots 1–4 were unhighlighted video frames.
- The title said "customer addresses", which a reader can take as Acme's customers. The domains are outside business domains.

**Count check.** About 30 of the 57 addresses are outside egalvanic.com. About 23 are business domains; the rest are gmail, example.com and .edu. This is consistent with "about 22".

**Priority.** The ticket is **Highest**. A skeptical developer would push back: it is Acme's own 246 accounts, shown to signed-in Acme users who can add readings. My Client Portal probe could not reach the Add-reading screen (the asset page did not load). Suggest **Medium** unless the owner wants Highest. The payload does not change priority.

**Attach:**
- `ZP-4893-key-highlighted-v2.png`
- `ZP-4893-1-add-reading-technician-box.png`
- `ZP-4893-2-technician-list-emails-start.png`
- `ZP-4893-3-technician-list-more-emails.png`
- `ZP-4893-4-technician-list-more-emails-2.png`

Existing `ZP-4893-expected-mockup.png` and the video are kept.

**Now unreferenced:** `1-add-reading-dialog-technician-field.png`, `2-technician-list-shows-email-addresses.png`, `3-technician-list-customer-contact-emails.png`, `4-panelboard-technician-list-emails-and-staff-logins.png`, `ZP-4893-key-highlighted.png`.

---

## Leads seen on the way (not verified, not for filing as-is)
- **Add reading "Taken at" default looks about 11 h behind UTC.** At 14 Oct 15:19 IST (09:49 UTC) the dialog pre-filled 13/10/2026 10:49 PM. In the owner's 13 Oct video, at 7:16 PM IST it pre-filled 13/10 02:44 AM. Check the site timezone before calling it a bug.
- **The Project Manager's menu has no SLD link.** The SLD page is reachable only by URL or an asset's "Open in SLD"; staff see Engineering › SLD. This may be by design.
- **The SKM review table cannot show Name and Class / subtype together at 1600 px.** Scrolling right hides the name.
