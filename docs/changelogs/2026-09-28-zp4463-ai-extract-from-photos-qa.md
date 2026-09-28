# ZP-4463 filed: Extract from Photos (AI nameplate extraction) — deep sanity test on QA

**Date:** 2026-09-28 · **Env:** acme.qa.egalvanic.ai (build index-Bzh0axDQ.js) · **Ticket:** https://egalvanic.atlassian.net/browse/ZP-4463 (High, Z-26-09-S3, Web v2.3, To Do)

Trigger: owner's production capture (acme.egalvanic.ai, 5 nameplate photos → `POST /api/extraction/extract-nameplate-data` 504).
Owner then said to test on QA only, find more problems, and file ONE ticket.

## Fixtures (all "QA-DEMO … (delete me)", Circuit Breaker, uploaded through Create Asset › Nameplate)
Six synthetic breaker plates with known values (`docs/bug-evidence/zp-bulk-extraction-review-surfaces/CB*.jpg`, resized to phone
size) + real phone context photos from QA. Assets with 1, 5, 8, 12 (×4) photos, one not-a-nameplate photo, one create-mode asset.

## Results
| Photos | extract-nameplate-data | User saw |
|---|---|---|
| 1 | 200 / 21.0 s | success; all 9 values correct (Square D HJL36150 150 A 3P 25 kA 600 V + serial) |
| 5 | 200 / 39.8 s | success |
| 8 | 200 / 53.4 s | success |
| 12 | 504 / 60.5 s (×4) | raw "Failed to execute 'json' on 'Response' … is not valid JSON", out of view; server saved values 15 s later |
| 12 retry | 504 again / 60.4 s | second paid run, same error |
| 12 Attributes + Library | 504, then Bulk Extraction Job auto-submitted | second paid AI job |
| not a nameplate | 200 / 9.9 s | "No data was extracted" |
Passed: button disabled while extracting; Save after the late write keeps the values; create mode 5 photos 200 / 39.0 s.

## Mistakes caught on the way (none reached the ticket)
1. The editor lives in the asset page's ⋮ menu, not an Edit button. 2. The first "pass" recorded `engineering-matches` (fired by
the editor itself), not the extraction. 3. In the editor header "Extract from Photos" is a menu (Attributes / Attributes +
Library) — the first runs never picked an option. 4. "Shown twice" was my selector matching the alert and its text.
5. Stale-form Save looked like data loss; the field-by-field diff showed the extracted values kept.

## Files
- `src/test/java/com/egalvanic/qa/testcase/ExtractFromPhotosTest.java` (new; `-Dext.option`, `-Dext.afterError=save|retry`)
- `src/test/java/com/egalvanic/qa/testcase/ExtractionFixtureSetupTest.java` (new)
- `docs/bug-evidence/2026-09-28-ai-extraction-qa/` (5 screenshots incl. the owner's prod capture, per-run notes)

## Second pass (same day) + Artifact
- Background route (bulk-job, Step Function) with the SAME 12 photos: SUCCEEDED, chose the Square D plate with a written reason
  → the async route copes; supports moving single-asset extraction onto it.
- Re-extract on an already-filled asset: 200 / 28.5 s, updated 0, nothing overwritten; but a red "No data was extracted".
- Project Manager seat: allowed, 200 / 7.8 s.
- Every result message (success, no-data, error) renders at ~807 px in a ~600 px window — out of view in all cases.
- Artifact: https://claude.ai/artifact/3s35oJavxtXv9zanyPjGHT
