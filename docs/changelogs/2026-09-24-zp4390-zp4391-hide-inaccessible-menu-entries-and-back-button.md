# ZP-4390 and ZP-4391 filed for Avani (2026-09-24)

**Prompts:** "instead of lock just hide the tab that is not accessible to user. create a ticket assign to avani and for
all pages we need to cover this to do web 2.2 version" (screenshot: Site Data → Reports, Access Denied) · "assign one
more ticket to avani that back button should be visible below sign in" (screenshot: password form).

| Ticket | Summary | Fields |
|---|---|---|
| ZP-4390 | Web: Menu shows pages the user cannot open — hide them instead of leading to "Access Denied" (all sections) | Bug · Medium · sprint 1222 Z-26-09-S2 · fixVersion Web v2.2 · To Do · Avani |
| ZP-4391 | Web: Password sign-in form — the Back control should be a visible button directly below Sign In | Bug · Medium · sprint 1222 · Web v2.2 · To Do · Avani |

Both reproduced on `index-CC4S9HsJ.js` before filing. ZP-4390: the +admin seat (Super Admin · Admin · Portal Sales)
sees Maintenance Program / Compliance / Reports under Site Data and every one opens Access Denied; the ticket asks for
menu entries to be hidden wherever the route would refuse, across all sections (ZP-4138 covers the portal tile).
ZP-4391: "Back to faster options" is a footnote-sized text link under "Forgot your password?".

Evidence: `docs/bug-evidence/2026-09-24-rfq-retest/29-…` and `30-…`. Attachments could not be uploaded through the
Jira connector; the owner's own screenshots are the primary captures.

Side effect to note: while dismissing the profile popover on QA, the click landed on **Sign Out** — the shared Chrome
session is now on the login page and needs a fresh sign-in by the owner.
