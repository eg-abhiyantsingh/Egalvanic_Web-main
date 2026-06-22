# Transformer ENGINEERING — extending the library-match coverage to a second asset class

- **Date:** 2026-06-22
- **Time:** ~17:15 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** Follow-up workflow spec describing the transformer create flow (class → subtype →
  secondary voltage → manufacturer → library match showing kVA/impedance → configure kVA / % impedance
  / primary connection (Delta) / secondary connection (Wye-Ground) → name → Create Asset).

## What was built

`TransformerEngineeringTestNG` — 10 tests covering the Transformer Engineering section, the second
engineering/library-match class after the Circuit Breaker (`AssetEngineeringTestNG`). Validated
**10/10 headed, twice (stable)**.

## How the Transformer flow differs from the Circuit Breaker

| | Circuit Breaker | Transformer |
|---|---|---|
| Derived/read-only | System Voltage | **Primary Voltage** |
| Picker | Subtype → Pole Count → Manufacturer | **Secondary Voltage** + **Type** (Dry/Oil) + Manufacturer |
| Library card | `Emax 2 · E1.2 · Ekip DIP — LI` | `Generic · Oil Air · 3 kVA · R% · X%` |
| Post-match config | Frame/Sensor/Plug + LTPU/LTD/INST/i²t/GF | kVA Rating + % Impedance + **Primary/Secondary Connection** |
| Custom attrs | breaker settings | **BIL · Winding Configuration** |

## Depth explanation (for learning + manager review)

The big win here was **leverage**: because I'd already paid the discovery + robustness cost on the
Circuit Breaker suite, the Transformer suite reused the exact same battle-tested helpers (retrying
form-open, retrying autocomplete, hardened drawer cleanup, `@BeforeMethod` reset). The result —
**10/10 stable on the first full validation** — is the payoff of building reusable, well-hardened
primitives instead of one-off selectors. Two lessons worth calling out:

1. **Discover, then assert.** I drove the live flow with a throwaway diagnostic that dumped the DOM to
   a file *before* writing a single assertion. That's how I learned the Transformer-specific facts
   that no screenshot would reveal: the Type toggle only renders *after* a manufacturer is chosen
   (which is why my first XFMR_05 failed against bare-open), the Secondary Voltage list is 16 specific
   levels, and the Primary/Secondary Connection dropdowns only appear *after* a library match. Every
   one of those is a timing/cascade fact, not a static selector.

2. **Persistence, honestly scoped.** This tenant has *no* Transformer assets (confirmed via the
   dashboard's "Assets by Type"), so a read-back persistence test would skip forever. Rather than ship
   a perpetual skip, XFMR_10 proves **write-persistence**: it builds a matched transformer, saves it,
   and asserts the create round-trip succeeds (drawer closes → back on /assets, no validation error) —
   then *attempts* a read-back best-effort. The save-success is the reliable signal; the read-back is a
   bonus. Same principle as CB's ENG_12: pick the assertion you can make reliably, and say which one it
   is.

## Wiring & hygiene
- `suite-asset-transformer.xml` (dedicated, 10 TCs) + added to `suite-asset-4-5.xml` + a
  `parallel-suite.yml` matrix row under the existing `ASSET_5` toggle.
- Non-destructive except XFMR_10 (creates one `XFMR-PERSIST-<ts>` transformer to prove write
  persistence). Disabled DOM-dump diagnostic retained.
