# ZP-4512: Journal fold Lambda reads web-uploaded photos (presigned query string) and archives ai_usage

**Verdict:** PARTIAL. On QA the user outcome passes: a photo uploaded from the web is read by the interpreter, and its text lands in the proposal. The fix itself sits in the SQS fold **Lambda**, and QA does not run that Lambda (QA runs `runner: "sfn"`; the ticket says QA is not bootstrapped). So the Lambda code path and the ai_usage archive are NOT WEB-TESTABLE on QA.
**Build:** QA `index-BdXGlAq5.js`, then `index-D1IWZ6SI.js` (redeployed 18:48 IST; same frontend source). Every interpretation job on QA reports `runner: "sfn"`.
**Test data (mine):** walk `QA-DEMO ZP-4561/4564 journal (delete me)` https://acme.qa.egalvanic.ai/site-walks/ce8b53ea-6566-420b-b243-2e0311f51b29 on site `QA-DEMO ZP-4561 site (delete me)` (863c83ab-3aac-42a9-9cb1-29b86ad92659).

## Checked
| Check | How | Result |
|---|---|---|
| Upload a photo from the web to a journal entry | Journal Review › Add photos (2 synthetic plates: an MSB torque label and an LP-1 panel nameplate) | PASS. The photo tile renders (`naturalWidth 1312`); the row is stored as `site_walk_journal_entry`. |
| Stored URL carries the presigned query (the ticket's precondition) | `GET /api/photo/by_entity/<entry>?types=site_walk_journal_entry` | See the "Stored URL" row below. |
| The interpreter reads the web photo | Job `157a5117` (4 entries, `photo_count 2`, succeeded). Compared with the notes-only control walk `88d2bb50` (same notes, no photos). | PASS. Values that exist **only in the photos** reached the proposal: LP-1 `serial number 20261012-QA-LP1`, `aic rating 22` (22 kA), and the evidence quote "…Cat. No. NQ430L2C (read from nameplate photo)". The MSB notes hold the torque table from its photo (500 lb-in main lugs, and so on). The control walk has none of these values. The units carry `proposal_photos` (MSB 1, MSB main 1, LP-1 1). |
| No "not readable from eg-pz-qa-s3-asset-photos-ohio" warning | Job `warnings` on all photo runs | PASS. No unreadable-photo warning was returned. |
| The photo travels to the asset on release (ZP-4561) | `GET /api/photo/by_entity/<node>` after Create assets | PASS. LP-1 got `LP1-panelboard-nameplate.jpg`; MSB got `MSB-torque-label.jpg` (3 photos copied). |
| ai_usage archive written by the fold Lambda (no AccessDenied) | n/a | NOT WEB-TESTABLE (S3/IAM). |
| Negative: a photo row whose URL has no object key still folds | n/a | NOT WEB-TESTABLE. The web always uploads to a real key, so this row cannot be created from the UI. |
| Lambda `_split_url` path | n/a | Not exercised on QA (the runner is `sfn`). |
| Performance | Photo upload PUT and createPhoto | About 6 s per photo including the S3 upload; the review page loads the photos lazily per location. |
| Roles / tenant | Covered by the shared walk role sweep (see ZP-4561). Photo linking to another walk's issue is refused: `bad_photo` "Only this walk's journal photographs can be linked." | PASS |
| LD flag | `feature-site-walks` = true for Acme (live evalx). The photo flow has no flag of its own. | n/a |

Stored URL: filled from the API run (see the "Stored URL detail" section below).

## Bugs found
None for this ticket.

## Enhancement suggestions
- Graph card photo tiles stay blank until you open that location's timeline. The page only signs URLs for the visible location. Signing every claimed photo on open would let the card show the photo the model used.
- Show a small "read by AI" badge on photos the interpreter actually used. It helps a reviewer trust the extracted serial or AIC values.

## Mobile part for Nency
iOS/Android upload journal photos the same way. Please take a nameplate photo in a journal entry on the phone and check that the web Journal Review shows a value only the photo carries (serial number, AIC). That proves phone photos are read too.

## Not covered and why
The SQS fold Lambda, its IAM grant for `eg-pz-<env>-s3-ai-usage-ohio`, and the "URL with no key" negative. QA runs the Step Function path. Developer evidence: a CloudWatch Logs Insights query on the dev `journal-fold-lambda` log group, `filter @message like /not readable from/ or @message like /AccessDenied/`, after the fix date (expect 0). Also `aws s3 ls s3://eg-pz-dev-s3-ai-usage-ohio/<prefix for journal folds>/` showing new objects.

## Proposed Jira comment
```
h3. QA 12 Oct (QA env, build index-BdXGlAq5.js / index-D1IWZ6SI.js): PARTIAL (user outcome PASS; Lambda path not on QA)

*Checked on QA (runner = sfn):*
# Uploaded 2 photos from the web to a journal walk (QA-DEMO ZP-4561/4564 journal). Both render.
# The interpreter read them: LP-1 serial "20261012-QA-LP1" and AIC 22 kA exist only in the photo and appear in the proposal. The MSB notes carry the torque table from its photo. The notes-only control walk has none of these values.
# No "not readable from …-s3-asset-photos-ohio" warning on any run.
# Create assets copied the photos to the new assets (LP-1, MSB).

!QA-12Oct-ZP-4512-1-web-photo-uploaded-to-journal-entry-PASS.png|width=720!
!QA-12Oct-ZP-4512-2-photo-only-values-in-proposal-PASS.png|width=720!

*Not testable on QA:* the SQS fold Lambda (QA is not bootstrapped; every job says runner "sfn"), the ai_usage archive grant, and the "URL with no key" negative.
Please attach dev CloudWatch for journal-fold-lambda (no "not readable", no AccessDenied) and the ai_usage S3 objects.

Recommended: stay In QA until the Lambda evidence is attached.
```

**Recommended status:** stay In QA (needs Lambda evidence; the QA outcome is fine).
