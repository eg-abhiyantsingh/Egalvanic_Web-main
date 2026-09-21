# Validating "Doubt1: Portal Sales — I am not able to create this role in dev or QA" (2026-09-21)

**Prompt:** "this question is correct righty" — is the doubt worth raising as written?

**Verdict: the observation is true, the framing is wrong.** Reworded below.

## What is actually true on QA (build `index-BV-phiFE.js`)

1. **Nobody can create any role.** There is no Roles section in Admin — the rail offers Setup, Platform
   Users, Guest Portal Users, Customers, Offices, Asset Classes, Connection Classes, Issue Classes, Devices.
   No role CRUD. The frontend bundle carries **no** role create/update/delete endpoints either; the only
   `role`-shaped path in the whole bundle is `/lookup/device-roles`, which belongs to device rules.
   So "I can't create this role" is true of every role, not of Portal Sales.

2. **"Portal Sales" is not seeded on QA.** Admin → Platform Users → edit a user → Roles offers exactly five:
   Technician, Admin, Account Manager, Electrical Engineer, Project Manager. No Portal Sales.
   (Dialog opened read-only and cancelled — no user was modified.)

3. **The gate is a hardcoded role-NAME string.** The bundle defines `Ocs = "Portal Sales"` and uses it two
   ways: the rail computes `roles.map(r=>r.name).includes("Portal Sales")`, and the `/maintenance-portal/*`
   route guard is `orRoles:[Ocs]`. It is a name match, not a permission.

## The rewording to send

> Portal Sales isn't seedable from the app — there's no role-management UI in Admin at all, and the frontend
> has no role CRUD endpoints, so no role can be created from the product on any environment. On QA the
> assignable roles are only Technician / Admin / Account Manager / Electrical Engineer / Project Manager, so
> Portal Sales hasn't been seeded here yet. Could it be added by migration to QA (and dev) so the portal can
> be positively tested? Right now only the negative case is testable — a seat without the role correctly gets
> Access Denied on /maintenance-portal/*.

## Risk worth raising alongside

The gate matches on the literal string `"Portal Sales"`. Given the V1.36 rename of 'Admin' ↔ 'Super Admin'
(see `project_rbac_role_rename_v136`), a future rename of this role would silently un-gate or over-gate the
Maintenance Portal with no error anywhere. A permission would not have that failure mode.

Evidence: `docs/bug-evidence/2026-09-21-portal-sales-role/`

## Resolution — Eric Ehlert, 21 Sep

> "No, just direct DB add"

Confirms the finding: roles are not creatable in the product on any environment; Portal Sales is added
straight to the database. The doubt is therefore closed as *works as designed* on the creation point, and
what remains is a seeding request for QA.

**Still open for QA:** `Portal Sales` has not been inserted on QA, so ZP-4138 can only be verified on the
negative path. The positive path (assign the role → the portal section appears) needs the row added on QA
and attached to a test seat. The insert must use the exact name string `Portal Sales`, because the gate is
a literal name match rather than a permission.
