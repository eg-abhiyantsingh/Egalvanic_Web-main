# 2026-09-08 — QA: ZP-3942 [Web] Disconnect switches could not be linked to a real catalog entry — the search looked in the wrong SKM catalog

**Prompt:** the ticket text with its 5-step QA review ("test this ticket also").

## What was done
1. **Searched for each product family the ticket names** — BPS, HVL and MINIBREAK all come back by name, each row carrying a bus id rather than a curve-only device, and each marked as sourced from the catalog.
2. **Searched for the fourth and reported the miss** — neither "bolted" nor "pressure" returns anything for this equipment type.
3. **Proved the manufacturer filter actually narrows** — 15 entries offered; one manufacturer yields 17 rows, another exactly one. Not a decorative dropdown.
4. **Resolved a saved link end to end** — the grid chip and the reopened dialog both resolve to a named catalog record with its manufacturer, category, poles, amps and fuse class. A fresh save was deliberately avoided on shared library data.
5. **Ran the regression across the other types** — transformers, cables, fuses, breakers and panelboards each resolve in their own id space, unaffected by the reroute.
6. Verdict, evidence, artifact page.

## Results (short)
- **PASS** — the reroute to the bus catalog works and saved links resolve.
- **FINDING 1 (Low)** — 8 of the 15 "manufacturers" offered are ANSI and UL standards documents, and selecting one returns 6 real rows. They are how some bus models are attributed upstream, so the filter works — but it leaves the ticket's negative case with nothing to select.
- **FINDING 2 (Low)** — bolted-pressure lineups are not findable by that name.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3942-disconnect-switch-bus-catalog-verdict.md`
- Evidence: `docs/bug-evidence/zp-3942-disconnect-bus-catalog/`
- Artifact: https://claude.ai/code/artifact/cfc44623-3592-4e7b-acf7-af6f12d60bf5

## Depth notes (learning)
- **Search for the ticket's own words.** Three of four families came back; the fourth failing on the exact term a user would type is the finding, and it only surfaces if you use the ticket's vocabulary rather than a term that happens to work.
- **A filter is only proven by asymmetry.** Two manufacturers returning 17 and 1 rows proves narrowing; one manufacturer returning "some rows" proves nothing.
- **Read shared data, don't rewrite it.** The saved-link step was verified by resolving the existing link rather than overwriting it — the QA tenant is shared, and the evidence is just as strong.
