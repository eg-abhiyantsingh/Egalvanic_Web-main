# Round-3 QA comments posted on Jira — 6 Oct 2026

One comment per ticket, body = the `ZP-<n>.txt` beside this file (Jira wiki markup), screenshots attached to the issue as
`QA-6Oct-<n>-<k>_<slug>.png` and shown inline at width 640. Posted from the signed-in Jira page through the REST API
(`POST /rest/api/3/issue/<key>/attachments`, then `POST /rest/api/2/issue/<key>/comment`) after a server-side render check
(no `span.error`, every image resolves, caption count matches).

| Ticket | Comment id | Attachments | Verdict line |
|---|---|---|---|
| ZP-4421 | 44882 (+ 44883 duplicate, see below) | 4 | PASS |
| ZP-4590 | 44884 | 2 | FAIL on QA — the new severity formula is not on QA acme |
| ZP-4148 | 44885 | 4 | FAIL as written — and the rule changed in today's build |
| ZP-4301 | 44886 | 1 | PARTLY TESTED — the server names the view; the web app still never shows it |
| ZP-4305 | 44887 | 2 | PARTLY TESTED — unchanged, 2 small defects |
| ZP-4372 | 44888 | 1 | PASS for the web part — the database checks still need database access |
| ZP-4394 | 44889 | 1 | PARTLY TESTED — unchanged |
| ZP-4423 | 44890 | 1 | NOT TESTABLE FROM THE WEB APP — unchanged |
| ZP-4529 | 44891 | 1 | NOT CONFIRMED — unchanged |
| ZP-4080 | 44897 | 6 | PASS — the whole print flow works on QA (addendum 2, 15:55–16:25 IST on index-CfC0Y-hD.js; held in Ready for QA: no fix version, no written acceptance) |
| ZP-4435 | 44892 | 7 | FAIL on the keyboard path — everything else passes (addendum, tested 14:40–16:20 IST on index-CfC0Y-hD.js) |

**ZP-4421 duplicate:** comments 44882 and 44883 are identical and were created 1 ms apart (03:24:46.166 / .167 CDT). The
first attempt uploaded the screenshots through Jira's own hidden file input, which froze the page renderer; the comment
request had already gone out, and the retry posted it again before the duplicate guard could see the first one. The
duplicate was left in place (comment deletion is the owner's call); delete 44883 from its "..." menu.

**Status changes this round:** ZP-4421 Ready for QA → READY TO RELEASE (13:38 IST, transition 7, status only); ZP-4435 Ready for QA → To Do
(addendum, transition 2: the keyboard path never opens the photos — certain on the current build, control = the mouse path works).
Everything else stays in Ready for QA with the reason in its comment.
