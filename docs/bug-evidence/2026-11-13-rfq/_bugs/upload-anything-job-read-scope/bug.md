# [Web/API] A user can read another site's Upload Anything job and download its workbook without access to that site

## Simple explanation
- Upload Anything jobs belong to a site. Users should only see jobs for sites they are assigned to.
- A Project Manager who is **not** assigned to a site can still call three Upload Anything endpoints for that site's job and get everything back. The endpoints are `GET /api/onboarding/jobs/active?sld_id=…`, `GET /api/onboarding/jobs/<id>/status` and `GET /api/onboarding/jobs/<id>/result`.
- What comes back: the uploaded file names and storage keys, the AI extraction warnings, who answered the feedback (and the revision note, when there is one), and a **download link to the extracted workbook**.
- The new feedback endpoint from ZP-5243 is scoped correctly: the same user posting feedback for the same job gets **404 "job not found"**. Only the read endpoints are open.
- There is no screen path to it: the site picker does not offer the site. You need the site id or job id. This is within one company only; another company's site returns no job.

## Links (QA)
- App (site picker): https://acme.qa.egalvanic.ai/assets
- `GET` https://acme.qa.egalvanic.ai/api/onboarding/jobs/active?sld_id=7a1af0cb-156e-4e4c-869b-1e41b254da44
- `GET` https://acme.qa.egalvanic.ai/api/onboarding/jobs/36b01bd9-d815-4a32-bd9e-ce19d6f7903b/status
- `GET` https://acme.qa.egalvanic.ai/api/onboarding/jobs/36b01bd9-d815-4a32-bd9e-ce19d6f7903b/result
- Control: `POST` https://acme.qa.egalvanic.ai/api/onboarding/jobs/36b01bd9-d815-4a32-bd9e-ce19d6f7903b/feedback returns 404 for the same user.

## Actual vs Expected
- **Actual:** the unassigned Project Manager gets **HTTP 200** on all three GETs, with the same job JSON a Super Admin gets, including a presigned S3 link to `onboarding_template.xlsx`.
- **Expected:** these endpoints refuse a user who cannot reach the site, the same way `POST …/feedback` returns 404 and `GET /api/sld/<site>/power-scheme` returns 422 for that user and site.

## Test data
| Item | Value |
|---|---|
| Environment | QA, https://acme.qa.egalvanic.ai |
| Build | `index-C6DvLko9.js` (13 Nov 2026), re-proved 14:35 IST |
| Seat | Project Manager test seat: customer, `is_eg_admin: false`, role "Project Manager", **122 sites** |
| Site | "QA-DEMO ZP-5243 13 Nov UA job C scope", `7a1af0cb-156e-4e4c-869b-1e41b254da44`. QA test data, made by QA; **not** assigned to the PM |
| Job | `36b01bd9-d815-4a32-bd9e-ce19d6f7903b` (status `imported`; 1 tiny `.txt` source file) |
| Positive control seat | Super Admin (staff, `is_eg_admin: true`) |

## Steps (from a clean start)
There is no UI path to the leak; the UI correctly hides the site. These are the steps a developer can follow.

1. As an admin, create a site the Project Manager is **not** assigned to. On it, run a small Upload Anything import: Assets › Upload Anything › drop a small `.txt` › Extract › Review & import › Process Import. Note the site id and job id. Or reuse the ids above.
2. Sign in as the Project Manager test seat. Open **Assets** and type the site name in the **Current site** picker. The site is **not offered** (screenshot 1). `GET /api/auth/me` shows `accessible_sld_ids` with 122 ids, without this site.
3. In the same browser session (DevTools console or address bar), call `GET /api/onboarding/jobs/active?sld_id=<site id>`. You get **200** with the full job (screenshot 2).
4. Call `GET /api/onboarding/jobs/<job id>/result`. You get **200** with a `url` that downloads the extracted workbook (screenshot 3). `/status` also returns 200.
5. Controls, same user: `POST /api/onboarding/jobs/<job id>/feedback {"verdict":"ok"}` → **404 job not found**; `GET /api/sld/<site id>/power-scheme` → **422 permission_denied**; random job id → 404 (screenshot 4).

## Notes for the developer
**Positive control.** The Super Admin (staff) gets exactly the same job id from all three GETs (screenshot 4, right column). So the PM really receives another site's real job, not an empty placeholder.

**The seat really is not assigned.**
- The PM's `/api/auth/me` `accessible_sld_ids` (122 ids) does **not** contain `7a1af0cb…`.
- The site picker search offers only "QA-DEMO ZP-5243 13 Nov UA job A looks good", the one Upload Anything site this PM is assigned to (screenshot 1).

**The feedback POST already does the right thing** (ZP-5243 says it uses `caller_can_reach_sld`). The fix is probably to apply the same check to:
- `GET /onboarding/jobs/active` (check `sld_id`);
- `GET /onboarding/jobs/<id>/status`, `/result` and `/markup` (look up the job's `sld_id` first).

Check the other by-id onboarding routes too.

**Seats affected:** Account Manager, Electrical Engineer and Facility Manager behave the same as the PM (all hold `bulk_operations.view`). Client Portal is refused with 422 "Required: bulk_operations.view", which is correct.

**Not cross-company:** another company's site id returns 200 `{success, feedback_due:false}` with no job.

**Same family, already fixed elsewhere:** ZP-4901 and ZP-4905 (by-id endpoints that trusted the id in the path).

**Severity:** Medium. It stays inside one company and needs the id, but it exposes customer documents' names and keys and gives a working download link to the extracted data.

**Suggested assignee:** Avani (backend).

## JQL run (no existing ticket found)
- `project = ZP AND (text ~ "onboarding jobs active" OR text ~ "Upload Anything scope" OR (text ~ "Upload Anything" AND text ~ "accessible")) ORDER BY created DESC`
- `project = ZP AND (text ~ "accessible_sld_ids" OR text ~ "site scope leak" OR text ~ "unassigned site" OR text ~ "caller_can_reach_sld") ORDER BY created DESC`

## Evidence
- `QA-13Nov-BUG-upload-anything-job-read-scope-video-steps.mp4` (50 s; contact sheet checked; no email shown).
- `…-1-pm-site-list-without-site.png`: orange = the PM's site list; red = the site is not in it.
- `…-2-api-active-returns-job.png`: red = leaked fields.
- `…-3-api-result-workbook-link.png`: red = workbook link (signature redacted), keys and files.
- `…-4-controls-staff-gets-same-job-write-is-scoped.png`: red = leak rows; orange = control rows.
