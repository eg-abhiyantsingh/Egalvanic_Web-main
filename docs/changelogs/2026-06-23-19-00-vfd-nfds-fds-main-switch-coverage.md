# VFD / NFDS / FDS main-switch coverage (mains-config cascade completed)

- **Date:** 2026-06-23
- **Prompt:** "create a new VFD asset … Phase 3P4W … Mains Type NFDS … create a main switch …" then "now try, cover everything".
- **Type:** 3 new tests in `MainsConfigEngineeringTestNG` (14 → 17), completing the Mains-Type behavior matrix.

---

## What the four mains types do (now all covered, verified live)

| Mains Type | Behavior |
|---|---|
| **MCB** (Main Circuit Breaker) | opens **"Create a Main Breaker?"** dialog — Name + Subtype (11 CB types) + **Pole Count (1P/2P/3P)** + Create Main → `MAINS_04/05/06` |
| **MLO** (Main Lug Only) | opens **no** dialog → `MAINS_07` |
| **NFDS** (Non-Fused Disconnect Switch) | opens **"Create a Main Switch?"** dialog — Name + Subtype (optional) + Create Main, **no Pole Count** → `MAINS_09` (NEW) |
| **FDS** (Fused Disconnect Switch) | opens **no** dialog, adds an **Ampere Rating** field → `MAINS_10` (NEW) |

## New tests

- **MAINS_09** — Mains Type=NFDS opens "Create a Main Switch?" (Name + Subtype + Create Main); asserts **no Pole Count** (the key difference from the breaker main).
- **MAINS_10** — Mains Type=FDS adds an Ampere Rating field and opens no main-device dialog.
- **MAINS_11** — full **VFD create + save** with a 3P4W / NFDS main switch (write-persistence): set name → 3P4W → NFDS → fill the main-switch Name → **Create Main** → **Create Asset** → asset persists (drawer closes → /assets). This is the exact workflow from the prompt.

## Validation
`MainsConfigEngineeringTestNG` ran **headed: 17/17 pass**. Discovered the NFDS/FDS behavior live (the "Create a Main Switch?" dialog vs the breaker dialog) before asserting. MAINS_11 creates one `VFD-MAINS-<ts>` asset (the workflow under test).

Engineering coverage total: **386 TCs** across 5 suites (the mains-config suite went 14 → 17). Suite count
updated in `suite-asset-mains-config.xml`, `suite-asset-4-5.xml`, and the `parallel-suite.yml` matrix row.
