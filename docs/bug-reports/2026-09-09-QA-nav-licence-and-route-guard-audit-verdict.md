# Navigation, licence and route-guard audit (V1.36) — QA verdict

**Ticket:** none — QA-initiated audit, prompted by the owner's "check everything, don't miss anything"
review of the V1.36 promotion board. Findings 1–3 deserve their own tickets.
**Related:** [ZP-4123](https://egalvanic.atlassian.net/browse/ZP-4123) (this audit generalises it) ·
[ZP-4033](https://egalvanic.atlassian.net/browse/ZP-4033) · [ZP-4036](https://egalvanic.atlassian.net/browse/ZP-4036)
**Artifact:** https://claude.ai/code/artifact/e13e2861-33d2-43ae-9c08-2ebde7d4fa5d
**Release context:** https://claude.ai/code/artifact/88483448-82ad-45f4-be80-822f638c43d1 (promotion board — "Every page in the product" section)
**Tested:** 2026-09-09 · **Env:** `acme.qa.egalvanic.ai` · V1.36, bundle `index-jYhUcFb4.js` · tenant acme
**Method:** the whole nav + router configuration extracted from the shipped bundle by four parallel
agents, then every claim that mattered checked live in the browser on real role seats. Permissions read
from `/auth/me` and `/features/access`. Read-only: no records created or changed; the one client-side
value written (a licence preview key in this browser's localStorage) was restored.

---

## Verdict — **three new High findings, one refuted claim, and a full page-by-page coverage map**

The board previously listed only ticket-driven work. Walking the product's own navigation instead
turned up three defects of the same family as ZP-4123 — a gate applied in the menu but not on the
route — including one with commercial consequences.

## FINDING 1 (High) — the Maintenance Portal licence lock is menu-only

The portal has a **LICENSE** selector offering **Free** and **Premium**. Choosing **Free** correctly
greys out and padlocks **Condition Assessment**, **Maintenance Program** and **Compliance** in the
menu, leaving **Site Health** and **Reports** available — matching the code, where a `no_license`
licence unlocks only `site_health` and `reports`.

**But the pages are not gated, only their links are.** With the licence set to Free:

| Typed URL | Result |
|---|---|
| `/maintenance-portal/program` | **opens in full** — schedule, "273 of 274 assets have not completed their condition assessment", "Next up: Clean, Tighten, Torque on 1 asset, May 2028", grouping controls |
| `/maintenance-portal/compliance` | **opens in full** — Overview, **Deviations 638**, Acknowledgements, **Program Elements 11**, **COMPLIANCE SCORE 0.6%** |

So a customer on the cheaper plan reaches the paid pages, with real data, by typing the address. The
lock lives in the nav-item filter; the routes carry no licence check.

**Honest scoping.** On this tenant the licence is a **client-side preview**: picking an option writes
`eg.maintenancePortal.previewLicense` (`no_license` for Free, `read_only` for Premium) into
localStorage, which is the non-T2 path — so on QA a user can also just set their own licence. On a real
T2 tenant the licence comes from `featureAccess.account_detail.license_type` instead, so that part of
the exposure is preview-only. **The URL bypass is not** — it is the same nav-only gate either way.

**Ask:** put the licence check on the routes as well as the menu, and confirm whether the
Free/Premium selector is meant to be visible to customers at all or is a demo control.

## FINDING 2 (High) — pages hidden from the menu that anyone can open by URL, with write actions

The Builder rail is filtered on `company_data.manage`. **No role tested holds that permission** —
Project Manager, Facility Manager, Electrical Engineer and Account Manager all lack it — and on the
Project Manager seat **the Builder category is not in the rail at all**.

Yet `/issue-suggestions` opens by URL on that same seat and is fully operable: ten issue-suggestion
sets, **Create Set**, **Import**, **Export**, and per-row edit/delete. The route only requires
`company_data.view`, which every role has. Two more of the same shape are in the router:

| Page | Menu requires | Route requires |
|---|---|---|
| `/issue-suggestions` | `company_data.manage` (no role has it) | `company_data.view` (every role has it) |
| `/services`, `/pm-plans` | `company_data.manage` | role ∈ [Admin, Super Admin] **and** `features.settings.view` |
| `/test-equipment` (Operations) | `features.test_equipment.view` | role ∈ [Project Manager, Admin, Super Admin] |

The first is "hidden but usable"; the other two are "offered then refused" for any role holding the
permission without the role name — the exact ZP-4123 failure. (`/test-equipment` happens to be
consistent for the four seats tested, because only Project Manager holds its permission and Project
Manager is in its role list.)

**Ask:** gate menu and route on the same predicate. Shared configuration that anyone can edit by URL —
issue-suggestion sets are tenant-wide — should require the manage permission on the route.

## FINDING 3 (High) — 21 pages have no route-level guard at all

These routes are deep-linkable regardless of permission or company flag; whatever protection exists is
inside the page or the API, not the router:

`/sales-overview` · `/pm-readiness` · `/arc-flash` · `/slds` · `/sld` · `/assets` · `/connections` ·
`/locations` · `/issues` · `/tasks` · `/scheduling` · `/panel-schedules` · `/opportunities` · `/notes` ·
`/goals` · `/agent` · `/short-circuit-ratings` · `/feeder-schedule` · `/ocpd-settings` ·
`/transformer-schedule` · `/custom-devices`

This is the general case of the scope gap filed on 2026-09-09 (a work order on an unassigned site opens
by URL for every role). Note `/agent` — an AI agent page with no guard and no menu entry.

**Ask:** decide which of these need a guard, and add them; the API must be the backstop for the rest.

## FINDING 4 (Medium) — a second permission model that only runs when you switch roles

The static route table in the bundle (`ZNo`) is **not consulted by the router**. Its only consumer is
the role switcher: after a role switch it computes which routes the new role may see using a separate
per-role exclusion map and then redirects or reloads. Those exclusions do not match the router's:

- Account Manager loses `/emps` and `/slds`
- Facility Manager loses the seven designation/connection routes
- Project Manager loses `/connections` and `/tasks`, and **gains** `/sites` and `/test-equipment`
- Electrical Engineer loses `/admin/audit-log`
- Account Manager and Electrical Engineer are barred from the admin section

So the same user can reach a page after signing in and be redirected away from it after switching to
the same role — which is a good explanation for "role behaviour looks random" reports.

## FINDING 5 (Medium) — three more role-NAME gates, in a release that renamed roles

Beyond ZP-4123's `orRoles`, the nav itself branches on role names:

- **Arc Flash Readiness moves category by role name** — it sits under **Site Data** for everyone except
  a user holding the role "Electrical Engineer", who finds it under **Engineering** instead.
- **The Maintenance Portal appears off-tier by role name** — the category requires tier T2 *unless* the
  user holds one of [Project Manager, Account Manager, Admin, Facility Manager, Super Admin].
- **The route guard's own role lists** — `/maintenance/overview` [Facility Manager, Super Admin];
  `/maintenance/program`, `/compliance`, `/reports` [Project Manager, Facility Manager]; the portal
  parent [PM, AM, Admin, FM, Super Admin]; `/services`, `/pm-plans`, `/labor`, `/materials`,
  `/offices`, the class pages and `/admin-dashboard` [Admin, Super Admin]; `/customers` [six roles].

V1.36 swapped the *names* Admin ↔ Super Admin on unchanged role ids, so every one of these is one
rename away from silently changing who gets in. This is what ZP-4036 asked to be swept.

## FINDING 6 (Low) — two labels change with permissions, which makes bug reports confusing

- `/sessions` is titled **"Assessments"** instead of "Work Orders" for a user with
  `features.site_visits.view` but not `workorders.manage`.
- `/customers` is titled **"Sites"** instead of "Customers" for a user without `accounts.view`.

The second explains an earlier confusion in the ZP-3978/multi-role work, where a page reading "Sites"
with a "New Customer" button looked like a different page. Worth knowing before reading any screenshot.

## FINDING 7 (Low) — "Pull-Through Work" is hard-coded English

The Sales nav item is a literal string, not an i18n key, so it stays English in French. Consistent with
the two untranslated strings already recorded on the ZP-4088 verdict.

## FINDING 8 (Low, question) — the Admin → Organization group bypasses the settings permission

The Admin rail entry requires `features.settings.view`, **except** the Organization group, which is
marked always-available. Worth confirming that Customers/Offices being reachable without the settings
permission is intended.

## Refuted — checked and NOT a defect

- **"The portal's own Condition Assessment link is dead."** The bundle shows no dedicated `<Route>` for
  `/maintenance-portal/condition`, which suggested it fell through to the catch-all. **It renders
  correctly** — Overview, Findings 29, Asset Details, 274 assets — so it is served by the parent layout
  route. Not a defect.
- **A `/test-equipment` menu-vs-route mismatch on the tested seats.** Only Project Manager holds
  `features.test_equipment.view`, and Project Manager is in the route's role list, so nav and route
  agree for all four non-admin seats. The divergence is latent, not live.

## Coverage map produced

Every rail category and item, with its gate, is now on the promotion board under "Every page in the
product — and who can actually reach it": Site Data (19 items), Operations (13), Engineering (6),
Sales (15), Builder (5), Maintenance Portal (10), Admin (16), plus the rules that rename labels, move
Arc Flash between categories, disable items behind company flags with tooltips, and reduce the rail to
Builder-only for a `company_data.manage` user without `features.site_visits.view`.

## Test data

- Maintenance Portal — https://acme.qa.egalvanic.ai/maintenance-portal/overview (LICENSE selector top-left)
- Locked-but-open pages — https://acme.qa.egalvanic.ai/maintenance-portal/program · https://acme.qa.egalvanic.ai/maintenance-portal/compliance
- Hidden-but-open page — https://acme.qa.egalvanic.ai/issue-suggestions
- Seats: `abhiyant.singh+project@`, `+fm@`, `+electric@`, `+accountm@` (acme)
- Evidence: `docs/bug-evidence/v136-nav-licence-audit/`

**Footprint: zero on the server.** One localStorage key (`eg.maintenancePortal.previewLicense`) was set
to Free during the test and restored to Premium afterwards.
