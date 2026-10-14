# Web v2.4: fourth set of 10 tickets (13 Oct 2026, evening)

- **Build:** QA `index-C-Cc1qD1.js`.
- **Jira:** all writes from abhiyant.singh@; the account id was checked before every write.
- **Memory:** saved the sanity-testing method and progress (owner: "sanity testing save in memory").

| Ticket | Result | Jira |
|---|---|---|
| [ZP-4820](https://egalvanic.atlassian.net/browse/ZP-4820?focusedCommentId=45135) issues list 500 on list filter | PASS | Ready for QA → In QA → READY TO RELEASE |
| [ZP-4824](https://egalvanic.atlassian.net/browse/ZP-4824?focusedCommentId=45136) Z University unknown page 500 | PASS | Ready for QA → In QA → READY TO RELEASE |

Details:
- **ZP-4820:** lists now work as "any of" (Open + Resolved = 67 = 64 + 3). Bad shapes get 400. No 500s.
- **ZP-4824:** unknown pages now return 400 "Page not found" (the ticket asked for 404, noted in the comment); the real page returns 200.

| [ZP-4581](https://egalvanic.atlassian.net/browse/ZP-4581?focusedCommentId=45138) AF readiness location filter | filter PASS; 1366 px hides Connection Details tab | stays In QA |
| [ZP-4751](https://egalvanic.atlassian.net/browse/ZP-4751?focusedCommentId=45139) dataset select search | PASS (web) | READY TO RELEASE |
| [ZP-4753](https://egalvanic.atlassian.net/browse/ZP-4753?focusedCommentId=45140) dataset multiselect search | PASS (web); Low: search clears after each tick | READY TO RELEASE |
| [ZP-4570](https://egalvanic.atlassian.net/browse/ZP-4570?focusedCommentId=45141) SLD quick photo view | partial: two photo cards on one hover (fix ZP-4832 on dev only) | stays In QA |
| [ZP-4591](https://egalvanic.atlassian.net/browse/ZP-4591?focusedCommentId=45142) connect onto unwired terminal | PASS (web) | READY TO RELEASE |
| [ZP-4799](https://egalvanic.atlassian.net/browse/ZP-4799?focusedCommentId=45143) placeholder dots | PASS (web) | READY TO RELEASE |
| [ZP-4800](https://egalvanic.atlassian.net/browse/ZP-4800?focusedCommentId=45144) hide/show connections | PASS (web); Low: PDF TO tag hidden behind symbol on left terminal | READY TO RELEASE |
| [ZP-4803](https://egalvanic.atlassian.net/browse/ZP-4803?focusedCommentId=45145) walks/reset/combine | canvas PASS; dual-asset + PDF dot rows unproven | stays In QA |

Unfiled leads (need owner go-ahead): ZP-4753 search reset (Low); CP typed /arc-flash opens full page + Export (ZP-4699 family); CP gets full 246 users list incl. 57 emails; builder Web View preview dataset fields empty; forms without key listed under another form's name; ZP-4800 PDF TO-tag overlap (Low); EE unassigned to site got 200 on PUT /api/edge/update (saved, restored) and PM 200 on PUT /api/node/update for an unassigned site (Medium, site-scope write).
