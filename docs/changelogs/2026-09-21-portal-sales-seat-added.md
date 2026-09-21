# Portal Sales QA seat added to AppConstants (2026-09-21)

**Prompt:** owner supplied `abhiyant.singh+portalsales@egalvanic.com` / `RP@egalvanic123` — "new role in qa" —
then "save this in code".

## Change

`src/main/java/com/egalvanic/qa/constants/AppConstants.java` — added the 8th role seat:

```java
public static final String PORTAL_SALES_EMAIL =
        getEnv("PORTAL_SALES_EMAIL", "abhiyant.singh+portalsales@egalvanic.com");
public static final String PORTAL_SALES_PASSWORD =
        getEnv("PORTAL_SALES_PASSWORD", "RP@egalvanic123");
```

Follows the existing seat pattern (env-overridable with a default), consistent with CLAUDE.md's standing
note that the hardcoded QA credentials in `AppConstants` are intentional for this project.

Compiled clean — the stale `.class` was deleted first so this is not the
`project_maven_stale_class_hides_syntax_error` trap.

## Why this seat matters

Portal Sales gates `/maintenance-portal/*` (ZP-4138). Until now only the **negative** case was testable on
QA — rail hides the section, direct URL renders Access Denied. This seat is the only way to test the
**positive** case.

The comment block on the constant records three things worth not re-discovering:

* the role is **not creatable from the product** (no Roles UI, no role CRUD endpoint) — Eric Ehlert,
  2026-09-21: *"No, just direct DB add"*;
* the gate is a **literal role-NAME match** in the bundle (`Ocs = "Portal Sales"`, used as
  `roles.map(r => r.name).includes(Ocs)` and as the route guard `orRoles:[Ocs]`), not a permission, so a
  rename silently un-gates or over-gates the portal;
* therefore the DB row must carry that exact string.

## Deliberately NOT done

Not added to `CUSTOMER_SEATS_NON_STAFF`. That array is load-bearing for tenancy tests and every member has
a measured `is_eg_admin=false`. This seat has not been signed into yet, so its staff flag is unknown —
adding it unverified would be exactly the error that voided the cross-tenant P1 in September
(`feedback_check_is_eg_admin_before_any_tenancy_claim`). It goes in once `/auth/me` has been read from it.

## Still open

The QA browser session expired, so the positive-case run has not happened yet. Needs a sign-in as the new
seat, then: confirm `/maintenance-portal/*` appears in the rail and the routes render, and capture
`is_eg_admin` + permission count for the seat table.
