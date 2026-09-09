# 2026-09-08 — QA: [Web] An accepted resolution could not price its own quote line, and there was no company-wide place to work the issue funnel — backend #1116 / frontend #1289

**Prompt:** the ticket text with its 13-step QA review ("test this ticket too").

## What was done
1. **Built the fixture rather than hunting for one** — accepted "Fit current-limiting Class J fuses at the upstream position" on an SCCR issue, priced its fuse at $45 in the Set-prices step, and produced quote `488ee745` through the product's own dialog.
2. **Checked the price by arithmetic, not by eye** — $235 = the resolution's 60 min of Journeyman labor ($100 sell) + 3 × $45 fuses ($135). Neither figure comes from a service-class default, which is the whole claim of tier-0 pricing.
3. **Chased the stale price four ways** — after editing labor 60 → 120 min and adding a material, the quote read $235 through a 25-second wait, a hard reload, a fresh navigation and the Quotes list. Only Edit Quote → Save & Regenerate moved it to $335.
4. **Pull-Through Work exercised as a company-wide surface** — four live stat cards, five filters, rows spanning five sites with no site selector, and the list call confirmed company-scoped.
5. **Manual resolution born accepted** — the create returns 201 with `accepted_at` already set; a full-page search found no Execute control before, during or after quoting.
6. **Probed the negative gate from both doors** — Add to Quote is correctly gated on an accepted resolution; Edit Quote is not, and its master checkbox put 29 issues onto a customer-facing draft. Restored afterwards.
7. **TBD quantity** — stored as null correctly, rendered as "×0 · $0 · $0" on the quote's Pricing tab.
8. Verdict, evidence with every request quoted, artifact page.

## Results (short)
- **Tier-0 pricing PASS**; Pull-Through Work, the Issues register trim and the manual-resolution path all PASS.
- **DEFECT 1 (High)** — the quote keeps the stale price after the accepted resolution is edited; only an explicit regenerate repriced it.
- **DEFECT 2 (Medium)** — an indeterminate material quantity prices as ×0 / $0 instead of TBD.
- **FINDING (Medium)** — the Add-to-Quote gate does not cover Edit Quote.
- 5 steps unreachable: no corrective service, no `mains_type` rule, no break/fix form, no evaluated-material role resolved, no French locale switcher.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-issue-resolution-pricing-pull-through-verdict.md`
- Evidence: `docs/bug-evidence/zp-issue-resolution-pricing-pull-through/`
- Artifact: https://claude.ai/code/artifact/cbdaeffb-6bb8-4cd5-8a5e-cb1bf5889ba4

## Depth notes (learning)
- **Arithmetic beats a screenshot.** "The quote shows $235" is weak; "$235 is exactly 60 min × $100/hr + 3 × $45, and neither number exists on the service class" is a claim a developer cannot wave away.
- **A stale-cache defect needs the cheap invalidations ruled out first.** Wait, reload, renavigate, then a different list — four independent reads before writing it down, because any one of them succeeding would have made it a UI refresh bug instead.
- **Gates live on call sites, not on features.** The same rule was enforced on one path and absent on another two clicks away. Ask where else this object can be reached.
