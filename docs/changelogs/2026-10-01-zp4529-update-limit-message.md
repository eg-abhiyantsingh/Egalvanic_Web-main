# ZP-4529 updated — the expected result is a "limit used up" message with the reset time (1 Oct 2026)

**Prompt:** "update the ticket actually the user should get valid message that he used limit is over and reset message will
also he will get".

## Jira change (only the ticket's text, as asked)
https://egalvanic.atlassian.net/browse/ZP-4529 — summary and description rewritten; status (To Do), assignee (Avani),
priority (Medium), sprint (Z-26-09-S3), fixVersion (Web v2.3) and the attached screenshot are unchanged. No comment added.

- **New title:** "Web: Reporting › Edit with AI — when the AI usage limit is used up, the dialog shows "Could not start the AI
  edit (HTTP 200)" instead of saying the limit is reached and when it resets".
- **What should happen** now says: a clear message that the user's AI usage limit is used up, plus when it resets
  (example: "You've used up your AI editing limit. It resets on 2 Oct 2026 at 9:00 AM."), and no "HTTP 200".
- **Steps** step 1 now starts from a user whose AI usage limit is used up.
- **For the developer:** the editor only shows the reply's `error` / `message` and otherwise falls back to "Could not start
  the AI edit (HTTP <status>)". The web build has no limit-reached handling for this call; show the limit message and reset
  time from the reply, and the server must send them if it doesn't yet.

Checked before writing: the QA build (`index-DXSz_B6n.js`, all 290 chunks) has no AI limit / quota / "resets" message anywhere
(only mock activity-log rows and the error tracker's own rate-limit code).

## Release page
https://claude.ai/artifact/3e3VwLmEk5fDa7pQEyb24y (version 3): the "Bugs filed" row, the note under it, the ZP-4398 pointer and
the header chip use the new wording.
