# ZP-5218: Add Existing by Service, "Downstream of" keeps rows that don't match the typed text

**Verdict: PASS**

Video: `QA-17Nov-ZP-5218-downstream-of-video-steps.mp4` (60 s). Screenshots 1–4 `QA-17Nov-ZP-5218-*-PASS.png`. Jira draft: `ZP-5218.txt`.

- **Build:** QA `index-oFAZQ3rK.js`. The build was the same at the start and end of every run on 17 Nov 2026 (11:15–11:27 IST).
- **Seat:** Super Admin (`VALID_EMAIL` seat).
- **Fix:** eg-pz-frontend PR #2028, merged to `cicd/qa` on 13 Nov. It adds `getOptionKey={(o) => o.id}` to the Downstream of Autocomplete in `WorkOrderAssetPicker.jsx`. Both screens use that component.
- **Ticket comments:** one comment (Dharmesh, 16 Nov: "Merged to QA via PR #2028"). It adds no test rows. The PR's own test rows are the two screens below, and both are covered.

## What was run
Work order "Work Order - Nov 3, 5:15 PM" (site "(s) Wild Goose Brewery"): https://acme.qa.egalvanic.ai/sessions/48e9907c-dd41-4479-baa6-fb544e32d929

Path: Assets › Actions › Add Existing by Service › Arc Flash Data Collection (284 of 284 qualifying assets) › Narrow scope › Downstream of.

| # | Case | Result |
|---|---|---|
| A1 | Ticket repro: click Downstream of (the list opens with 100 rows), then type `ZP-5171` key by key (160 ms per key) | PASS. 2 rows, both contain ZP-5171: "QA-DEMO 4 Nov ZP-5171 Main Distribution Panel (QA5171-1002)" and "… Main Distribution Panel (Copy)". 0 leftover rows (5 Nov: 18 rows, 17 wrong). |
| A1b | Pick the real match | PASS. "Downstream of" shows the panel, and the dialog says "7 of 284 qualifying assets selected" (its 7 breakers Q4NZMDP-Circuit Breaker 1–7). 5 Nov: a wrong asset was picked and the dialog said "0 of 158". |
| A2 | Same text typed fast (15 ms per key) | PASS. 2 rows, both match. |
| A3 | Type `Main Distribution` (5 Nov: 35 rows, Capacitor 1 first) | PASS. Only the 2 matching rows. |
| A4 | Duplicate names: type `Capacitor 2` (3 assets share this name, none has a QR code) | PASS. 3 rows "Capacitor 2", each with its own React key (asset id). No other names. |
| A4b–d | In the same open list: backspace to "Capacitor", then type " 5", then clear and type ZP-5171 | PASS. 24 Capacitor rows → 3 "Capacitor 5" rows (3 different ids) → 2 ZP-5171 rows. No leftovers at any point. |
| A5 | Paste control: fill `ZP-5171` without opening the list | PASS. 2 rows. |
| A6 | Open the list, then paste (fill) | PASS. 2 rows. |
| A7 | QR search typed after opening: `QA5171` | PASS. 7 rows, all carry a QA5171 QR code. |
| A8 | Parent asset control (typed) | PASS. 2 rows. |
| A9 | Text that matches nothing (`zzqq-none`) | PASS. "No options". |
| keys | React key of each Downstream of row (read from the React fiber) | Asset UUIDs (e.g. 2529924b…, 968c7dc6…). No duplicate keys in any list. |
| C1 | Create Work Order wizard (now 5 steps: Work order › Scope › Assignment › Schedule › Review): site (s) Wild Goose Brewery › Scope › Add service Arc Flash Data Collection › Edit assets › "Pick individual assets…" › Downstream of: open, then type ZP-5171 | PASS. 2 rows, both match, keys are asset ids. |
| C2 | Wizard: type `Capacitor 2` | PASS. 3 rows, 3 different ids. |
| C3/C4 | Wizard: paste ZP-5171, then pick the match | PASS. "7 of 284 assets selected". |

**Writes:** none. The only POSTs were `/api/ir_session/scope-preview` (read-only scope counts) and list reads. Every dialog was cancelled and the wizard was never submitted. The work order's Assets tab shows "Assets 1" before and after.

**Console:** no app errors. There was one third-party `ERR_FAILED` and one 400 on a third-party resource (Beamer/DevRev family), the same as on other pages.

## Not covered
- Other sites. Any site with same-name assets is the risky case, and Wild Goose has 3× "Capacitor 2", 3× "Capacitor 5" and so on, so the risky case was covered here.
- Roles other than Super Admin. The picker is the same component for every role that can open the dialog.
- Mobile not checked. This dialog is a web screen; iOS and Android have no "Add Existing by Service" picker of this kind.

## New defects
None.

## Side note (not a defect)
The work order now has 1 asset. The ticket said it had 2 on 5 Nov. Someone else changed it after 5 Nov; this run did not.
