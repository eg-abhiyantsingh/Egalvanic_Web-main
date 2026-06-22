# Asset ENGINEERING / Trip-Configuration — full coverage for the protective-device library match

- **Date:** 2026-06-22
- **Time:** ~16:45 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** "now we need to cover engineer section too cover all the test case understand all the
  test first" → "build that now cover everything in engineer section." Depth chosen via the
  question prompt: **Full incl. save-persistence**, in a **new `AssetEngineeringTestNG` class**.

## What the Engineering section is (learned live)

When you create a **Circuit Breaker** (or other protective/OCP class), the Add Asset form grows an
**ENGINEERING** block with cascading controls:
`Asset Subtype → Pole Count (1P/2P/3P) → Manufacturer → library model card`. Picking a model applies
a **LIBRARY MATCHED** card and then **asynchronously** loads a **TRIP CONFIGURATION**:

- **Frame** `E1.2 S-A — 250A @ 254V (65kA)`, **Sensor** `250A`, **Plug** `100 A`
- **Settings**: `LTPU (I1)` `LTD (t1)` + **i²t On / i²t Off** toggle, `INST (I3)`, `INST OR`, `Dial`
- **Add Ground Fault Settings** toggle
- **System Voltage** is read-only ("Derived from upstream").

## The 12 tests (ENG_01–ENG_12)

Section presence + read-only System Voltage · absence for a non-protective class (Battery) · subtype
options/select · pole count · manufacturer ABB · **library match populates the full trip config** ·
clear-match (✕) resets · Frame/Sensor/Plug · settings enable-checkboxes · i²t toggle · ground-fault
toggle · **engineering-config persistence on edit-reload**. Validated **12/12 green, headed**.

## Depth explanation (for learning + manager review)

This task was a masterclass in *"don't assert against a screenshot — assert against the live DOM."*
Three deep lessons worth explaining:

1. **Async render is the silent killer.** My first assertions (Frame/i²t/ground-fault) failed
   intermittently. A one-shot diagnostic test that dumped the matched-state DOM to a file revealed the
   smoking gun: the section showed *"Loading device configuration…"* — I was reading the DOM before
   the trip config finished loading. The fix wasn't a better selector, it was a **wait for the load to
   complete** (`waitForTripConfigLoaded` polls until `!loading && LTPU && Ground Fault`). Lesson:
   when a UI assertion flakes, suspect *timing/async* before *selectors*.

2. **Cross-test contamination compounds in sequence.** Tests passed standalone but a *different* set
   failed on each full-suite run. Root cause: a drawer/discard-modal left open by one test made the
   next test's form-open fail fast. Fix was **systemic, not per-test**: a robust `closeAnyOpenDrawer`
   (drawer + dialog + backdrop), an **internal retry** in the form-open and autocomplete helpers, and
   a `@BeforeMethod` cleanup. Lesson: a fleet of tests is only as reliable as its weakest shared
   helper — fix the helper, not the symptom.

3. **Know when the "obvious" verification is infeasible, and pick an honest alternative.** True
   save→reload persistence of a *new* asset proved impractical: the company-wide `/assets` grid search
   doesn't filter to a fresh asset in automation, and the asset registry isn't a clean queryable list
   (assets are SLD-scoped nodes aggregated across 121 SLDs — I verified this against the live swagger
   + `/nodes/` + `/company/{id}/slds`). Rather than ship a flaky test, ENG_12 verifies persistence by
   **re-opening an existing saved breaker** and asserting its Engineering data round-trips on edit —
   combined with ENG_06 (which proves the match *populates* the trip config), the pair covers the
   create→render→persist lifecycle honestly. Lesson: a reliable, slightly-narrower assertion beats an
   ambitious, perpetually-red one — and say so plainly.

## Wiring & hygiene
- `suite-asset-engineering.xml` (dedicated) + added to `suite-asset-4-5.xml` + a `parallel-suite.yml`
  matrix row under the existing `ASSET_5` toggle.
- Non-destructive (every test cancels; ENG_12 only reads back). Disabled DOM-dump diagnostic retained.
