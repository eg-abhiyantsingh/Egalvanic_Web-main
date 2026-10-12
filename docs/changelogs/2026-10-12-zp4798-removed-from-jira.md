# 12 Oct, 11:19 IST check: ZP-4798 removed from Jira

- **Ready for QA:** only ZP-3305, as before.
- **Web v2.3.1** (release date 12 Oct): ZP-4767, ZP-4768 and ZP-4776 are all still To Do with Krunal.
- **Builds:** unchanged since 10:39 IST.
  - prod: `index-CY_YgNqA.js`
  - stage: `index-BH4gulmz.js`
  - QA: `index-BJGNnWD7.js`

## ZP-4798 is gone
- At 10:45 IST its history still loaded.
- At 11:19 IST, Jira search, the issue API and the browser (all signed in as abhiyant.singh@) return 404: "Issue does not exist or you do not have permission to see it".
- QA comments 45072 and 45073 return 404 too.
- So it was deleted, or moved somewhere this account can't see, within those 35 minutes. Chat gives no reason. The likely reason is that it duplicated ZP-4768 part 2, the same blank `/login` page.
- The screenshots still exist in the repo:
  - `docs/bug-evidence/2026-10-10-blank-after-sign-in-qa/`
  - `docs/bug-evidence/2026-10-10-v23-prod/zp4798-and-z-icon/`
- When ZP-4768 comes to QA, also test the repro that works for any user: sign in, sign out (the app lands on `/login`), then sign in again. The page must leave `/login` and show the dashboard.

## Other changes
- ZP-4766 (site photo in reports) now has fixVersion **Web v2.4**, a new version with release date 23 Oct.
- eg-internal-dev, about 11:02 IST: Dharmesh asked Abhiyant and Nency to join (a call).
