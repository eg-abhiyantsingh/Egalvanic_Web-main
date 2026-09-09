# 2026-09-08 — QA: ZP-3936 [Web] Materials-library equipment links were untyped, so transformers, cables and busways could not be identified at all

**Prompt:** the ticket text with its 7-step QA review ("test this ticket too").

## What was done
1. **Confirmed the type pick is genuinely the required first step** — the section opens with the type empty and the rest inert; no catalog search can be issued until a type is chosen.
2. **Searched per type and recorded the id space each resolves in** — disconnect switch and panelboard → bus model, transformer → transformer model, cable → cable, fuse and circuit breaker → device. Six of the nine offered types returned rows; Switchboard, Relay and Busway were not searched individually, and the report says so.
3. **Watched the fields retailor with the type** — the fuse-class field disappears for a breaker, and a Frame axis appears once an entry is picked, each option carrying an ampere rating.
4. **Checked the grid chip leads with the equipment type** on every linked row, with a dash for unlinked ones.
5. **Ran the backwards-compatibility negative** — one entry carries the pre-change shape with a free-text family and a non-catalog id; chip and dialog both render cleanly.
6. **Reported the unseeded-table negative as not run** — every type sampled returned rows, so no unseeded table existed to provoke the fail-closed path.
7. Verdict, evidence, artifact page.

## Results (short)
- **PASS** — type-first linkage, per-type catalog routing, and the grid chip.
- Not covered: three of the nine types individually, the cable-size and transformer-kVA pickers, a fresh save cycle, the unseeded-table negative, and agent-minted rows.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3936-typed-equipment-linkage-verdict.md`
- Evidence: `docs/bug-evidence/zp-materials-library-equipment-linkage/`
- Artifact: https://claude.ai/code/artifact/9cf81bd5-c985-4174-ad3c-e7059c057296

## Depth notes (learning)
- **The claim is routing, so the evidence is the id space.** Rows coming back is necessary but weak; each type's reference resolving in *its own* id space is what proves the fan-out is type-scoped rather than one table serving everything.
- **Say which of the nine you actually searched.** "All nine types work" from six searches is the kind of small overclaim that quietly destroys trust in a report.
- **Field tailoring is observable without saving.** A field that disappears and an axis that appears are both read-only proofs.
