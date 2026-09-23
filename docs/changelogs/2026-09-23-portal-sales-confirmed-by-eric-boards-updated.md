# Portal Sales answered by Eric; both v2.2 boards updated (2026-09-23, evening)

**Prompt:** the owner relayed Eric Ehlert's answers from chat (Monday 21 Sep) and said "eric do all coding so
for blocker that you mention in artifact … so update our artifact if this make sense to you", linking
<https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu#zp-4042>.

## What Eric confirmed

- **"I am not able to create the Portal Sales role in dev or QA. Is there any way to create this role?"**
  → **"No, just direct DB add"**.
- **"Will the Portal sales role not be able to access the web?"** → **"Portal Sales alone should not access
  web; it is just a secret entitlement that we grant to users who want to 'show their customers what the
  maintenance portal will look like'."**

## Why it changes the boards

1. **The ask was wrong in shape.** Both boards asked for "a Portal Sales seat on QA" as if an admin could
   create one. It cannot be created from the product at all. The ask is now precise: one **direct DB add of
   Portal Sales on top of an existing web-capable seat** (`abhiyant.singh+admin@egalvanic.com`), leaving
   `+fm@` without it so the ZP-4138 negative control survives. That one add unblocks ZP-4061 and the portal
   halves of ZP-4042 / ZP-4043.
2. **ZP-4138 gets the expectation it was missing.** My finding was "route gated, nav tile leaks" — true, but a
   reader could call it cosmetic. Eric's own description ("just a **secret** entitlement") makes a tile
   advertising the portal in every staff seat's rail the opposite of the intent.

## Verified in the shipping bundle before publishing (index-CH4p1H5S.js)

- **App shell** (`RRe`) checks `hasPermission("platform.web")` and renders a blocking card without it — which
  is exactly why a Portal-Sales-only seat never reaches web, as Eric said.
- **Route gate:** `/maintenance-portal/*` wrapper is `orRoles: ["Portal Sales"]`.
- **Nav tile gate:** `requiresFlag: company_features.includes("maintenance-portal")` +
  `requiresTier2: !(seat holds PM / AM / Admin / FM / Super Admin)`, hidden only when
  `requiresTier2 && !isT2Licence`. A staff seat makes `requiresTier2` false, so the hide never fires and the
  tile shows **with no Portal Sales check at all**. Two gates, two different conditions — that is the defect,
  now stated with its mechanism in the "For the developer" line rather than as a symptom.

## Changes published (same URLs)

- Readiness <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26> (v8) · Deep Pass
  <https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu> (v5).
- New band on both pages, **"What the developer confirmed about Portal Sales"**, quoting Eric with attribution
  and date, plus the two bundle facts that corroborate him.
- Decision 2 rewritten; ZP-4138 ledger basis, walkthrough step, What happens / should happen pair and
  developer line rewritten; ZP-4061 basis now names the DB-add route.

## Memory

`project_portal_sales_entitlement.md` — the entitlement's shape, the three gate mechanisms, and the rule never
to write "ask an admin for the role" again. Release-state memory corrected to point at it.

## Unchanged

No Jira edits. The six hold comments and the new bugs still need the owner's go-ahead.
