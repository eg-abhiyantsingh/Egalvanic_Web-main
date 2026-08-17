# AI quote editor — site-walk-scoped quotes (ZP-3607 / pipeline PR #37) · QA verdict

**Tested:** 2026-08-17 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PRs:** eg-pz-engineering-ai-pipeline **#37** · eg-pz-backend **#956** (merged as **ZP-3607**)
**Test data (mine, labelled):** site walk `4a063404` *"QA-AUTO ZP3607 … (delete me)"* — 7 items, quantities summing to **8**; walk-sourced quote (plan) `610fcb3d`; non-walk control plan `7660780e`.

---

## Verdict in one line

**The walk-shaped plan content is live and correct on QA — a walk-sourced quote reads as a fully populated plan and counts assets by quantity.** But **PR #37's *validation* claims could not be verified**, because the endpoint I tested performs **no instruction validation at all**, and the AI-editor path that most likely hosts that validation had not finished running. **One defect filed (Medium).**

> **Correction to my own first pass.** I initially concluded that two QA items *failed* (the `site_walk_id` refusal and the `intake_overrides` rejection). An adversarial review plus a negative control overturned that: `/generate` accepts *every* malformed instruction I sent it, including rules that **predate** this PR. So those results are evidence that **this endpoint has no validator**, not that PR #37's validator is broken. Filing them against #37 would have been wrong.

---

## What is VERIFIED live (read back from `GET /api/plans/{id}`)

| QA item | Result | Evidence |
|---|---|---|
| Walk-sourced quote reads as a **populated** plan, not near-empty | ✅ | `content.site_walk` block present; 8 coverage entries; 1 work order with **8 lines**; priced **$600** (`$200` cost / `$400` margin). UI shows the full quote, not an empty state. |
| Each walk work order reports its **true asset count**, not "1 asset" | ✅ | `content.audit = {walk_asset_count: 8, walk_item_count: 7}` — counted **by quantity** (8), not by line item (7), and not 1. Confirmed end-to-end: the **customer proposal** reads *"covering **8 assets** across 1 planned visit"*. |
| **`pricing_mode`** and the **formula trace** exposed per work order | ✅ | `workorders[0].pricing_mode = "formula"`; `workorders[0].formulas = [{name:"service_price", expr:"evaluated_labor", is_price:true, value:600.0}]`. Also on `site_walk_pricing.services[0]`. This is the PR's "service with no pricing equation is priced from its implementation-method labor" case. |
| Walk line **labels, quantities and locations** carried | ✅ | Each of the 8 lines has `walk_label` ("ATS 1"…"ATS 8"), `walk_room_label`, `locations:[{label:"Unassigned", qty:3}]`, `walk_item_id`, `walk_unit_id`. |
| A revision with **`scope:"site_walk"`** submits and regenerates | ✅ | `POST /api/plans/{id}/generate` with a walk-scoped row → 200, plan regenerates to 1 WO / 8 lines / $600. |
| **Regression:** non-walk (node-based) plans unchanged — change is additive | ✅ | Control plan `7660780e` with `scope:"assets"`: node-based lines carrying `node_id`, **no** walk block (`source:null`, `site_walk:null`), node-based audit (`asset_count:2, covered_count:14, site_asset_count:231`), priced $1,050. Walk fields do not leak in. |

**On the original complaint** — *"a walk-sourced quote read as an almost-empty plan"* and *"get_plan_content reported '1 asset' for every walk work order"* — both are **fixed** on the surface I could observe.

## What could NOT be verified (and why)

| QA item | Status |
|---|---|
| "Refuses to drop `site_walk.site_walk_id` while a row still claims walk scope" | ⚠️ **Not verifiable on the path tested** — see below |
| "Rejects `intake_overrides` keys no service asks about" | ⚠️ **Held, not filed** — see below |
| `get_plan_content` as an **agent read tool**; `PLAN_SYSTEM` prompt documentation | ❌ **Never exercised** — these are AI-pipeline artifacts, not REST responses |

### Why the two validation items are unverifiable here — the negative control

`POST /api/plans/{id}/generate` is the endpoint the plan editor itself submits to (frontend: `` `/plans/${id}/generate` ``, POST, body = the `instructions` object). I sent it six deliberately-invalid instruction sets, including **rules that predate this PR**:

| Probe | Result |
|---|---|
| `scope: "qa_garbage_scope_zz"` (not a real scope) | **200 accepted** |
| work row with **no `service_id`** | **200 accepted** |
| `service_id` = nonexistent UUID | **200 accepted** |
| work row missing its `id` | **200 accepted** |
| no `version` field | **200 accepted** |
| `scope:"assets"` with **no** `all_assets` / `node_ids` / `filters` | **200 accepted** |

That last one is the decisive case: the PR's own premise is that *"validation demands all_assets / node_ids / filters"*. This endpoint does not demand them. **There is no instruction validator on the REST `/generate` path at all** — so its acceptance of the two walk-specific payloads says nothing about whether PR #37's validator works.

PR #37 lives in the **AI pipeline** repo; its validation, `get_plan_content` and `PLAN_SYSTEM` most plausibly sit on `POST /api/plans/{id}/ai-edit`, which dispatches an AWS Step Function (`eg-pz-qa-ai-service-spec-sfn-ohio`). I fired the exact adversarial request — *"Remove the site walk link but keep the walk-scoped work row"* — and it was **still `status:"running"` after ~35 minutes**, so that layer remains unresolved.

**Consequently this run does not establish that pipeline PR #37 is deployed to QA at all** — every green observation above is equally explained by backend #956 alone. I am not failing ZP-3607.

## The one defect I am filing (Medium)

**`POST /api/plans/{id}/generate` silently accepts an incoherent walk instruction set and empties a priced quote.**

Sending `site_walk = {"service_ids":[…]}` with **`site_walk_id` removed** while `work[0].scope` is still `"site_walk"` returns **HTTP 200, `success:true`, with no `error` and no `code`** — and **no soft warning anywhere in the 19.5 KB response** (I grepped the full body at every nesting level for `warning/ignored/skipped/unknown/invalid/orphan`; only `error: null` appears). The orphaned state is persisted verbatim, and regenerating from it yields `source:null`, `site_walk:null`, **0 work orders**, no `walk_asset_count`, total **$600.00 → $0.00**.

The empty plan is arguably the *correct* output for instructions with no asset source — I am **not** calling that a pricing bug. The defect is that the incoherent input is **accepted silently** instead of refused, which is precisely the guard the PR describes.

**Reachability is real, not theoretical.** The editor derives the walk id from stored instructions (`V = p.site_walk?.site_walk_id || null`) and only emits the `site_walk` block when that id is truthy (`...V && rows.length ? {site_walk:{…}} : {}`). So once the id is lost, every subsequent editor save omits the block while walk-scoped rows persist — the same state, re-sent. (I restored the plan via API; whether the UI offers a re-link path was **not** tested.)

`site_walk_id: null` explicitly behaves identically to omitting it.

## Held, deliberately NOT filed

**Unknown `intake_overrides` key is persisted, not rejected.** `site_walk.intake_overrides = {"qa_bogus_key_zz_not_a_real_question":"whatever"}` → 200, stored verbatim in the plan's instructions.

I am **not** filing this. The acceptance criterion is *"keys **no service asks about**"*, and **all 13 walkable services on acme QA have `site_walk_config.intake = []`** — none asks any intake question. With an empty question set, "skip validation when nothing is known" is a defensible implementation, and there is no positive control (no real key exists to show accepted-and-effective). Without that contrast, *not rejected* is indistinguishable from *check not applicable*. Retest when a service with a real intake question set exists.

This also makes two clauses of the PR untestable on this tenant: the `site_walk` block's *"intake questions with their current answers"* would always be empty here.

## Notes, scope limits, and honesty about what I touched

- **Every claim is scoped to `POST /api/plans/{id}/generate` and `GET /api/plans/{id}`.** I tested endpoints, not a validator whose home I could not locate.
- The REST `content.site_walk` block exposes only identity (`name`, `site_walk_id`, `sld_id`, `status`, `walk_date`) — not the counted assets, intake Q&A, or the quote-owns-vs-walk-owns statement the PR describes. **I am not calling this a defect**: `get_plan_content` builds the *agent-facing* view, which may legitimately be richer than the REST projection. (The counts do exist, in `content.audit`.)
- **Data I created on acme** (all labelled "QA-AUTO ZP3607 … delete me"): site walk `4a063404`, walk-sourced plan `610fcb3d`, non-walk control plan `7660780e`. The plan was left **restored and valid** (1 WO / 8 lines / $600). One `ai-edit` job (`bd89f4c5…`) was still running at write-up.
- **Nothing of yours was modified.** Separately: your walk `0e07e774` "abhiyant 17 monday" showed `sld_id` set on one fetch and `null` minutes later — almost certainly your own concurrent edit, so I am recording it as an observation, **not** a defect.
- **Alembic single-head check** (from the ZP-3607 backend list) needs DB/deploy access — not verifiable by me.

## Recommended next steps

1. **Settle the layer.** Run both edits through `POST /api/plans/{id}/ai-edit` once a job completes; if it refuses them, PR #37 is correct in its own layer and the filed defect belongs to the backend REST path.
2. **Confirm PR #37 deployment to QA** (image tag / Lambda LastModified for the Step Function task), since nothing observed here requires #37.
3. Consider whether `/generate` should validate at all — today it accepts arbitrary scopes, missing service ids and nonexistent service UUIDs.
