# 2026-11-13: ZP-5277 stage check: recipient job title in eg_get_plan_details

**Prompt:** "you just need to check that eg_get_plan_details query returns the recipients job title https://egalvanic.atlassian.net/browse/ZP-5277 just only this much in stage as we cant test this ticket fully in stage"

- Stage acme, build index-CbAeE1dg.js, STAGE_OPS seat.
- Ran the query through `POST /api/reporting/test-query/run` with `{primary_id: <plan>, page_config: {query/render_query: eg_get_plan_details, type: page}, type: plan_standard, reporting_config_id: <Short Form Proposal>}`.
- **Result: PASS.**
  - `recipient.job_title`, `recipients[].job_title` and root `contact_job_title` are present and follow the chosen recipient: A P "Manager", Acme Stage "D-Cathelon". All 11 contacts return their own title.
  - With no recipient: `recipient` null, `recipients` [], `contact_job_title` "Client" (the customer's first contact).
- Comment [46172](https://egalvanic.atlassian.net/browse/ZP-5277?focusedCommentId=46172). Status left In Progress (assigned Avani; the owner asked only for this check).
- **Data changed on stage:** the QA-DEMO quote "Site Walk — QA-DEMO ZP-4980 export to quote (site set)" (96251f9f) now has recipient "Acme Stage". It had none before, and the UI can't clear a recipient once set (side note in the comment).
- Not checked: the KROON print with Energized Maintenance Proposal [EN], re-printing after a title edit, a contact with no title.

## 15:15 IST: sign-off posted in eg-internal-dev (owner: "give sign off in group for this ticket dev group")
- Context in the space: the owner had asked Avani "is this ticket going in hot-fix or not?"; Avani replied "Yes, it is … I am deploying the changes in Stage" (~14:36 IST), before my stage runs.
- Re-checked just before posting: stage still on index-CbAeE1dg.js, /api/health 200.
- New message tagging @Avani Patel, with three bullets:
  - PASS: recipient.job_title / recipients[].job_title / contact_job_title follow the recipient (A P → Manager, Acme Stage → D-Cathelon).
  - No recipient → contact_job_title still returned (fallback).
  - Not checked: the KROON print with the [EN] template.
  - Closing line: "QA sign-off from my side for the hotfix", with the Jira comment 46172 link.
- Chat composer trap: a line starting with "- " becomes a bulleted list, and the next shift+Return lines stay bullets, so typed "- " shows up literally. Type list lines without "- " (or delete the stray "- " with keyboard edits), and press Backspace at the start of the final line to leave the list.
