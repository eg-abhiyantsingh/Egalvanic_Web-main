# RBAC front-end permission MATRIX — role × module × action UI gating (~336 cases)

- **Date:** 2026-06-29
- **Prompt:** "I want front end [RBAC] to be more than 300 test cases — editing asset for different roles,
  work order editing/deleting by different roles, etc." (gating, non-destructive; all 7 roles × ~12
  modules × 4 actions).
- **Type:** New data-driven test (`RbacUiPermissionMatrixTest`) added to the RBAC Front-End suite.

---

## What it does
A `role × module × action` matrix (7 roles × 12 modules × {View, Create, Edit, Delete} = **336 cases**)
that logs in **once per role** in a real browser (session reused across all 48 of that role's checks)
and asserts the live web app's affordances against the role's **live `/auth/me`** permission set
(the same oracle the UI gates on, via `RbacFixtures`):

- **View** → the module's nav route is present **iff** the role can view it — and a role without
  `platform.web` (e.g. Technician) is web-restricted, so it sees **no** web nav regardless of its
  `features.*.view` grants (those govern mobile). Sales modules (Opportunities/Accounts/Goals) are also
  company-feature-flag-coupled, so for them only the security direction is hard-asserted.
- **Create / Edit / Delete** → gate on `<entity>.manage`. The **hard, security-critical** invariant:
  a role that can't manage (or can't even view) the module must **not** be offered that control. The
  positive direction (has manage ⇒ control present) is logged, not failed (control rendering can depend
  on data/feature flags) — mirroring the nav flag-coupled handling.

**Module catalog (12)** with verified permission keys: Assets (`features.assets.view`/`nodes.manage`),
Work Orders (`features.jobs.view`/`jobs.manage`), Issues, Locations, Tasks, Connections (`edges.manage`),
SLDs, Panel Schedules (`nodes.manage`), Forms (`forms.view`/`forms.manage`), Opportunities, Accounts,
Goals. All 7 roles; Admin + Electrical Engineer SKIP (unusable QA accounts). `-Drbac.roles="..."` runs a
subset.

## Why a matrix (not 336 destructive ops)
Per the chosen approach, this is **non-destructive gating**: it verifies the UI exposes/withholds
controls per role, never actually creates/edits/deletes records. That's the right shape for a 336-case
regression suite (fast-ish, repeatable, safe) and is what catches RBAC UI mis-wiring.

## Wiring
Added to `suite-rbac-frontend.xml` (the **rbac-frontend** Parallel-Suite-2 group), so it runs via the
"RBAC — Front-End (UI)" dropdown and merges into the consolidated client + detailed reports. Mapped
`RbacUiPermissionMatrixTest → "RBAC — UI"` in `consolidated-report.py`; group display count 14 → 350.

## Validation
_Headed (no headless)._ Iterating to green surfaced two issues, both fixed:
- **platform.web gate** — Technician has `features.*.view` but no web access; View now expects no nav for
  web-restricted roles (was 8 false failures).
- **precise Create detection** — dropped the generic "Create/Add/New" fallback that matched unrelated
  controls (e.g. "Create Report") and produced a false manage-leak on Forms.

## Results

**Matrix size:** 7 roles × 12 modules × 4 actions = **336 cells** in `RbacUiPermissionMatrixTest`, added to
`suite-rbac-frontend.xml` alongside `RoleLoginE2ETest` / `RolePermissionUiGatingTest` / `WorkOrderEditUiTest`
→ the **rbac-frontend** Parallel-Suite-2 group now totals **350**. (Admin + Electrical Engineer rows SKIP —
their QA accounts aren't usable — so they're reported as skips, not failures.)

**Flake hunt → two timing races found and fixed.** Iterating to a stable result surfaced that the lone
"Client Portal · Forms · Create" failure was **not a real RBAC leak** — it was flakiness. Re-running Client
Portal in isolation swung **0 → 7 → 0** failures across identical runs. Root causes:

1. **Async-disabled MUI controls.** A freshly-loaded page renders an action button *enabled* for a beat,
   then disables it (`aria-disabled` / `Mui-disabled`) once `/auth/me` + feature flags resolve client-side.
   Selenium's `isEnabled()` ignores MUI's disabled semantics, so a probe in that window saw "Create Form"
   enabled → false leak. Fix: `displayedEnabled` now treats `disabled` / `aria-disabled` / `Mui-disabled`
   on the element **or any ancestor** as not-actionable, **and** a detected leak must remain actionable
   across a 2.5 s re-probe before it's reported. Verified live: Client Portal (perms: `forms.view` +
   `form_instances.view`, **no** `forms.manage`/`form_instances.manage`) renders the "Create Form" button
   **disabled** — correct gating, not a leak.
2. **Incomplete sidebar snapshot.** The nav renders section-by-section in bursts with >1 s gaps; the old
   "two equal consecutive reads" heuristic could lock in a between-bursts lull → whole sections missing →
   false "nav route ABSENT" on View cells (this is what produced the 7-failure run: all 7 were CP's
   viewable modules). Fix: the nav is permission-gated and grows **monotonically**, so `readSidebarHrefs`
   now takes the **union** of repeated reads across a stabilization window (settles when unchanged ~2.4 s),
   plus each View cell does a fresh re-read if the live observation disagrees with the permission oracle.

**Stability validation (headed, local, no headless):** after the fixes, Client Portal — the role that had
been flaking 0→7→0 — ran **3× consecutively at 48/48, 0 failures, 0 flakes**. The full 7-role suite is
validated in CI (Parallel Suite 2 → "RBAC — Front-End (UI)"), where it merges into the consolidated client
+ detailed reports.

**Independent oracle audit → 2 permission-key bugs caught and fixed.** A separate review validated the
matrix's *expected* view/manage outcomes for each role against that role's live `/auth/me`. It confirmed 10
of the 12 module keys are correct, and caught two that were wrong for this tenant's vocabulary — bugs the
new (complete) sidebar read would otherwise have surfaced as false failures for SALES roles:
- **Accounts** view key was `features.accounts.view`, which doesn't exist here → corrected to the real
  un-prefixed `accounts.view`.
- **Goals** had no view key at all (`features.goals.view` is absent; only `features.goals.manage` exists) →
  view now gates on `features.goals.manage` (manage implies access).
With those corrected, the oracle faithfully models the tenant's RBAC (green == correct gating, not green ==
lenient test). (Informational, not bugs: Panel Schedules shares `nodes.manage` with Assets, and Work Orders
uses legacy `jobs.manage` alongside the newer `workorders.manage` — both keys co-present for current roles.)
