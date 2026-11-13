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
