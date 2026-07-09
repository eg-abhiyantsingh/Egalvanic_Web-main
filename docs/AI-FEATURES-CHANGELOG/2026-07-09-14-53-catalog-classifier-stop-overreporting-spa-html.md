# Catalog classifier: parameterized SPA catch-all is not a per-endpoint critical

**Date:** 2026-07-09
**Time:** 14:53 UTC
**Author:** Claude Code (automation, Fable 5), for abhiyant.singh@egalvanic.com

## What was asked
"Is this correct?" for a list of critical catalog findings (account paths returning HTML, /agent/clear
auth gate, /agent/health 503).

## Verdicts after live verification
- 6× account/`/api` HTML → **false criticals** (real id → JSON; HTML only for a random id = SPA catch-all).
- `/agent/clear` unauth → **not reproducing** (415, no data).
- `/agent/health` 503 → **transient** downstream blip (200/healthy now).

## Fix
The catalog classifier flagged all `200+HTML` GETs critical. Now: parameterized path + HTML = `info`
(expected catch-all for a synthetic id); fixed path + HTML = `critical` (SPA shadows a real route).
Live: 127 reclassified to info, 2 genuine criticals remain (`/api`, `/equipment-library/datstyle/resolve`).

## Depth explanation (for learning + manager review)

**A test that cries wolf is worse than no test.** The catalog probe substitutes a random UUID into
`{account_id}` to exercise parameterized routes safely. On this SPA-served backend, an unmatched
`/api/{random-uuid}` falls through to the app's HTML shell (200 HTML) instead of a JSON 404. The
classifier read "200 + HTML on an API path" as a critical SPA-fallback bug — but for a *parameterized*
path that's just "no record with this synthetic id", not a defect. The discriminator is already in the
code (`parameterized = !template.equals(path)`); the classifier just wasn't using it for the HTML branch.
Real ids returning JSON (verified with a live account id) is the proof the endpoints are fine.

**Why keep the unparameterized case critical.** A *fixed* path like `/equipment-library/datstyle/resolve`
has no id to miss — if it serves HTML, the SPA route genuinely shadows a registered JSON route (a real
routing bug). Keeping that critical while downgrading the 127 parameterized ones is the precise line
between signal and noise, and it's exactly the "verify falsifiable claims before HIGH/CRITICAL" discipline
the repo owner set after a prior over-report.

**On the other two.** `/agent/clear` returning 415 (not a 200-JSON bypass) and `/agent/health` being
200-healthy (the 503 was a downstream flap) are both reminders that point-in-time report labels must be
re-verified live before acting — a scheduled probe captures a moment, not a standing truth.

## Validation
Live full-catalog run: 1078 ops, BUILD SUCCESS, account paths now `pass`, 127 info reclassifications,
2 legitimate criticals retained.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/ApiFullCatalogHealthApiTest.java`
