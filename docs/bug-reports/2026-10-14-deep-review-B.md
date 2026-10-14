# Deep review B: 14 Oct 2026 (skeptical developer + non-technical manager)

Tickets: ZP-4849, ZP-4850, ZP-4853, ZP-4854, ZP-4855, ZP-4865, ZP-4866, ZP-4868. QA only. No Jira writes. The corrected
descriptions for the FIX tickets are in `docs/bug-evidence/2026-10-14-deep-review/payload-B.json`, and the new images are in the same folder.

## How it was checked

- **Live tickets:** each one was read with getJiraIssue (description, attachments, comments, status, priority). All eight are To Do.
  None has a developer comment. The only comment is QA's own addendum on ZP-4865.
- **Images:** all 44 images referenced in the eight descriptions were looked at by eye. The local copies have exactly the same file sizes as
  the Jira attachments.
- **Builds:** the QA build changed during the session.
  - The session started on `index-G6uKkjPx.js`. The full click-through runs with screenshots were done there.
  - `index-D-HsTlcR.js` was deployed at about 15:28 IST.
  - Every ticket was then re-confirmed on `index-D-HsTlcR.js` in two ways:
    - non-destructive API checks (run r7);
    - the four frontend code paths, compared in the new bundle (unchanged).
  - ZP-4850 and ZP-4853 were re-run end to end on the new build (run r3b).
- **Seats:** everything was printed with `is_eg_admin`.
  - Customer seats (all `is_eg_admin` false): Project Manager (`+project`), Electrical Engineer (`+electric`) and Client Portal (`+clientportal`).
  - Super Admin (`+admin`, staff) was used only where a ticket says so.
- **URLs:** every Test data URL was opened signed in, with the right role:
  - `/assets`, `/slds`, `/arc-flash`, `/equipment-designations` and the work order `/sessions/22a22b9c…`;
  - the asset pages `/assets/525225bf…` (QA-DEMO 4799 JBOX), `/assets/e77ee2e3…` (QA G-Meter) and `/assets/bb0b3d39…` (Generator-1).
  - All of them open the named record. There are no dead links.
- **Duplicates:** JQL over everything created since 13 Oct (SKM, bulk import, arc flash, multiselect, batch-positions, "not assigned").
  - No duplicates.
  - Related but different: ZP-4848 (another company's engineering file download), ZP-4876 (SKM re-import changes classes) and
    ZP-4870 (form builder preview user fields).
- **Data hygiene:**
  - **ZP-4849:** the run created one bus, "QA-DEMO PM scope test (delete me)" (`34449681-…`), on the QA-DEMO site. It was deleted again
    (200, `is_deleted` true).
  - **ZP-4865:** the connection, the asset value and the meter position on the QA-DEMO site were changed by the test and put back. Read-back
    after the restore was identical. The clean-up snippet proposed for the ticket was also validated as a no-op (3× 200, data unchanged).
  - **ZP-4854:** *Process Import* was never clicked (0 process calls). The server check was run with validate only.
  - **ZP-4868:** the pick was removed again; no form write was sent.
  - **Flags:** `feature-skm-export` was only simulated OFF, inside the test browser. Its real value is ON (LaunchDarkly evalx v724).
  - **SKM exports:** these are read-only.

## Verdicts

| Ticket | Verdict | One line |
|---|---|---|
| [ZP-4849](https://egalvanic.atlassian.net/browse/ZP-4849) | **FIX** (small) | Still reproduces exactly as written (read 422, check "will create 1", import 200 "created_nodes: 1"). Screenshot 4 shows other probe buses and no highlight, "an admin" is vague, the bus appears as a "Panelboard", "SKM" is not explained for a manager, and the build/date is stale. |
| [ZP-4850](https://egalvanic.atlassian.net/browse/ZP-4850) | **FIX** | Still true, but step 4 no longer works as written. A new "Before you export to SKM" window has two required ticks, so a dev following the steps sees a disabled button. With them ticked, the file downloads even with the switch simulated OFF. Name the flag key. |
| [ZP-4853](https://egalvanic.atlassian.net/browse/ZP-4853) | **FIX** | The help text is still wrong in EN and FR. Screenshot 3 is a green "PASS ZP-4746/ZP-4750" table from 12 Oct, which is confusing on a bug. Screenshot 1 was taken as Super Admin, not as the PM the step names. Step 4 also misses the new acknowledgement window. |
| [ZP-4854](https://egalvanic.atlassian.net/browse/ZP-4854) | **FIX** | Reproduces with the same numbers (20 assets, 4 connections). The top text calls "56 → 56.0" "harmless", which is wrong for text fields, and contradicts its own notes. It also never mentions the change shown on 19 of the 20 assets (empty Voltage / Ampere Rating filled with 240 / 800). |
| [ZP-4855](https://egalvanic.atlassian.net/browse/ZP-4855) | **OK** | Reproduces on both builds: export, validate and process give 500, an empty id gives 400, and SKM export gives 400 "Invalid SLD ID format". Clear, honest (API only, Low), and images match. |
| [ZP-4865](https://egalvanic.atlassian.net/browse/ZP-4865) | **FIX** | Reproduces (EE read 422, all saves 200, PM sees the changes). Step 1's "Site Data › SLD" does not exist for the PM, step 4 claims an on-screen message the steps cannot reach, and Screenshot 4 claims a value that is not in the pixels. Steps 6 and 7 have no code. The batch-positions images carry another ticket's captions. |
| [ZP-4866](https://egalvanic.atlassian.net/browse/ZP-4866) | **FIX** (small) | Reproduces on both builds. Screenshots 6 and 7 are the same frame with no highlight, and 7 claims "clicking does not open the page" without visible proof. One highlighted frame replaces them. Update the build/date. |
| [ZP-4868](https://egalvanic.atlassian.net/browse/ZP-4868) | **OK** | Reproduces on both builds: "abh" gives 25 of 246, a tick wipes it back to 246, and re-typing gives 25 again. Both "13N-H1-1" rows open the same form, so step 1 is not ambiguous. The ZP-4753 quote is verified and the images match. |

No ticket is INVALID-NOW: all eight still reproduce on the current build `index-D-HsTlcR.js`.

---

## ZP-4849: SKM import to a site the Project Manager is not assigned to

**Re-check:**
- **On G6uKkjPx:** I ran the exact console snippet from the ticket as the PM (`is_eg_admin` false, 116 sites, site not assigned, `data.import`
  yes).
  - The read gave 422.
  - The preview gave "will create 1".
  - The commit gave 200 `{"created_nodes":1,…,"skm_namespace":"QA-DEMO-PM-SCOPE-TEST"}`.
  - The Super Admin saw "QA-DEMO PM scope test (delete me)" in Assets, as a Panelboard.
- **On D-HsTlcR:** read 422 and preview "will create 1" (no commit).
- **Not reachable from the screen:**
  - The PM's picker shows "No sites available".
  - Forcing the site id into localStorage was ignored by the app (audit B).

**Problems found (developer / manager view):**
1. **Screenshot 4 is the wrong frame.** The step code creates "QA-DEMO PM scope test (delete me)", but Screenshot 4 (`2b-…`) shows a search
   for "PM scope probe". Its two rows come from earlier runs and the frame has no highlight.
2. **Vague role.** Step 5 says "Sign in as an admin"; it should name the Super Admin (staff) seat.
3. **Word mismatch.** The text says "bus" but the Assets list says "Panelboard", and a manager would not connect the two.
4. **Jargon in the title and top text.** "SKM import" is never explained.
5. **Noisy first-run image.** Screenshot 3 (13 Oct first run) has "other company site" rows that are irrelevant here and invite a wrong
   cross-tenant reading.
6. **Stale build/date and no clean-up step.**

**Fix:**
- The title now says "only by sending the request directly, not from the screen".
- The top text adds one plain line about SKM.
- New highlighted Screenshots 1 to 3 (14 Oct): `ZP-4849-1/2/3-*.png`.
- Super Admin is named, the Panelboard listing is noted, and a clean-up step is added.
- The old images stay attached.

## ZP-4850: SKM export switch OFF still leaves "Export Engineering XML" on the SLD page

**Re-check:**
- **Real switch value:** `feature-skm-export` is ON for Acme on QA (evalx).
- **Simulated OFF, Assets > SKM menu:** Import (Full) | Import (XML→Excel) | Settings Verifier. This is correct.
- **Simulated OFF, SLD > Export menu:** Export PDF | Export DXF | **Export Engineering XML** | Settings Verifier | Export JSON | Export Ground
  Fault JSON.
- **Clicking the XML item on the current build:**
  - It opens a new window, "Before you export to SKM", that lists 4 model errors.
  - It has two required "I understand" ticks; until both are ticked, the export button is disabled.
  - After ticking both, `POST /api/sld/9138fd14…/export-skm` answers 200 and `skm_export_9138fd14_2026-10-14_095836.zip` (61 KB) downloads.
    It holds the XML and "READ THIS BEFORE IMPORTING.pdf".
- **Code:** the SLD item is hidden only in PDF-only mode, and Assets reads `skm-export`. This is the same in both bundles.
- **Same file from both menus:** ZP-4482's own release note says both entry points download the same zip.

**Problems found:**
1. **Step 4 cannot be followed on today's build.** It does not mention the new window and its two ticks.
   - My first automated run did not tick them, so nothing downloaded.
   - A developer following the ticket would hit the same thing and could call it "cannot reproduce".
2. **The flag key is missing.** `feature-skm-export` is not in the steps or the test data; it only appears inside an image caption.
3. **Duplicate screenshots.** Screenshots 2 and 3 (13 Oct) are near-duplicates.
4. **Stale build/date.**

**Fix:**
- New step 5 with the window and ticks.
- New images (14 Oct, all labelled SIMULATED): `ZP-4850-1…4-*.png`.
- The flag key is in the steps and the test data.
- The SLD page is opened by its address, because the PM menu has no SLD link.
- The ZP-4482 quote is added.

**Real flip still needed:** `feature-skm-export` → OFF, QA, company Acme (d59d449b-…), for a few minutes, then back ON.

## ZP-4853: help text promises "-1, -2" that the export no longer adds

**Re-check:**
- **Help text:** the tooltip key `labelFromLocationHint` still says "plus -1, -2, …" in English and French in both bundles. The runtime tooltip
  text was captured on D-HsTlcR.
- **Export (Super Admin, QA-DEMO ZP-4582 site, option ticked):** after the "Before you export to SKM" window and its two ticks, the server got
  `af_label_from_location=true` and answered 200, and `skm_export_cfc07c0d_2026-10-14_100017.zip` downloaded.
- **Labels inside that zip's XML:**
  - QDA-BUS-MAIN, QDA-BUS-LOAD and QDB-BUS-MAIN are all "QA Bldg-North/Fl 1-2/Rm A-B".
  - QDC-BUS-MAIN, QDD-NODEBUS and QDD-DCBUS are all "QA Bldg South/F2/R2".
  - Rooms were confirmed from the graph. There is no suffix.
- **ZP-4760** says the export "Writes AF_LabelNumber … verbatim (unsuffixed)".

**Problems found:**
1. **Confusing Screenshot 3.** `4853-3-…` is a QA table headed with a big green **PASS** for ZP-4746/ZP-4750. It was made on 12 Oct on an older
   build. On a bug ticket a manager reads "PASS" and is lost.
2. **Wrong role in Screenshot 1.** `B3-4853-…` was taken as Super Admin (Engineering/Sales/Builder rail, "aa" avatar), while step 2 names
   the PM. Its file name says "tooltip-text" but no tooltip is shown.
3. **Step 4 is out of date.** It misses the new acknowledgement window and that the file is now a zip.
4. **The help text is in a second place, not mentioned.** The same text also sits on the "i" icon in the "Before you export to SKM" window
   (SLD export path).

**Fix:**
- New Screenshot 1 (PM, highlighted).
- Screenshot 2 stays: the 13 Oct tooltip, still accurate.
- New Screenshot 3 (option ticked) and Screenshot 4: a clean table of the real file, grouped by room, with no "PASS".
- Step 4 is updated.
- A note says one key fixes both places.

*Note:* my own two attempts to re-shoot the tooltip on 14 Oct lost the tooltip before the capture. Those images were discarded and not used.

## ZP-4854: unchanged Bulk Export re-imported shows 20 + 4 changes

**Re-check:**
- **Round trip (G6uKkjPx):** Bulk Export, then Bulk Import of the same file.
  - Resolve Conflicts showed "Panelboard-1" matching 2 assets; I chose skip.
  - The Review showed **20 assets / 4 connections to update**.
- **Server check (D-HsTlcR):** the validate call gave `assets_to_update` 20, `connections_to_update` 4, 1 conflict and 5 warnings.
- **What the 20 / 4 are:**
  - 19 assets: voltage (empty) → 240.0, ampere_rating (empty) → 800, Catalog Number 56 → 56.0, Size 67 → 67.0, Serial Number 89 → 89.0.
  - Generator-1: voltage 240.0 → 200.0.
  - 4 connections: Length 89 → 89.0.
- **Proof from the untouched file:**
  - For the 19 assets, `node.voltage` and `node.ampere_rating` are empty, while the "Voltage" and "Ampere Rating" columns hold "240V" and "800A".
  - Generator-1 has `node.voltage` "240V" and a second "voltage" column of "200".
  - Catalog Number, Size, Serial Number and Length are text cells.

**Problems found:**
1. **Wrong "harmless" claim.** The top text says "Some of these are harmless (a number shown as '56' becoming '56.0')". For text fields
   (Serial or Catalog Number) the stored text really changes, which the ticket's own notes admit, so the top contradicts the notes.
2. **The most common change is missing.** The text never mentions the change visible first in every screenshot: on 19 assets, empty Voltage /
   Ampere Rating would be filled with 240 / 800. A manager comparing the text and the picture cannot match them.
3. **The 4 connection changes are not explained** (Length "89" → "89.0").
4. **No picture for the conflict step.**

**Fix:**
- The top text names the three kinds of misreading in plain words.
- The Actual result lists exactly what the Review shows.
- New key image v2 (orange note for the 19-asset pattern).
- Screenshot for step 4 (skip the ambiguous name) and for the connection updates. The label was corrected so its count matches the pixels:
  "3 of the 4 visible".
- The Generator-1 asset URL was added and verified.
- The title adds "4 connection" and "240 → 200".

## ZP-4855: Bulk Edit calls give 500 on a malformed site id: OK

On G6uKkjPx with the ticket's exact snippet:
- Export, validate and process all gave **500** `internal_error`. The trace ids were `6b612405…`, `26a2ff11…` and `9aa37c2d…`.
- The controls behaved as described: an empty id gave 400 "sld_id query parameter is required", own-site export gave 200, and SKM export
  with the bad id gave 400 "Invalid SLD ID format".
- The literal placeholder `<your user id>` gives the same 500, so a developer does not even need their id.

On D-HsTlcR: export 500 (`18106fe3…`), validate 500 (`ee1e246d…`) and SKM 400.

The text is clear, Low is honest and the images match. Optional only: the build line could add "still reproduces on index-D-HsTlcR.js,
14 Oct".

## ZP-4865: an unassigned engineer can change a site's SLD

**Re-check on G6uKkjPx:**
- **The engineer:** `is_eg_admin` false, 4 sites, not this one, with `nodes.manage` and `edges.manage`. The picker shows "No sites available".
- **The ticket's exact code:** read **422**, connection save **200**, asset save **200**, and `batch-positions` **200**.
- **After 12 s the PM read back:**
  - `edge_manips` null (the line is shown);
  - `node_manips` `{qa_probe:'EE-not-assigned'}`;
  - QA G-Meter moved from 2700,1900 to 3100,1700.
- **Restore:** all three were put back and verified.

**On D-HsTlcR:** read 422 and no-op saves 200 (still not gated).

**PM menu:** Site Data, Operations and Maintenance Portal only, with **no SLD link anywhere** (`a[href="/slds"]` count 0). `/slds` works only by
its address.

**Problems found:**
1. **Wrong menu path in step 1.** It says "open **Site Data › SLD**", but that item does not exist for the PM. Screenshot 1's own caption
   repeats the wrong path.
2. **Unreachable claim in step 4.** It says "In the app the engineer still sees 'The diagram couldn't be loaded'", which the steps never
   produce: the engineer's SLD page shows their own site. Screenshot 3's caption says "site id forced", a trick that is not in the steps.
3. **Value not in the pixels.** Step 5 says Screenshot 4 shows the asset "now carries the engineer's test value `qa_probe`", but the pixels
   only show the line. The value lives in data that is not on screen.
4. **Clean-up has no instructions.** Step 6 says "put both values back" without saying how.
5. **Step 7 cannot be reproduced from the ticket.** It has no request body for `batch-positions`. Its three images come from the ZP-4621 test:
   - x0's caption lists "Instrument Transformer … Relay (SKM style) · Meter", it is at a different zoom and has no highlight on the meter;
   - x1's caption starts "Out of scope".
6. **Small overclaim.** "Anyone with an engineer login" overstates it; this was proven for an engineer of the same company.

**Fix:**
- The steps are reworked. The SLD page is opened by its address. There is a read snippet for the hidden value, plus the `batch-positions`
  code and the clean-up code; all were validated live.
- New images (14 Oct):
  - before/after for the connection and before/after for the meter, at the same zoom with highlights;
  - a cropped console answer (the original frame showed the engineer's own-site dashboard under leftover picker text);
  - a restored frame.
- The title adds "by sending the save requests directly".

## ZP-4866: Client Portal opens /arc-flash by typing it

**Re-check on G6uKkjPx and D-HsTlcR:**
- **The CP seat:** `is_eg_admin` false, 36 permissions, no `features.arc_flash.view`.
- **Menu:** Site Data and Maintenance Portal only.
- **Typed addresses:**
  - `/slds` and `/equipment-designations` show Access Denied.
  - `/arc-flash` opens the full page with "Export readiness report". The report window ("Generate Report", "Arc Flash Engineering Turnover
    Report") opens; it was not submitted.
- **API calls:** `/api/sld/<site>/edges`, `/api/lookup/nodes/<site>` and `/api/sld/<site>/library-designations` all give 200.
- **Assets card:** "Arc Flash Readiness 13%, 261 of 2048 fields • 4 unclassified". A click leaves the page on `/assets`.
- **Code:** the direct-render of `/arc-flash` is still in the D-HsTlcR bundle.

**Problems found:**
- Screenshots 6 (D4) and 7 (D5) are the same frame with no highlight or caption.
- Step 6 says Screenshot 7 shows "clicking that card does not open the page", which nothing in the pixels shows.
- Stale build line.

**Fix:** one highlighted frame, `ZP-4866-5-cp-assets-headline-card-click-does-nothing.png` (caption: the page stays on /assets), replaces both.
The build line is updated; everything else is unchanged.

## ZP-4868: multi-pick search text is wiped after each tick: OK

**Re-check:**
- **G6uKkjPx and D-HsTlcR:**
  - WO "QA-DEMO ZP-4801 Tuned column (delete me)" > More > Forms > "13N-H1-1" opens the form "QA-DEMO ZP-4751 ZP-4753 dataset
    type-to-search (delete me)".
  - In M1, "abh" gives **25** of **246**. A tick of "abhiyant admin" leaves the search empty and **246** options. Re-typing gives 25.
  - The pick was removed and no write was sent.
- **Both "13N-H1-1" rows:** they open the same form (checked on D-HsTlcR).
- **ZP-4753 QA review:** it says "Tick several filtered options".
- **Code:** in the D-HsTlcR bundle the dataset multiselect still has no controlled `inputValue`.

The text, numbers and images match.

Optional only:
- "the company's user list" could read "a list of 246 users".
- The build line could add D-HsTlcR.

---

## Files

**New images (all under `docs/bug-evidence/2026-10-14-deep-review/`):**
- **ZP-4849:** `ZP-4849-1-pm-site-not-offered.png`, `ZP-4849-2-pm-import-accepted-on-unassigned-site.png`,
  `ZP-4849-3-admin-sees-new-bus-on-site.png`
- **ZP-4850:** `ZP-4850-1-SIMULATED-OFF-assets-skm-menu-no-export.png`, `ZP-4850-2-SIMULATED-OFF-sld-export-menu-still-offers-xml.png`,
  `ZP-4850-3-SIMULATED-OFF-before-you-export-window.png`, `ZP-4850-4-SIMULATED-OFF-file-still-downloads.png`
- **ZP-4853:** `ZP-4853-1-export-dialog-option-and-i-icon.png`, `ZP-4853-3-admin-export-option-ticked.png`,
  `ZP-4853-4-exported-file-same-room-same-label.png` (rendered from the real file with PIL)
- **ZP-4854:** `ZP-4854-key-highlighted-v2.png` (PIL annotation on the existing key image),
  `ZP-4854-1-resolve-conflicts-skip-ambiguous-name.png`, `ZP-4854-4-connections-length-89-to-89-0.png`
  - Also produced but not used: `ZP-4854-2-review-unchanged-file-lists-changes.png` (overlapping labels) and
    `ZP-4854-3-generator-1-voltage-240-to-200.png` (same as the attached C2).
- **ZP-4865:** `ZP-4865-1…7-*.png` and `ZP-4865-3-ee-read-refused-saves-accepted-console.png`
  - The uncropped `ZP-4865-3-ee-read-refused-saves-accepted.png` is not used: it shows leftover picker text over the engineer's own dashboard.
- **ZP-4866:** `ZP-4866-5-cp-assets-headline-card-click-does-nothing.png`
  - `ZP-4866-1…4` were re-shot as proof but are not needed; the attached 13 Oct frames are still accurate.
- **ZP-4868:** `ZP-4868-1/2-*.png` were re-shot as proof but are not needed.

**Other files:**
- **Payload:** `docs/bug-evidence/2026-10-14-deep-review/payload-B.json`. It has 6 FIX tickets with `key`, `summary`, `desc` (Jira wiki) and
  `files_to_attach` (file names in the folder above).
  - Machine checks passed: every referenced image is either already attached or listed, `{noformat}` blocks are balanced, there are no braces
    inside `{{}}`, no stray `!`, `[` or line-leading `-`/`#`/`|`, and the section order is kept.
  - Summaries change only for ZP-4849, ZP-4854 and ZP-4865.
- **Scripts and run outputs:** `.playwright-mcp/v24/deep-review-b/` (r1–r9).
- **Discarded images (wrong or empty captures, not for Jira):** `.playwright-mcp/v24/deep-review-b/discarded/`.
- **Shared folder:** `2026-10-14-deep-review/` also holds other reviewers' files (ZP-4869, ZP-4880, `raw/`). None of them were touched.
