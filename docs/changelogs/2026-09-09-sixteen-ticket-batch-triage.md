# Sixteen-ticket batch triage

**Date:** 2026-09-09
**Prompt:** sixteen pasted tickets (ZP-3874, 3887, 3859, 3901, 3902, 3904, 3905, 3906, 3908, 3910, 3911,
3912, 3913, 3933, 3938, 3941) + "test all this ticket too"
**Env:** `acme.qa.egalvanic.ai` · V1.36 · bundle `index-jYhUcFb4.js` · read-only

## Outcome

Six verify on QA, four are blocked by the environment, four are AI-pipeline work the web tier cannot
reach, and two need a UI pass that mandatory 2FA prevents.

**Every ticket in the batch was labelled "cicd/dev only" and that is wrong for most of them** — the code
is already deployed on QA. The `feedback_ignore_dev_only_deploy_notes` rule paid off again.

| Ticket | Verdict |
|---|---|
| ZP-3911 Issues list perf | **PASS** — 1.56s cold / 0.51s warm; four funnel stages reconcile exactly |
| ZP-3938 Issues list 500 | **N/A** — QA has `resolution_processing`, not `_at`; bug never existed here |
| ZP-3904 Designations schedules | **LARGELY PASS** — routes + kind scoping + winding attrs live; 2 observations |
| ZP-3905 Confidence chip | **PASS by code** — all four review points, incl. the no-chip negative |
| ZP-3912 Workbench panel_type crash | **PASS by code** — object→`.code` at both sites + `—` placeholder |
| ZP-3901 Panelboard SCCR | **SUPERSEDED** by ZP-3902, correctly |
| ZP-3874 Staff elevation | **DEFAULT-DENY PASS**; positive path needs a staff Cognito ID token |
| ZP-3902 Bus SCCR first-class | **BLOCKED ON SEED** — 0 panel manufacturers on QA (documented caveat) |
| ZP-3908 Core/custom split | **PARTIAL** — split + Open in SLD shipped; lock tooltip absent from bundle |
| ZP-3941 Per-section pricing | **PARTIAL** — control shipped; option label is the attribute name |
| ZP-3859 Extract from Photos | **CODE PRESENT** — placement needs UI |
| ZP-3887 MCP connector gate | **NOT VERIFIABLE** — route resolves to the SPA on the QA tenant host |
| ZP-3906 many-image 2000px cap | **NOT WEB-TESTABLE** — external Lambda |
| ZP-3910 resolution-agent lambda | **NOT WEB-TESTABLE** — needs lambda + Step Function + DynamoDB |
| ZP-3913 kA voltage rule | **NOT WEB-TESTABLE** — prompt-level, needs agent narrative |
| ZP-3933 eligible-fix menus | **NOT WEB-TESTABLE** — prompt + validator in the pipeline repo |

## Findings worth carrying

- **ZP-3911's funnel "mismatch" is not a bug.** `stat_counts` sums to 935 vs a 924 total, but
  `unset 841 + ready 2 + quoted 27 + resolved 54 = 924` exactly; the excess 11 is `awaiting_review`, a
  fifth overlapping counter the ticket never names.
- **ZP-3904 silently accepts an unknown `kind`** (`kind=banana` → the `all` scope, no 400) and returns
  `total: null` on every kind-scoped response while `stats` is populated.
- **The staff MCP server points at PRODUCTION**, not QA (`acme` = `0a61e613…`,
  `eg-pz-prod-s3-branding-ohio`, 36 real customer tenants). Read-only calls only; never QA evidence.
  This forced a retraction in the ZP-3863 verdict published earlier the same day.
- **`"Core Attributes"` in the bundle proves nothing** about ZP-3908 — it is a long-standing i18n key
  used by unrelated screens.

## Blocker needing an owner decision

Mandatory 2FA enrollment covers the app for every seat held; a fresh login is recognised then buried
under the enrollment screen, the session-priming recipe does not clear it, and the auth storage key is
computed rather than literal. Enrolling one seat in Email OTP would unblock UI verification (the repo
carries a Gmail app-password) but changes account state and breaks the headless RBAC suite — owner's
call, not mine.

## Deliverables

- Verdict — `docs/bug-reports/2026-09-09-QA-sixteen-ticket-batch-triage-verdict.md`
- Artifact — https://claude.ai/code/artifact/a4b8d0da-50fa-4ccd-8b54-acf4632239fe

**No screenshots.** The 2FA gate screenshot was attempted three times and every capture timed out on
"waiting for element to be stable" (the blocked page animates a spinner indefinitely). Recorded honestly
rather than substituting an unrelated image.

## Footprint

Read-only. No work order, asset, class, user, site, report config, procedure or LaunchDarkly flag was
created, modified or deleted. No bulk-extraction job was submitted (billable, auto-submits).

---

## Update — UI verification completed (2026-09-10)

The 2FA "blocker" was my own error: the enrollment screen has a **"Set up later"** button, and one click
goes straight to the dashboard with no enrollment and no account-state change. No seat was enrolled; the
headless RBAC suite is untouched. Saved as `feedback_finish_unblocked_work_then_ask_at_end` and the stale
`project_mfa_enrollment_blocks_suite` note has been corrected.

Revised verdicts:

- **ZP-3904** — now **PASS on function, FAILS on access control**. All six routes render with
  scope-specific data (162/40/111/2/153), both required headlines exact, transformer page correctly has
  no Class column. **But `features.equipment_designations.view` is held by only the Electrical Engineer
  seat, and all four schedules load with full data on PM** — declared permission, never enforced.
  Column deviation: SCCR page shows *Available Fault Current*, not the ticket's *Devices* column.
- **ZP-3902** — **UI PASS, data BLOCKED**. Manufacturer / Panel Type / "No SCCR marking…" / SCCR (Label)
  all render correctly. But the old free-text Manufacturer/Type/Model are *still* on the Panelboard class,
  so `sccr_first_class_migrate` has not run here either.
- **ZP-3908** — split **confirmed** in the editor (Core Attributes then Custom Attributes, own
  sub-headers). Two gaps: no lock icon, and Open in SLD is gated on `features.slds.view` which **PM and AM
  do not hold** while all six roles hold plain `slds.view`.
- **ZP-3859** — **PASS**. Extract from Photos in the editor header, Asset Subtype present, Engineering
  section ordered before Schedule/OCP.
- **ZP-3901** — inline SCHEDULE section (Edit Panel Schedule / Status / Circuit Count) reachable in the UI.

Still code-verified only: **ZP-3905** (needs a billable auto-submitting bulk-extraction job) and
**ZP-3912** (needs a resolution proposal on a panel with a panel type, which the unseeded library
prevents).
