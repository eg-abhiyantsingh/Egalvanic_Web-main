# ASSUMPTIONS — Opportunities automation + full-spectrum audit (2026-06-03)

Decisions taken autonomously where the brief was ambiguous, so work never blocked.

1. **Scope of "do it all".** The framework was already hardened earlier this session
   (Phase-2 verifiers exist + wired; Phase-3 `AIPageAnalyzer` covers all pages; Phase-4
   quality gates run a11y/perf/boundary/assets/responsive/detail-tabs across 59 routes;
   API/security/visual scaffolds exist; the `Qe` crash is filed). So this pass spent the
   budget on the **highest-yield remaining gap the brief prioritizes: full CRUD-lifecycle
   edge cases on a real entity** — **Opportunities** (the #1 target in the gap analysis) —
   plus the Phase-0 matrix and Phase-1 diagnosis. Other modules are inventoried with an
   honest NOT-YET and a ranked next-8 list, not silently claimed done.

2. **Opportunities is SLD-scoped + sales-core gated.** `Opportunities.jsx` fetches
   `fetchOpportunitiesBySLD` and the nav is gated by `hasSalesCore`. Tests therefore accept
   **either** the grid **or** a graceful "select an SLD" empty-state as healthy, and skip
   (with reason) the data-dependent cases when the active site/SLD has no opportunities —
   per the standing rule "0 data is not a bug."

3. **Create may require more than a name (SLD select).** The create dialog binds a
   `selectedSldId`. When only a name can be supplied and Save stays disabled, the create
   tests `SkipException` with an explicit "create needs SLD select" reason rather than
   failing — this is a documented precondition, **not** a softened assertion.

4. **Destructive CRUD is allowed in QA with cleanup.** Create/double-submit tests create a
   uniquely-named `AutoOpp_<ts>` / `AutoOppDup_<ts>` record and delete it in a `finally`-style
   cleanup (matching the existing `WorkOrderPlanning` `AutoPlan_*` pattern). The hardcoded QA
   credentials in `AppConstants` are intentional per project policy and were reused, not moved.

5. **UI↔API verification = best-effort, fail-closed on contract.** The web repo's REST-Assured
   layer authenticates via `POST /api/auth/login`. The Opportunities API test asserts the **auth
   contract** hard (200 + token; wrong-password → 4xx, no token). Entity-level UI↔API consistency
   for `/api/opportunity/by-sld/{sldId}` needs the live `sldId`/`companyId`, which isn't exposed in
   a simple localStorage key — that deeper check is left as a documented next step rather than a
   fragile, flaky assertion.

6. **`SkipException` ≠ pass-masking.** Per the Phase-1 finding that skip-return guards hide bugs,
   skips here are used **only** for genuine environment/data preconditions (no SLD, empty grid,
   no create control for this role, API unreachable) and always carry a reason string. Behavioural
   expectations use hard `Assert`.

7. **Locators.** CSS/structural selectors first (`.MuiDataGrid-root`, `input[type=search]`,
   `[role=dialog]`); relative — never absolute — XPath only for MUI text-keyed buttons
   (`New`/`Save`/`Cancel`/`Delete`). `getAttribute` is used to match the repo's existing
   convention (43 call-sites) despite the Selenium-4 deprecation warning.

8. **No headless.** Per project policy the suite runs in a real browser; verification runs were
   non-headless. CI uses `macos`/`ubuntu` runners with a real Chrome.

9. **Opportunity detail has no tabs.** The frontend renders the detail as a single scroll (no
   tab strip); the detail test walks any tabs that happen to exist (0 is fine) and still
   hard-asserts page health + accessibility on the detail screen.
