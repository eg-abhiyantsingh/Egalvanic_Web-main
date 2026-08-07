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
  and [docs/bug-evidence/a11y-shared-chrome-wcag/EVIDENCE.md](docs/bug-evidence/a11y-shared-chrome-wcag/EVIDENCE.md)
- **Quarantined-red tests:** `Phase4QualityGatesTestNG.testRouteAccessibility` (per route),
  `AccountsTestNG.testAcc14_Accessibility`, `OpportunitiesTestNG.testOpp43_Accessibility`.
- **PINNED TO EXACT NODES 2026-08-08 — and they are OURS, not third-party.** "28/28 routes" is
  explained: the worst offenders live in **shared chrome**, so every route inherits them. Verified
  byte-identical on `/customers` and `/assets`:
  | axe rule | Impact | Node | Owner |
  |---|---|---|---|
  | `listitem` | serious | `li.MuiListItem-root` **"Legacy Procedures"** whose parent is a `div.MuiBox-root`, inside `MuiDrawer`/`nav` | app sidebar |
  | `button-name` | critical | `button.MuiFab-root` under `div#root`, no accessible name | app (global FAB) |
  | `button-name` | critical | `button.MuiIconButton-root`, icon-only, no `aria-label` | app (page-level, count varies) |
  The app embeds Beamer and DevRev (both in the CSP allow-list, both present in the DOM) but **no
  violating node belongs to either** — so this cannot be deferred as a third-party problem.
- **Fix hint:** (1) wrap the sidebar nav items in a `<ul>` (or give the container `role="list"`) —
  an `<li>` inside a `<div>` is invisible to screen-reader list navigation; (2) add `aria-label` to
  the global `MuiFab` and the shared icon-only `IconButton`s; (3) raise theme contrast for
  secondary/disabled text; (4) label the shared `CircularProgress`.
- **Test-suite note:** because these are shared chrome, a whole-page scan re-reported them in every
  module and, in `TC_OPP_30`, threw *before* the functional assertions so the quote-editor tabs went
  untested. Functional tests now use `A11yVerifier.assertNoPageSpecificViolations` (shared chrome
  excluded); the dedicated a11y tripwires keep whole-page scope.

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

## ~~BUG-E~~ — **RETRACTED 2026-08-08. NOT A BUG. Never report this to engineering.**
> This was **our own test artifact**, not a product defect. It was filed as Broken Access Control,
> which is exactly the kind of finding that gets escalated — so the retraction is recorded in full
> rather than deleted, to stop it being re-filed.

- **The original claim:** `GET /api/opportunities/`, `/api/quotes/` and `/api/accounts/` return 200
  with no auth token while the scoped sibling correctly returns 401 — an auth-enforcement
  inconsistency (OWASP API1/API5), returning "a **null-field template** (all fields `null`)".
- **What is actually happening (measured directly, 2026-08-08):**
  ```
  GET /api/opportunities/  -> 200  text/html  2089 bytes
  GET /api/quotes/         -> 200  text/html  2089 bytes
  GET /api/accounts/       -> 200  text/html  2089 bytes    <- byte-identical
  GET /api/assets/         -> 200  text/html  2089 bytes    <- byte-identical
  GET /api/issues/         -> 200  text/html  2089 bytes    <- byte-identical
  ```
  Every one of them is **the same SPA `index.html`**. Unmatched paths under `/api` fall through to
  the app's catch-all route. **These are not API endpoints at all** — there is no handler, no data,
  and therefore no auth to enforce. Nothing is inconsistent about the scoped sibling returning 401.
  The real accounts endpoint is `/api/account/v2` (**singular, v2**), and it correctly returns 401.
- **Where the "null-field template" came from:** REST Assured's `response.jsonPath().getString(f)`
  returns `null` for every field when the body is HTML rather than JSON. Those nulls were read as a
  JSON object with all-null fields. There was never a template — just the app shell.
- **Why the tests fired forever:** they asserted `statusCode() == 401 || == 403` against a static
  HTML page, so they could never pass, and each run re-published a fake BAC finding.
- **Lesson (now enforced in code):** never assert an auth conclusion from a status code without first
  proving the route exists. Both tests now reject `text/html` explicitly — a 200 HTML body proves
  nothing in *either* direction, and would equally have produced a false PASS on a route that had
  genuinely lost its auth guard.
- **Replaced by:** `TC_OPP_57` now asserts the property that actually matters and is falsifiable —
  an unauthenticated request must never return opportunity/quote **data** (JSON content-type or
  domain keys in the body ⇒ real BAC ⇒ fail). `TC_ACC_..._FlatAuth` now probes the real
  `/api/account/v2`. Both are out of `known-product-bug` and expected **green**.
- **Still genuinely proven:** `testOpp56` (scoped endpoint enforces 401 for both a real and a bogus
  company id) and `testOpp58` (authed list schema). Auth enforcement on the real routes is fine.

## BUG-026 (REGRESSED) — `/slds`: the duplicate "Select View" dropdown is back (MEDIUM)
- **What:** `/slds` renders **two** separate "Select View" dropdowns at different screen positions,
  on a page whose own heading is **"Select a View to Load Assets"** (singular).
- **Verified live 2026-08-08** — and specifically checked against the by-design explanation:
  - two `div.view-selector > button.view-selector-button > span.view-selector-label` trees;
  - rendered at **`x=789,y=395`** and **`x=339,y=432`** — different subtrees, `sharedParent=false`,
    at different DOM depths under `div.app`;
  - **not** a per-row control: `sldRowCount=0`, and neither is inside any row/card/list item;
  - both visible (`offsetParent != null`, non-zero bounding boxes).
- **Likely root cause — same double-mount as SLD-BUG-14** (see BUG-G: "the app mounts TWO diagram
  components and lays out every node into both — Wild Goose loaded 490 nodes into BOTH"). Two
  view-selectors in two subtrees is exactly that fingerprint. Fixing the double-mount should remove
  this selector too; worth treating as one defect, not two.
- **Found by:** `BugHuntPagesTestNG.testBUG026_SLDsDuplicateDropdown` — **correctly red.**
- **Counting corrected 2026-08-08 (the finding stands, the number did not):** the test previously
  counted every element whose `textContent` equalled "Select View". `textContent` includes
  descendants and each dropdown is a nested trio, so it reported **"6 labels"** for **2 dropdowns** —
  inflated 3×. It now keeps only innermost matches and dedupes by rendered position, reporting
  `distinctDropdowns=2, rawTextMatches=6, positions=[789,395 | 339,432]`. Still fails, as it should.

## BUG-F — Goals (and SALES pages): "notes" fetch returns HTML → severe-error storm (MEDIUM, intermittent)
- **What:** On `/goals` the client's notes fetch intermittently receives **HTML (the SPA `<!DOCTYPE …>`
  shell) instead of JSON**, so `JSON.parse` throws and the console fills with a storm of **NATIVE_SEVERE**
  errors — observed **81 in one load**: `Failed to fetch notes: SyntaxError: Unexpected token '<',
  "<!DOCTYPE "... is not valid JSON`. When it hits, page rendering degrades (subsequent Goals
  grid/dialog elements intermittently fail to appear → tests SkipException).
- **Intermittent:** a clean reload showed 0 severe errors; the next showed 81. So it's an
  unstable/racy API response (the notes endpoint occasionally routing to the SPA fallback).
- **ROOT CAUSE SHARPENED 2026-08-08 — this one IS real, and the BUG-E retraction explains it.**
  Any path under `/api` with **no matching handler** falls through to the SPA catch-all and returns
  `200 text/html` (the 2089-byte `index.html`) instead of a 404. So a client that requests a wrong,
  renamed or not-yet-deployed notes path gets **HTTP 200 with an HTML body**, and `JSON.parse` throws
  exactly the observed `Unexpected token '<', "<!DOCTYPE "... is not valid JSON`.
  That makes this a concrete, checkable lead rather than a vague race:
  1. Capture the notes request URL from the failing load (DevTools → Network → the failing fetch).
  2. `curl -sk` that exact path with a valid token. If it returns `text/html` 200, the **path is
     unmatched** — the client is calling a route the backend does not serve. That is the bug, and the
     intermittency is just whichever code path builds the URL.
  3. Separately, the backend should return **404 JSON** for unmatched `/api/*` paths instead of the
     SPA shell. That single change converts this whole failure class from a confusing
     `JSON.parse` explosion into an honest 404 — and would have prevented the BUG-E false finding too.
  Note the contrast with BUG-E: there, OUR TEST called a nonexistent path; here, THE APP does. Same
  mechanism, but this one is a genuine product defect.
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
- **⚠️ Session-3 RETRACTION (2026-06-10, after owner review):** the owner judged none of the SLD
  findings major, and re-examination agrees. **CRIT-1 retracted** (connections are made by
  bus-attachment `Add to…`/`Box`/`Source-Target`, not free GoJS edges; `allowLink=false` is by-design —
  a just-placed unconnected node is an unfinished action, not a defect). **Export "no-op" retracted**
  (inconclusive — automated download capture unreliable; likely works manually). **S1/S2/S6/S7/S8 +
  490-nodes/0-edges + overlap + dup-labels + `<script>`-named node are TEST-DATA quality, not product
  bugs** (per the standing rule "0/empty data is not a bug"). Interactive items are by-design/cosmetic.
  Only the *negative* security results stand: stored-XSS does not execute and SLD read+write endpoints
  enforce 401 on both hosts (no BOLA). The session-3 detail below is retained for reference only.
- **Session-3 deep critical pass (2026-06-10) — full repro+screenshots in
  `docs/bug-hunts/2026-06-10-sld-CRITICAL-findings-with-repro.md`:**
  - **CRIT-1 (HIGH, NEW) — web "+ Asset" creates structurally-orphan nodes.** The web editor can add
    (+Asset → pick type → click-to-place, `POST eg-pz…/api/node/create` 201) and move nodes, but
    **cannot draw edges** (`diagram.allowLink=false`; no link tool in the UI). So every web-created node
    is born with 0 connections and there is **no in-web way to connect it** → the live, current-code root
    cause of systemic **S1** (orphan nodes), and the "No issues" badge doesn't flag it. (Create lands at
    the clicked coordinate, so it is NOT the cause of S2's (100,100) pile-up; logs an `iOS bridge not
    available for handler: graphUpdate` dead-call.)
  - **VERIFIED-SAFE (not bugs, tested):** the stored `<script>alert('XSS')</script>` node on Wild Goose
    does **NOT execute** — React-escapes it on canvas + in the Edit Asset name input (downgrades
    SLD-BUG-10 to write-time-sanitization only). And the SLD API enforces auth: real-id `/api/sld/{id}`,
    `/lookup/nodes/{id}`, `/users/{id}/slds`, and all node/edge write endpoints return **401** on BOTH
    hosts (`acme…` and `eg-pz…`) unauthenticated — **no BOLA / data-exposure**. (This used to say
    "contrast BUG-E"; BUG-E is retracted — those flat paths were never API routes, so there is no
    contrast to draw. Auth enforcement is consistent across every REAL route measured to date.)
  - **Corroborated:** node delete = soft-delete (`POST /api/node/bulk-delete` 200) that feeds the S6
    leak; double-mount (SLD-BUG-14) re-confirmed (Wild Goose loads 490 nodes into both diagrams);
    Export still a no-op (SLD-BUG-15); AF-readiness false-negative (SLD-BUG-04) reproduced again.

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
