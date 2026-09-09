# V1.36 promotion board + ZP-4123 role-based access rendering

**Date:** 2026-09-09
**Time:** 13:10 – 15:20 IST
**Prompt:** "artifact create that content all new things are going to production, with proper
screenshot… include all ticket that we have tested this week and previous" + (mid-turn) "ZP-4123 …
check this too after that"

---

## Delivered

1. **V1.36 Promotion Board** — https://claude.ai/code/artifact/88483448-82ad-45f4-be80-822f638c43d1
   Everything QA tested between 17 Aug and 9 Sep 2026 (65 verdicts) on one page: eight decisions to
   make before promotion, 14 "verified and ready" areas each with a live QA screenshot, the
   not-testable-from-QA list, and the full 65-row ledger linking every per-ticket artifact.
2. **ZP-4123 verdict + page** — https://claude.ai/code/artifact/e90e971d-49f8-4987-97dd-521f75cd4d44
   Tested live across all six role seats.

## ZP-4123 — the ticket's symptoms don't reproduce; a different defect does

| Ticket claim | Result |
|---|---|
| Condition Assessment not rendering consistently for all roles | **does not reproduce** — `/pm-readiness` renders with data on all five web-capable seats; `features.condition_assessment.view` is granted to all six roles |
| Project Manager can't access Maintenance Program | **does not reproduce** — PM's nav shows it, `/maintenance/program` renders (gantt, 31 assets) |
| — | **DEFECT (High): the nav and the route enforce different rules** |

**Root cause, from the shipped bundle.** The nav entry gates on a permission alone:

```js
{ subgroup:"maintenance", text:t("nav.maintenanceProgram"),
  to:"/maintenance/program", permission:"features.site_overview.view" }
```

The route adds a role-NAME list, and the guard requires **both** despite the prop being called `orRoles`:

```js
<Ume orPermission="features.site_overview.view"
     orRoles={["Project Manager","Facility Manager"]} fallback={<AccessDenied/>}>

function Ume({fallback, orPermission, orRoles, children}) {
  if (portal && tier === "T2") return children;              // portal bypass
  if (orPermission && has(orPermission)) {                   // permission REQUIRED
    if (!orRoles?.length) return children;
    const names = (portal?.user_roles || userDetails?.roles || []).map(r => r?.name);
    if (orRoles.some(n => names.includes(n))) return children;   // AND a named role
  }
  return fallback;
}
```

Measured live:

| Role | site_overview.view | in orRoles | MP nav link | MP page |
|---|---|---|---|---|
| Project Manager | yes | yes | shown | renders |
| Facility Manager | **no** | yes | **hidden** | **renders** |
| Account Manager | no | no | hidden | Access Denied |
| Electrical Engineer | **yes** | **no** | **shown** | **Access Denied** |
| Client Portal | yes | no | shown | renders (portal bypass, incl. "Edit Maintenance Program") |
| Technician | yes | no | — | no web access |

Four routes share the shape: `/maintenance/overview`, `/program`, `/compliance`, `/reports`.
Also recorded: the Client Portal 422 on Condition Assessment from the 7 Sep verdict **no longer
reproduces**, and the acceptance criterion "role permissions match the approved access matrix"
**cannot be asserted** until the approved matrix arrives as data (which permission per role), because
the code gates on a role-name list that does not correspond to it.

## Depth explanation

**1. Read the gate, then measure it — in that order.** Grepping the bundle for the route table found
the guard in one pass, and the guard explained which roles were even worth testing. Measuring first
would have produced five confusing observations; measuring second turned them into a matrix with a
cause. The reverse order is what makes role bugs look "random across seats".

**2. The contradiction I could not explain, and said so.** By the guard's own logic Facility Manager
should be denied (it lacks the permission), yet the page renders — twice, with a 15-second settle and
a reload. Rather than invent a mechanism, the verdict records the observation and marks the path
through the guard as not isolated. The alternative — asserting a tidy explanation — is how a wrong
root cause gets shipped to a developer.

**3. Two near-miss false positives were killed by controls.**
- *Facility Manager's empty work-order grid* ("0–0 of 0") looked like a scoping bug; all five of FM's
  work orders are `active:false` and the grid defaults to Status = Open. Switching to Closed showed
  1–5 of 5.
- *The board's own ledger* first marked ZP-3948 **BLOCKED** because the word appeared in a checklist
  row about a bulk action that "must be blocked". The classifier now reads only verdict-bearing lines
  (the title plus lines containing "verdict"), which moved 14 spurious BLOCKED rows to their real
  status. A release board that mislabels a PASS is worse than one with no chips at all.

**4. The count strip has to agree with the ledger.** After the reclassification the header said
18/14/8 while the table said 22/21/13. Both numbers were computed, both were on the same page, and
they contradicted each other — fixed before publishing by deriving the strip from the same pass.

**5. Session-priming stopped working mid-run.** The trick from the earlier role pass (calling
`/api/auth/login` from inside the page so the browser takes the cookies) began bouncing every seat to
the login form. All ZP-4123 results were therefore taken through the real login form, dismissing the
MFA prompt with "Set up later" each time — slower per seat, but the only path that reproduces what a
user does.

## Files

- `docs/report-artifacts/2026-09-09-QA-V136-promotion-board.html` (new, 944 KB, 14 embedded screenshots)
- `docs/report-artifacts/2026-09-09-QA-ZP-4123-role-based-access-rendering.html` (new, 316 KB)
- `docs/bug-reports/2026-09-09-QA-ZP-4123-role-based-access-rendering-verdict.md` (new)
- `docs/bug-evidence/zp-4123-role-based-rendering/` (6 QA screenshots)
- `docs/qa-review-board.html` rebuilt
