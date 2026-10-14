# Web v2.4: third set of 10 tickets (13 Oct 2026, afternoon)

- **Build:** QA moved to `index-C-Cc1qD1.js` during this round.
- **Picked:** nine tickets whose 12 Oct verdicts were never posted, each re-verified before posting, plus ZP-4823.
- **Jira:** all writes from abhiyant.singh@; the account id was checked before every write.

| Ticket | Result | Jira |
|---|---|---|
| ZP-4481 SLD issue for custom classes | PASS (re-checked) | 45126 → RTR |
| ZP-4597 node groups in All Nodes | PASS (re-checked) | 45127 → RTR |
| ZP-4601 note author byline | PASS (re-checked) | 45128 → RTR |
| ZP-4645 issue severity + SKM export gate | PASS (re-checked) | 45129 → RTR |
| ZP-4594 SLD canvas notes | FAIL: new note shows placeholder; re-opening it then erases the saved text (data loss) | 45130 → To Do |
| ZP-4511 journal review | mostly PASS; re-read hangs new transformers from the board (only 4–5 of 12 feeders kept); empty state says "Run Interpret" | 45131, stays In QA |
| ZP-4535 journal capture menu | FAIL: "NQ130L1C" saved as catalog number 130.0; re-read dropped 8 accepted breakers | 45132 → To Do |
| ZP-4561 site walk release | FAIL: assets created from a walk room have no location | 45133 → To Do |
| ZP-4564 walk issues | FAIL (High): re-read copies existing issues and brings back a dismissed one | 45134 → To Do |
| ZP-4823 report status shows AWS details | PASS | Ready for QA → In QA → comment 45125 → READY TO RELEASE |

Details for ZP-4823:
- 6 bad handles (CP and Super Admin seats) all return 400 "Invalid report id" with no AWS text.
- Positive control: `generate_simple` on the QA-DEMO WO polled RUNNING → SUCCEEDED.
- Evidence: `docs/bug-evidence/2026-10-12-v24-qa/zp-4823/`.

All 10 posted.
