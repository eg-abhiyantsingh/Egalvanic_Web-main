# ZP-5031: BCES portal, read-only Engineering tab for Lite seats + Export Issue Report replaces CSV

**Verdict: stays In QA (BLOCKED).** The BCES behaviour is correct on today's build with SIMULATED tiers, and the real non-BCES negatives pass. A real BCES-Lite / BCES-Viewer portal login still can't exist on QA: no in-app path offers BCES tiers (details and the ask in ZP-5026/verdict.md).

- **Build:** `index-CkkAeA9o.js`, 17 Nov 2026 14:50–14:55 IST.
- **Seat:** Super Admin (staff portal License preview).
- **Settings changed:** none.

## Checked today
| Check | How | Result |
|---|---|---|
| REAL tiers on QA | `GET /api/features/access` | `read_only`, `no_license`. No BCES |
| BCES-Lite: Issues shows Export Issue Report, not Export CSV | SIMULATED BCES-Lite › Issues (Test site A) | PASS (sim): "Export Issue Report" ×1, "Export CSV" ×0 (screenshot 1) |
| Export Issue Report opens the report dialog | click | PASS (sim): "Generate Report" with Report Configuration "Issue Report (Copy)" (Custom, HTML), Output PDF, options for resolved issues, IR photos and issue photos. Not generated (screenshot 2) |
| BCES-Lite: Engineering tab, view-only | SIMULATED BCES-Lite › Assets › AST-2 › Engineering | PASS (sim): tabs Basic Info · Engineering · Maintenance · …; fields shown (catalog no. 56, contact type, interrupting rating 30 kA, 800 A, Cummins, ATS…); **0 editable inputs**, no edit / export buttons, no readiness ring (screenshot 3) |
| BCES-Viewer: no Engineering tab; Issues locked | SIMULATED BCES-Viewer | PASS (sim): tabs Basic Info, Maintenance, Issues, Connections, Photos, Attachments; Issues shows "locked on the BCES-Viewer plan" |
| Negative (REAL): non-BCES Lite keeps Export CSV | Lite preview › Issues | PASS: Export CSV ×1, Export Issue Report ×0 (screenshot 4) |
| Negative (REAL): Lite has no Engineering tab | Lite preview › AST-2 | PASS: no Engineering tab |
| Real BCES-Lite / BCES-Viewer portal login | — | **BLOCKED** (no BCES tier on QA) |

Not covered: the missing-attribute badge on an asset with gaps (AST-2 is fully filled), and generating the report.

## Ask
Same as ZP-5026:
- Enable `bces_lite` and `bces_free` in `offered_license_types` for EG-ACME on QA.
- QA then sets one QA-DEMO account to BCES-Lite and one to BCES-Viewer, each with a Client Portal login, runs these rows for real and restores both accounts.

## Cross-platform
- **Twins:** none (JQL "BCES"). This is a web Maintenance Portal change.
- **iOS source** (release/qa 34bd67e) has no portal or license-tier code, so this is not on iOS.
- **Android:** no source access; the portal is web-only.

### MOBILE-CHECKS
1. **iOS and Android: Client Portal test seat (CP_EMAIL).**
   - Steps: sign in.
   - Expected: there is no Issues export and no asset Engineering edit path for this customer-only role. Record what the app shows (blocked, or read-only data).
2. **iOS and Android: staff seat, Issues list for Test site A.**
   - Expected: no "Export Issue Report" or BCES wording. The mobile app has no portal tiers, so nothing should change there.
