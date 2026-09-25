# ZP-4407 filed for Krunal: Client Portal user is offered "Create Work Order" (2026-09-25)

**Prompt:** "create a jira ticket that client portal user should not have option to create work order assign to krunal
current sprint" (with a screenshot of the Client Portal user's Maintenance Portal → Work Orders page).

| Field | Value |
|---|---|
| Key | [ZP-4407](https://egalvanic.atlassian.net/browse/ZP-4407) |
| Summary | Web: Maintenance Portal — a Client Portal (view-only) user is offered "Create Work Order" |
| Type / priority | Bug · Medium |
| Assignee | Krunal lunagariya |
| Sprint | Z-26-09-S2 (1222, active, ends 26 Sep) |
| Fix version | Web v2.2 |
| Status | Backlog → To Do |
| Attachment | the owner's screenshot (`docs/bug-evidence/2026-09-25-v22-release-day-sanity/11-…png`) |

Description: steps (sign in as Client Portal → Maintenance Portal → Work Orders), what happens (Create Work Order offered),
what should happen (not shown for a view-only role; the create screen refuses it if reached another way), and a
"For the developer" line: the role holds only `*.view` + `notes.manage` (35 permissions, `/api/auth/me` 24 Sep).
Related ZP-4061 (the same page's list reads "No rows" for this role) — referenced, not changed.
