# Engineering exhaustive option coverage — 308 cases, the cached data-driven pattern

- **Date:** 2026-06-23
- **Time:** ~14:30 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt:** "at least create 300 test cases for the engineer section and share me in chat."

## What was built

`AssetEngineeringExhaustiveTestNG` — 308 data-driven cases verifying every Engineering **option value**
(subtype/phase/mains/voltage per class) + per-class section presence, voltage mode, conductor fields,
transformer fields, manufacturer and pole/fuse-count presence. Engineering coverage: **75 → 383**.

## Depth explanation (for learning + manager review)

1. **Where 300 genuine cases actually live.** Hitting a big number honestly means finding the real
   combinatorial surface, not padding. The Engineering section's value-space *is* combinatorial: 65
   (class, subtype) pairs, 8 classes × 5 phase configs, 8 × 4 mains types, 4 classes × 16 voltage
   levels, etc. Each pair is a genuine assertion ("class X offers option Y"), sourced from gold
   (subtypes) or discovered live (voltages/phase/mains). That sums to 308 without a single vacuous test.

2. **The cached-snapshot pattern — 308 cases, ~39 form-opens.** A naive data-driven UI suite would
   re-open the create form 308 times (~30+ min). Instead each class's form is opened *once*, its
   dropdown option lists are read and cached in an instance map, and every per-value test is a cache
   lookup. The `present` dimension (priority 1, all 39 classes) pre-warms the cache; all later
   dimensions are instant. This is the key technique for large data-driven UI suites: separate the
   expensive *acquire* (once per class) from the cheap *assert* (once per value).

3. **Discover live, encode as data — same discipline at scale.** I confirmed the 16 voltage levels are
   identical across Battery/Generator/Utility/Transformer (one Playwright sweep), pulled the 65 subtype
   names from gold, and verified phase/mains option sets — then encoded them as `@DataProvider` tables.
   The providers generate the combinations programmatically (class × value), so the data is the spec.

## Wiring & hygiene
`suite-asset-engineering-exhaustive.xml` (dedicated) + `suite-asset-4-5.xml` + a `parallel-suite.yml`
matrix row under `ASSET_5`. Non-destructive (every open cancelled).

Engineering now spans 5 suites / 383 cases: 12 (CB depth) + 10 (Transformer depth) + 39 (per-class
matrix) + 14 (mains-config cascade) + 308 (exhaustive options).
