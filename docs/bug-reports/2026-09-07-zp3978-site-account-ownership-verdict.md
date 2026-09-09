# QA Verdict — ZP-3978: Site Account Ownership & Work Order Mapping

**Ticket:** [Web] Site Account Ownership & Work Order Mapping - Implementation
**Environment:** acme.qa.egalvanic.ai · V1.36 · 2026-09-07 · admin seat
**Method:** full ownership lifecycle via UI with fresh QA-DEMO data + API traces
**Artifact:** https://claude.ai/code/artifact/7a4e0040-cbaf-48cb-a0b4-6ef05626c432

## Verdict: core flow VERIFIED (steps 2/3/4); steps 1 & 5 NOT IMPLEMENTED (single-owner model); step 6 mixed

| Step | Verdict | Detail |
|------|---------|--------|
| 1. Multiple accounts per site, distinct primary | **NOT PRESENT** | Edit Site = single required Account autocomplete; no site-accounts endpoints; Customers tree shows each site under exactly one account. Site "Manage Access List" is users, not accounts |
| 2. Transfer ownership A→B | **PASS** | Edit Site → Account → Save; `PUT /api/sld/update/{id}` with new `account_id` → 200; grouping updates (A: 0 sites, B: 1 site) |
| 3. Historical WOs stay under A | **PASS** | WO-1 (created under A) still shows Acct A in grid + panel + API after the transfer |
| 4. New WO maps to B | **PASS** | WO-2 wizard-created after transfer: `POST /api/ir_session/create` returned `account_id` = Acct B, stamped automatically (wizard never asks for account) |
| 5. Both accounts access site during transition | **NOT PRESENT** | Post-transfer, site exists only under B; A's account page shows Sites: none; only tie is historical WOs |
| 6. Edge: remove account from site with WOs | **MIXED** | ✅ Site can't be left ownerless (clearing Account disables Save). ⚠️ Deleting an account with mapped WOs succeeds via generic confirm ("Are you sure you want to delete this item?") with no WO-impact warning — soft delete (`/api/account/{id}/soft-delete` → 200); WO rows keep resolving the deleted account's name, so no data loss |

## The controlled experiment
1. Created `QA-DEMO ZP3978 Acct A` (`3fd1c782-a427-480a-af64-d50924cd11a8`) and `Acct B` (`a4499f71-ef53-4f10-9f6b-8c8fb1233948`).
2. Created `QA-DEMO ZP3978 Site` (`344ddef5-de54-4171-8bf0-15e75d5f1fff`), owner = A.
3. WO-1 `07beee76-086d-44cb-b46b-7cae8ed8ad77` (due 09/30) → stamped Acct A.
4. Transferred site to B (`PUT /api/sld/update`, 200).
5. WO-1 re-checked → still Acct A ✅. Site → B ✅.
6. WO-2 `1e6f95ca-0d1e-4fb6-bdad-3a8eab820550` (due 10/15) → stamped Acct B ✅.
7. Edges: clear-account blocked ✅; Acct A soft-deleted with generic confirm ⚠️ (WO-1 label still resolves).

## Round 2 — side impact of an account change on a POPULATED work order

Round 1 used empty WOs. Round 2 added real scope, then transferred again (Acct B → new **Acct C**
`12179f34-fb75-43bb-a1ba-21e5558506b5`):

1. Created asset `QA-DEMO ZP3978 Asset` (`ed12b661-6674-48a8-b6be-520ce6b69ada`, Panelboard) via
   Assets → Create Asset. **Gotcha:** empty site first shows the "Let's get your assets in"
   Upload-Anything interstitial — dismiss with "Not now"; the real form is a right-hand DRAWER
   (`.MuiDrawer-paper`), not a `[role=dialog]`.
2. Wizard-created **WO-3** `42d3093a-5e97-4df9-b551-c5dee9000dea` — service rules resolved the
   asset into scope ("1 of 1 matching asset selected", Panelboard / COM 1). NOTE: the wizard's
   Review step still prints "Scope: starts empty" even when 1 asset is resolved — cosmetic
   mismatch with the Scope step's "1 asset — click to edit" (worth a cosmetic ticket).
3. Raised an **NEC Violation issue** on the asset inside the WO (`POST /api/issue/create` → 201).
4. Transferred site B→C, then re-checked everything:

| Entity after B→C transfer | Verdict | Detail |
|---|---|---|
| Site owner | CHANGED (intended) | Acct B "0 sites", Acct C "1 site" / "1 asset" — asset count follows the site |
| WO-3 account | **RETAINED** | Panel + API still Acct B — populated WOs are not remapped, same as empty ones |
| WO-3 asset scope | INTACT | Assets tab "1"; `summary/v2` counts unchanged (assets 1, asset_nodes 1) |
| Issue on asset | INTACT | Issues tab "1", `badges.open_issues: 1`; issues carry no account ref of their own |
| Asset itself | INTACT | Site-scoped, not account-scoped; `/assets/{id}` renders, zero JS page errors |
| WO ledger | CONSISTENT | One site, three-owner history: WO-1→A (soft-deleted, name resolves), WO-2+WO-3→B, site→C |
| Console health | CLEAN | No app JS errors on WO detail / assets grid / asset detail; only pre-existing 3rd-party DevRev `plug.js` integrity noise |

**Conclusion:** an account change is a pure pointer move on `sld.account_id`. Nothing downstream
(assets, scope, issues, counts) is account-scoped, so no cascade and no orphaning — the risk of
this feature is *reporting/billing attribution*, not data integrity.

## Round 3 — asset created INSIDE the work order (the real technician path)

Round 2 built the asset at site level. Round 3 used **WO → Actions → Add New Asset**:

1. In WO-2 (`1e6f95ca…`, Acct B): Actions → **Add New Asset** → `QA-DEMO ZP3978 Asset-in-WO`
   (Switchboard, `0794a33d-a759-4952-afd9-534bdaf07746`) — `POST /api/node/create` → 201, Assets
   badge 1→2 immediately.
2. Issues tab → Actions → **Add Issues** → `POST /api/issue/create` → 201.
3. Transferred the site **B → C** (WO stays on B → divergence).

| Check | Verdict |
|---|---|
| Create asset from within the WO | WORKS (lands in scope immediately) |
| Raise issue from within the WO | WORKS |
| WO account after B→C transfer | **RETAINED** (panel + list API still Acct B) |
| In-WO asset + issue after transfer | INTACT (Assets 2 / Issues 1; both rows render) |

### VERIFIED BUG — new in-WO asset missing from the Add Issue picker until reload
**Repro:** open a WO → Actions → Add New Asset → create it → Issues tab → Actions → Add Issues →
open the Asset dropdown.
**Actual:** the just-created asset is NOT listed (only pre-existing assets), so the issue can't be
attached to it. **Expected:** it is listed — the Assets tab badge has already incremented to include it.
**Confirmed:** after a page reload the same picker lists both assets. Stale asset list in the
Add Issue drawer; the two views disagree within the same WO.

## Round 4 — ROLE VISIBILITY: can an account's own users see that account's work orders?

Setup: moved the QA-DEMO site into account **"Abhiyant"** (`43f1d80a-9214-434f-b26a-44d0de07160d`),
which owns a real Client Portal seat, so the site belongs to that user's account while its WOs stay
mapped to Acct B.

### ANSWER: NO — and it fails twice over

| Role | WOs visible? | Evidence |
|---|---|---|
| **Client Portal** (`+clientportal@`, acct "Abhiyant") | **NO — blocked** | No Work Orders nav entry (portal nav = Site Overview/Assets/Connections/Locations/Issues/Maintenance/Attachments/Notes). `/sessions` renders empty; `/sessions/{id}` → **Access Denied**, including for a WO on the site their own account owns. Yet `/auth/me` grants `sessions.view` + `workorders.view` and their dashboard tile counts **22 Active Work Orders**. |
| **Facility Manager** (`+fm@`, acct "Abhiyant Singh") | YES | Has `/sessions` nav; opens WO-2 fully — a WO mapped to **Acct B** on a site owned by the **"Abhiyant"** account. Neither matches FM's own account. |

### The visibility model (measured)
Visibility is **per-site, by explicit assignment**, then gated by role module permissions — it is NOT
derived from account ownership:
- CP carries **234 of the tenant's 238 sites** in `accessible_sld_ids` while their account owns 2.
  **This is NOT over-exposure** — `/api/sld/{id}/access-list` reports `has_access: true, via: "assigned"`
  for those sites (QA seeding). State this explicitly so it isn't mis-filed as a leak.
- The site their **own account owns** reports `has_access: false, via: null` — transferring a site to
  an account grants that account's users nothing.
- Therefore WO `account_id` is an **attribution label, not an access control**. Who sees a WO is
  decided by who was assigned its SITE.

### Consequences
1. **QA step 5 ("both accounts can access the site during transition") has nothing to verify** —
   access never followed ownership. Admins must add/remove people site by site. If ownership is
   meant to imply access, that is unbuilt.
2. **DEFECT — Client Portal advertises work orders it cannot open.** Dashboard shows an Active Work
   Orders count and the role holds `sessions.view`/`workorders.view`, but no nav route exists and the
   direct URL is refused. Permissions/tile and route/nav disagree; the customer-facing role is the one
   left at a dead end.

Evidence: `docs/bug-evidence/zp-3978-cp-role-visibility/01-cp-access-denied-own-account-wo.jpg`,
`02-cp-dashboard-advertises-work-orders.jpg`

## Observations for the team
- **Backfill caveat (confirm with backend):** seeded "WO CECCO A" on Common Site A (created 2026-08-26, pre-#1214) carries **Meta's** account id despite its name recording CECCO intent. Live retention provably works, so existing WOs were most likely backfilled from the site's *current* owner when the feature shipped → pre-feature history does not reflect past ownership (the exact Cecco→Meta case).
- **"Add Site" inherits its account invisibly:** Create Site dialog has no Account field; target account implied by which group's "Add Site" was clicked. Easy to create a site under the wrong account (this run did, first attempt).
- **"Viewing WOs by account" = grid column only:** WO list Account column (sortable/searchable) is the entire surface; account detail page (Details/Internal Team/Contacts/Quotes/Sites/Notes) has no Work Orders tab.

## Test data (labeled, left in place per convention)
Accounts A (soft-deleted during edge test) & B, site, and 2 WOs all named `QA-DEMO ZP3978 … (delete me)`.

## Test data — full URLs (QA)
- WO-1 (historical, Acct A): https://acme.qa.egalvanic.ai/sessions/07beee76-086d-44cb-b46b-7cae8ed8ad77
- WO-2 (post-transfer, Acct B): https://acme.qa.egalvanic.ai/sessions/1e6f95ca-0d1e-4fb6-bdad-3a8eab820550
- WO-3 (asset + issue, stays Acct B after B→C): https://acme.qa.egalvanic.ai/sessions/42d3093a-5e97-4df9-b551-c5dee9000dea
- Asset (Panelboard, site-level): https://acme.qa.egalvanic.ai/assets/ed12b661-6674-48a8-b6be-520ce6b69ada
- Asset (Switchboard, created INSIDE WO-2): https://acme.qa.egalvanic.ai/assets/0794a33d-a759-4952-afd9-534bdaf07746
- Acct A (soft-deleted): https://acme.qa.egalvanic.ai/accounts/3fd1c782-a427-480a-af64-d50924cd11a8
- Acct B: https://acme.qa.egalvanic.ai/accounts/a4499f71-ef53-4f10-9f6b-8c8fb1233948
- Acct C (current site owner): https://acme.qa.egalvanic.ai/accounts/12179f34-fb75-43bb-a1ba-21e5558506b5
- Site management (no routed site URL): https://acme.qa.egalvanic.ai/customers
- WO list: https://acme.qa.egalvanic.ai/sessions

## Evidence
- `docs/bug-evidence/zp-3978-site-ownership/01-wo1-under-acct-a-before-transfer.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/02-wo-list-same-site-two-accounts.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/03-customers-grouping-after-transfer.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/04-wo3-asset-issue-intact-after-transfer.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/05-customers-site-under-acct-c.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/06-wo2-in-wo-asset-after-transfer.jpg`


---

## ROLE COVERAGE — added 2026-09-09 (the original run was single-seat)

The verdict above was produced on ONE seat (the `+admin` multi-role seat with Super Admin active).
Asked whether it holds for all roles, it does not, and the difference is a permission gate:

    const Yli = ["accounts.view", "features.accounts.view"];   // bundle index-jYhUcFb4.js
    const C = Sot(Yli);                                        // hasAnyPermission
    ... C && detailRow(t("common.account"), session.account_name) ...

The same gate hides the **Account column and Account filter** on the work-order list.

| Role | perms | accounts.view | WO list | Account column | Details panel | Account row |
|---|---|---|---|---|---|---|
| Super Admin / Admin | is_admin | yes | all | shown | renders | **shown** (8 rows) |
| Project Manager | 94 | yes | 996 | shown | renders | **shown** (8 rows) |
| Account Manager | 77 | yes (+features) | 988 | shown | not reached in UI this run | — |
| Electrical Engineer | 80 | yes | 71 | shown | not reached in UI this run | — |
| Facility Manager | 75 | **no** | 5 (all closed) | **absent** | renders | **absent** (7 rows) |
| Client Portal | 35 | no | `422 permission_denied` | n/a | n/a | n/a |
| Technician | 95 | yes | no web access | n/a | n/a | n/a |

So the checklist item "the Account row appears above Facility" needs the qualifier **"for roles with
`accounts.view`"** — otherwise a Facility Manager retest will be filed as a regression that is
actually the gate working as written.

**Also found (beyond this ticket):** `GET /api/ir_session/{id}/full`, `/team` and `/summary/v2`
return **200 with the full payload** — including `account_name` — for a work order on a site OUTSIDE
the caller's `accessible_sld_ids`, for every role tested including Client Portal, while the list
endpoint `POST /company/{id}/workorders/v2` scopes correctly (FM 5, EE 71, PM 996). Within one
tenant, and all those roles do hold `sessions.view` globally, so it may be intended that site scope
is a filter and not a boundary — but the two halves disagree and it is worth a decision.

**Checked and dismissed:** FM's Work Orders grid opens "0–0 of 0" — not a scoping bug. All five of
FM's work orders are `active: false` and the grid defaults to Status = Open; switching to Closed
shows 1–5 of 5.

**Role-coverage artifact:** https://claude.ai/code/artifact/7aa2ccd7-13c2-4357-a4f1-0a2e3ecda48c
· evidence `docs/bug-evidence/roles-wo-panel-account-gate/`

**ZP-3978 specific:** ownership transfer needs BOTH `locations.manage` and account read. Facility
Manager holds `locations.manage` but `POST /api/account/v2` returns **422 permission_denied** for
that seat, and Account is a *required* field on Edit Site — so the transfer flow verified above is
effectively **Admin / PM / AM only**. Not proven by attempting a save (read-only recheck).
