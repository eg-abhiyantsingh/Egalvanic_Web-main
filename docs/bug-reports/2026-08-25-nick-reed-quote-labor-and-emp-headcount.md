# QA verdict — Nick Reed's two quoting-module reports

**Investigated:** 2026-08-25 on `acme.qa.egalvanic.ai` (V1.36), entirely through the frontend
(real mouse clicks + real keystrokes; no API-crafted requests). Network traffic was observed
only as evidence of what the UI itself sent.

---

## Report 1 — "Edit labor times, save, then add an asset → times revert to defaults"

### Verdict: REAL BUG — and reproducibly worse than reported.

On quote **"Site Walk — Site Walk Navratna"** (`/plans/a3855a99-3195-4d2a-9e6e-4eb3aa34e9ae`),
Pricing tab, LABOR section:

| Step | Electrical Engineer "Est" |
|---|---|
| Baseline | 0.3 |
| Typed 5, pressed Enter | 5 |
| **Plain page reload (no asset added)** | **0.3 — reverted** |

Reproduced twice (once with 5, once with 750.3). **The override does not survive even a plain
reload** — adding an asset is not required to lose it.

**What the UI sends.** Committing the field fires:

```
POST /api/plans/{id}/generate   → 200
{"plan_type":"standard",
 "pricing":{"labor_overrides":[{"workorder_id":"wo-walk-d625cfa0-5",
                                "labor_type_id":"9c6bbb73-fa23-4eff-ad61-9f32d35903a0",
                                "billed_hours":5}]}, ...}
```

Two things stand out:

1. The edit is submitted as a **`labor_overrides` entry on the `/generate` (regenerate) call** —
   i.e. the override is an argument to a regeneration, not independently stored state. Anything
   that regenerates later without replaying the same overrides drops them.
2. The server answers **200**, so the UI shows success. Nothing tells the user the value was
   not kept.

Also observed: after the edit, **Cost and Sell did not recalculate** ($33/$50, Labor total
$37/$83 unchanged) even before the reload — so the entered value was never folded into pricing.

**Why "adding an asset" is the trigger the user notices.** Adding an asset goes through
**Edit Quote → "Save & Regenerate"** (button literally named that). That dialog is titled
*"Pick the work: a service, the assets it covers, and when it happens."* So the asset change
re-runs generation, which is exactly the path that discards overrides. Nick's repro is the
visible case of a more general problem.

### Caveat — read before writing the ticket
Nick's screenshot shows both labor lines badged **"BY EQUATION"**. I reproduced the same badge
on a Standard quote (`C_03_7`, $19,531): there the Est column renders **62.5 BY EQUATION with no
editable input at all**. On the site-walk quote I tested, Est *was* editable and had no badge.
So there are (at least) two labor modes, and I verified the revert only in the editable mode.
The likely product intent is that equation-derived hours recompute on scope change — but
silently discarding a user's explicitly saved override, with a 200 and no warning, is not
defensible either way.

### Suggested question for the devs
Does `/plans/{id}/generate` persist `pricing.labor_overrides`, and does the Edit-Quote
"Save & Regenerate" payload replay existing overrides? The revert-on-plain-reload says the
answer to the first is effectively no.

---

## Report 2 — "EMP template labor hours not totalling by headcount (13.8 shown, not 27.6)"

### Verdict: NOT CONFIRMED — suggestive, but I could not find the headcount field. Do not file yet.

What I found on **"test kd 2"** (`/plans/6d6e1f0b-bb01-4dbc-b68b-ab0376cd8431`), an EMP whose
layout matches Nick's screenshot exactly (Infrared Thermography with N occurrences, Q3 2026 1/N,
labor lines Journeyman Electrician + Thermographer):

| Quote | WO in Planned Work | Its two labor lines | Sum if headcount counted |
|---|---|---|---|
| test kd 2 (EMP) | Infrared Thermography Q3 2026 — 2 assets · **0.33h** | JE 0.3 + Thermographer 0.3 | 0.6–0.67h |
| Site Walk Navratna | Arc Flash Data Collection — 2 assets · **0.67h** | EE 0.3 + JE 0.3 | 0.6–0.67h ✓ |

The site-walk WO's total **does** equal the sum of its two labor lines; the EMP WO's total is
**about half** of its two lines — consistent with Nick's "shows 13.8 instead of 27.6".

**Why I am not calling it confirmed.** The LABOR panel on the Pricing tab is scoped to the
*selected occurrence* (that pane read "$40 sell / $7 cost" against a FULL QUOTE of $27,732), so
I cannot yet prove the 0.3 + 0.3 I read belongs to the same scope as the 0.33h I read. I also
never located an explicit "headcount" / "people assigned" control — not on the Pricing tab, not
on Planned Work, and not on the Infrared Thermography service page. Executed WOs
(`/sessions/{id}`) have no Labor tab at all (tabs: Assets / Forms / Issues / Attachments).

### What would settle it (next session, ~20 min)
Open Planned Work → a specific WO → its inner **Labor** sub-tab (per
`QuoteLaborInflationRepro`, the inner tabs are Summary / Labor / Line Items / Test Equipment)
and read Est. Hours per labor type against that one WO's displayed total. If the WO total
equals the largest single labor line rather than the sum, Nick is right and it is a clean bug.

---

## Note on prior art
This sits in the same family as the **Shea Electric "Cancer Center quote" labor-inflation
report (2026-07-22)**, for which `QuoteLaborInflationRepro` already exists — it demonstrates
three labor-time surfaces disagreeing (line items Σ est_mins/60, labor-line EST vs BILLED,
pricing total). Worth reading before ticketing either of the above.

## Test data
No cleanup needed: because the override does not persist, the quote returned to its original
0.3 on its own. Nothing was left modified.
