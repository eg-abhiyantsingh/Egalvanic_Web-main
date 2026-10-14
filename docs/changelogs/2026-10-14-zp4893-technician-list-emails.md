# ZP-4893: Add reading › Technician list shows every company user, many as raw emails (14 Oct 2026)

- **Request:** the owner sent a video (`email_show.mov`) and a screenshot, then said "create a jira bug … no need to check this in qa".
- **Filed:** [ZP-4893](https://egalvanic.atlassian.net/browse/ZP-4893) from abhiyant.singh@.
  - Fields: Medium, Avani, Web v2.4, sprint Z-26-10-S1, To Do. Relates ZP-4710 and ZP-4867.
  - Attachments: 4 screenshots plus the video.
- **Facts used (checked on QA `index-G6uKkjPx.js` before the owner's last message):**
  - The list is `GET /api/eg-form-sources/users`: 246 users, all of them Acme company users, 0 from other companies.
  - About 22 old accounts have real customer email domains and no name, role or site.
  - The ticket does not claim "other companies' users", which would have been rejected.
- **Evidence:** `docs/bug-evidence/2026-10-14-technician-list-other-companies/`.
