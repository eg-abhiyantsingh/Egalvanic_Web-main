# 2026-09-08 — QA: ZP-3944 [Web] Materials-library entries were minted during evaluation, and de-energized work had nowhere to be recorded

**Prompt:** the ticket text with its 10-step QA review ("test all this ticket too").

## What was done
1. **Ran the check the ticket exists for** — counted the materials library, re-evaluated two issues without accepting anything, counted again: 17 → 17, with not one non-read call to the library. Evaluation-time materials come back unbound, with no library id and no price.
2. **Then tested acceptance, and found the design had moved** — accepting a resolution with an unpriced part also minted nothing; the write happens at quote time, exactly as the later acceptance-to-quote ticket describes.
3. **Verified de-energization is first-class** — the flag lives on resolutions, renders a De-Energized chip, and the manual dialog offers a LOTO checkbox that round-trips in the save payload.
4. **Checked all three set-paths** — agent-set confirmed, and a rule-minted proposal carrying the flag confirmed; manual was not confirmed because the box was deliberately left unticked on shared data.
5. **Swept 39 procedures for method-level seeding** — zero methods carry the flag, so the service-overview caption had nothing to show.
6. Verdict, evidence, artifact page.

## Results (short)
- **Core regression PASS** — evaluation never writes to the materials library.
- **FINDING 1 (Medium, documentation)** — this ticket's accept-time minting has already been moved to quote time, so three of its checklist steps cannot be run as written. Anyone following the checklist would conclude the feature is broken when it has been superseded.
- **FINDING 2 (Low)** — method-level de-energization seeding is absent on this tenant, so the ticket's own "a missing chip is not proof work is safe energized" caveat currently applies to every method.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3944-accept-time-minting-de-energized-verdict.md`
- Evidence: `docs/bug-evidence/zp-3944-accept-time-minting-de-energized/`
- Artifact: https://claude.ai/code/artifact/82876c81-f854-4770-8536-df517a8fb445

## Depth notes (learning)
- **A negative claim needs a counted baseline.** "Evaluation doesn't mint" is only checkable if you write the count down *first*. Watching the network for the absence of a write call is the second, independent proof.
- **When a ticket's steps fail because a later ticket changed the design, say so as documentation, not as a defect.** The distinction matters: nobody should open a bug against superseded behaviour.
- **The part that survives both designs is the part worth verifying hardest.** Evaluation-never-writes holds under accept-time *and* quote-time minting, so that is where the effort went.
