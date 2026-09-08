# 2026-09-08 — QA: Asset PM maintenance programs, explicit check-off registry, check-driven completion (backend #1171 / frontend #1339 / iOS #522)

**Prompt:** the ticket text with its 6-step QA review ("test this ticket").

## What was done
1. **Deployment fingerprint** — bundle `index-CSsDpG3c.js` + the live `asset-maintenance/*` API family confirmed the whole web half is on QA, contradicting the ticket's dev-only note.
2. **Established a program both ways** on MAIN-BUS-3836: from the Suggested-ranked plan with per-service last-serviced dates, and (on 13N-H1-1) from directly composed services via the Custom Program tab.
3. **Proved the effective-last-serviced rule** in both directions — a registry date beat an earlier stated date, a stated date won where no registry entry existed, an undated line fell to today + cadence — then proved it survives a single-line cadence re-tune (merge) and a whole-plan replace, including an anchor the replace payload never resent.
4. **Bulk configurator**: same-class selection applied one plan to two assets in a single call (8 schedules, per-asset dates); mixed-class selection makes the button disappear. Legacy "Edit PM Designations" no longer renders; the old dialog's copy is unused i18n only.
5. **Check-driven wheel**: created a PM Forms work order through the wizard (8 assets / 9 forms) → the ring is keyed by the service name and one tick moved it 0 % → 13 %; on the AF+IR work order the rings summed exactly to 56 % and one tick took it to 63 %. The same tick wrote the check-off registry and re-dated the asset's next due — the whole loop in one action.
6. **Negative shutdown gate**: `never` skipped exactly the de-energized lines with a warning; `planned_shutdowns_only` pulled them into a 3-year outage window; restored afterwards.
7. Verdict doc, evidence folder (31 screenshots + full API captures), artifact, memory, adversarial refuter workflow.
8. **Also patched ticket 3** (reserved formula names) per its refuter pass: checklist item 2 downgraded PASS → PARTIAL, the AI-build defect's causal claim marked as not isolated, the pipeline verdict re-worded to "not deployed" with an explicit n = 1 caveat; artifact republished at the same URL.

## Results (short)
- **6 of 6 web-testable steps PASS.** iOS not web-testable.
- **DEFECT 1 (Medium):** the bulk Apply PM Plans path drops de-energized schedules silently — a 4-service plan on two never-shutdown assets wrote 1 service each; `skipped_never_shutdown: 6` and its `warnings[]` never reach the screen, and the bulk dialog omits the per-line "Won't be set up" labels the single-asset dialog shows.
- **5 Low findings:** restriction not applied retroactively; an undated line keeps its outage-aligned date after the restriction is relaxed; two label sets for the same four shutdown values; identical duplicate rows in the plan picker; the Locations-rail readiness endpoint disagrees with the completion endpoint (API only).

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-asset-pm-programs-check-driven-completion-verdict.md`
- Evidence: `docs/bug-evidence/zp-asset-pm-programs-check-driven/`
- Artifact: https://claude.ai/code/artifact/7f56422d-f1ad-45f7-869e-907c058ba8e4
- Memory: `project_asset_pm_programs_check_driven.md`

## Depth notes (learning)
- **Test the rule, not the screen.** Every due date was recomputed by hand from cadence + anchor before believing the table. That is what caught the undated-line finding, which the UI presents as if it were derived.
- **The end-to-end loop is worth one extra API read.** Ticking a check-off and then reading the asset's maintenance record turned four separate PASS statements into one causal chain: check → registry → effective date → next due.
- **Warnings in a 200 response are a UI contract, not a detail.** The server did the safe thing and reported it; the defect is entirely in who gets told. Capturing the response body is what made that reportable.
- **A gate that renders differently at scale is the interesting case.** The single-asset dialog warns, the bulk one does not — the same feature, tested twice, with opposite disclosure.
