# Technician role has web access and outranks the Project Manager — QA V1.36

**Env:** `https://acme.qa.egalvanic.ai` · **Date:** 2026-08-25 · **Severity:** High · **Writes made:** none
**Artifact:** https://claude.ai/code/artifact/8ca35956-54c9-4c95-92f9-108c3705a2cd (consolidated register — the per-report artifact was superseded)

Found by logging all seven role accounts into a real browser and comparing their live
`/api/auth/v2/me` permission sets against what each role can actually reach.

---

## RBAC-1 — HIGH — Technician holds `platform.web` and full sales-pipeline access

`RoleLoginE2ETest.technicianCannotAccessWeb` encodes the intent: a Technician should hold no
`platform.web` permission and should land on a **"Web Access Restricted"** page. Live, the account
logs straight into `/dashboard`.

```
GET /api/auth/v2/me   as abhiyant.singh+tec@egalvanic.com
  roles           [ "Technician" ]     <- single role, not an elevated overlay
  permissions     93
  platform.web    true                 <- the test asserts this must be false
  quotes.approve  true
  data.export     true
  landing         /dashboard
```

### Permission count by role (live, this run)
| Role | Permissions |
|---|---|
| Admin (reports as "Super Admin") | 111 |
| **Technician** | **93** |
| Project Manager | 90 |
| Account Manager | 79 |
| Facility Manager | 75 |
| Client Portal | 34 |

- **Technician ⊃ Facility Manager** — holds every FM permission except three view flags
  (`features.customers.view`, `features.opsdb.view`, `features.panel_schedules.view`).
- **10 permissions the Project Manager does not have:** `forms.manage`, `reports.manage`,
  `features.opportunities.view`, `features.accounts.view`, `features.arc_flash.view`,
  `features.condition_assessment.view`, `features.equipment_insights.view`, `features.planning.view`,
  `features.site_overview.view`, `accounts.view_detail_page`.
- Concrete consequence: the Technician can open the Forms Builder and Report Builder; the Project
  Manager gets **Access Denied** on both.

### What the role can see
Screenshot: `evidence/2026-08-25-rbac-technician/technician-sees-quotes-pipeline.jpeg` —
Draft $37.5K/16 quotes, Closed Won $37.1K/26, Cancelled $31.2K/85, 131 quotes listed with customer
names and per-row values, a **New Quote** button and a delete control on every row.

Also reachable: `/customers` (26 rows), `/assets`, `/sessions`, `/issues`, `/eg-forms`.

### Regression window — datable from CI
| Run | Date | Result | "technician has platform.web" assertions |
|---|---|---|---|
| 27755200923 | 2026-06-18 | success | 0 |
| 27761891658 | 2026-06-18 | success | 0 |
| 29005461732 | 2026-07-09 | failure | 3 ← first failure |
| 29095717609 | 2026-07-10 | failure | 3 |
| 31376201768 | 2026-08-10 | failure | 3 |
| 32725303359 | 2026-08-24 | failure | 3 |

The Technician role gained `platform.web` between **2026-06-18 and 2026-07-09** and has been in
that state for ~7 weeks.

### Reproduce
1. Sign in as `abhiyant.singh+tec@egalvanic.com` at `acme.qa.egalvanic.ai`.
2. You land on `/dashboard`, not a restriction page.
3. Open **Sales → Quotes** — full pipeline and 131 quotes.
4. Console: `fetch('/api/auth/v2/me')` → `permissions` contains `platform.web`, `quotes.approve`,
   `data.export`; `roles: ["Technician"]`.

### Open product question (drives severity)
Is the Technician role meant to reach the web portal at all? If **yes**, the permission set still
needs pruning — `quotes.approve`, `accounts.manage`, `data.export` are not field-technician rights.
If **no**, the `platform.web` grant is the whole bug.

**Scope:** QA only. Role definitions are known to drift QA↔prod — check the production Technician
role before sizing impact.

---

## Checked and NOT reported (candidates that did not survive verification)

| Candidate | Why it was dropped |
|---|---|
| PM gets Access Denied on `/eg-forms`, `/reporting/builder`, `/services` despite holding `forms.view`/`reports.view` | Gates key on `.manage`, not `.view`. PM genuinely lacks `forms.manage`/`reports.manage`. Correct behaviour. |
| PM's nav omits Planned Work despite `features.planned_work.view` | Nav keys on `features.planning.view`, which PM lacks. Technician and FM hold it and **do** see the link. Consistent. |
| Client Portal data exposure | 13 routes probed as the external role: 10 Access Denied, 3 render with zero rows. No leak. |
| `/admin/audit-log` hangs on "Loading…" | Renders **Access Denied** correctly; first read caught it mid-render. |
| `alliance-config` 500 (would block login per known incident) | Did not recur — 12/12 consecutive 200s. |

Route gating is otherwise solid: every role tested gets a clean Access Denied on admin routes it has
no claim to, and the backend enforces independently of the UI.

## Also observed (low confidence, not filed)
Landing route appears to be restored from `localStorage` and survives a **different** user signing in
on the same browser (FM landed on `/ops-dashboard` once and `/slds` another time; clearing storage
gave `/dashboard`). The landed route is still permission-gated, so nothing leaks. Worth a look at
session hygiene, not a defect as it stands.
