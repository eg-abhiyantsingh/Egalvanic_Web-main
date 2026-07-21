# SLD v3 — fresh findings (2026-06-11)

- **Driver:** Claude Code, headless Playwright/Chromium against `https://acme.qa.egalvanic.ai/slds`
  (logged in as `abhiyant.singh+admin`). Network-capture + GoJS introspection.
- **Ground rule honoured:** the prior session's data-quality findings (orphan nodes, default-coord
  pile-up, duplicate labels, soft-deleted leak, `<script>`-named node, 490-nodes/0-edges) were
  **retracted by the owner** as messy/incomplete *test data*, not product bugs. None of those are
  re-raised here. This pass deliberately looked only for **data-independent product behaviour** —
  things that are wrong regardless of how clean the SLD's data is.

---

## FINDING 1 (MED → MED-HIGH on the heaviest page) — SLD load fires the same backend endpoints many times over

**Confirmed and quantified, two independent runs.** Opening the `/slds` view fires **~80 `/api`
requests across only ~29 unique endpoints (~2.8× average redundancy)** and takes **~14 s** to go
quiet. Worst single endpoints in one load:

| Endpoint | Times fetched in ONE load |
|---|---|
| `GET /api/shortcut/by-node-class/{id}` | **11** |
| `GET /api/lookup/node-subtypes/{id}` | **10** |
| `GET /api/enum-node-voltages` | 6 |
| `GET /api/node_classes/user/{id}` | 5 |
| `GET /api/sld/{id}` | 4 |
| `GET /api/edge_classes/user/{id}` | 4 |
| `GET /api/sld/{id}/library-designations` | 3 |

**Why this is a product bug and not test data:** the counts are a property of the client's request
logic, not of any SLD's contents — a perfectly clean SLD still triggers them.

**Mechanism (confirmed live):** the SLD view **double-mounts its diagram**. GoJS introspection found
**two `Diagram` instances for the same SLD** — one in a `display:block` but **0×0** container
(`scale ≈ 0.0003`, never visible to the user) and one in the real 1316×936 container — *both* fully
populated (31 nodes / 8 links each on the SLD tested). The hidden duplicate re-runs the whole
fetch+layout chain, which is the bulk of the redundancy; the rest is missing per-id
de-dup/memoization on the `node-subtypes` / `shortcut` / `enum-*` lookups.

**Impact:** this is the app's heaviest page; multiplying its GETs 3–11× multiplies backend load and
slows first render. This is the same issue logged as **BUG-007** ("duplicate API calls ×2") and the
earlier **SLD-BUG-03**, but measured here it is **far worse than ×2** (up to ×11) — worth re-grading.

**Fix direction:** unmount/lazy-render the hidden 0×0 diagram, and memoize per-`{id}` lookup GETs
(`node-subtypes`, `shortcut/by-node-class`, `enum-*`) for the lifetime of a single SLD load.

**Regression coverage added:** `SLDTestNG#testSLD_072_DuplicateApiFetchBudget` (TC_SLD_072) reads
the browser Resource-Timing buffer after a fresh `/slds` load and fails if any single endpoint is
fetched more than 4× — tagged `known-product-bug` (expected RED until de-duped). Lives in the SLD
suite, which is already excluded from CI, so it documents the defect without breaking the pipeline.

---

## FINDING 2 (LOW confidence — intermittent, needs a manual look) — transient 403s under the request storm

In run 1, three `GET /api/shortcut/by-node-class/{id}` and one `GET /api/lookup/node-subtypes/{id}`
returned **403 / request-failed** to a logged-in admin. In run 2 (same flow, same account) **all 80
`/api` calls were 200** — so this is **intermittent, not a consistent authorization defect.** The
most plausible read is that the duplicate-fetch storm (Finding 1) is briefly tripping a backend
throttle/race for a couple of those repeated lookups. Flagged as a lead, **not** a confirmed bug;
de-duping the requests (Finding 1) would likely make it disappear.

---

## FINDING 3 (INCONCLUSIVE — unchanged from prior session) — Export shows no observable result in automation

Clicking the visible **Export** button produced **no download event, no menu/dialog, and no console
output** in headless automation. This matches the earlier session's result, which the owner correctly
judged **inconclusive** (automated download capture is unreliable). Re-stated here only so it isn't
forgotten: **needs a one-minute manual check** with a real browser to confirm Export produces a file.
Not filed as a bug.

---

## VERIFIED CLEAN (negative results worth keeping)
- Typing `<script>alert(1)</script>` into the SLD search box did **not** execute and produced no new
  console errors — consistent with the prior "stored XSS does not execute / output is escaped" result.
- No `TypeError: Qe is not a function` (BUG-A) fired on the SLD load/interaction paths exercised here.

## Third-party console noise on every `/slds` load (LOW, known)
Beamer SDK fails to load (`beamer-embed.js` blocked, CSP `font-src` violations), `[PLuG] Error
initializing with authentication: {}`, and Sentry `envelope` 403s. Third-party, but it floods the
console with SEVERE entries that can mask real errors. Already noted in earlier hunts (SLD-BUG-05).
