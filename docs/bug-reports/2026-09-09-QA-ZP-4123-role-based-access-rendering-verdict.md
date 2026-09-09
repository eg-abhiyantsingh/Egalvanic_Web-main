# ZP-4123 — Web: Role-Based Access Rendering Issue · QA verdict

**Ticket:** [ZP-4123](https://egalvanic.atlassian.net/browse/ZP-4123) — "Web: Role-Based Access Rendering Issue" (Bug, High)
**Artifact:** https://claude.ai/code/artifact/e90e971d-49f8-4987-97dd-521f75cd4d44
**Tested:** 2026-09-09 · **Env:** `acme.qa.egalvanic.ai` · build **V1.36**, bundle `index-jYhUcFb4.js` · tenant acme (`d59d449b`)
**Method:** real login-form sign-in on each role seat (no token priming), both routes loaded directly,
15 s settle + reload on the surprising case; permissions read live from `/auth/me` **and**
`/features/access`; route guard read from the shipped bundle.

---

## Verdict — **the ticket's two stated symptoms do NOT reproduce. A different, real defect does.**

| Ticket claim | Result |
|---|---|
| "Condition Assessment is not rendering consistently for all roles" | ❌ **Does not reproduce.** `/pm-readiness` renders with data for **all five** web-capable roles. |
| "Project Manager users are unable to access/view the Maintenance Program" | ❌ **Does not reproduce.** PM's nav shows it and `/maintenance/program` renders fully (gantt + 31 assets). |
| — | ✅ **DEFECT found: the nav and the route enforce two different rules**, so one role is offered a page it is then refused, and another is refused the link to a page it can open. |

## Measured, per role

`/pm-readiness` = Condition Assessment · `/maintenance/program` = Maintenance Program

| Role | `features.condition_assessment.view` | Condition Assessment page | `features.site_overview.view` | in the guard's `orRoles` | MP nav entry | MP page |
|---|---|---|---|---|---|---|
| Project Manager | YES | ✅ renders | YES | YES | shown | ✅ **renders** |
| Facility Manager | YES | ✅ renders | **no** | YES | **hidden** | ✅ **renders** |
| Account Manager | YES | ✅ renders | **no** | no | hidden | ⛔ **Access Denied** |
| Electrical Engineer | YES | ✅ renders | YES | **no** | **shown** | ⛔ **Access Denied** |
| Client Portal | YES | ✅ renders | YES | no | shown | ✅ renders (portal bypass) |
| Technician | YES | n/a | YES | no | n/a | n/a — no `platform.web` ("Web Access Restricted") |

`features.condition_assessment.view` is granted to **all six** roles, which is why the first claim
cannot reproduce: the permission the Condition Assessment nav entry and page depend on is universal
on this tenant. Both permission sources agree (`/auth/me` == `/features/access`) for every seat.

## Root cause — two rules for one page

**Nav entry** (`Site Data → MAINTENANCE → Maintenance Program`) is permission-only:

    { subgroup:"maintenance", text: t("nav.maintenanceProgram"),
      to:"/maintenance/program", permission:"features.site_overview.view" }

**Route** adds a role-NAME list, and the guard ANDs it with the permission despite the prop name:

    <Route path="/maintenance/program" element={
      <Ume fallback={<AccessDenied/>}
           orPermission="features.site_overview.view"
           orRoles={["Project Manager","Facility Manager"]}>   // ← "or", but used as AND
        <MaintenanceProgram/>
      </Ume>} />

    function Ume({fallback, orPermission, orRoles, children}) {
      const tier = VJt(), portal = fot(), has = zDr(), userDetails = ku(s => s.userDetails);
      if (portal && tier === "T2") return children;                 // portal bypass
      if (orPermission && has(orPermission)) {                      // permission REQUIRED
        if (!orRoles?.length) return children;
        const names = (portal?.user_roles || userDetails?.roles || []).map(r => r?.name);
        if (orRoles.some(n => names.includes(n))) return children;  // AND a named role
      }
      return fallback;
    }

Consequences, all observed live:

- **Electrical Engineer** holds the permission, so the **nav shows the link**, but its role name is not
  in `orRoles` → clicking it lands on **Access Denied**. This is the ticket's "some roles receive
  Access Denied", with the cause identified.
- **Facility Manager** lacks the permission, so the **nav hides the link**, yet the page **renders in
  full** by URL. (FM is in `orRoles`; the strict reading of the guard says it should still be denied,
  so the exact path FM takes through it is not fully isolated — the observation was reconfirmed with
  a 15 s settle and a reload.)
- **Account Manager** is refused consistently (no permission, not in `orRoles`) — the only role where
  nav and route agree on "no".
- The same `orPermission` + `orRoles` shape is on `/maintenance/overview`
  (`["Facility Manager","Super Admin"]`), `/maintenance/compliance` and `/maintenance/reports`
  (`["Project Manager","Facility Manager"]`), so this is a family of four routes, not one page.

## Findings

**DEFECT 1 (High) — nav↔route rule mismatch on the /maintenance/* family.** Two different gates for
the same page in both directions: a link that leads to Access Denied (EE), and a page reachable by
URL whose link is hidden (FM). Fix: gate both on the same predicate. Per
[ZP-4036](https://egalvanic.atlassian.net/browse/ZP-4036)'s PermissionGate pattern that predicate
should be a permission, not a role name — and if `orRoles` is meant to widen access, the guard must
actually OR it (`has(perm) || names.some(...)`), which today it does not.

**FINDING 2 (Medium) — role-NAME gating is back, and V1.36 renamed roles.** `orRoles` hard-codes
"Project Manager", "Facility Manager", "Super Admin". V1.36 swapped the *names* Admin ↔ Super Admin
on unchanged ids, so any name-keyed gate is one rename away from silently flipping. Same class as the
prod first-role gate (`roles.find(g => g)`) filed in the ZP-4033/ZP-4036 family.

**FINDING 3 (Medium, privilege) — Client Portal sees the whole Maintenance Program.** The external
client-portal seat (35 permissions) renders the program page *including* the "Edit Maintenance
Program" action, via the guard's `portal && tier === "T2"` bypass, which skips both the permission and
the role list. Worth confirming that is intended for a customer-facing role.

**NOT REPRODUCED (was real on 2026-09-07):** the Client Portal 422 on Condition Assessment recorded in
`2026-09-07-maintenance-program-compliance-reports-verdict.md` no longer occurs — CP's
`/pm-readiness` renders cleanly today.

## Against the ticket's acceptance criteria

| Criterion | Status |
|---|---|
| Condition Assessment renders successfully for every role | ✅ **met today** (all five web roles) |
| Project Manager can access and view Maintenance Program | ✅ **met today** |
| No Access Denied shown for authorized scenarios | ❌ **not met** — EE is offered the link and then denied |
| Role permissions match the approved access matrix | ⚠️ **cannot be confirmed** — the matrix in the ticket's screenshot lists expectations per role, but the code gates on a role-NAME list that does not correspond to it. Needs the approved matrix as data (which permission each role should hold) before it can be asserted. |

## Test data

- Condition Assessment — https://acme.qa.egalvanic.ai/pm-readiness
- Maintenance Program — https://acme.qa.egalvanic.ai/maintenance/program
- Portal variants (company-feature gated, not role gated) — https://acme.qa.egalvanic.ai/maintenance-portal/condition · https://acme.qa.egalvanic.ai/maintenance-portal/program
- Seats: `abhiyant.singh+project@`, `+fm@`, `+accountm@`, `+electric@`, `+clientportal@`, `+tec@` (acme)
- Evidence: `docs/bug-evidence/zp-4123-role-based-rendering/`

**Test-data footprint: zero** — read-only navigation, no records created or changed.

---

## GENERALISED 2026-09-09 (later) — this is one of several, not a one-off

A full read of the shipped nav + router (see
`docs/bug-reports/2026-09-09-QA-nav-licence-and-route-guard-audit-verdict.md`) shows the menu-vs-route
mismatch in this ticket is structural, not local:

**The router uses five different gate mechanisms** — a permission gate, a permission wrapper, a pure
role-NAME gate, a company-flag gate ("Feature Not Available"), and the `orPermission`+`orRoles` guard
in this ticket. The nav uses a sixth rule of its own (`permission` / `portalFeature` per item). Nothing
reconciles them.

**Three more live instances of the same class:**

| Page | Menu requires | Route requires | Effect |
|---|---|---|---|
| `/issue-suggestions` | `company_data.manage` — **no role holds it** | `company_data.view` — every role holds it | Builder is hidden from every tested role, yet the page opens by URL with Create Set / Import / Export / delete working |
| `/services`, `/pm-plans` | `company_data.manage` | role ∈ [Admin, Super Admin] **and** `features.settings.view` | a link that can refuse |
| Maintenance Portal pages | plan (Free / Premium) | **no licence check at all** | on a Free plan the padlocked pages open by URL — Compliance showed 638 deviations, 0.6% score |

**And 21 routes have no guard at all**, so the "hidden but reachable" half of this ticket is the norm
rather than the exception.

**Two more role-NAME branches in the nav itself**, beyond `orRoles`: Arc Flash Readiness moves from
Site Data to Engineering for a user holding the role "Electrical Engineer", and the Maintenance Portal
appears off-tier for five named roles. With V1.36 having renamed Admin ↔ Super Admin on unchanged ids,
each is one rename from changing access silently.

**Why FM rendered the page** — partially explained, still not isolated. The guard short-circuits to
allow before checking anything when a portal/tier condition holds; that is the likeliest path for the
Facility Manager result recorded above, but the same short-circuit would also have admitted Account
Manager and Electrical Engineer, and it did not. Reported as observed; the mechanism stays open.

**Scope of the fix:** gating `/maintenance/*` on a permission fixes four routes. Reconciling the menu
rule with the route rule fixes the class.
