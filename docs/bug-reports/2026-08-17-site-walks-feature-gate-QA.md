# Site Walks — gate the surface on a company feature flag · QA verdict

**Tested:** 2026-08-17 · **Build:** QA **V1.36** · **Tenants:** `acme.qa.egalvanic.ai` (feature **ON**) + `demo.qa.egalvanic.ai` (feature **OFF**)
**PRs:** eg-pz-frontend **#1114** (gate Site Walks on a company flag) · eg-pz-backend **#946** (feature key + alembic merge)
**Ticket:** *Site Walks: gate the surface on a company feature flag* (created retroactively by the PR merge monitor)

---

## Verdict

**4 of 5 web checks PASS. One fails: the route table is NOT gated — direct URL entry reaches the full Site Walks surface on a company that does not have the feature.** That is the ticket's own QA item #2, verbatim: *"Confirm the navigation.js route table is gated too, so direct URL entry is covered."*

The sidebar half of the change is well done — greyed, lock icon, correct tooltip, correct convention. Only the route-table half is missing.

## Why these two tenants are the right test

Both tenants hold the **permission**; only one holds the **feature**. That is exactly the situation the gate exists for (the PR: *"it showed for every role holding `site_walks.view`"*).

| Tenant | `site_walks.view` permission | `site-walks` in `company_features` | Expected |
|---|---|---|---|
| **acme** (`d59d449b…`) | ✅ held | ✅ held (33 features) | surface available |
| **demo** (`93611164…`) | ✅ held | ❌ **not held** (31 features) | greyed + tooltip, route blocked |

Source of truth: `GET /api/auth/v2/me` → `company_features[]`. Demo holds every other group add-on (`sales-core`, `ops-core`, `emp`, `eng-lib`, `reporting-v2`, `eg-forms`) and lacks only `site-walks` — a clean single-variable test.

---

## Results

| # | QA item (from the ticket) | Result |
|---|---|---|
| 1 | Without the feature, `/site-walks` is **greyed with a tooltip, not hidden**, for every role holding `site_walks.view` | ✅ **PASS** |
| 2 | The **navigation.js route table is gated too, so direct URL entry is covered** | ❌ **FAIL** |
| 3 | Enable the feature → the surface becomes available | ✅ **PASS** |
| 4 | Behaves like the existing group add-ons | ✅ **PASS** (sidebar); ❌ inherits the same route-gate gap |
| 5 | `alembic upgrade head` runs cleanly against a single head | ⚠️ **NOT VERIFIABLE BY ME** — backend/DB, no access |

### ✅ QA-1 — sidebar gating is correct

On demo, the *Site Walks* item under **SALES** renders **visible but disabled**:
* `aria-disabled="true"`, `Mui-disabled`, `opacity: 0.38`, `pointer-events: none`, **no `href`**
* a **lock icon** on the row
* tooltip on hover: **"Site Walks isn't enabled for your company. Contact your admin to enable."**
* the same string is exposed as `aria-label` on the wrapper `<span>` — so it is reachable by screen readers, and the `<span>` wrapper is the correct MUI workaround for tooltips on disabled elements (a common bug this PR avoided)

Evidence: `docs/bug-evidence/site-walks-feature-gate/demo-greyed-tooltip.png`

### ❌ QA-2 — direct URL entry is NOT gated (the defect)

On the same demo session, typing `https://demo.qa.egalvanic.ai/site-walks` in the address bar:

* **no redirect** — the URL stays `/site-walks`
* **the complete Site Walks surface renders**: page header, **"New Site Walk"** button, search box, the full data grid (Date · Walk · Site · Account · Services · Assets · Locations · Status · Actions) and pagination
* **no** "not enabled" message, no access-denied state, no 404
* the **"New Site Walk" button is fully enabled** — `disabled=false`, no `Mui-disabled`, `pointer-events:auto`, `cursor:pointer`. (I deliberately did **not** click it: creating records in another tenant is out of scope for a read-only QA pass. So "create actually succeeds" is *untested*, not "confirmed".)

So the app tells the user *"Site Walks isn't enabled for your company"* in the sidebar and then serves them the entire feature one URL away. That is an internal contradiction — no external control needed to call it.

Evidence: `docs/bug-evidence/site-walks-feature-gate/demo-direct-url-renders.png`

**The backend is not feature-gated either.** `GET /api/site-walk/list` returns **HTTP 200** with a normal payload on the feature-less tenant:

```
demo (feature OFF) → 200 {"data": [], "success": true}
acme (feature ON)  → 200 {"data": [ …10 site walks… ], "success": true}
```

Being precise about what this does and does not show: demo returns **0 rows because demo has no site-walk records**, *not* because a gate refused it. The finding is that **the endpoint answers normally instead of refusing** — there is no feature check on the API. It is not a data-exposure issue.

### ✅ QA-3 — enabled path works

acme (feature held) → `/site-walks` renders with **10 real site walks** (rows with account, services, status "In Progress"), nav item is a normal active link.
Evidence: `docs/bug-evidence/site-walks-feature-gate/acme-enabled-works.png`

### ✅ QA-4 / role coverage

The gate is **company-scoped, not role-scoped**, so it applies uniformly to every role — which is the right design. Verified on demo across the roles that account can assume:

| Role | `site_walks` perms | `site-walks` feature | Sidebar |
|---|---|---|---|
| Super Admin | `site_walks.view`, `site_walks.manage` | not held | greyed + tooltip ✅ |
| Admin (EG-Admin overlay) | *none* | not held | n/a (no permission) |

On demo only **Super Admin** holds `site_walks.view`, and that is the role the greyed state was confirmed under. I could not enumerate every role in the system (`/api/roles` is not a list endpoint on this build), so "all three roles named in the PR (Super Admin / Project Manager / Account Manager)" was verified for Super Admin only — the other two don't exist on this tenant's account. **This does not weaken the QA-2 defect**, which is company-scoped and therefore role-independent.

### ⚠️ QA-5 — alembic single head: not verifiable from here

*"Confirm `alembic upgrade head` runs cleanly against a single head"* requires DB/deploy access I don't have. The QA environment is up and serving migrations-dependent endpoints normally, which is weak positive evidence, but **it is not a verification** — it needs a backend engineer to run it.

---

## Recommendation

Gate the **route** the same way the sidebar is gated — the sidebar and `navigation.js` must consult the same `company_features` check, so a typed/bookmarked/shared URL lands on the same "not enabled" state rather than the live surface. Ideally add the check to the API too (`/api/site-walk/*` → 403/422 without the feature), so the entitlement is enforced server-side rather than only in the client.

Worth a follow-up sweep: if `navigation.js` gates other add-on routes the same way, **the other group add-ons may have the same hole**. I tested only `site-walks` because it is the only add-on demo lacks — a same-tenant control for the others wasn't constructible.

## Method notes

- Feature state read from `GET /api/auth/v2/me` → `company_features[]` on both tenants (not inferred from the UI).
- Sidebar state measured from computed styles + ARIA (`aria-disabled`, `Mui-disabled`, opacity, `pointer-events`, absence of `href`), and the tooltip triggered by synthetic hover on the `<span>` wrapper, then read from the rendered `.MuiTooltip-tooltip` portal.
- Network capture confirmed the page's own call `GET /api/site-walk/list → 200` on the feature-less tenant; re-verified server-side with a direct authenticated request.
- **No data was created or modified in either tenant.** Write surface probed with `OPTIONS` only (`/api/site-walk/list` → `Allow: OPTIONS, GET, HEAD`).
