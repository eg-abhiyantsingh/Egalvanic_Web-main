# PROD V2.0 — Customers page loses its Accounts tab for a multi-role user

**Env:** production, web V2.0 · **Found:** 2026-09-02 (reported by owner, analysed by QA)
**Family:** union-of-roles UI gates — the "remaining 11 RoleGate uses" deferred by eg-pz-frontend #1351

## Repro (prod, two real users)

1. Log in as `mukul+newkirkelectric@egalvanic.com` (Newkirk Electric) — roles **EE + Super Admin + Admin**.
2. Open Sales → Customers (`/customers`).
   **Actual:** tab bar **Accounts | Sites**, "New Account" button, account list renders. ✅
3. Log in as `abhiyant.singh+acme@egalvanic.com` (Acme) — roles **PM + Account Manager + EE + Super Admin + Admin**.
4. Open Sales → Customers.
   **Actual:** **no tab bar at all** — page falls straight to Sites content with "Create Site".
   **Expected:** the Accounts tab, since this user holds every role Mukul does, plus two more.

## Why this is a role-NAME gate (not permissions), by elimination

The affected user's roles are a strict SUPERSET of the working user's. Under the ZP-4033 union
model, extra roles can only ADD permissions — so a permission-gated tab cannot vanish when PM and
Account Manager are added. The hiding therefore keys on role NAME (or account-assignment scoping /
tenant flag). Prime suspect: an Account-Manager (or PM) role-name branch that forces the
account-scoped Sites view, overriding the admin roles — exactly the f(union) ≠ f(single-role)
class that #1351 fixed for /dashboard and deferred for the other 11 RoleGate uses.

## Discriminators (prod-only; QA cannot reproduce — see note)

- Console, both users, compare:
  `fetch('/api/features/access',{credentials:'include'}).then(r=>r.json()).then(d=>console.log(d.user_permissions.filter(p=>/account|customer/i.test(p))))`
  Grants equal-or-greater for the Acme user + tab still missing ⇒ frontend gate confirmed.
- Killer experiment (prod admin): make an Acme test user with EE+SA+Admin only → expect tabs.
  Add Account Manager → if the tab bar vanishes, root cause proven (AM role-name gate).
  Repeat with PM instead of AM to identify which role triggers it.
- Also rule out tenant delta: the two tenants may differ in LaunchDarkly org flags; the same-tenant
  role add/remove above removes that confound.

## Why QA cannot reproduce this today

QA's /customers is the OLDER single-list design (no Accounts|Sites tabs, "New Customer" button) —
the suspect component exists only on the V2.0 frontend, which has not reached QA. Verified
2026-09-02 with a same-shape 5-role seat on QA: Customers entry present (under ORGANIZATION in the
older Admin nav), page loads, no tabs to lose. QA mirrors:
`docs/bug-evidence/union-roles-zp4033/qa-mirror-*.png`.

**Correction to earlier analysis:** V2.0 did NOT drop Customers from the Admin nav — it MOVED it to
Sales → DATA → Customers. The defect is the missing Accounts tab within that page, not the nav entry.

## Ask

- Fix the role-name gate in the V2.0 Customers page (or confirm AM-scoping is intended and define
  precedence for AM+admin unions — the #1351 pattern: gate on permission, fall back explicitly).
- Sweep the remaining RoleGate uses (#1351 named 11) for the same subtractive pattern.
- When V2.0 lands on QA, QA will regression-test the role matrix on this page.

---

## ROOT CAUSE — found in the shipped V2.0 bundle (2026-09-02, prod access)

Verified live as `abhiyant.singh+acme` on prod: the seat holds EVERY account grant
(`accounts.view/manage/view_detail_page`, `features.accounts.view`, `features.customers.view`),
`/me` and `/features/access` agree (129==129, union model working) — yet the page renders zero
`role="tab"` elements. The gate is in the frontend, and here it is (deminified from
`assets/index-DmC-eyhY.js`, fn `Vwr` @ ~11905168):

    const y7o = new Set(["Account Manager", "Admin", "EG Admin", "Super Admin"]);
    function x7o(e){ return y7o.has(e || "") }
    // Customers page tab builder:
    const n = userDetails?.roles?.find(g => g)?.name || "";   // <-- FIRST role in the array
    const tabs = x7o(n) ? [Accounts, Sites] : [Sites];

`roles.find(g => g)` returns the FIRST element, so the Accounts tab depends on which role happens
to be listed first in the user's roles array. The affected user's /me order is
["Project Manager", "Account Manager", "Super Admin", "Electrical Engineer", "Admin"] — PM first,
so three qualifying roles (AM, SA, Admin) are never consulted. A user whose array leads with a
qualifying role (Mukul) gets the tab. Order-dependent per user: the same role SET can behave
differently, which is why it looks random across seats.

**Second instance, same bundle:** the Test Equipment page (fn `wGo`) hides the
"Test Equipment Library" tab when `roles.find(v=>v)?.name === "Project Manager"` — identical
first-role read, identical order-dependence.

**Fix:** `roles?.some(g => y7o.has(g?.name))` — the correct `.some()` pattern already exists 7
times in this bundle, including three lines below the broken gate. Better still, gate on the
`accounts.view` permission per #1351's PermissionGate pattern.

## Prod verification of last week's releases (same session, read-only)

- `/staff/*` routes on prod: `401 eg_staff_denied` (PUT configs + dataprep probed) — closes the
  fork ticket's "confirm allowlist unset on prod (#1080)" action item.
- SKM: `/skm-cable-library/sizes?cable_oid=436089` ships `skm_size_id` (1 AWG -> 3841) on prod.
- NULL-key backfill on prod: **0 NULL keys across 1,862 live classes and 3,539 subtypes** — the
  ticket's prod spot-check item, clean.

## Is this on QA? (checked in QA's own bundle, index-BCC_7hbn.js, 2026-09-02)

- **Customers Accounts-tab bug: NOT on QA — the component does not exist there.** `label:"Accounts"`
  occurs ZERO times in QA's bundle; QA still ships the older single-list /customers page. The bug
  will ARRIVE on QA when the V2.0 frontend is promoted — regression-test it then (or pre-empt by
  fixing before promotion).
- **The second instance IS on QA today:** the identical Test Equipment gate exists in QA's bundle
  (fn `Bea` ≡ prod's `wGo`): first role === "Project Manager" hides the Test Equipment Library tab,
  regardless of other roles held. Demonstrable on QA with a PM-first multi-role seat (e.g. the
  PM+Admin fixture from the ZP-4033 verification — /me order was ["Project Manager","Admin"]).
- Note for the sweep: QA's bundle also carries a different role-name Set
  `["Account Manager","Electrical Engineer"]` gating something else — include it when sweeping the
  role-name gates (#1351's remaining-RoleGates follow-up).
