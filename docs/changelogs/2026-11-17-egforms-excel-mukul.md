# 17 Nov 2026: Mukul's question on EG Forms › Export to Excel / Import from Excel (eg-pz-qa)

Owner prompt: Mukul (project owner) asked in eg-pz-qa: *"What are these 2 options? When I try to export I get an error saying export failed."* The screenshot showed work order "EdgeCore EEM - SV01" › Actions › EG FORMS. The owner: *"reply to mukul … reply correctly"*.

## Checked on QA (build index-oFAZQ3rK.js, SA seat)
- **Export to Excel** = `GET /api/eg-form-excel/export?session_id=…`. It returns an .xlsx laid out as:
  - one sheet per EG form type;
  - row 1 = field keys, row 2 = labels;
  - one row per form instance and one column per reading;
  - Status and Test Equipment as dropdowns.
- **Import from Excel** = a file picker (.xlsx/.xls) → `POST /api/eg-form-excel/import`, with the green message "Updated N form instances across N sheets". Rows are matched by Instance ID.
  - Verified round trip on WO "Job - Nov 6, 12:03 PM": instance 454c2392… Test Voltage went from empty to 500, then 1000, then 750, then 600, with the comments set.
- **"Export failed" cause:** a WO without EG form instances. The server returns 404 `{"error":"No EG form instances found for this session"}`, but the UI shows only "Error – EG Form export failed".
  - Reproduced on WO "QA-DEMO ZP-5244 13 Nov check-off".
  - 3 of about 100 open/closed QA WOs export fine (they have forms).
- "EdgeCore EEM - SV01" is not on QA, so Mukul is on production. It was not opened (no prod without the owner's ask). The reply says to add forms via Actions › Manage Forms first, or send the link if forms already exist.

## Sent / filed
- Reply to @Mukul Panchal in eg-pz-qa with the 73 s video QA-17Nov-EGForms-Export-Import-Excel.mp4 (local; *.mp4 is git-ignored).
- **New bug [ZP-5302](https://egalvanic.atlassian.net/browse/ZP-5302):** Export to Excel shows only "EG Form export failed" instead of the server reason. Low, Krunal, To Do, next sprint Z-26-11-S2, no fix version, relates ZP-1724. Video and screenshot attached.
- A first take wrongly captioned the import as "no success message": the green snackbar was hidden under the caption bar. The take was re-recorded before sending.
