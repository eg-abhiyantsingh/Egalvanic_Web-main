# Engineering Mains-Config cascade — the Main Breaker dialog (Phase Config → Mains Type → MCB)

- **Date:** 2026-06-23
- **Time:** ~12:30 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** "how many test cases for the engineer section" + a Panelboard workflow spec listing Phase
  configuration, Mains type, Main breaker subtype, Pole count.

## Answer + what was built

Engineering coverage was **61** (12 CB + 10 Transformer + 39 per-class matrix). The matrix covered the
phase+mains classes only at the label level, so I added `MainsConfigEngineeringTestNG` (14 tests) for
the **mains-configuration cascade** the spec describes → new total **75**. Validated **14/14 headed**.

## Depth explanation (for learning + manager review)

1. **The spec's "Main breaker subtype / pole count" weren't on the form — they were behind a dialog.**
   Driving Panelboard live in Playwright, selecting Mains Type = **MCB** popped a *"Create a Main
   Breaker?"* modal: it adds the main breaker as a child of the panel and wires it up, with its own
   Subtype (the 11 circuit-breaker subtypes) and Pole Count (1P/2P/3P). Selecting **MLO** (Main Lug
   Only) opens nothing — because a lug-only panel has no main breaker. That MCB-vs-MLO behavioral
   difference is the heart of the feature, and you only find it by *clicking through*, not by reading
   the static form. This is why I always discover live before asserting.

2. **A passing-looking test can assert the wrong element.** MAINS_05 first "found" subtype options
   `Branch Panel | Control Panel | Power Panel` — those are the *Panelboard Asset Subtype* values, not
   the *Main Breaker* subtypes. My placeholder regex `subtype` matched the drawer's "Select Subtype"
   field sitting *behind* the dialog, before the dialog's "Select subtype (optional)". The fix scopes
   the lookup to `[role=dialog]`. Lesson: when a modal overlays a form, every selector must be scoped
   to the modal, or it silently reads the layer underneath — and the test goes green on a lie.

3. **Breadth + depth, deliberately split.** Deep cascade on the canonical class (Panelboard, 7 tests)
   + a data-driven breadth pass (MAINS_08 × 7) confirming Phase Configuration + Mains Type are
   populated on MCC/Switchboard/PDU/Motor Starter/Other/VFD/VFD Panel — without re-testing the dialog
   on all 8 (it's the same shared component). That keeps the suite fast and non-redundant with the
   matrix.

## Wiring & hygiene
- `suite-asset-mains-config.xml` (dedicated) + added to `suite-asset-4-5.xml` + a `parallel-suite.yml`
  matrix row under `ASSET_5`. Non-destructive (every test cancels the form/dialog).
