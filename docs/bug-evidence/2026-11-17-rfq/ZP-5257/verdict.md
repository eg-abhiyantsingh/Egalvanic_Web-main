# ZP-5257 [Android] Issue and asset pages crash when a table attribute holds numbers instead of text (Sentry EGALVANIC-PZ-8S)

**Verdict: PASS**

- Build: Android Z Platform (QA) **1.40 (54)**, emulator 1080x2400 density 420 (phone, portrait). Web QA build at read-back: index-LGhrn-48.js / index-CkkAeA9o.js.
- Seat: Super Admin QA seat, EG-ACME, site Android Site 2. 17 Nov 2026, ~14:15-14:45 IST.
- Fix: eg-pz-mobile-android PR #604 (Rajat's note: the 11 unit tests don't run in CI yet, so the device check is the protection).

## Test data (made through the web API, on my own QA-DEMO asset)
Issue class "Thermal Anomaly" (9cbb3bd9…), whose table attributes are "Current Draw (A)", "Current Rating (A)" and "Voltage Drop (mV)" (columns A/B/C/N). Asset "QA-DEMO ZP-3021 17 Nov Child CB3 no library".

| Issue | Current Draw (A) | Current Rating (A) | Voltage Drop (mV) |
|---|---|---|---|
| QA-DEMO ZP-5257 17 Nov numbers in table (318dd9f5-c855-4b6b-81f8-89e957479ab2) | `{"A":35,"B":"38","C":33,"N":25.50}` (numbers) | `{'A': 35, 'B': '38'}` (Python style) | `{"A":null,"B":12000000000,"C":{"x":1},"N":true}` (null, big number, nested, boolean) |
| QA-DEMO ZP-5257 17 Nov numbers video (5fc57307-5919-4836-9226-3f750b3ff437) | same as above | same | same |
| QA-DEMO ZP-5257 17 Nov text control (71ee0878-3b11-4dae-a4d4-79ace68aff79) | `{"A":"35","B":"38","C":"33","N":"0"}` (text, as every client writes it) | — | — |

The server stored every value exactly as sent (GET /api/issue/<id>).

## What was run
| # | Acceptance row | Result |
|---|----------------|--------|
| 1 | Issue with numbers / Python style / null / nested opens without crashing and shows the cells | Asset > Issues (3) > open the issue > Properties: no crash (logcat has no FATAL or ClassCastException, checked twice). Current Draw: 35, 38, 33, **25.50** (kept as written, no 25.0). Current Rating (Python style): 35, 38. Voltage Drop: A empty (null), B **12000000000** (no 1.2E10), C `{"x":1}`, N `true`. PASS |
| 2 | Opening writes nothing | Save Changes stays disabled after opening. PASS |
| 3 | Editing one cell and saving keeps the other cells; saved as text | Current Draw A 35 → 36, Save Changes. Server: Current Draw = `{"A":"36","B":"38","C":"33","N":"25.50"}` (other cells kept, all now text). Current Rating and Voltage Drop were not rewritten (unchanged). Done on both numeric issues. PASS |
| 4 | Normal text table looks and saves as before | The text control shows 35, 38, 33, 0. PASS |
| 5 | Opened from the asset page (the Sentry path) | Opened from the asset's Issues section. PASS |
| 6 | Phone and tablet, both orientations | Phone portrait only. Tablet (density 216) and landscape were not run for this ticket (see Not covered). |

## Observations (not blocking)
- A long value (12000000000) is cut off by the narrow cell on a phone ("12000(" visible). The value is complete in the field (the accessibility text shows the full number).
- Saving the issue on Android also added an empty "Delta T" detail (`"Delta T" = ""`), because the calculated Delta T has no inputs. It does no harm to the table, but it is a write to a field the user didn't touch.

## Not covered
- Tablet size and landscape for this screen.
- iOS twin ZP-5259 (To Do): not checked.

## New defects
None in the ticket's scope.

## Cross-platform
- **Web** (index-LGhrn-48.js, issue › Class Details), the same records after the Android save: Current Draw "A: 36, B: 38, C: 33, N: 25.50" (PASS, read-back). It differs on odd data: web shows the Python-style value as raw text "{'A': 35, 'B': '38'}" (Android parses it into cells), and for Voltage Drop web shows "A: null, … C: [object Object]" (Android shows an empty cell and {"x":1}). No client writes this data, so this is a note, not a filed bug.
- **iOS** (ZP-5259, To Do, iOS v1.76): per the ticket, iOS empties the whole table for this data. Not checked.
- MOBILE-CHECKS (iOS, when ZP-5259 is built): open the three issues above from the CB3 asset's Issues section: no crash, the same cells as Android. Edit one cell and save: the others are kept (check with GET /api/issue/<id>).

## Test data
- Asset https://acme.qa.egalvanic.ai/assets/30248abc-0157-415b-868b-58a2fd6be56c
- Issues https://acme.qa.egalvanic.ai/issues/318dd9f5-c855-4b6b-81f8-89e957479ab2 , https://acme.qa.egalvanic.ai/issues/5fc57307-5919-4836-9226-3f750b3ff437 , https://acme.qa.egalvanic.ai/issues/71ee0878-3b11-4dae-a4d4-79ace68aff79
- Cleanup: see the final report. QA has no issue-delete route (DELETE /api/issue/delete/<id> → 404, DELETE /api/issue/<id> → 405) and the web issue page has no Delete item.

## Evidence
- QA-17Nov-ZP-5257-table-numbers-no-crash.mp4 (49 s)
- QA-17Nov-ZP-5257-1-asset-issues-section-3-test-issues.png, -2-issue-tables-numbers-python-null-nested.png, -3-edit-one-cell-36-save-enabled.png, -4-web-same-issue-numbers.png, -5-text-control-issue.png, -6-web-text-control.png
