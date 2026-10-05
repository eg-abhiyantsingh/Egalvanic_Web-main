# Web v2.3 round 2 — Jira comments with screenshots on every ticket; certain fails moved to To Do (5 Oct 2026)

**Prompts:**
1. "add comment for pass or fail both with screenshot."
2. "if you are sure than make ticket to do with valid screenshot and proof"
3. "but you should be 100% sure that ticket is failing really"

**Result:**
- All 22 tickets tested in round 2 now have one QA comment, posted under the owner's account, with the result line, numbered
  steps, what happened, what wasn't covered, full test-data links and inline screenshots (57 in total).
- Two tickets went back to **To Do**: ZP-4421 and ZP-4398, the only certain fails.
- No other status changes, fields or new tickets.
- Release page republished (v5): https://claude.ai/artifact/3e3VwLmEk5fDa7pQEyb24y. Its sections are the same text as the Jira
  comments.
- Comment texts and index: `docs/bug-evidence/2026-10-05-v23-qa-r2/jira-comments/` (`POSTED.md` lists the comment ids).

## How "100% sure" was applied
A ticket went to To Do only if all of these held:
- the failure was re-proven on the **current** QA build;
- there was a real screenshot and a comparison (control);
- the ticket's own text states the failed requirement;
- no later comment changes the design.

A new QA build (`index-C913tyjW.js`) was deployed in the afternoon, so every fail was re-run on it first.

| Ticket | Decision | Why |
|---|---|---|
| ZP-4421 | **To Do** | On the new build the box reads "Search forms by name…". "automatic" → "No forms match", while 34 of 42 forms read "Applies to: Automatic Transfer Switch …". "transfer" shows only forms named "Transfer…", so forms whose Applies-to says Transfer Switch are left out. Control: "ATS" finds forms by name. The ticket says "match against form name and the 'Applies to' text". |
| ZP-4398 | **To Do** | On the new build the preview auto-picks "10 SEP 2026" (16 assets). Android Site 2 (425) is 39th in the same list, and "test site" (1,983) is not in the list. Counts were checked two ways. The QA Review says "the auto-picked preview site is the company's site with the most assets", and backend #1359 is on QA because the SLD AI edit works. |
| ZP-4305 | not moved | Control: /pull-through-work shows a blank page with only the old heading, and a made-up address shows a blank page with no heading. The route is gone from the router; only the page-title map keeps the name. This is a cosmetic leftover, not a working route. The header lag isn't one of the QA Review items. |
| ZP-4148 | not moved | Dharmesh's 10 Sep comment: "DUPLICATE - do not action … close as a duplicate of ZP-4138". The /condition page is the same Condition Assessment page the user has under Site Data, so no extra data shows. |
| ZP-4529 | not moved; result **NOT CONFIRMED** | A used-up AI limit can't be produced on QA, and the replies were simulated in the browser. The comment asks the developer what the server sends at the limit. |
| ZP-4590 | not moved | The developer had already moved it back to In Progress (13:36 IST). |

## How the comments were made correct
1. **Drafts** were generated from the release-page data.
2. **Adversarial check (workflow, 44 agents).** Two independent checkers per ticket:
   - an evidence checker: every number, quote and caption against the notes and the screenshot pixels;
   - a ticket checker: the verdict against the ticket's own ACs and comments, Jira markup and plain words.

   They found 21 blockers. Among them:
   - two 30 Sep JSON screenshots with a wrong burned-in "without sign-in" label;
   - ZP-4421's attached images not showing the Applies-to text the fail depended on;
   - overstatements in ZP-4368, ZP-4406 and ZP-4529;
   - the ZP-4305 and ZP-4398 headers.
3. **Fixes before editing:**
   - images recaptured on the new build;
   - mislabelled images replaced;
   - role-probe labels cropped off (8 images);
   - the ZP-4427 image cropped to its card, so sales pipeline figures weren't posted;
   - a decision policy written (`jira-comments/POLICY.md`).
4. **Editor + re-check (workflow, 44 agents):** an editor per ticket applied the findings under the policy, and a fresh checker
   re-checked each final. One blocker remained: ZP-4529 couldn't honestly be called FAIL, so it was relabelled NOT CONFIRMED.
   48 minor wording fixes were applied after review.
5. **Checkers overruled where wrong:**
   - ZP-4505's create mode is the agreed design per the ZP-4504 comments;
   - ZP-4423's end-to-end wording follows the ticket ("profile shot").
6. **Posting guard (in the Jira page, per ticket).** The screenshots and the comment file were uploaded through the page's file
   input. The script then:
   - waited until every attachment had landed;
   - rendered the comment with Jira's wiki renderer (`/rest/api/1.0/render`) and required every image to resolve, no stray
     strike/underline/superscript markup, and one italic caption per image;
   - refused to post if a 5 Oct QA comment already existed, or if the uploaded file name didn't match the ticket;
   - then posted through `/rest/api/2/issue/<key>/comment`.

   A read-back confirmed exactly one QA comment on each of the 22 tickets.

## Files
- Evidence added: `zp4421/2–5_newbuild_*`, `zp4398/3–5_newbuild_*`, `zp4303/1_emp_lite_preview_*`, `zp4427/1_*_card.png`, `*_clean.png`
  (labels cropped), `jira-comments/` (posted texts + `POSTED.md` + `POLICY.md`).
- Notes updated: `zp4421`, `zp4398`, `zp4305` results.txt ("RE-CHECK on the NEW QA build").
