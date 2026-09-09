# 2026-09-08 — QA: ZP-4024 — Block reserved formula names in pricing (blended_rate) — frontend #1333 + pipeline #86

**Ticket:** https://egalvanic.atlassian.net/browse/ZP-4024

**Prompt:** ticket "Block reserved formula names in pricing (blended_rate) across web and the spec builder" with its 4-step QA review ("test this ticket").

## What was done
1. **New bundle overnight** (`index-CSsDpG3c.js`): fingerprinted the reserved-name set, the "Reserved name" helper, the alert copy and the Save gate → frontend #1333 live on QA despite the "dev only" note.
2. **MFA became mandatory on QA** ("Set up later" gone). Enrolled the `+admin@` seat in **Email OTP** so the UI could be reached; recorded the consequence for the framework (LoginPage MFA bypass dead, no OTP reader) in memory.
3. **Server contract probed** on the QA-DEMO hand-built service: reserved names → 400 with a row index (`pricing.formulas[0]: 'blended_rate' is a reserved name`), `service_price` → 200, burden flag without `total_labor_hours` → 400 with the rule text.
4. **Pricing Setup editor driven live:** `blended_rate` → red field + "Reserved name" + alert + Save disabled; `Blended Rate` normalised and flagged; `evaluated_hours` flagged; rename to `rate_blend` → Save 200, persisted.
5. **Walk pricing:** the global Arc Flash Data Collection formula was switched to READ `blended_rate` → walk "Test_sitewalk_21" priced US$350 with the calculation panel showing the $125/hr blend; restored to `evaluated_labor` (US$291.68). Noted that the API let a tenant admin write a global service the UI only lets you fork.
6. **Spec builder:** one real "Update service" build with "Set up pricing" → version 2 applied, but the spec's pricing block names a formula `blended_rate`, has no `assumes_burden_rate`, and the service stayed "Needs pricing" with no error — pipeline #86 not effective on QA and the refusal is swallowed.
7. Verdict doc, evidence folder, artifact, memory, adversarial refuter pass.

## Results (short)
- Frontend #1333 **PASS** (all four checklist behaviours on web).
- Backend reservation live and row-indexed; `service_price` allowed.
- **DEFECT 2 (High):** spec builder on QA still authors `blended_rate` without `assumes_burden_rate`; the rejected pricing block is dropped silently (job "applied", `error null`, service "Needs pricing").
- **FINDING 1 (Medium, out of scope):** tenant admin can PUT a global service's pricing config via API (UI only offers Customize/fork).
- The $2,600 dev repro and a genuine legacy `blended_rate` config are not reproducible on QA (server refuses new ones; none exist).

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-blended-rate-reserved-formula-names-verdict.md`
- Evidence: `docs/bug-evidence/zp-blended-rate-reserved-names/`
- Artifact: https://claude.ai/code/artifact/bd050b37-a129-4cb7-9764-8650043677a9
- Memory: `project_pricing_setup_reserved_names.md`, `project_mfa_enrollment_blocks_suite.md` (MFA change)

## Depth notes (learning)
- **Fingerprint the bundle hash every session.** It changed overnight; a stale assumption about what is deployed would have mis-scoped the whole run.
- **Test the guard from both sides.** The UI blocks the name; the API also refuses it (400 with the row index). Knowing the server refuses explains why the "broken legacy config" cannot be manufactured on QA — and why the AI build's pricing vanished.
- **Read what the AI wrote, not what the job status says.** `build_job.status: applied` looked green; the version spec exposed the reserved formula and the missing flag. The version endpoint is the observability window into the external pipeline.
- **Restore what you touch.** The walk-pricing proof needed a formula on a global service; the write was reverted within minutes and the restoration was verified by re-pricing the walk.
- **Environment drift is a finding too.** Mandatory MFA changes how every future automated login must work; recording it saves the next session hours.
