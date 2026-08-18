# Services that price themselves: burden-rate equations, equations on site-sourced plans, rate pins — QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PRs:** eg-pz-backend **#990** · eg-pz-frontend **#1167** · eg-pz-reporting-lambdas **#269/#271**
**Ticket's stated environment:** *"dev only (both PRs are in cicd/dev, not yet promoted to cicd/qa)."*

---

## Headline finding — the "dev only" note is STALE: this is deployed to QA

The ticket says QA review is dev-only, but **both PRs are live on QA today.** This is the same stale deploy-status pattern I've flagged on several tickets this session (SKM, Site Walks, AI-editor) — the merge monitor's "not yet in QA" notes have been consistently out of date.

Evidence:

**Frontend #1167 is in the QA bundle** — every string this PR introduced is present in the shipped JS:

| String (from the PR) | In QA bundle |
|---|---|
| `assumes_burden_rate` | ✅ (5×) |
| `total_labor_hours` | ✅ (5×) |
| "prices off a base" (the burden consequences text) | ✅ |
| `SELL < COST` / "sells below cost" (the suppressed badge/warning) | ✅ |
| `pricing_inputs` | ✅ |
| "cost rate" (dropdown leads with cost) | ✅ |

**Backend #990 behavior is live** — a plan generated from a **site's real assets** (`scope:"assets"`, *not* a walk) for a service that carries an equation now publishes:
- `wo.pricing_mode = "formula"` (the equation ran — pre-#990 this was labor×rates, equation ignored)
- `wo.formulas` present and `wo.pricing_inputs` present (engine occurrences now publish them — pre-#990 they didn't, which is why the "Show calculation" dialog was empty)
- `content.pricing_inputs` present

So the two core fixes of this ticket — **equations run on site-sourced plans**, and **engine work orders publish `formulas`/`pricing_inputs`** — are confirmed live on QA.

## What is confirmed

| QA item | Result |
|---|---|
| Equations now run on site-sourced (non-walk) plans | ✅ **PASS** (site plan → `pricing_mode:"formula"`, equation evaluated) |
| Single-service engine occurrences publish `wo.formulas` / `wo.pricing_inputs` | ✅ **PASS** (present on the site-plan WO; the "Show calculation" dialog now has data to read) |
| Frontend burden-rate authoring + truth-telling shipped | ✅ **Deployed** (all #1167 strings in the bundle: burden checkbox, consequences text, SELL<COST suppression, cost-rate-first) |
| Ordinary (non-burden) equation service unaffected | ✅ **PASS** (Cleaning's `evaluated_labor` equation prices normally; no `assumes_burden_rate` on it) |

## What could NOT be verified — needs a burden-rate service fixture

The remaining items all require a service **authored with the burden flag** — `pricing.assumes_burden_rate = true` **and** an equation declaring `total_labor_hours`, ideally with a non-trivial equation (charges more than labor) and intake questions. **No service on acme has this** (all 13 walkable services carry the trivial `evaluated_labor` equation, `assumes_burden_rate` absent, `intake:[]`), and authoring one is a **versioned "build" flow** (`build_job`/`build_brief`/`service_version_id`) that isn't a simple API create — it's the pricing-setup dialog in the UI.

Blocked on that fixture:
- **Item 1** — burden checkbox consequences text + backend rejecting a save without `total_labor_hours`. *(The create API returned `400 "name required"` — the service-build schema needs reverse-engineering or the UI.)*
- **Item 3/8** — answering intake in the calculation dialog, the value persisting, price moving to the equation's answer, and the derivation (mobilization/adders/access multiplier/MROUND); Assets & Method-hours tabs populated. Needs a service with intake questions — none exist on acme.
- **Item 5/6** — burden-priced line: no SELL<COST badge, sell = N/A, billed hours = N/A, cost follows `total_labor_hours`, sell unchanged. Needs a burden service.
- **Item 7** — rate pin moves both cost and quoted price on a self-pricing service. Needs a burden service.
- **Item 4** — intake survives the wizard's edit flow. Needs intake answers to survive.
- **Item 10** — negative cases (chain-fail falls back to rate pricing; multi-service WO doesn't double-charge). Needs a burden service + a failing chain.
- **#271 reconciliation** — running the plan-pricing reconciliation over the v2 plans (0 mismatches, the mixed-work-order factored check, the walk-unfactored check) is a **DB/reporting-lambda query**, not reachable from the client. This must be run by a backend engineer (the ticket already reports "all 31 v2 plans on dev reconcile" — the QA equivalent needs the query run against QA's DB).

## Recommendation

Since it **is** on QA, the highest-value next step is a short manual pass: **author one burden-rate service** through the pricing-setup dialog (tick the burden checkbox, give the equation a `total_labor_hours` and one intake question), generate a site-sourced plan for it, and walk items 1–10. I can drive that in the browser if you want — it's the only way to exercise the burden-rate margin/cost/rate-pin/intake behaviors, which are the substance of this ticket. The #271 reconciliation needs a backend engineer to run the query on QA.

## Method notes
- Deployment confirmed two ways: frontend strings in the shipped bundle + backend site-plan behavior (`pricing_mode:"formula"` on a `scope:"assets"` plan).
- Probe plan left in place (labelled `QA-DEMO burden probe (delete me)`), per your "leave test data" note.
- No burden-rate service was authored (versioned build flow); no service configs were modified.
