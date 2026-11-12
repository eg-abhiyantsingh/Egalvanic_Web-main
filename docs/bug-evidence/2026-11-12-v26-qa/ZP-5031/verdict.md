# ZP-5031 · BCES portal: read-only Engineering tab for Lite seats; Export Issue Report replaces CSV (QA, 12 Nov 2026)

**Verdict: PARTLY** on QA, Web v2.6 build `index-C1ZRIunF.js` (checked 16:01 and 17:04 IST).

The BCES behaviour works in the Maintenance Portal staff License preview with **SIMULATED** BCES tiers (`/api/features/access` rewritten):
- BCES-Lite gets a view-only Engineering tab.
- BCES-Lite gets "Export Issue Report" instead of "Export CSV", and it opens the report dialog.
- BCES-Viewer has no Engineering tab.

The non-BCES negatives pass on real tiers.

A **real BCES-Lite / BCES-Viewer portal login is BLOCKED**: QA acme offers no BCES tier.

Seats used:
- Super Admin (staff License preview).
- Client Portal seat on a No License account (real, non-BCES).

## Checked
| Check | How | Result |
|---|---|---|
| BCES-Lite: Engineering tab visible on asset details | SIMULATED BCES-Lite › Assets › AST-2 | PASS (SIMULATED): tabs Basic Info · **Engineering** · Maintenance · Issues · Connections · Photos · Attachments. Fields shown: catalog no., contact type, interrupting rating, amps, manufacturer (Cummins), mechanism, model, mains type, serial, size, voltage, type (screenshot 3) |
| … view-only: no edit, no export, no readiness ring, no missing-attribute badge | same | PASS (SIMULATED): 0 editable inputs, no edit or export buttons, no readiness ring. The tab shows a "Custom attributes 4 / 4" count with green ticks. This asset has no missing attributes, so the "missing" badge case was not seen |
| BCES-Viewer: no Engineering tab (Lite only) | SIMULATED BCES-Viewer, same asset | PASS: no Engineering tab |
| Non-BCES portal (real Lite tier preview): no Engineering tab | Lite preview, same asset | PASS: no Engineering tab, unchanged |
| BCES-Lite: Issues shows Export Issue Report, not Export CSV | SIMULATED BCES-Lite › Issues (Test site A) | PASS (SIMULATED): "Export Issue Report" present, "Export CSV" absent (screenshot 1) |
| Export Issue Report opens the report modal | click | PASS (SIMULATED): the "Generate Report" dialog opens with Report Configuration preselected to "Issue Report (Copy)" (Custom, HTML); the other option is "Issue Report". Output PDF, IR photos, issue photos. Not generated (screenshot 2) |
| BCES-Viewer: Issues | SIMULATED BCES-Viewer | Issues is locked on this tier ("locked on the BCES-Viewer plan"), so its export button can't be reached. Consistent with the tier map (Viewer = Site Overview + Assets) |
| Negative: a non-BCES seat keeps its usual Issues export | real Lite preview › Issues; staff Site Data › Issues | PASS: "Export CSV" present, no "Export Issue Report" (screenshot 4) |
| Negative: real Client Portal seat (No License, non-BCES) | CP › Issues | Issues is locked on No License (plan lock), so there is no export. Unchanged |
| Role / API | UI-only permission per the ticket (no backend change) | n/a. The export runs through the existing report modal |
| Flags | none on this change | n/a |
| Performance | Issues page / report dialog | No call over 2 s observed |

## Bugs found
None.

Note: "the matching issue-report configuration" preselects the company's custom copy "Issue Report (Copy)" rather than the global "Issue Report". This looks deliberate (company template first); confirm with Eric.

JQL run: `project = ZP AND (… OR summary ~ "BCES" …)`. ZP-4978 (On Hold, "remove only the legacy Issue Report option from Condition Assessment exports") is related but different. No ticket covers this.

## Enhancement suggestions
In the Export Issue Report dialog, show which configuration matches BCES (for example a "BCES" chip), so a portal user doesn't pick the wrong template.

## Mobile impact
None. This is a web portal only change.

## Not covered (and why)
- A real BCES-Lite and BCES-Viewer portal login, and generating the issue report as one.
  - QA acme offers no BCES tier and has no BCES account.
  - Needed: BCES-IQ (shipped there as a prod hotfix), or a QA company offering `bces_lite` / `bces_free` with a Client Portal seat on each.
- The missing-attribute badge on an asset with gaps. The tested asset was fully filled.

## Proposed Jira comment
See `ZP-5031.txt`.

## Recommended status
**Stay In QA**, or the owner's call. The behaviour is correct with SIMULATED BCES tiers and the non-BCES negatives pass, but the BCES side needs a real BCES-Lite / BCES-Viewer portal seat.
