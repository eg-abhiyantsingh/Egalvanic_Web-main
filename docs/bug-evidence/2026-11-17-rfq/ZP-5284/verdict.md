# ZP-5284 [Web/API] Upload Anything job readable without access to its site: verdict

**Result: PASS.** I re-ran the 13 Nov repro on the current build. The unassigned Project Manager now gets no job data from `jobs/active`, `/status`, `/result` or `/markup`. The same is true for the Account Manager, Electrical Engineer and Facility Manager seats. The positive controls still work: the staff seat and seats assigned to a site get their own site's job and workbook link.

- **Build:** QA `index-oFAZQ3rK.js`, 17 Nov 2026, 11:20–11:42 IST. Fix: eg-pz-backend PR #1816 (merged to QA 16 Nov). I couldn't read the PR: the backend repo isn't visible to the QA GitHub account.
- **Seats:** PM (customer, is_eg_admin false, 122 sites, site C not among them), AM, EE, FM, CP. The Super Admin seat (staff) is the positive control.
- **Test data (QA-owned, from 13 Nov):**
  - site C "QA-DEMO ZP-5243 13 Nov UA job C scope" `7a1af0cb-156e-4e4c-869b-1e41b254da44`, job C `36b01bd9-d815-4a32-bd9e-ce19d6f7903b` (imported);
  - site "UA job A looks good" `74415f17-82eb-4fc0-a737-6992f6630ddf` (assigned to PM, AM, CP), job A `228e6bc2-5bee-4a40-8653-c823fdb261f8`.
- **Flag:** none.
- **Never triggered:** "Request revision" (DevRev). The only feedback call was `{"verdict":"ok"}` from unassigned seats, and it was refused.

## Results (all from the same browser session as each seat)
| Call | PM / AM / EE / FM (site C not assigned) | SA staff (control) | 13 Nov (bug) |
|---|---|---|---|
| `GET /onboarding/jobs/active?sld_id=<site C>` | **400** `{"error":"site not found"}` | 200 + job C | 200 + full job |
| `GET /onboarding/jobs/<job C>/status` | **400** `job not found` | 200 + job | 200 |
| `GET /onboarding/jobs/<job C>/result` | **400** `job not found` (no link) | 200 + job + workbook link | 200 + presigned link |
| `GET /onboarding/jobs/<job C>/markup` | **400** `job not found` | 404 `no_markup` (job has no markup) | not tried |
| `POST …/<job C>/feedback {"verdict":"ok"}` | 404 `job not found` | not sent | 404 |
| `POST …/<job C>/cancel`, `POST …/<job C>/imported` | 400 `job not found` | not sent | not tried |
| `POST /onboarding/jobs` (submit a tiny QA-DEMO .txt) for site C, as PM | 400 `site not found`, no job created (SA re-check: site C still shows only job C) | – | not tried |
| Made-up job id / made-up site id | 400 `job not found` / `site not found` (the same answer as a real job the user can't reach, so a response doesn't reveal that a job exists) | 400 | 404 |
| Other company's site id (f86052b7…) | 400 `site not found` | 400 | 200, no job |
| `GET /onboarding/jobs` (a "list") | 405 Method Not Allowed. There is no list endpoint; POST is the submit call | 405 | – |
| Client Portal (no bulk_operations.view) | 422 `permission_denied` on every call | – | 422 |

**Positive controls on the same build:**
- PM and AM on their own site (job A): `active` 200 + job A, `result` 200 + workbook link.
- EE on its own older Upload Anything site (9fbd40f2…): `active` 200 + job 868d2e5e…, `/status` 200, `/result` 200 + link.
- SA on site C: 200 + job C on all three reads.
- PM on 6 other assigned sites with no job: 200 `{success, feedback_due, job}` without a job. The /assets "Let's get your assets in" invite relies on this, and it is unchanged.
- UI: the PM's site picker still doesn't offer site C (only "UA job A looks good").

## Notes (no change needed for the fix)
- A refused read now answers **400** with `site not found` / `job not found`. The brief expected 403/404. It returns no data and looks identical to a non-existent id, which is the important part. The status codes are just inconsistent: `POST …/feedback` answers 404 for the same case, and a made-up job id answered 404 on 13 Nov and now answers 400. The frontend treats only 404 on `/markup` as "no markup". It still gets 404 `no_markup` for jobs the user can reach, so nothing breaks today. A 404 everywhere would be tidier.
- No S3 URL, bucket, ARN or account name appears in any screenshot or raw file. The table only says "download link" yes/no. No `private/` folder was needed.

## Not covered
- `PUT /onboarding/jobs/<id>/markup`, `…/markup/normalize` and `…/markup/apply` for an unassigned seat. Not sent, because they could change data. The read of `/markup` is refused, which suggests the same lookup guards them.
- Another company's real job id (only a foreign site id was tried).
- Mobile not checked. Upload Anything is web-only.

## Test data / links
- https://acme.qa.egalvanic.ai/assets (site picker)
- https://acme.qa.egalvanic.ai/api/onboarding/jobs/active?sld_id=7a1af0cb-156e-4e4c-869b-1e41b254da44
- https://acme.qa.egalvanic.ai/api/onboarding/jobs/36b01bd9-d815-4a32-bd9e-ce19d6f7903b/status
- https://acme.qa.egalvanic.ai/api/onboarding/jobs/36b01bd9-d815-4a32-bd9e-ce19d6f7903b/result
- https://acme.qa.egalvanic.ai/api/onboarding/jobs/36b01bd9-d815-4a32-bd9e-ce19d6f7903b/markup
- Control: https://acme.qa.egalvanic.ai/api/onboarding/jobs/active?sld_id=74415f17-82eb-4fc0-a737-6992f6630ddf (PM/AM own site)

## New defects
None.

## Evidence
- `QA-17Nov-ZP-5284-upload-anything-scope-video.mp4` (41 s)
- `QA-17Nov-ZP-5284-1-pm-site-picker-site-not-offered.png`, `-2-pm-reads-refused-no-job-data.png`, `-3-controls-and-roles.png`
- Raw: `raw/matrix-a.json` (SA, PM, AM), `raw/matrix-b.json` (EE, FM, CP), `raw/extra.json` (assigned-no-job, submit probe, EE control), `raw/video-out.json`
- ZP-5284: screenshots re-saved without text tags (17 Nov, owner rule); outlines only, PASS/FAIL wording only in the .txt captions.
