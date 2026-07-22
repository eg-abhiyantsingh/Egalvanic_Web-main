# Repro — Quote labor hours/dollars inflate (Shea Electric "Cancer Center quote")

**Reported:** Shea Electric (Andrew Borgardt, Desmond Vincent), live call 2026-07-21.
**Reproduced:** acme.qa.egalvanic.ai (QA — NOT production), 2026-07-22, read-only.
**Verdict:** REAL, reproducible bug. A quote's labor time is stored in three places that
disagree, and the number a user reads depends on which surface they look at.

Reproduction fixture (a pre-existing QA quote that shows the same defect family):
`https://acme.qa.egalvanic.ai/quotes/d7cc59cf-3f2f-4467-ae57-18bd0a70e814`
("testtt" → Planned WO "Q3 2026 Shutdown", 17 line items, $3,588).

> This is a **safe stand-in** for Shea's Cancer Center quote. It is a different tenant/quote —
> Shea's production data was never opened. The mechanism is identical; the magnitude on this
> fixture is smaller (≈2× rather than Shea's ≈2.7×: 219h shown vs ~80h estimated).

---

## One-command repro (headed Chrome, no headless — repo convention)

```bash
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -cp "target/classes:target/test-classes:$(cat /tmp/cp.txt)" \
  -Dchrome.binary="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  com.egalvanic.qa.testcase.QuoteLaborInflationRepro
# override the target quote if the fixture rotates:  -Drepro.quoteId=<uuid>
```

Source: [`src/test/java/com/egalvanic/qa/testcase/QuoteLaborInflationRepro.java`](../../../src/test/java/com/egalvanic/qa/testcase/QuoteLaborInflationRepro.java)
(a `main()`, excluded from all suites). It logs in, opens the quote, screenshots each step,
and prints the reconciliation table. Screenshots land in `test-output/screenshots/`.

---

## Manual repro steps (what the screenshots show)

**Step 1 — Overview.** Open the quote. The headline stat row reads
**Total Hours `8.3` · Line Items `17` · Assets `16`**.
→ `step1-overview-8.3h-17lines.png`

**Step 2 — Planned Work Orders → Labor.** Click **Planned Work Orders**, then the inner
**Labor** sub-tab. The Planned-WO chip reads **`9 h`** while the page header still reads
**`8.3`** — two different hour totals on the same screen. The labor grid lists
Electrical Engineer (**Est. Hours 0.08** — a 5-minute line) and two Journeyman rows; an
**"Add Row"** button is how extra/blank labor rows get introduced.
→ `step2-labor-tab-9h-chip-vs-8.3h-header.png`

**Step 3 — Planned Work Orders → Line Items.** The first line's asset is
**`_UC3a_1779882698559`** — an auto-generated node id, not a real asset label (this is the
"unspecified / blank row" family). **"Bulk Add Procedures"** and **"Add Line Item"** are the
two ways duplicate lines get created (no labor-line dedup exists).
→ `step3-lineitems-autogen-node-bulk-add.png`

---

## The bug, in numbers (API-authoritative, printed by the repro)

Same quote, same labor, queried three ways:

| Surface | Source endpoint | Hours | Should be |
|---|---|---|---|
| Quote **line items** (Σ `est_mins` / 60) | `GET /api/quote/{id}/lines` | **16.0 h** | one number |
| Labor-line **`billed_hours`** | `GET /api/workorder/{id}/labor-lines` | **16 h** | one number |
| Labor-line **`est_hours`** | same | **8.25 h** | one number |
| **Pricing** (what the customer pays) | `GET /api/quote/{id}/pricing` | **8.25 h → $3,588** | one number |
| Planned-WO chip (UI) | — | **9 h** | one number |
| Overview headline (UI) | — | **8.3 h** | one number |

They span **8.25 → 16 h** for identical work. Per-line dollar detachment is stark:

```
Electrical Engineer   est_hours 0.08 (5 min)   →  billed_hours 4   →  $856
Journeyman Electrician est_hours 0.17 (15 min)  →  billed_hours 4   →  $684
Journeyman Electrician est_hours 8    (CTT 14×420m) → billed_hours 8 → $2,048
```
A **5-minute** Arc Flash line bills **4 hours / $856**. That is exactly Shea's report —
"presets correct in Settings, inflation happens at the quote level."

---

## Root cause (from the frontend reference engine in-repo, `eg-pz-frontend-reference/`)

1. **Deprecated `est_mins` still used as a fallback (ZP-1092).** `QuoteDetail.jsx:743-745` picks
   WO hours as `total_labor_hours → est_hours → total_est_mins/60`. When these diverge (16 vs
   8.25 here) different screens show different totals; the `est_mins/60` path is the inflated one.
2. **No duplicate detection for labor lines** — dedup is materials-only (`WorkUnitsTab.jsx:3733`).
   `POST /quote/bulk-add-procedures` and bulk-EMP recalc (**ZP-783**: "bulk EMP recalc creates
   DUPLICATE workorders") can add the same asset+procedure twice, doubling minutes.
3. **Blank / "Unspecified" rows still count** — null `labor_type_name`/`node_class` renders as
   "Unspecified" but its `est_mins` still sums (`WorkUnitsTab.jsx:2397-2429`). Live analogue: the
   auto-generated `_UC3a_…` node line.
4. **Scope multiplier not shipped (ZP-1407, "Ready to Release").** Multi-procedure assets are
   meant to be discounted ~0.75× on est time; if left at 1× they inflate — the multi-procedure
   Cancer Center job is exactly this case.

## Why "delete duplicates helped, regenerate didn't reconcile"
Deleting dup lines removes double-counted `est_mins` so some totals drop, but the
`est_mins ↔ est_hours ↔ billed_hours` divergence and the missing scope multiplier remain, so a
regenerate re-derives inflated numbers. Matches the call notes exactly.

## Fix ownership
The math lives in the **backend pricing/quote service** (this QA repo only drives the UI). The
fix and the related tickets (ZP-1092, ZP-783, ZP-1407) belong to app engineering. This folder is
evidence + a re-runnable reproduction, not a fix.
