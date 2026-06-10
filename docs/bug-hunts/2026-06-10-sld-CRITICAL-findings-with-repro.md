# SLD v3 — CRITICAL findings with step-by-step repro + screenshots

- **Date:** 2026-06-10 (session 3 — "find more critical bugs")
- **Tester:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Method:** live Playwright (non-headless) on `https://acme.qa.egalvanic.ai/slds` + GoJS-model
  introspection + parallel unauthenticated API sweep (workflow, 8 agents) + direct `curl` auth probes.
- **Companion docs:** full hunt `2026-06-10-sld-v3-bug-hunt.md` (SLD-BUG-01..20 + S1–S8) · `BUGS.md` BUG-G.
- **Screenshots:** `docs/bug-hunts/critical-2026-06-10/*.png` (referenced inline below).

> Each finding has: **severity · screenshot · numbered repro · expected vs actual · impact.**
> Honesty note: two plausible "criticals" were tested and found **NOT** exploitable (stored XSS
> execution; unauthenticated API access) — documented as VERIFIED-SAFE so the gate isn't misled.

---

## 🔴 CRIT-1 (HIGH, NEW) — The web "+ Asset" flow creates **structurally-orphan nodes** that can never be connected

The single biggest *new* finding. The web SLD editor can ADD and MOVE nodes, but **cannot draw edges**
(`diagram.allowLink === false`, and there is no connect/link tool anywhere in the UI). So **every node
created in the web is born with 0 connections and there is no in-web way to ever connect it.** This is
the live, in-current-code root cause of systemic bug **S1** (orphan nodes, 82/107 SLDs) — not just a
legacy migration artifact.

**Screenshots:**
- `critical-2026-06-10/01-after-add-asset-click.png` — the "Select Node Type" palette (+Asset).
- `critical-2026-06-10/02-after-select-fuse-type.png` — the "Click to place asset: Fuse" mode.
- `critical-2026-06-10/03-created-orphan-fuse3.png` — the created node **"Fuse-3"** sitting fully
  disconnected from the rest of the diagram (no edges).

**Repro:**
1. Open `https://acme.qa.egalvanic.ai/slds`, pick site **gyu** in the *Site* selector.
2. Click the red **Lock Graph** FAB (bottom-right) once to **unlock** editing.
3. In the top toolbar click **+ Asset** → the **Select Node Type** palette opens (screenshot 01).
4. Click any type, e.g. **Fuse** → banner shows **"Click to place asset: Fuse"** (screenshot 02).
5. Click an empty area of the canvas → a node **"Fuse-3"** is created and persisted
   (`POST https://eg-pz.qa.egalvanic.ai/api/node/create` → **201**).
6. Inspect the new node's connections (GoJS: `node.findLinksConnected().count`) → **0**.
7. Look for any way to connect it to another node — there is **none** (no link tool;
   `diagram.allowLink === false`, `allowInsert === false`).

**Expected:** a newly created device can be wired into the single-line diagram (that is the whole point
of an SLD). **Actual:** the node is permanently isolated; the web offers no edge-drawing affordance.

**Impact (HIGH):** every web-created node is an orphan → directly produces the S1 connectivity-loss
cluster; the on-canvas **"No issues"** badge does **not** flag the disconnected node (validation gap,
ties SLD-BUG-02/13). A user "building" an SLD in the web ends up with a pile of unconnected symbols.

**Related observations captured during this test:**
- The create node lands at the **clicked coordinate** (here `(-166, 593)`), *not* the (100,100) default
  — so S2's pile-up is **not** caused by the web create flow; it comes from mobile/offline-sync/import
  where no coordinate is assigned. (Correctly attributing the bug.)
- The create call posts to a **different host** `eg-pz.qa.egalvanic.ai` than reads (`acme...`).
- Console logs `⚠️ iOS bridge not available for handler: graphUpdate` on create → the web invokes a
  **mobile/iOS native bridge that does not exist in the browser** (dead code path; confirms the module
  is mobile-first). New device nodes are auto-named sequentially ("Fuse-3").

---

## 🟢 VERIFIED-SAFE #1 — Stored `<script>` node does **NOT** execute (no XSS)

A node literally named `<script>alert('XSS')</script>_1780930307763` is stored on the **Wild Goose
Brewery** SLD (key `9b05a428-e878-470c-9f9e-57a6afb549ee`). I tested whether it executes.

**Screenshot:** `critical-2026-06-10/04-xss-node-edit-panel.png` — the **Edit Asset** dialog shows the
payload sitting inertly in the **Asset Name input field** (escaped text, not parsed as HTML).

**Repro / test performed:**
1. Site selector → **(s) Wild Goose Brewery**; wait for the 490-node diagram to load.
2. Hook detection: override `window.alert`; scan DOM for live `<script>`/`<img onerror>` carrying the payload.
3. Select the payload node and open **Edit** → the Edit Asset dialog renders the name in an `<input>`.
4. Result: `window.alert` **never fired**; **0** injected `<script>` elements; **0** `img[onerror]`;
   canvas render is plain GoJS text. The payload is React-escaped on every surface tested (canvas,
   edit panel).

**Verdict:** **not exploitable** as stored XSS today (React default escaping + input-value rendering).
Residual issue is only data-hygiene: the field **accepts and stores raw `<script>`** (no write-time
sanitization) — tracked as the downgraded SLD-BUG-10 ("input not sanitized at write; output safe").
**Do not file as XSS.**

---

## 🟢 VERIFIED-SAFE #2 — SLD API enforces authentication (no unauthenticated data exposure / BOLA)

Ran a parallel unauthenticated sweep (8-agent workflow) + direct `curl` with **no token/cookie** against
the real SLD endpoints on **both** API hosts.

**Results (all unauthenticated):**
| Endpoint | Method | Result |
|---|---|---|
| `acme…/api/sld/{realId}` | GET | **401** "No authorization provided" |
| `acme…/api/lookup/nodes/{realId}?include_bus_nodes=true` | GET | **401** |
| `acme…/api/users/{realId}/slds` | GET | **401** |
| `acme…/api/node_classes/user/*`, `edge_classes/*`, `enum-*` | GET | **401** |
| `acme…/api/node/update/*`, `edge/update/*` | PUT | **401** |
| `eg-pz…/api/node/create`, `/node/update/*`, `/node/bulk-delete` | POST/PUT | **401** "Missing or invalid authorization header" |

**Verdict:** SLD read **and** write endpoints correctly reject unauthenticated requests on both hosts —
**no BOLA / data-exposure bug** (unlike the flat `/opportunities//quotes//accounts/` endpoints of BUG-E).
The only quirk: unmatched `/api/*` paths return **200 with the SPA `index.html`** instead of a 404 JSON
(e.g. `/api/sld/undefined`) — this is the same SPA-fallback routing behind BUG-F's `JSON.parse` storms.

---

## 🔴 CRIT-2 (HIGH, release-gate blocker) — Export is a silent no-op  *(confirmed session 2, SLD-BUG-15)*

**Repro:**
1. Open any SLD (e.g. gyu). 2. Click the blue **Export** button (bottom-left).
3. Observe: **no download, no format menu, no dialog, no network request, no console log/error.**
(Reproduced across 3 attempts total over sessions 2–3.)

**Expected:** an exported PDF/PNG/JSON of the diagram (explicit v3 release-gate capability).
**Actual:** nothing happens; zero user feedback. **Impact:** the "export diagrams" feature is
effectively non-functional.

---

## 🟠 Supporting / corroborated this session

- **Delete is a soft-delete that feeds the S6 leak.** Deleting a node (toolbar **Delete** → confirm
  *"This action cannot be undone"*) fires `POST /api/node/bulk-delete` → **200**, but per S6 these
  `is_deleted=true` records keep coming back through `GET /api/sld/{id}` → the UI's "cannot be undone"
  copy contradicts both the toolbar **Undo** (SLD-BUG-17) and the soft-delete reality. Verified live by
  creating then deleting "Fuse-3".
- **Double-mounted diagram (SLD-BUG-14)** re-confirmed: Wild Goose loads **490 nodes into both** the
  visible and the hidden (`display:none`, 0×0) GoJS instances → 2× render + the duplicate-fetch storm.
- **Live S1 render:** Wild Goose draws **490 nodes / 0 edges** (connectivity entirely lost in the view).
- **AF-readiness false-negative:** `🔴 [AF not ready] test fuse {… missingFields: Array(0)}` — a node
  with zero missing fields flagged not-ready (SLD-BUG-04), reproduced again this session.

---

## Critical/HIGH scoreboard for the SLD v3 gate (new + prior)

| ID | Severity | Status | One-liner |
|---|---|---|---|
| CRIT-1 | HIGH | **NEW** | Web +Asset creates orphan nodes; no way to draw edges (live S1 cause) |
| SLD-BUG-15 / CRIT-2 | HIGH | confirmed | Export does nothing (release blocker) |
| SLD-BUG-14 | HIGH | confirmed | Diagram double-mounted → 2× render + duplicate fetches |
| BUG-G S1/S2/S4/S6 | HIGH | confirmed | Orphans / default-coord pile-up / unclassified edges / soft-delete leak (87/107 SLDs) |
| SLD-BUG-09 | HIGH | confirmed | Wild Goose 490 nodes / 0 rendered edges |
| Stored `<script>` exec | — | **VERIFIED SAFE** | Does not execute (React-escaped) |
| Unauth API / BOLA | — | **VERIFIED SAFE** | All SLD read+write endpoints 401 on both hosts |

**Verdict:** SLD v3 remains **not release-ready** — a non-functional Export, a create flow that can only
produce orphans, the S1/S2/S6 data-integrity cluster, and a 2× render cost are all open. Security and
stored-XSS, however, tested clean.
