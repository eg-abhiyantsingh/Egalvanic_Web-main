# How to create Facility Manager (FM) and Client Portal (CP) users on QA

**Date:** 2026-09-16
**Prompt:** how are Facility Manager and Client Portal users created on QA
**Env:** `acme.qa.egalvanic.ai` · Admin seat (`abhiyant.singh+adminqa@`, 108 perms) · company `d59d449b-09d8-45d6-8f0a-ef70024b1293`

## Finding: FM and CP are not platform users

Admin → Platform Users (`/users`, "Create User") offers only **5** roles in its Roles picker: Technician,
Project Manager, Account Manager, Electrical Engineer, Admin. Facility Manager and Client Portal are absent
there — even though `GET /api/users/roles/company/{companyId}` returns **9** roles for the company,
including Facility Manager (`54021b71-a055-4c58-91e1-05c705f643f4`) and Client Portal
(`2a85145f-31ca-4e2c-8dd2-82c958cd6380`), plus Super Admin and Portal Sales.

They are **account-scoped guest portal roles**, created as a **contact** on a customer account with
"Portal Access" switched on:

`/accounts/{accountId}` → Contacts tab → **Add Contact** → fill First/Last/Email/Job Title (Job Title is
required) → optional "Primary Site Overseen" (only the account's own sites are offered) → toggle
**Portal Access** → pick Role: Facility Manager or Client Portal → optionally tick "Send a temporary
password" → **Add Contact**.

API behind the dialog: `POST /api/account/{accountId}/users` (`mainAPIService` — "Creating account user").
Domain rules come from `GET /api/account/{accountId}/allowed-domains`.

## Gate: License Type decides which roles are offered

The account's License Type (`no_license` / `read_only` / `interactive`) controls the Role list:
**Interactive** → both FM and CP offered; **Read-only** → Client Portal only; **No license** → no portal
roles (empty list). The dialog states this directly: *"Available roles are determined by the account's
Interactive license."*

## Read-only listing page

Admin → Guest Portal Users (`/guest-portal-users`, `GET /api/users/guest-portal?page&page_size`) lists
every portal user with Customer / License / Roles / Status columns. It has **no create button** — its own
empty state says to "grant portal access from a customer account's Users tab."

## Defect found: allowed-domain check breaks on bare-label domains

Worth a ticket. The Add-Contact email-domain check is an **exact match on the full domain**, but
`allowed_domains` comes back as a **bare label**. From the bundle:

```
G = allowed domains list
pe = email.split("@")[1].toLowerCase()
if (!G.includes(pe)) → error "Email must use an allowed domain ({{domains}})"
```

while the on-screen hint uses a wildcard formatter: `r.includes(".") ? "@"+r : "@"+r+".*"`.

Live proof:
- Account "AP New ACC" → `allowed_domains: ["egalvanic"]`, hint reads "Allowed: @egalvanic.*", but
  `abhiyant.singh+qafm0916@egalvanic.com` was **rejected**: "Email must use an allowed domain
  (@egalvanic)."
- Same on account "Abhiyant" → `allowed_domains: ["acme"]`, hint "@acme.*", same rejection pattern.

So on any account whose allowed domain is a bare subdomain label (not a full domain), **no email can pass
and no portal user can be created from the UI** — the hint promises a wildcard match the create-check
doesn't implement. Editing an existing contact skips the check entirely (email splits into a locked domain
suffix on edit).

## Workaround that works today

Use an account whose `allowed_domains` is **empty** — the check is skipped when the list is empty.
Verified by creating two seats on "Default EG-ACME Account"
(`8756b414-72ce-4a58-8d4a-1eb2b6598473`, license interactive, `allowed_domains: []`):

| Name | Email | Role | Temp password sent |
|---|---|---|---|
| QA-DEMO FM Sept16 delete me | `abhiyant.singh+qafm0916@egalvanic.com` | Facility Manager | yes |
| QA-DEMO CP Sept16 delete me | `abhiyant.singh+qacp0916@egalvanic.com` | Client Portal | yes |

Both now appear in the account's Contacts tab. Full URL:
https://acme.qa.egalvanic.ai/accounts/8756b414-72ce-4a58-8d4a-1eb2b6598473

## How to reach `/accounts/{id}` — there's no nav entry

`/accounts` redirects to `/customers`, and the Admin → Customers rows only **expand** (row click toggles;
the row menu is Edit Customer / Add Site / Manage Access List / Delete Customer — none link to the detail
route). The only in-app links to `/accounts/{id}` in the bundle are:

1. Admin → Setup dashboard "Action Items" cards (*"X has no sites"* / *"X has no contacts"*)
2. The "Manage contacts" icon in a quote/plan contact picker → `/accounts/{id}?tab=contacts`

The detail route itself is guarded by permission `accounts.view_detail_page`.

## Existing seats for reference

`+fm@egalvanic.com` (Facility Manager) and `+clientportal@egalvanic.com` (Client Portal) are portal users
on the "Abhiyant" account (`43f1d80a-9214-434f-b26a-44d0de07160d`, license interactive).

## Deliverables

- Screenshot — `.playwright-mcp/fm-cp-add-contact-dialog.png` (filled Add Contact dialog showing the FM/CP
  role cards and the Interactive-license line)

## Footprint

Two labelled portal-user contacts on "Default EG-ACME Account" (QA-DEMO FM Sept16 delete me, QA-DEMO CP
Sept16 delete me) — QA-DEMO/delete-me labelled, no cleanup required. Everything else read-only.
