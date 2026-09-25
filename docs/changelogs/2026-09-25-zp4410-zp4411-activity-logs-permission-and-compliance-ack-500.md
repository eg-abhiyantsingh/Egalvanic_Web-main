# ZP-4410 and ZP-4411 filed for Krunal (2026-09-25)

**Prompt:** "in qa activity log are showing permission sab ma sa nikal da. create a jira ticket assign to krunal. and also
create one more ticket [screenshot: Compliance → Acknowledge deviations → API call failed: 500] for this assign to krunal
so total 2 ticket".

| Ticket | Summary | Priority | Screenshot |
|---|---|---|---|
| [ZP-4410](https://egalvanic.atlassian.net/browse/ZP-4410) | Web: Activity Logs — remove the Activity Logs permission from all roles | Medium | Admin → Activity Logs page |
| [ZP-4411](https://egalvanic.atlassian.net/browse/ZP-4411) | Web: Compliance — acknowledging deviations fails with a raw "API call failed: 500" error | High | owner's screenshot (61 deviations, trace 0ac59e72…) |

Both: Bug · Krunal lunagariya · sprint Z-26-09-S2 (1222) · fixVersion Web v2.2 · Backlog → To Do · attachment verified
via the Jira API. ZP-4411 also records QA's single-deviation 400 and withdraw-500 findings and links ZP-4045/4060; the
Defect Register's acknowledge rows can now point at it.
