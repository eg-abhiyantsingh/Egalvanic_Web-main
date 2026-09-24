# QA comment + screenshot on every remaining Ready-for-QA ticket (2026-09-24, evening)

**Prompt:** "add comment and screenshot in 20 remainings ticket" — the 20 Web v2.2 tickets still in Ready for QA after the
evening moves. This is the go-ahead for commenting on failing/partial/blocked tickets that was pending since 23 Sep.

## Format used on every comment
Build line (`index-CC4S9HsJ.js`, 24 Sep) + verdict in the first line → numbered steps in plain words → **What happens /
What should happen** → what QA could not do and what would close it → "Screenshot attached: …" → "For the developer" last.
No status changes; nothing but the comment and the image touched.

| Ticket | Verdict in the comment | Comment | Image(s) attached |
|---|---|---|---|
| ZP-4218 | Still fails (Sub stays No, no RFQ) — 3 builds | 44501 | 27 (after Convert, Sub = No) |
| ZP-4061 | Defect: customer seat WO list refused + Create offered | 44502 | 10 (customer WO page) |
| ZP-4042 | Defect: report history 500, no history section | 44520 (first try timed out in the connector) | 09 (customer Reports) |
| ZP-4138 | Positive half passes; negative half last failed 23 Sep | 44503 | 23 Sep/20 (tile + Access Denied) |
| ZP-4315 | Web 3 of 10; iOS side still missing (Krunal's image is web) | 44504 | 36 (ring tooltip) |
| ZP-4045 | 5 of 6 pass; human ack 400 + withdraw 500 block the last | 44505 | 35 + 33 |
| ZP-4060 | Legend + acknowledged segment ✔; human ack 400 | 44506 | 34 + 28 |
| ZP-4040 | Type UI renders; NULL case invisible from web | 44507 | 37 (PM Forms WO) |
| ZP-4272 | WO grid still forgets; Site Data grid remembers | 44508 | 23 Sep passes/17 |
| ZP-4153 | Seven walks "Nothing interpreted yet" | 44509 | 23 Sep/27 |
| ZP-4086 | Blocked: needs company-owned class with core attributes | 44510 | 38 (class editor) |
| ZP-4127 | Blocked: no device with a continuous SKM segment | 44511 | 41 (CB4 PowerPact HJ) |
| ZP-4185 | Cannot force a refresh 400 from the UI | 44512 | none — no screen |
| ZP-4363 | Support task, not a web change | 44513 | none |
| ZP-4128 | Asset-agent run, no web surface | 44514 | none |
| ZP-4176 | Merge chore; QA serves the merged build | 44515 | 40 (Updates panel, V1.36) |
| ZP-4183 | Android trace, no web surface | 44516 | none |
| ZP-4190 | AI-pipeline PR review; pieces covered on their own tickets | 44517 | none |
| ZP-4216 | Alembic graph; backend serving fine | 44518 | none |
| ZP-4261 | Ledger has no page; Activity Logs is a request log | 44519 | 39 (Activity Logs) |

## Method notes
- Attachments via the issue page's hidden `input[type=file]` (recipe in memory). **Navigating away in the same turn as the
  upload loses the file** — Jira had not finished the POST. Wait ~5 s and confirm with `GET /rest/api/3/issue/{key}?fields=attachment`
  before leaving the page. Four uploads (4218, 4045, 4060, 4040) were lost that way the first time and redone.
- Two Jira tabs in parallel halve the wall-clock; the Rovo connector's `addCommentToJiraIssue` can hang (ZP-4042, 300 s) —
  check the ticket's comments before re-posting.
- Bonus evidence: Admin → Activity Logs lists today's `acks/remove` 500 (trace f418ae51…) and the two ack 400s, while its
  tiles read "BLOCKED · 5XX 0" beside a 500 row.

Evidence: `docs/bug-evidence/2026-09-24-rfq-retest/` captures 36–41 + NOTES.md.
