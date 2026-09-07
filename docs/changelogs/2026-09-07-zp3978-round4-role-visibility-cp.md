# 2026-09-07 — ZP-3978 round 4: role visibility — can an account's users (CP etc.) see that account's work orders?

**Prompt:** "always check for roles too like cp or other role that we have added in account can able to see
that work order that is present in their account" (+ "for no license you have change to full access as admin",
"we already have user you can just select existing user", Manage Access screenshot).

## Answer: NO — and it fails twice over

| Role | Sees its account's WOs? | What happened |
|---|---|---|
| **Client Portal** (`+clientportal@`, acct "Abhiyant") | **NO — blocked** | No Work Orders nav entry; `/sessions` blank; `/sessions/{id}` → **Access Denied**, even for a WO on the site their own account owns |
| **Facility Manager** (`+fm@`, acct "Abhiyant Singh") | YES | Opens WO-2 fully — a WO mapped to Acct B on a site owned by the "Abhiyant" account (neither is FM's account) |

## The model, measured not assumed
`GET /api/sld/{id}/access-list` returns every person with `has_access` + `via`:
- Foreign site (Android Site 2, acct "Test op") → CP user `has_access: true, via: "assigned"`
- The site the CP's **own account owns** → `has_access: false, via: null`

So **visibility is per-site explicit assignment + role module permissions — never account ownership**.
The WO's `account_id` is an attribution label, not an access control.

**Deliberately NOT reported as a leak:** the CP seat carries 234 of the tenant's 238 sites. That breadth
comes from `via: "assigned"` (QA seeding), not a broken scope rule. Checking `via` before claiming
over-exposure is what kept this from becoming a false HIGH.

## DEFECT found — Client Portal advertises work orders it cannot open
The CP dashboard shows an **Active Work Orders** tile (22) and `/auth/me` grants `sessions.view` +
`workorders.view`, but no nav route exists and the direct URL is refused. Permissions/tile and
route/nav disagree, and the customer-facing role is the one left at the dead end.

## Consequence for the ticket
QA step 5 ("both accounts can access the site during transition") **has nothing to verify** — access
never followed ownership. During a Cecco → Meta transition neither side's portal users gain or lose
anything automatically; an admin must add and remove people site by site.

## Side findings
- **Account license type cannot be changed after creation** — Edit Customer has no license field and
  the account page shows License Type read-only. A "No license" account (which blocks all portal users)
  can never be upgraded through the UI.
- The account-level **Manage Access** dialog (owner's screenshot) is the correct place to grant existing
  users access to an account — Manage Access on a *site* is a different, per-site list.

## Deliverables
- Artifact (updated, same URL): https://claude.ai/code/artifact/7a4e0040-cbaf-48cb-a0b4-6ef05626c432
- `docs/bug-reports/2026-09-07-zp3978-site-account-ownership-verdict.md` (Round 4 section)
- `docs/bug-evidence/zp-3978-cp-role-visibility/01-cp-access-denied-own-account-wo.jpg`, `02-cp-dashboard-advertises-work-orders.jpg`
- New memory: `feedback_always_test_account_role_visibility`

## Depth notes (learning)
- **Why admin-seat testing was insufficient:** rounds 1–3 proved the data model is correct. Round 4 shows
  the correct data reaching nobody — the customer-facing role can't open it. Model correctness and
  customer visibility are independent properties and need independent tests.
- **`via` is the field that separates a bug from test data.** "CP sees 234 sites" reads like a serious
  leak until you see `via: "assigned"`. Always find the mechanism before assigning severity.
- **Isolated browser contexts** (`page.context().browser().newContext()`) let one tool call drive the
  admin session and a second role's session simultaneously — admin mutates, other role re-reads.
