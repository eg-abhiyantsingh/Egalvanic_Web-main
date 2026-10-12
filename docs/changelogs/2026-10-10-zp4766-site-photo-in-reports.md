# ZP-4766: the site photo can't appear in reports (Report Builder / Edit with AI)

- **Date:** 10 Oct 2026, 10:05–10:35 IST
- **Env:** QA `acme.qa.egalvanic.ai`, build `index-BJGNnWD7.js`, admin seat
- **Asked by owner:** "if I have uploaded one site photo, will it show in my report builder if we ask AI to build the report … if not create an enhancement and assign to [Avani]"

## Verdict
Not today. An uploaded (or Google) site photo never reaches the report's data, so no report can show it.

## What was done
1. Uploaded a labelled test photo to QA-DEMO site `bd598806…` via Edit Site. The site page shows it, and `GET /api/sld/{id}/profile` returns it under `site_photos.upload`.
2. Ran the Report Builder's own test-run (`POST /api/reporting/test-query/run`; note that the page config needs `query`, and `render_query` alone returns 0 pages) for all site envelopes:
   - `site_profile_photo` = `{"url": ""}` and count 0 on 6 v2 site queries;
   - the same value on `eg_get_work_order_details` for 2 sites that have a Google photo;
   - no photo field at all on `eg_get_sld_details` and the old-gen `get_issue_report_info` / `get_arc_flash_sld_info`.
3. Edit with AI on QA-DEMO config `d2b9d8fa…`:
   - run 1 said "Edits applied" and bound `site_profile_photo.url`;
   - it rendered an empty grey band for the photo site;
   - run 2, with the photo site as context, found the field empty itself and made no change, but its header still says "Edits applied".
4. Created site `8b7381d5…` "00 QA-DEMO site photo in report (delete me)" via Add Site with an uploaded photo. The name sorts first, so it gets around the ZP-4680 limit of 50 sites in the picker.

## Outputs
- Jira **ZP-4766**:
  - Task + label `enhancement`, Medium, assigned to Avani;
  - sprint Z-26-10-S1 (1224), status To Do;
  - no fixVersion (Web v2.3 shipped today; there is no next Web version yet);
  - 8 attachments, QA comment 45068.
- Artifact: https://claude.ai/artifact/WRnZVuU7eSnSJ9UhPJ6JAh
- Evidence: `docs/bug-evidence/2026-10-10-site-photo-report-builder/` (1–10 + annotated 10)
