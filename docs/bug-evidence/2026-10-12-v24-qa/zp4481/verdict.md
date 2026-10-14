# ZP-4481 — SLD Issues: warn on custom asset classes with no device role (uncategorized pass-through)

**Verdict:** PASS. Note that the issue now shows as an **error**, not the "warning" this ticket describes, because the later ZP-4645 deliberately raised uncategorized and SKM-unsupported classes to error.
**Build:** QA `index-D1IWZ6SI.js` (SLD chunks identical to `index-BdXGlAq5.js` with hashes ignored).
**Tested:** 12 Oct 2026, 19:15–19:40 IST, acme.qa.egalvanic.ai, Project Manager seat (company "EG-ACME").
**Test data:** my site `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)` (`bb593be6-…`). I added `QA-4481 MCC Bucket 1` there; "MCC Bucket" is an acme custom class whose device role is `uncategorized`. Acme QA has 10 such classes. Read-only checks ran on acme sites "Abhiyant test 12", "Web Copy Asset (location)", "Test 123456" and "Migration testing 31 to 32".
**Flags:** none for the issue. SKM export sits behind `feature-skm-export`, which is live ON.

## Checked

| Check | How | Result |
|---|---|---|
| One issue per SLD lists the assets | My site with one MCC Bucket; "Abhiyant test 12" with 3 custom classes | PASS. My site: "Custom asset class not supported by Egalvanic (1 Asset)". Abhiyant test 12: **one** row, "Default, MCC Bucket, QA_ATS1 are custom asset classes … (14 Assets)". There is no per-class or per-parent duplication (shots 1, 3). |
| Copy names the company | Read the row text | PASS. "This is a configuration **EG-ACME** needs to make to its instance …". |
| CSV link | Click "these assets" in the row | PASS. It downloads `uncategorized-assets.csv` with header `Asset,Asset Class,Parent Asset` (UTF-8 with BOM, Excel-safe) and the row `QA-4481 MCC Bucket 1,MCC Bucket,` (no parent). |
| The CSV also travels in the SKM export | SLD → Export → Export Engineering XML → acknowledge → download | PASS (bonus). The zip holds the XML, `READ THIS BEFORE IMPORTING.pdf` (which lists this error under "Issues standing at export") and the same `uncategorized-assets.csv`. |
| LOTO is blocked while the issue stands | Unlock, right-click an asset | PASS. "Generate LOTO Procedure" is greyed, with the tooltip "Resolve 2 SLD issues before generating a LOTO procedure" (shot 2). |
| LOTO unblocks once the asset is moved to a supported class | Move MCC Bucket 1 to a supported class, give the Utility a voltage, reload | PASS. The widget says **"No issues"** and "Generate LOTO Procedure" is enabled (shot 5). Moving the asset back to "MCC Bucket" blocks it again: "Resolve 1 SLD issue" (shot 6). |
| No uncategorized classes means no issue | "Test 123456", "Migration testing 31 to 32", and my site after moving the asset | PASS. "No issues", and no class row once the asset is reclassified (shot 4). |
| A class with **no role at all** is not flagged | Code check plus the data | All 62 acme classes have a role, so this cannot be built without creating a company class (an admin config change). The bundle only flags `device_role_code === "uncategorized"`. Not exercised live. |
| Performance | Network | The issue list is computed in the browser from the already-loaded `/sld/{id}/graph` (350 ms debounce). No extra request. |
| Roles | | Visible to every role that can open the SLD editor (PM, Admin; AM/EE/FM have the same SLD permissions). Client Portal has no `/slds` access ("Access Denied"). |

## Bugs found
None.

## Enhancement suggestions
1. **Show the classes inline as well as in the CSV.** The row already names up to 3 classes; also show the asset count per class, so admins know which class to fix first.
2. **"Fix in Admin › Classes" link for Admin users.** The copy says the company must configure the class, but nothing leads to where that is done.
3. **Name the blocking issues in the LOTO tooltip,** not just the count. Today a single "Source without voltage" warning blocks LOTO, and the tooltip does not say so.
4. **Locate the affected assets.** Clicking the row could select or zoom to all uncategorized assets on the canvas.

## Mobile part for Nency
- Open site `bb593be6-…` in the iOS embedded SLD viewer (after the SLD-viewer OTA republish). The same error row must appear, worded with **"your company"** instead of "EG-ACME", because there is no signed-in web user there.
- Check LOTO is blocked the same way on iOS, if iOS offers LOTO generation.

## Not covered
- A class with an empty role, for the reason above.
- The iOS wording ("your company"); that is Nency's.

## Re-check 13 Oct on index-C-Cc1qD1.js
Live QA build `index-C-Cc1qD1.js` (deployed 13 Oct 15:21 IST, newer than `index-COeCBn0z.js`). Re-run 13 Oct 15:58–16:01 IST, PM seat. Live ticket re-read: still In QA, no comments. LD live for Acme: `feature-gojs-sld` ON, `feature-skm-export` ON. Scripts: `.playwright-mcp/v24/sld-13oct/b.body.js`, `d.body.js`.

| Check | Result 13 Oct |
|---|---|
| My site (1 "MCC Bucket" asset, acme custom class, role uncategorized) | PASS. Red error row "Custom asset class not supported by Egalvanic (1 Asset)"; copy names "EG-ACME" (shot 1). |
| "these assets" CSV | PASS. `uncategorized-assets.csv`, header `Asset,Asset Class,Parent Asset`, row `QA-4481 MCC Bucket 1,MCC Bucket,`. |
| LOTO blocked while issues stand | PASS. Right-click Panelboard 1: "Generate LOTO Procedure" greyed, tooltip "Resolve 2 SLD issues before generating a LOTO procedure" (shot 2). |
| Positive control: site with no issues | PASS. "Migration testing 31 to 32" shows "No issues"; "Generate LOTO Procedure" is enabled there (shot 4). Read-only; graph re-locked afterwards. |
| Populated site, many custom classes | PASS. "Abhiyant test 12": ONE row "Default, MCC Bucket, QA_ATS1 are custom asset classes … (14 Assets)", no per-class duplicates (shot 3). |
| CSV inside the SKM export zip | PASS. Export zip still holds `uncategorized-assets.csv` next to the XML and the PDF. |

What still holds: every 12 Oct claim (the row shows as an *error*, as raised by ZP-4645). What changed: nothing. Note: the first LOTO probe of this run hit my own ZP-4597 test group frame (run in parallel) and was discarded; the clean re-run is shot 2.
Out-of-scope bugs: none.

## Proposed Jira comment
```
h3. QA re-check (13 Oct, QA build index-C-Cc1qD1.js) — PASS

Tested as Project Manager on my site *QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)* with one "MCC Bucket" asset (acme custom class, no supported device role), plus read-only checks on acme sites.
Note: the row shows as an *error* (red), not a warning, because ZP-4645 raised it to error.

||Check||Result||
|Issue row for the custom class, naming the company|PASS — "Custom asset class not supported by Egalvanic (1 Asset)", "EG-ACME needs to make…"|
|"these assets" link|PASS — downloads a CSV: Asset, Asset Class, Parent Asset|
|Site with 3 custom classes|PASS — one row only, "Default, MCC Bucket, QA_ATS1 … (14 Assets)"|
|LOTO while the issue stands|PASS — greyed, "Resolve 2 SLD issues before generating a LOTO procedure"|
|Control: site with no issues|PASS — "No issues", LOTO enabled|
|SKM export zip|PASS — also contains uncategorized-assets.csv|

Not tested: a class with no role at all (no such class on QA).

!QA-13Oct-ZP-4481-1-PASS-uncategorized-class-error-row-and-csv-link.png|width=720!
!QA-13Oct-ZP-4481-2-PASS-loto-blocked-while-issues.png|width=720!
!QA-13Oct-ZP-4481-4-PASS-control-no-issues-loto-enabled.png|width=720!
```

## Recommended status
READY TO RELEASE
