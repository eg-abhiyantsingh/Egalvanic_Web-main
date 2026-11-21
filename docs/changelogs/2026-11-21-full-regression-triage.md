# Full regression on QA, 21 Nov 2026: web triage and two test-framework fixes

- **Ask (21 Nov 2026, 13:35 IST):** run the full web and iOS automation and share the report.
- **Web run:** https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/actions/runs/44883499175
  - Started 13:39 IST. First pass finished 18:32 IST. Re-run of failures finished 21:12 IST.
- **QA build:** web v1.36 (sidebar label), `acme.qa.egalvanic.ai`.
- **iOS run:** still running (GitHub runs 5 macOS jobs at a time). It will be triaged separately.

## Result after the re-run

| Tests | Passed | Failed | Skipped | Pass rate |
|---|---|---|---|---|
| 1,506 | 1,239 | 202 | 65 | 82.3% |

Two tests failed first and passed on re-run (`MOT_AST_02`, `TC_TD_004`).

**Bottom line: this run found no new product bug.**
- 167 failures are test upkeep. The app's screens changed in v1.36, and 3 failure groups are bugs in our own tests.
- 34 are an open product question that has not changed since 10 Oct: engineering fields removed from some asset classes.
- 1 is the known open ticket ZP-2025.

## What the 202 failures are

"By hand" means it was checked on QA in the browser on 21 Nov, without saving anything.

| Cause | Count | What it is |
|---|---|---|
| Voltage control changed (by hand) | 57 | Voltage is now a type-ahead box ("Select or type a voltage") instead of "Select voltage". All values are still offered (120V to 69kV for Generator; Transformer Secondary Voltage has 29). The tests look for the old placeholder. **Test upkeep.** |
| "Battery" class renamed (by hand) | 21 | The class list has only "DC Battery". The tests open "Battery". **Test upkeep.** |
| Library match behind a switch (by hand) | 12 | Circuit Breaker and Transformer library matching now sits behind a new "Use library" switch, which is off by default. The library search appears when it is turned on. **Test upkeep.** |
| Location names get an extra "a" (root cause found) | 21 | **Bug in our test**, fixed in this commit (see below). Every building, floor and room was saved as `a<name>` (for example `aP2_Floor_86534`), so the exact-name lookups failed. |
| Work Orders redesign (by hand) | 24 | The list columns are now Work Order, Site, Window, Service, Crews, On site, Priority and Status. "Show planned" is gone, and the create dialog changed. The list itself works (212 open). **Test upkeep.** |
| Accounts is now Customers (by hand) | 7 | `/accounts` redirects to `/customers`, which shows a customer tree with a detail panel. **Test upkeep.** |
| Opportunities is now Quotes (by hand) | 1 (+32 skipped) | The page is "Quotes" and the name column is "Quote". **Test upkeep.** |
| Arc Flash Readiness (by hand) | 7 | "Generate Report" is now "Export readiness report". A new "All buildings" filter is read instead of Connection Type. Test site A's connections all have "No Type", so that filter is empty. The CB grid columns changed. **Test upkeep and test data.** |
| Login bug-hunt tests | 8 | Old one-step login locators: the password field appears only after "Use my password". Same as 10 Oct. **Test upkeep.** |
| Wrong class or control picked | 3 | `TRF_AST_01` types "Transformer" and takes the first match, which is now the new "Instrument Transformer" (by hand: "Transformer" still offers "Dry Transformer"). `MOT_AST_01` reads the wrong dropdown. `AS-07` reads a grid that is now a hierarchy view. **Test upkeep.** |
| Single test-side failures | 6 | Goals has a new status, "Needs Attention". Equipment Designations has a new table (seen 10 Oct). The Task form check is timing (seen 10 Oct). Issues "cancel delete" counts rows while the grid loads (17→1 on the first try, 21→2 on the re-run; the next test, "deleted issue not in search", passed). `BUG-012` also fails on the normal `alliance-config` call; the warning itself is not shown. `BUG-008` ("all pending tasks overdue") no longer reproduces (6 pending, 0 overdue), so that check can be retired. |
| **Open product question** (by hand for Motor Starter) | 34 | **Unchanged since 10 Oct, still needs a product decision:** <br>• Motor Starter and VFD show no Phase Configuration or Mains Type (by hand, the Motor Starter Engineering section shows only System Voltage). <br>• Busduct and Junction Box show no Mains Type. <br>• Switch has no Manufacturer. <br>• Rectifier shows Primary/Secondary Voltage. <br>• Disconnect Switch lost the >1000V subtypes and Load-Interruptor. <br>If this is intended, the tests need updating; if not, it is a product bug. |
| Known open ticket | 1 | ZP-2025, tracked in Jira. |

Check: 57 + 21 + 12 + 21 + 24 + 7 + 1 + 7 + 8 + 3 + 6 = 167 test-side, plus 34 + 1 = 202.

Also checked: the Locations overview says exactly "100 Floors". That is not a page limit. The API returns 100 floors and the buildings' own floor counts add up to 100. Today's new floors are included.

## Code changes in this commit

### 1. `LocationPage.typeField`: select-all with the right key for the platform

**Before:** to clear a field, it sent **both** Ctrl+A and Cmd+A, so one of them would work on any OS.

**Problem:** on Linux (GitHub Actions), Cmd (Meta) + A does not select anything. Chrome types a literal **"a"** instead. The field was empty in create dialogs, so every name became `a` + name:
- `aP2_Bldg_3628`
- `aNoNotes-64667`
- `aCorporate Tower 20830`

The tests then searched for the exact name they typed (`P2_Floor_86534`), found nothing, and failed: 18 Location Part 2 tests and 3 Location tests.

**How it was found:**
- The QA locations API showed the test's floors existed, but all with a leading "a".
- The only place that types location names is this helper.
- Every other page object already picks the modifier by OS. `InputUtil` documents the mirror-image macOS problem.

**After:** it sends one select-all using `InputUtil.selectAllModifier()`, which is Cmd on macOS and Ctrl elsewhere. This is the same helper the rest of the suite uses.

### 2. Report scripts: hide Java array references in test names

**Problem:** data-driven tests that pass a `String[]` showed it in the client report as `Battery · [Ljava.lang.String;@624a24f6 · EDITABLE`.

**Cause:** `consolidated-report.py` and `consolidated-detailed-report.py` filtered object references with `^[\w.$]+@[0-9a-fA-F]+$`. That pattern misses the leading `[` and the `;` of array references.

**Fix:** both scripts now use the same pattern that `customer-bug-report.py` already used: `^\[*[\w.$;]+@[0-9a-fA-F]+$`.

## Validation

- `mvn -o compile` passes.
- I checked the pattern on real samples. It filters `[Ljava.lang.String;@…`, `com.foo.Page@1a2b` and `[[I@5e91993f`. It keeps `Battery`, `13.8kV`, `Disconnect Switch (> 1000V)` and `user@example.com`.
- I rebuilt the per-class matrix report from this run's results with the patched script. It now reads `Battery · EDITABLE`, with 0 `Ljava` occurrences.
- A CI run of only the Location + Task group proves the Locations fix on Linux. The result is added below when it finishes.

## Learning notes

- A test that types into a field must use the platform's own select-all key. Sending "both to be safe" is not safe: the key that isn't bound on that OS can type a character.
- When many tests fail with "element not found" after a create step, check the stored data before blaming the UI. Here the record existed with a changed name.
- A round number in the UI (100) is worth checking against the API, because it is often a page limit. Here it was not: the site really has 100 floors.
