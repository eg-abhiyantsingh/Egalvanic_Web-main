# ZP-5243 · Upload Anything post-import feedback (Looks good / Request revision → DevRev) — QA 13 Nov 2026

**Verdict: PASS** on QA, build **index-C6DvLko9.js**. The earlier build of the day, `index-CDvWXKDj.js`, has byte-identical `Assets` and `UploadAnythingInvite` chunks.

There is **1 new bug outside this ticket's scope**: the read endpoints for Upload Anything jobs are not site-scoped.

**Test data:**
- Four own QA-DEMO sites: "QA-DEMO ZP-5243 13 Nov UA job A looks good", "… job B revision", "… job C scope" and "… job D video".
- Each got the same tiny 364-byte `.txt` equipment list through Upload Anything.
- Each finished through the real UI: Upload Anything › Review & import › Process Import, which calls `POST /onboarding/jobs/<id>/imported`.
- Each import gave 2 assets (TX-QA1, PNL-QA1); extraction took about 90 s.

## DevRev — what was sent (exactly one ticket)

| Time (IST) | Job | Call | Result |
|---|---|---|---|
| **13 Nov 2026 14:05:43** (08:35:43 UTC) | **B** `6f9fd2e0-4f3f-406f-b18c-edb247600396` on site `a063b16a-fafb-433a-8982-2dc9ebc91715` | `POST /api/onboarding/jobs/6f9fd2e0…/feedback` `{verdict:"revision", note:"QA TEST ZP-5243 13 Nov – automated QA check, please ignore", files:[QA-DEMO-ZP-5243-13Nov-extra-file.txt]}`, sent from the UI dialog (SA seat) | 201, **ticket TKT-1600** (`don:core:dvrv-us-1:devo/11Cfl5SySK:ticket/1600`), ticket_error null, files 2 |
| 13 Nov 2026 ≈13:52 IST (08:22 UTC) | **C** `36b01bd9-d815-4a32-bd9e-ce19d6f7903b` (already answered "ok" at 08:22:07 UTC) | `POST …/feedback` `{verdict:"revision", note: same QA TEST text}` — the first-answer-wins probe | 200 `already_recorded:true`; verdict stayed "ok"; **no ticket** |

- No other revision POST was made.
- DevRev was never opened and no DevRev endpoint was called directly.
- After the coordinator's STOP instruction (14:2x IST), no DevRev-path activity at all.
- The job D dialog in the video was cancelled, not submitted.

## Checked
| Check | How | Result |
|---|---|---|
| `GET /onboarding/jobs/active` returns `feedback_due` | API, jobs A–D before answering | `feedback_due: true` while imported, unanswered and created today ✅ |
| 14-day / state rule | 15 acme jobs imported 20 Jul–21 Aug; jobs in "succeeded" (not imported) state | All `feedback_due: false` ✅ (old jobs never prompt) |
| Banner on /assets, above the Arc Flash Readiness / Condition Assessment cards | UI, jobs A and D | "Upload Anything complete. Not working as expected?" with [Request revision] [Looks good!] ✅ (screenshot 1) |
| Looks good! hides it for good | Jobs A and D | `POST …/feedback {verdict:"ok"}` → 200. The banner disappears and stays gone after reload. `feedback_due:false`; `counts.feedback = {at, by, verdict:"ok"}` ✅ (2) |
| Dialog: note is required | Job B and job D | "Request a revision" › "What's wrong? *". The submit button is **disabled when blank and when only spaces**. The API also refuses a blank note: 400 "tell us what's wrong with the result" ✅ (3) |
| Optional files | "Add files we missed" | File listed. On submit: `POST /attachment/presigned-upload` 200 → S3 PUT 200 → included in the feedback body ✅ (4) |
| Confirmation with the request id | Job B submit | "Revision requested (request TKT-1600). We'll email you updates and let you know when your assets have been updated." ✅ (5) |
| Files in Attachments › "Upload Anything" folder (internal) | Site B › Attachments; `GET /sites/<id>/folders`, `/attachment/?sld_id=` | The folder "Upload Anything" (visibility internal) holds the source `.txt` (364 B) and the extra file (148 B), both **Internal**; `sources_failed: []` ✅ (6) |
| First answer wins | Job C: ok → ok → revision; job B: revision → ok | Second and third POSTs → 200 `already_recorded:true`; the stored verdict is unchanged; no second ticket ✅ |
| Validation | `{verdict:"banana"}`, `{}` | 400 "verdict must be 'ok' or 'revision'" ✅ |
| Permission `bulk_operations.manage` | PM, AM and CP with site A assigned; EE and FM by API | PM and AM hold it → banner and Upload Anything button shown ✅. **CP lacks it** → no banner, no Upload Anything button. `GET active` 422 "Required: bulk_operations.view"; `POST feedback` 422 ✅ |
| Foreign site / foreign job on POST | PM posts `{verdict:"ok"}` to job C, whose site is **not** assigned to PM; random job id | **404 "job not found"**, nothing recorded ✅. Positive control: SA, same job → 200. Another company's site in `GET active` → 200 `{success, feedback_due:false}` with no job (nothing from the other company) |
| `DEVREV_SUPPORT_TOKEN` on QA | Job B result | Present: a ticket was filed |
| Flags | Bundle | **No LaunchDarkly flag**. The banner is gated on `bulk_operations.manage` + canEdit; Upload Anything has no LD gate. Nothing to flip |
| Performance | /assets reload (site D) | 33 API calls; **1** `onboarding/jobs/active` call (562 ms); no call > 2 s. Earlier, while 3 browsers were running, single `active` calls took 7–33 s once; not reproduced on the control reload |

## Bugs found

### 1. A user can read another site's Upload Anything job and download its workbook without access to that site
This is outside ZP-5243's own scope: the new feedback POST **is** scoped.

**Steps:**
1. Sign in as the Project Manager test seat (customer seat, `is_eg_admin:false`). Site "QA-DEMO ZP-5243 13 Nov UA job C scope" (`7a1af0cb-156e-4e4c-869b-1e41b254da44`) is **not** in this seat's sites; the site picker does not offer it.
2. `GET /api/onboarding/jobs/active?sld_id=7a1af0cb-156e-4e4c-869b-1e41b254da44`.
3. `GET /api/onboarding/jobs/36b01bd9-d815-4a32-bd9e-ce19d6f7903b/status` and `/result`.

**Actual:** 200 with the full job: source file names and S3 keys, execution ARN, extraction warnings, and `feedback` (who answered and, for revisions, the note). `/result` returns a **presigned link to the extracted workbook**. AM, EE and FM behave the same.

**Expected:** refused, the same way the same seat gets 404 from `POST …/feedback` and 422 from `/api/sld/<site>/power-scheme` for that site.

**Positive control:** SA gets the same JSON.

**Proof:** reproduced twice (r08, r10 and r17/r19), same PM seat, same site.

| Field | Value |
|---|---|
| Severity | Medium (within one company; needs the site or job id) |
| Suggested assignee | Avani (backend) |
| Evidence | `QA-13Nov-ZP-5243-7-BUG-pm-reads-unassigned-site-job.png` |
| JQL run | `project = ZP AND (text ~ "onboarding jobs active" OR text ~ "Upload Anything scope" OR (text ~ "Upload Anything" AND text ~ "accessible"))` and `project = ZP AND (text ~ "accessible_sld_ids" OR text ~ "site scope leak" OR text ~ "unassigned site" OR text ~ "caller_can_reach_sld")`. No existing ticket; similar family ZP-4901 and ZP-4905 (Done) |

## Enhancement suggestions
- **Show the request id somewhere after the banner closes.** For example, on the Upload Anything dialog: "Revision requested on 13 Nov — TKT-1600". Today the confirmation disappears on reload, so the user cannot find their request id again.
- **"Looks good!" gives no visible acknowledgement.** The banner just vanishes. A short toast such as "Thanks — feedback recorded" would confirm the click registered.
- **List the attached extra files on the confirmation** (for example "2 files sent: …"), so users know what support received.

## Mobile impact
None. The banner and dialog are web-only, on /assets. The iOS/Android apps have no Upload Anything feedback.

## Not covered
- **`DEVREV_SUPPORT_TOKEN` missing → "contact support" path.** I cannot remove a backend secret on QA.
- **TECH seat.** No web access.
- **Real 14-day boundary on my own job.** I cannot age a job; checked with old acme jobs instead.
- **The DevRev ticket's part/tag/reporter.** DevRev must not be opened (owner instruction).

## Test data and settings changed
- 4 own QA-DEMO sites with 2 imported assets each.
- Attachments on site B.
- Assigned site A and the ZP-5211 site to the **PM, AM and CP test seats** (`POST /api/users/<id>/sld-assignments`).
- No company settings changed.

## Evidence
- `QA-13Nov-ZP-5243-video-steps.mp4` (96 s; contact sheet checked; step 4 shows the real job B confirmation frame).
- Screenshots 1–7.
- Source and extra test files.
- Scripts: `r05`, `r07`, `r08`, `r09`, `r10`, `r13`, `r14`, `r17`, `r19`.

## Recommended status
**READY TO RELEASE.** The read-scope bug is separate.
