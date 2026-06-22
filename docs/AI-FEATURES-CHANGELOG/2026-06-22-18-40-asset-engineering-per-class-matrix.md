# Asset Engineering per-class matrix — covering what every asset class shows

- **Date:** 2026-06-22
- **Time:** ~18:40 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** "check everything in engineer section … create test case and cover everything according
  to asset it will show" — the Engineering section is class-dependent; cover all of it.

## What was built

`AssetEngineeringMatrixTestNG` — a data-driven suite, **one test per asset class (39 total)**, asserting
the exact Engineering fields + voltage mode each class renders. Validated **39/39 headed**. This is the
breadth layer atop the deep flows (`AssetEngineeringTestNG` = Circuit Breaker, `TransformerEngineeringTestNG`).

## Depth explanation (for learning + manager review)

The user's insight was that **the Engineering section is not one thing — it's 39 different things**, one
per asset class. So the right move wasn't more assertions on Circuit Breaker; it was a *map* of the whole
surface. Two lessons:

1. **Discover the whole surface in one sweep, then encode it as data.** I wrote a throwaway diagnostic
   that opened the Create form for every class and dumped its Engineering section to a file — a single
   ~6-minute headed run produced the ground truth for all 39. That dump *became* the test: a
   `@DataProvider` table of `{class, expected-labels, voltage-mode}`. When the form's behavior for a
   class changes, exactly that class's row goes red. This is far more maintainable than 39 hand-written
   methods, and the data table doubles as living documentation of what each class shows.

2. **Find the real discriminators, not just "a field exists."** The interesting structure was the
   **voltage mode**: most classes show a read-only "System Voltage" (Derived from upstream), but
   Battery/Generator/Utility show an *editable* "Select voltage", Transformers show *both* (derived
   Primary + selectable Secondary/Tertiary), and conductor classes (Busway/Cable) show *no* voltage at
   all. Asserting that distinction (including the negative — EDITABLE asserts the section does NOT say
   "System Voltage") is what makes the matrix meaningful rather than a box-tick. The other discriminators
   — Pole/Fuse Count, Manufacturer library match, Phase Configuration, Mains Type, kVA/Impedance/Tertiary
   Voltage, conductor fields — were all read straight from the live dump, never guessed.

3. **`innerText` ≠ field values (again).** Same trap as the WO Priority combobox: I scoped the section
   text to the create drawer (not the left-nav's "ENGINEERING" heading) and read the *voltage control's*
   disabled/placeholder state via the input, not the page text.

## Wiring & hygiene
- `suite-asset-engineering-matrix.xml` (dedicated) + added to `suite-asset-4-5.xml` + a
  `parallel-suite.yml` matrix row under `ASSET_5`.
- Non-destructive (every case opens the create form and cancels). Disabled discovery diagnostic retained.
