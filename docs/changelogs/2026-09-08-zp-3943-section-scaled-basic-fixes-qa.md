# 2026-09-08 — QA: ZP-3943 [Web] Auto-minted basic fixes priced a single section on multi-section gear, and a method-less rule swallowed issues before the wildcard fix catalog

**Prompt:** the ticket text with its 7-step QA review ("test this ticket too").

## What was done
1. **Confirmed the dispatch half is live** — rule-minted resolutions exist, arrive method-bound with labor already on them (45 min termination repair, 120 min grounding restoration, 180 min conductor resize) and need no Suggest step, which no longer exists.
2. **Swept the procedures for the scaling axis** — MCC 15/15 methods and Switchboard 25/27 carry the section axis, but the corrective fix flow's **53 methods carry no unit attribute at all and no break/fix form**.
3. **Looked for a four-section fixture and found none** — no switchboard or MCC on the tenant records a section count, so the headline 720-vs-180 assertion has nothing to run against.
4. **Judged the method-less-rule negative from the product's own warnings** — the fix flow carries 50 rules and the editor warns about both hazards explicitly; no issue in the survey came back empty. A rule deliberately saved with no method was not authored.
5. **Noted the two paths are structurally distinguishable** — rule proposals are method-bound (6 of 6), agent proposals are not (0 of 12).
6. Verdict, evidence, artifact page.

## Results (short)
- **Rule dispatch PASS**; the section machinery exists in the PM procedures.
- **Headline unreachable** — the 720-minute check and its 180-minute control could not be posed.
- **FINDING 1 (Medium)** — the corrective fix catalog carries neither a unit attribute nor a break/fix form here; those are the two things this ticket adds to corrective fixes.
- **FINDING 2 (Low)** — no asset on the tenant records a section count, so the change is currently inert.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3943-section-scaled-basic-fixes-verdict.md`
- Evidence: `docs/bug-evidence/zp-3941-per-section-pricing/api-captures.md` (the unit-attribute sweep)
- Artifact: https://claude.ai/code/artifact/ee8f908a-ec4c-4327-bc1b-6032cd545f43

## Depth notes (learning)
- **Split the ticket at its seam.** Dispatch and scaling ride together in the title but fail independently; reporting them separately is what let one half be a clean PASS.
- **Contrast where a feature *is* present against where it is not.** "PM procedures 40/42, corrective 0/53" is a far sharper report than "no unit attribute found" — it points at seeding rather than at the code.
- **A missing fixture is a finding about data readiness, not a testing failure** — and saying what fixture is needed is what lets whoever picks it up finish the job.
