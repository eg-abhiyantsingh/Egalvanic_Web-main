# 2026-10-14 — Deep review of the 23 rewritten bugs, 19 fixed in Jira

**Prompt:** "check in deepth"
**Who / where:** Jira writes as abhiyant.singh@ (accountId 712020:8f78…, checked before every write). QA only (acme.qa.egalvanic.ai), build `index-D-HsTlcR.js`.

## What was done
1. Three reviewers re-ran every bug on today's QA build and read each ticket twice: once as a skeptical developer ("can I reject this?") and once as a non-technical manager ("do I understand it?").
   - Detailed findings are in `docs/bug-reports/2026-10-14-deep-review-{A,B,C}.md`.
2. **All 23 bugs still reproduce. None is invalid.**
3. 4 tickets were fine as written: ZP-4844, ZP-4845, ZP-4855, ZP-4868.
4. 19 tickets had wrong details, so they were fixed. Each got a new summary and description plus new highlighted screenshots: 58 images, 8.2 MB.
   - Payload: `docs/bug-evidence/2026-10-14-deep-review/payload-ALL.json`.
5. **Not changed on purpose:**
   - **Priorities:** ZP-4842 and ZP-4843 stay Medium (set by Dharmesh); ZP-4893 stays Highest (set by Abhiyant).
   - **Assignees and statuses:** all unchanged (all 19 tickets are To Do).
   - The text no longer states a priority anywhere, so it cannot contradict the field.

## What changed per ticket
| Ticket | Fix |
|---|---|
| ZP-4840 | Steps now use a QA work order instead of a customer's production one. Step 2 uses the real button name, "Bulk Ops". The title says "more than 500" (the real limit), not "hundreds". |
| ZP-4841 | Wrong price fixed: "Transformer-Repair" is 150.00, and 116,134.11 is an abandoned EMP quote. New key image shows exactly what the text says. The step no longer uses a Quotes menu that the PM does not have. |
| ZP-4842 | Removed an unproven comparison with a well-formed id. Added a clean key image. |
| ZP-4843 | The wrong example `/reporting` (Reporting is ON for Acme) is replaced by `/planning-board`. The ticket now says the page is admin-only. |
| ZP-4846 | Removed the "no way back" overclaim (the side menu and Back still work). Added a full URL for the well-formed id case. The target list now has one name. |
| ZP-4848 | The step-2 screenshot is now from the PM (it was the Super Admin). Removed the unverified "working download link" claim. Clearer title. |
| ZP-4849 | Clean screenshots without other test buses. Names the Super Admin instead of "an admin". Explains SKM in plain words. Notes that the bus shows as "Panelboard". |
| ZP-4850 | Step 4 now covers the new "Before you export to SKM" window, which needs two ticks. Names the flag key `feature-skm-export`. Images are labelled SIMULATED-OFF. |
| ZP-4853 | Removed the confusing green PASS-table screenshot. Screenshot 1 is now from the PM. Step 4 includes the new acknowledgement window. |
| ZP-4854 | Removed the wrong "harmless" wording for 56 → 56.0. Added the change shown on 19 of the 20 assets: empty Voltage / Ampere Rating gets filled with 240 / 800. |
| ZP-4865 | Steps are now reachable by the PM; there is no "Site Data › SLD" menu. Removed an on-screen message claim that the steps cannot reach. Screenshot values match the pixels. Added code for steps 6 and 7. Replaced captions copied from another ticket. 7 new shots. |
| ZP-4866 | Two duplicate unhighlighted frames are replaced by one highlighted frame that shows the proof. |
| ZP-4869 | The screen tag is shortened with "…"; it is not "fully readable". No "Site Data › SLD" step. Screenshot numbers match the text. New key image, mock-up, canvas shot and 2 PDF crops. |
| ZP-4870 | Screenshot labels match the steps. "10 items" is used consistently. Caption says Builder › Forms. Real reach is stated: 1 of 660 forms uses a users list. |
| ZP-4871 | Step 4 names the Super Admin, because the PM gets "Access Denied" on Builder. Numbers refreshed: 42 of 660 forms have no key, 35 of them real. Added the "asset" row control. |
| ZP-4876 | Steps updated: Export Engineering XML now downloads a .zip. "Confirm" is described as the two "I understand…" ticks. "4 assets changed" is corrected to 3. Says the Class / subtype column does show the new class. Notes that 6 of the 12 are flagged as generic loads. Added a "before" screenshot. |
| ZP-4880 | Actual result quotes the literal `—` that the screen shows. Step 3 covers the Maintenance tab menu (Maintenance / Inspections / Infrared). 4 fresh highlighted PM shots replace the video frames. |
| ZP-4882 | Fixed the dead step 3 link: Jira had swallowed "):" into the URL. |
| ZP-4893 | Steps name the PM seat and use an asset the PM can open (QA PNL1). 4 clean highlighted shots; the old shot 3 had a pop-up over the emails. "Customer addresses" is now "outside email domains". Never says "other companies". |

## Verification (after the write)
- **Writes:** all 19 attachment uploads returned 200 and all 19 description PUTs returned 204.
- **Rendered text, per ticket:**
  - summary and description match the payload byte for byte;
  - no "Unable to render/embed" errors;
  - the first heading is "Simple explanation";
  - Actual result, Expected result, Test data and Steps to reproduce are all present.
- **Images:** all 113 embedded images across the 19 tickets resolve to a real image attachment (> 1 KB, image type), with no duplicate file names.
- **In the Jira UI:**
  - ZP-4841 opens with Simple explanation at the top, the key image renders, and Actual / Expected read cleanly.
  - On ZP-4869, all 8 description images loaded (naturalWidth 1020–1024).

## Old attachments still on the tickets (no longer shown in the description)
Deleting attachments is left to Abhiyant (I do not delete data).
- **Misleading, worth removing:**
  - ZP-4841 `ZP-4841-key-highlighted.png` (claims prices it does not show)
  - ZP-4842 `ZP-4842-key-highlighted.png`, `b2-1-create-standard-malformed-id-500.png`
  - ZP-4846 `ZP-4846-key-highlighted.png` ("no way back"), `QA-13Oct-ZP-4680-12-…-FAIL.png`
  - ZP-4848 `4848-0-sld-export-menu-export-engineering-xml.png` (Super Admin, not PM)
  - ZP-4853 `4853-3-export-three-buses-same-label-no-suffix.png` (a green "PASS ZP-4746 / ZP-4750" table, which is confusing on a bug)
  - ZP-4854 `ZP-4854-key-highlighted.png` ("harmless")
  - ZP-4865 `QA-13Oct-ZP-4621-x0/x1/x2-…png` (captions from ZP-4621, PASS labels)
  - ZP-4869 `ZP-4869-key-highlighted.png`, `5-1-canvas-hidden-connection-tag-readable.png` ("readable")
  - ZP-4876 `QA-13Oct-ZP-4603-*.png` (captions from ZP-4603)
- **Only superseded:** older 13 Oct copies on 4841, 4842, 4843, 4849, 4850, 4853, 4865, 4866, 4869, 4870, 4880, 4882 and 4893.
- **Keep:** the screen recordings `bug_service_bulk.mp4` (4840), `future_date_infrared.mov` (4880) and `email_show.mov` (4893).

## Still open
- ZP-4850: a real `feature-skm-export` OFF flip on QA for Acme (ask Shubham in eg-internal-dev); today's images are a simulated OFF.
- ZP-4847: close or keep (owner's call).
- Leads not filed (need the two-pass check first):
  - the WO grid says 214 assets while "Select all" says 222;
  - the Add reading "Taken at" default looks ~11 h behind;
  - `/edge/preview-rules` answers FM/EE on an unassigned SLD.
