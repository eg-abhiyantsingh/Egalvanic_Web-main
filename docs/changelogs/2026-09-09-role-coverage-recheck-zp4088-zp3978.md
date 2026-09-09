# Role coverage recheck — ZP-4088 details panel and ZP-3978 account ownership

**Date:** 2026-09-09
**Time:** 12:25 – 13:05 IST
**Prompt:** “did you check this for all roles right” (about ZP-4088 / PR #1391 and ZP-3978)

---

## Answer

**No.** Both verdicts were produced on ONE seat — the `+admin` multi-role seat with Super Admin
active. Asked the question, I re-ran both tickets' surfaces across all seven QA roles, and two
checklist items turn out to be role-dependent.

## The gate

Both surfaces read the same permission pair (deminified from today's QA bundle
`index-jYhUcFb4.js`):

```js
const Yli = ["accounts.view", "features.accounts.view"];
function Sot(e) { return U8(r => r.hasAnyPermission)(e) }

// SessionDetail — the row ZP-4088 asks QA to verify
const C = Sot(Yli);
… C && detailRow(t("common.account"), session.account_name) …

// Sessions list — the Account column + Account filter (ZP-3978's "view WOs by account")
const Pr = Sot(Yli);
… accountId: Pr && Ir?.id || undefined … Pr && <Autocomplete label={t("common.account")} …>
```

## Measured per role (live logins, not inferred)

| Role | perms | accounts.view | sites in scope | WO list | Account column | Panel | Account row |
|---|---|---|---|---|---|---|---|
| Super Admin / Admin | is_admin | yes | all | 148 open | shown | renders | **shown** (8 rows) |
| Project Manager | 94 | yes | 114 | 996 | shown | renders | **shown** (8 rows) |
| Account Manager | 77 | yes (+features) | 113 | 988 | shown | not reached in UI this run | — |
| Electrical Engineer | 80 | yes | 4 | 71 | shown | not reached in UI this run | — |
| Facility Manager | 75 | **no** | 11 | 5 (all closed) | **absent** | renders | **absent** (7 rows) |
| Client Portal | 35 | no | 234 | `422 permission_denied` | n/a | n/a | n/a |
| Technician | 95 | yes | — | no web access | n/a | n/a | n/a |

## Findings

1. **Medium, beyond both tickets — a work order outside your site scope opens anyway.**
   `GET /api/ir_session/b2c2657a…/full`, `/team` and `/summary/v2` all return **200 with the full
   payload** (name, `account_name`, `account_id`, team, assets, method lines) for a work order on
   “Addtioanl Site”, which is **not** in the Facility Manager's 11 assigned sites — and the detail
   page renders completely. Same for PM, AM, EE and **Client Portal** (35 perms). Within one tenant,
   and those roles do hold `sessions.view` globally, so site scope may be intended as a filter rather
   than a boundary — but the list endpoint `POST /company/{id}/workorders/v2` *does* scope by it
   (FM 5, EE 71, PM 996), so the two halves disagree. Note it is `account_name` — the field the UI
   hides from FM — that the API hands over.
2. **Low, ZP-3978 step 2 — FM can edit sites but cannot read accounts.** FM holds
   `locations.manage`, but `POST /api/account/v2` → **422 permission_denied**. Account is a
   *required* field on Edit Site, so the ownership-transfer flow is effectively Admin / PM / AM only.
   Not proven by attempting a save (read-only recheck).
3. **Boundary, not isolated:** AM and EE list APIs work (988 / 71) yet neither seat reached the WO
   detail page in the browser this run — could be the V1.36 two-level nav, a feature gate, or my
   session priming. Recorded as “not reached”, not as a defect.

## Checked and dismissed (a near-miss false positive)

FM's Work Orders grid opens **0–0 of 0** while the API returns 5 work orders for the same seat. That
looks exactly like a scoping bug. It is not: all five carry `active: false`, and the grid defaults to
Status = **Open**. Switching the filter to **Closed** shows **1–5 of 5**. The positive control is
what settled it — the empty state proved nothing on its own.

## Also confirmed

- **ZP-4088's restore is still live.** `showDetailsInCompact` is present in today's QA bundle
  (`index-jYhUcFb4.js`, 2 occurrences), so the dev→qa promotion revert the ticket warned about has
  not happened yet. Still needs landing on `cicd/dev`.
- **No EG-Admin contamination.** Every role seat now returns exactly one role from `/auth/me` — the
  overlay that used to make the RBAC suite skip is gone.
- **`accounts.view` is present for PM on QA today**, contradicting the older “known QA drift” note.

## Depth explanation — the method, and what it teaches

**1. The code tells you which questions are role-dependent; only the login tells you the answer.**
Grepping the bundle for the gate (`Sot(Yli)`) took one minute and located *both* affected surfaces —
including the WO-list Account column, which nobody had thought to check. But the gate alone does not
say who is affected: `accounts.view` had to be read from live `/auth/me` per seat, and the old
memory note (“PM lacks accounts.view on QA”) was stale. Code for *where*, live login for *who*.

**2. Priming a browser session from an API login beats logging in as each role.** QA now forces MFA
enrollment on sign-in. Rather than enrol six QA role accounts (which would break the headless RBAC
suite that logs in as them), the session was primed by calling `/api/auth/login` from *inside the
page*, so the browser itself received the HttpOnly cookies:

```js
await page.evaluate(() => fetch('/api/auth/login',
  { method:'POST', credentials:'include', headers:{'Content-Type':'application/json'},
    body: JSON.stringify({ email, password, subdomain:'acme' }) }));
```

Caveat learned the hard way: this leaves the *previous* seat's `localStorage` in place, so filter
state can carry across seats. The FM result was therefore re-confirmed with a real login-form
sign-in and cleared storage before being written down.

**3. “Set up later” is still on the MFA dialog for the role seats** — contrary to the note in memory.
Password-only sign-in still works for `+fm`, `+project`, `+tec`, `+accountm`, `+clientportal`. The
`+admin` seat is genuinely enrolled: API login returns **426 UPGRADE REQUIRED** and the UI asks for
an authenticator code or an emailed OTP.

**4. Side effect to be aware of:** clearing cookies to swap seats destroyed the pre-existing admin
browser session, and it cannot be restored without the Email OTP. Future UI work should use a role
seat (PM is the closest to admin: 94 perms, `accounts.view`, `locations.manage`) or the owner
supplies the code.

## Files

- `docs/report-artifacts/2026-09-09-QA-role-coverage-zp4088-zp3978.html` (new, 170 KB) →
  https://claude.ai/code/artifact/7aa2ccd7-13c2-4357-a4f1-0a2e3ecda48c
- `docs/bug-reports/2026-09-07-pr1391-wo-details-panel-restore-verdict.md` (role-coverage addendum)
- `docs/bug-reports/2026-09-07-zp3978-site-account-ownership-verdict.md` (role-coverage addendum)
- `docs/bug-evidence/roles-wo-panel-account-gate/` (3 QA screenshots)
