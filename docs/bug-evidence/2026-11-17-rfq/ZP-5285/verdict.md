# ZP-5285: SKM round trip shortens asset names to 25 characters

**Verdict: PASS.** Two notes for the developer are below. Neither blocks the fix.

Evidence:
- Video `QA-17Nov-ZP-5285-skm-names-video-steps.mp4` (56 s, ticket site).
- Screenshots `QA-17Nov-ZP-5285-1…4-*.png` and `…-6-review-cut-name-kept-NOTE.png`. They have outline boxes only, no text drawn in. The edge rows appear on the end card of the video and in the tables below.
- The exported and crafted XML files.
- Jira draft `ZP-5285.txt`.
- Scripts: `.playwright-mcp/rfq17-avani/d5285a.js`, `d5285b.js`, `d5285c.js`, `v5285.js` (outputs `*.json`).

- **Build:** QA `index-oFAZQ3rK.js`, 17 Nov 2026, 11:24–11:45 IST. The build was the same at the start and end of every run.
- **Seat:** Super Admin (`VALID_EMAIL`).
- **Fix:** eg-pz-backend PR #1814 ("Merged to QA", Dharmesh, 16 Nov). The backend repo is not readable from this machine, so the diff was not read. That comment is the only one on the ticket, and it adds no test rows.
- **Paths used (all UI):**
  - Export: SLD › Export › Export Engineering XML › "Export Engineering XML" in the "Before you export to SKM" dialog.
  - Import: Assets › SKM › Import (Full) › choose file › Analyze › Update N assets.
  - Names were read back with `GET /api/lookup/nodes/{sld}`.

## What changed (seen behaviour)
1. **Export no longer cuts names.** `s:name` now carries the full eGalvanic name. Examples: 44, 60, 36 and 41 characters. The 13 Nov export wrote 25.
   - Special characters are escaped correctly: `&quot;A&amp;B&quot; &lt;TX&gt; é°C ü`.
   - The file parses as well-formed XML (Python minidom; 8 Xformer2 components).
2. **Import keeps the eGalvanic name when the file's name is just its first 25 characters.** It still applies a real rename typed into the file.

## Rows
**Ticket repro: site "QA-DEMO BUG SKM names 13 Nov", TX-1 `69c1dc30…`.** TX-1 was left as "QA-DEMO BUG TX-1 long nam" by the 13 Nov bug. I first put its 44-character name back with `PUT /api/node/update` (200).

| Row | Result |
|---|---|
| R3: export, then re-import the same unchanged file (UI) | PASS. The file has `s:name="QA-DEMO BUG TX-1 long name round trip 13 Nov"`. The review shows "Update", and the commit says `updated_nodes: 2`. TX-1 is still 44 characters. TX-2 (22 characters) is unchanged. |
| Video run: same steps again | PASS. Same result (screenshots 1–4). |
| R4a: re-import the **13 Nov file** (TX-1 written as "QA-DEMO BUG TX-1 long nam") | PASS. TX-1 keeps the 44-character name, and the commit says `updated_nodes: 2`. |

**Own site "QA-DEMO ZP-5285 17 Nov" (`bf23e07f-8525-4378-be5a-bfda14fa3089`), 8 transformers**

| Name (characters) | R1: unchanged export + re-import | R4b: file with every name cut to 25 characters |
|---|---|---|
| QA-DEMO ZP-5285 TX 24 ch (24) | kept | kept |
| QA-DEMO ZP-5285 TX 25 chr (25) | kept | kept |
| QA-DEMO ZP-5285 TX 26 char (26) | kept | kept (file: "…TX 26 cha") |
| …TX 60 chars long name for round trip 17 Novx (60) | kept | kept |
| QA-DEMO ZP-5285 TX space tail 17 Nov (36, character 25 is a space) | kept | kept (file: "…TX space " with a trailing space) |
| QA-DEMO 5285 "A&B" <TX> é°C ü #1/2 17 Nov (41, é is character 25) | kept | kept |
| QA-DEMO ZP-5285 same pref alpha 17 Nov (38) | kept | kept (file: "QA-DEMO ZP-5285 same pref") |
| QA-DEMO ZP-5285 same pref beta 17 Nov (37) | kept | kept (two components with the **same** 25-character name in one file, each matched by altId) |

- kVA and %Z are unchanged on all 8 after R1 (for example 1000/6, 750/5.5).
- Commit responses: `updated_nodes: 8`, `created_nodes: 0`, `removed_edges: 0`, 0 duplicates.
- **R2, a genuine rename typed into the file** (N26 → "QA-DEMO 5285 SKM rename"): applied. The review row shows Name "QA-DEMO 5285 SKM rename" and the chip "Update QA-DEMO ZP-5285 TX 26 char". The 7 other names were kept. Then I put the name back with PUT (200).
- **R5, renames that look like a cut** (run C, file `QA-17Nov-ZP-5285-own-site-prefix-and-25char-renames-crafted.xml`):
  - N60 shortened in the file to "QA-DEMO ZP-5285 TX 60" (a 21-character prefix of the 60-character name): **applied**. The importer protects only the exact 25-character cut, so a real shortening still goes through.
  - N26 renamed to "QA-DEMO ZP-5285 TX 26 chZ" (25 characters, not a prefix): **applied**.
  - The 6 other names were kept. Both renamed names were put back with PUT (200 each).
  - Side effect, by design of the rule: a deliberate rename to exactly the first 25 characters of the current name would be ignored. It cannot be told apart from a cut.

## Notes for the developer (not blocking)
1. **Names longer than 25 characters now go to SKM.** The ticket noted that "PTW names are short, so the cut itself may be needed". The export now writes names up to at least 60 characters. I can't check whether SKM PowerTools accepts or cuts them, because there is no PTW here. If PTW cuts them on its side, the importer's keep-the-full-name rule (R4) protects the round trip.
2. **The review screen looks the same whether the name will change or not.**
   - R4: the file says "QA-DEMO BUG TX-1 long nam" and the chip says "Update QA-DEMO BUG TX-1 long name round trip 13 Nov". The name is kept.
   - R2: the file says "QA-DEMO 5285 SKM rename" and the chip says "Update QA-DEMO ZP-5285 TX 26 char". The name changes.
   - Nothing tells the user which of the two happens. Enhancement: a "name will change from … to …" line (the ticket's "at minimum" ask), or show the name that will be kept.

## Not covered
- SKM PowerTools itself (no PTW install).
- Other asset classes (only transformers / Xformer2 were used). The name path is shared, but cables, breakers and buses were not exported in this run.
- Roles other than Super Admin.
- Mobile not checked. SKM import/export is web only.

## New defects
None.

## Test data (QA-DEMO, mine)
- Site "QA-DEMO ZP-5285 17 Nov": 8 transformers, all with their original names (restored after R2/R5).
- Ticket site: TX-1 is now back to its full 44-character name (it had been left cut by the 13 Nov bug).
