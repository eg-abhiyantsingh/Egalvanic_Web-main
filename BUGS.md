# BUGS — real product defects found by automation (2026-06-03)

Product bugs only (framework/test defects are tracked separately in the gap diagnosis,
`docs/test-coverage/COVERAGE_MATRIX.md`). Each has a runnable repro under `ready-bug/`.
Quarantine convention: a test that legitimately cannot pass because of a product bug is
left RED on purpose (never softened) and linked here.

## BUG-A — App-wide uncaught `TypeError: Qe is not a function` (HIGH → CRITICAL)
- **What:** Firing almost any interaction (search keystroke, tab click, opening a detail
  screen) throws an uncaught `TypeError: Qe is not a function` from the production bundle
  (`index-*.js:2729:221111`).
- **Blast radius (verified live):** Planning search (fires on every input incl. empty);
  Issue/Account/Opportunity **detail** pages; and **14 of 15** modules in the interaction
  sweep (Scheduling, Accounts, Opportunities, EMPs, Arc Flash, PM Readiness, Equipment
  Library, Panel Schedules, Sales/Ops Overview, Audit Log, Maintenance, Notes).
- **Persistence:** survived a same-day redeploy (bundle hash changed, crash stayed at the
  same `2729:221111`).
- **Repro / evidence:** [ready-bug/2026-06-03-planning-search-crash-qe.md](ready-bug/2026-06-03-planning-search-crash-qe.md)
- **Quarantined-red tests:** `Phase4QualityGatesTestNG.testSearchInputBoundary`,
  `testDetailPageHealth`; `Phase5ModuleInteractionTestNG.testModuleInteraction`.
- **Fix hint:** de-minify `2729:221111` via sourcemap; guard the `Qe` call site
  (`typeof === 'function'`) and fix the undefined binding — one call-site fix should clear
  all surfaces.

## BUG-B — App-wide WCAG 2 A/AA violations on all pages (HIGH)
- **What:** axe-core finds critical/serious violations on **28/28** scanned routes.
- **Dominant rules (shared components):** `button-name` ×54 (icon-only buttons w/o
  accessible name), `color-contrast` ×54, `aria-progressbar-name` ×20 (unlabeled spinners),
  plus `aria-required-children`, `scrollable-region-focusable`, `listitem`,
  `aria-input-field-name`.
- **Repro / evidence:** [ready-bug/2026-06-03-app-wide-wcag-violations.md](ready-bug/2026-06-03-app-wide-wcag-violations.md)
- **Quarantined-red tests:** `Phase4QualityGatesTestNG.testRouteAccessibility` (per route).
- **Fix hint:** add `aria-label` to the shared `IconButton`s + DataGrid toolbar; raise theme
  contrast for secondary/disabled text; label the shared `CircularProgress`.

## BUG-C (cross-repo, iOS backend) — `/auth/v2/me` rejects a valid token (HIGH)
- **What:** A valid login token is accepted by `GET /accounts/` (200) but rejected by
  `GET /auth/v2/me` (401) on `api.qa.egalvanic.ai` — per-endpoint auth inconsistency
  (likely tenant-routing).
- **Repro:** iOS repo `ready-bug/2026-06-03-api-auth-inconsistency-me-401.md`.
- Listed here for completeness; lives in the iOS automation repo.

## BUG-D — Opportunities: rapid double-submit creates a DUPLICATE (MEDIUM-HIGH)
- **What:** On the Create Opportunity dialog, clicking **Create twice in quick succession**
  creates **two** opportunities with the same name — the Save/Create button isn't disabled
  on first click and there's no debounce/idempotency guard.
- **Repro:** Site `gyu` → New Opportunity → Facility (pre-filled) + name `AutoOppDup_<ts>` →
  click **Create** twice rapidly → grid shows **2** rows named `AutoOppDup_<ts>`.
- **Found by:** `OpportunitiesTestNG.testOpp13_RapidDoubleSubmitNoDuplicate`
  ("Rapid double-submit created 2 opportunities … expected ≤1"). Quarantined-red, tagged
  `groups={"known-product-bug"}`.
- **Fix hint:** disable the Create button after first click (until the request resolves) and/or
  de-dupe server-side per (facility,name) within a short window.

## BUG-E — API: flat list endpoints (`/opportunities/`, `/quotes/`, `/accounts/`) accept UNAUTHENTICATED reads (LOW-MEDIUM, BAC)
- **What:** `GET /api/opportunities/`, `GET /api/quotes/` AND `GET /api/accounts/` return **200 with
  no auth token**, while the company-scoped sibling `GET /api/company/{id}/opportunities` correctly
  returns **401**. A systemic auth-enforcement inconsistency across the flat list endpoints
  (OWASP API1/API5 — Broken Access Control).
- **Impact (characterized live):** the unauthenticated flat endpoints return a **null-field
  template** (all fields `null`), not real tenant data — so there is **no data leak**; severity is
  bounded to the missing-auth inconsistency / attack-surface hygiene, not disclosure. Still a real
  defect: a public GET that should be gated like its scoped sibling.
- **Repro (no token):**
  ```
  curl -sk https://acme.qa.egalvanic.ai/api/opportunities/      # -> 200 (null template)
  curl -sk https://acme.qa.egalvanic.ai/api/quotes/             # -> 200 (null template)
  curl -sk https://acme.qa.egalvanic.ai/api/company/<id>/opportunities  # -> 401 (correct)
  ```
- **Found by:** `OpportunitiesTestNG.testOpp57_ApiFlatEndpointsShouldRequireAuth` (opportunities +
  quotes) and `AccountsTestNG.testAcc_ApiFlatEndpointRequiresAuth` (accounts) — both assert the flat
  endpoints SHOULD be 401/403; currently 200. Quarantined-red, tagged `groups={"known-product-bug"}`,
  assertions NOT weakened. The companion green tests `testOpp56` (scoped endpoint enforces 401) and
  `testOpp58` (authed list schema) prove the correct contract holds where it is enforced.
- **Fix hint:** apply the same auth middleware/decorator used by `/company/{id}/...` to the flat
  `/opportunities/` and `/quotes/` routes (or remove the un-scoped routes if unused).

## BUG-F — Goals (and SALES pages): "notes" fetch returns HTML → severe-error storm (MEDIUM, intermittent)
- **What:** On `/goals` the client's notes fetch intermittently receives **HTML (the SPA `<!DOCTYPE …>`
  shell) instead of JSON**, so `JSON.parse` throws and the console fills with a storm of **NATIVE_SEVERE**
  errors — observed **81 in one load**: `Failed to fetch notes: SyntaxError: Unexpected token '<',
  "<!DOCTYPE "... is not valid JSON`. When it hits, page rendering degrades (subsequent Goals
  grid/dialog elements intermittently fail to appear → tests SkipException).
- **Intermittent:** a clean reload showed 0 severe errors; the next showed 81. So it's an
  unstable/racy API response (the notes endpoint occasionally routing to the SPA fallback — same
  HTML-instead-of-JSON shape as BUG-E's flat endpoints).
- **Blast radius:** confirmed on `/goals`; the SALES page-health tripwires
  `GoalsTestNG.testTC_GOAL_09`, `AccountsTestNG.testAcc01`/`testAcc15` are quarantined-red because
  of this intermittent storm (kept OUT of the functional gate so it stays stable; assertions NOT weakened).
- **Fix hint:** make the notes API always return JSON (proper 200 JSON or a 4xx JSON error), never
  the SPA HTML fallback; and have the client guard `Content-Type`/parse failures instead of throwing.

## BUG-G — SLD v3 data-integrity cluster (HIGH — release-gate blocker)
- **What:** Deep SLD bug hunt (live Playwright + GoJS-model introspection + a parallel backend scan
  of ALL 107 SLDs) found 8 systemic data-layer defects. **87 of 107 SLDs are affected.** Full report
  + evidence: `docs/bug-hunts/2026-06-10-sld-v3-bug-hunt.md` (+ `sld-107-scan-result.json`, screenshots).
- **The 8 systemic bugs (affected-SLD count):**
  - **S1 Connectivity loss / orphan nodes (82/107, HIGH)** — nodes referenced by no edge; e.g. Wild
    Goose 488/489 orphan with 5 edges for 490 nodes; Android Site 1095 orphan (1228 nodes/122 edges).
  - **S2 Default-coordinate pile-up (79/107, HIGH)** — nodes persisted at unplaced (0,0)/(100,100);
    Android Site piles 754 nodes at (0,0) → overlapping/unreadable render.
  - **S3 Negative / out-of-range coords (61/107, MED)** — incl. wild outliers like (103105,-1888).
  - **S4 Unclassified edges — null `edge_class` (54/107, HIGH)** — whole diagrams 100% unclassified.
  - **S5 Duplicate node labels (49/107, MED)** — e.g. 'Fuse 1' ×60 in one SLD.
  - **S6 Soft-deleted edges leak through `/api/sld/{id}` (35/107, HIGH)** — `is_deleted=true` not
    filtered server-side (Wild Goose leaks 253 deleted edges); inconsistent vs `/api/lookup/nodes`.
  - **S7 Node coordinate overlap (40/107, MED)** — distinct nodes share identical coords.
  - **S8 Isolated SLDs — nodes but 0 edges (8/107, MED)** — Migration ios = 292 nodes / 0 edges.
- **Also (single-SLD live):** duplicate/redundant API calls on one SLD load (`/api/sld/{id}` ×4,
  `enum-node-voltages` ×6, `node_classes` ×6 — perf regression); on-canvas "No issues" badge while
  labels visibly overlap (validates electrical data, not layout); AF-readiness flags a node with 0
  missing fields as "not ready".
- **Root-cause hypothesis (HIGH-value lead):** the worst SLDs are named "Migration ios", "*offline*",
  "*sync*", "Android Site" → **edges + layout coordinates are dropped on offline-sync / migration /
  bulk-import**. Fix the create/import/sync persistence path (edge endpoints + node x/y), filter
  `is_deleted` server-side in `/api/sld/{id}`, and de-dupe the SLD-load fetches.
- **Found by:** manual deep hunt 2026-06-10 (no automated SLD test class yet — SLD excluded from CI
  per the deprecated-UI note, now shown to be a high-defect area worth re-adding coverage for).
- **Session-2 deep interactive pass (2026-06-10) — net-new findings (full detail in the report,
  SLD-BUG-14..20):**
  - **SLD-BUG-14 (HIGH, architecture/perf) — the SLD canvas is double-mounted.** Two GoJS `Diagram`
    instances render the same SLD; one lives in a `display:none` 0×0 container, and **both fully load +
    lay out every node** (verified: Wild Goose loaded 490 nodes into BOTH). This is the mechanism behind
    the duplicate `/api/sld`+`node_classes`+`enum-*` fetches and the ×2 console flood — fixing it (unmount
    / lazy-render the hidden diagram) removes most of the duplicate-fetch waste on the heaviest page.
  - **SLD-BUG-15 (HIGH, release-gate blocker) — Export is a silent no-op.** Clicking Export produces no
    download, no menu, no dialog, no network request, and no console log/error (2 attempts). The v3
    "export diagrams" capability appears non-functional.
  - **SLD-BUG-16 (MED-HIGH) — S1/S2 reproduce on a small web SLD (gyu), not just migration data:** 4/6
    nodes at (100,100) overlapping + 2 orphan nodes → the create/place path still mis-handles current data.
  - **SLD-BUG-17/18/19/20 (MED/LOW):** delete dialog says "cannot be undone" while an Undo button exists
    (hard-delete data-loss risk); Delete key + right-click do nothing (toolbar-button-only delete);
    dragging one node also moves a connected node by the same offset; aria-hidden focus-trap on the dialog
    + bus group swallows child-node clicks. **Green:** node MOVE persists three-layer (drag→PUT 200→reload).

## Opportunities suite — findings (live run 2026-06-03)

`OpportunitiesTestNG` (new this session) reproduced BUG-A and BUG-B on the Opportunities
module — confirming the crash/a11y defects extend to SALES screens:

- **BUG-A on Opportunities create dialog** — `testOpp07_CreateLongName`: typing a 300-char
  name into the create dialog triggers `Uncaught TypeError: Qe is not a function`
  (`index-*.js:2729:221111`). Quarantined-red, tagged `groups={"known-product-bug"}`.
- **BUG-A on Opportunity detail** — `testOpp30_DetailAndQuoteTabs`: opening an opportunity's
  detail throws the same crash (twice). Quarantined-red, tagged.
- **BUG-B on `/opportunities`** — `testOpp43_Accessibility`: axe finds critical/serious WCAG
  violations on the Opportunities route. Quarantined-red, tagged.

These are NOT new root-cause bugs — they are BUG-A / BUG-B manifesting on a module that had
**no functional coverage before**, which is exactly why adding the suite was high-yield: it
turned "green-but-blind" into "red-and-pointing-at-the-defect."

- **BUG-A escalation — intermittent interactivity break (NEW evidence).** Across repeated
  runs, a *different* interaction-heavy test fails each time with a `TimeoutException`
  (testOpp26 page-load, testOpp06 create-dialog, testOpp42 page-load). The grid/dialog
  sometimes never finishes rendering after the `Qe` crash fires — so BUG-A doesn't merely
  log an error, it **intermittently breaks the page's render/event handling**, leaving the
  UI unresponsive. This raises BUG-A's user impact: real users would see the page/dialog
  hang, not just a silent console error.

Tests that PASS prove real behaviour (not masked): whitespace-only name is rejected
(`testOpp06`), search actually filters to matching rows (`testOpp26`), the API auth contract
holds (`testOpp_ApiAuthContract` — 200+token; wrong-password 4xx, no token), and the page
loads within the perf budget (`testOpp42`).
