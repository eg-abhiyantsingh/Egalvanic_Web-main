# QA retest — 3 tickets (2026-08-27)

**Env:** acme.qa.egalvanic.ai · V1.36 · Writes: none
**Artifact:** https://claude.ai/code/artifact/40556cee-a655-41ae-b825-1012b7924847

| Ticket | Verdict |
|---|---|
| Web: Technician can see the whole sales pipeline | ✅ **FIXED** |
| Quotes Generation API returns 500 | ✅ **FIXED** |
| Web: Make all menu options permission-based | ❌ **NOT FIXED (not started)** |

## Technician web access — FIXED
`/auth/v2/me`: `platform.web` now **false** (was true), 93 → **92** permissions. Landing is the
**Web Access Restricted** page; `/opportunities` also blocked. Matches the ticket's Expected result.
- Not a regression, but note: the fix dropped `platform.web` only — the Technician still holds
  `quotes.approve`, `accounts.manage`, `data.export`. Harmless on web now; still applies on mobile.

## Quotes Generation 500 — FIXED
`POST /api/plans/{id}/generate`:
- Ticket's plan `3ff9c375` (empty draft) → **400** `standard.pm_standard_id is required` (was **500** + trace_id). A validation 400 on an empty draft is correct.
- Happy path — draft `6d6e1f0b` with a real `pm_standard_id`, body `{standard:{pm_standard_id}}` → **200 in ~0.6s**, generated plan of **71 assets / 312 covered**. Generation works end to end.
- Method correction: my first attempts 400'd on an **empty body**; the endpoint needs the field in the request, not read from the stored plan.

## Menu permissions — NOT FIXED
Checked Admin, Facility Manager, Client Portal, Project Manager:
```
maintenance.*   permissions → NONE (all roles)
menu.* / nav.*  permissions → NONE (all roles)
```
Fails its own acceptance criteria: no permission gates any menu option; Maintenance still shows for
Client Portal + FM and is absent for Admin + PM with nothing in the permission set driving it; there
is no permission to toggle. The route is still ungated too — a Technician (no Maintenance nav entry)
opens `/maintenance/overview` by URL and gets the full record.
- **Same underlying gap as Bug 3a** (`docs/bug-reports/2026-08-26-BUG3-maintenance-program.md`).
  Fixing this ticket closes Bug 3a. Link them in Jira.

Evidence: `evidence/2026-08-27-ticket-retest/` (v_ticket2_fixed, v_ticket3_fixed, v_ticket1_notfixed).
