# CI failure depth audit — every failure classified

**Date:** 2026-08-26 · **Prompt:** "did u check in ddepth"

Honest answer at the time was **no**. Gaps: Arc Flash's 3 triaged "Bugs" unreviewed, OPEN-1
unverified, and ~80 failures accepted on auto-triage labels. All now closed.

Method: cached both run logs locally (suite1 4.9 MB / suite2 10.5 MB), fanned out one analyst per
failure cluster, then verified live in the browser. **99 failures individually classified.**

## Confirmed — new
- **SEC-1 (High)** — `/api/auth/login` has no rate limit, lockout or CAPTCHA. Live: 12 consecutive
  failed logins → `401 ×12`, zero throttle responses, no `Retry-After`, latency *decreasing*
  (1021→354 ms, i.e. no backoff). Corroborated by two independent CI tests
  (`testTC_SEC_02_LoginRateLimitAfterFailures` 10×401; `testBUG008_NoRateLimiting` 6 attempts,
  0 signals). Already ticketed **ZP-2025**.
- **A11Y-1 (Medium)** — Quality Gates' 52 failures are **axe-core** WCAG violations, not "Test/Data".
  See correction below. `button-name` (critical) on /users 70 nodes, /sessions 25, /site-walks 24;
  `color-contrast` (serious) on /issues 19, /tasks 26; `aria-required-children` on MuiDataGrid.

## CORRECTION to the register
I previously wrote Quality Gates' 52 failures were "all triaged Test/Data". That label was the
triager's **fallback at 40% confidence** with root cause "Unclassified failure — needs manual
investigation" — not a diagnosis. Corrected in place.

## Refuted live (would have been false positives)
| Claim | Live result |
|---|---|
| Quotes search is case-sensitive ('account'→10, 'ACCOUNT'→1) | all four casings identical — no repro |
| /users renders blank at 390px | true CDP mobile emulation: 331 visible nodes, 1097 chars — no repro |
| Asset detail omits the asset name (2 tests) | renders "11N-H1-1" + all tabs — no repro |
| **OPEN-1** Circuit Breaker subtype "found 0" | **CLOSED** — 11 options live (Low-Voltage Molded Case, Medium-Voltage Vacuum, Recloser…) |
| Arc Flash `AFC_03` missing Conductor Material | **refuted by same-run positive control** — `AFC_04` passed using that exact field |

## Arc Flash cluster — resolved (was my "unreviewed" gap)
All 6 failures share one test-side root: **fixture-site selection failed**
(`'Android Qa Site1' selected=false` logged non-fatally in 3 shards; `'abhiiyant 17 june site'`
hard-failed Eng-E2E `classSetup` → 8 tests + 2 configs skipped = the "Skipped: 10").
AF_09/AF_11 then asserted values against the fallback site's degenerate dataset (all-Unknown
0/1959; percentages pinned at [0,0,100,100] which stricter requirements cannot move).
Auto-triage's 3 REAL_BUG(85%) calls all rested on "assertion + console errors" — unreliable given
the known ambient-502 console noise on /arc-flash.

## Still open (OPEN-2) — need a product answer, not more testing
- Arc Flash "Select role" control absent across two shards — but V1.36 deliberately removed the
  global role switcher, so plausibly intentional.
- Service catalog drift: `dga-fluid-sample-analysis`, `de-energized-testing` pinned but missing.
- WO list "Show planned" toggle (ZP-3027) — likely moved in the FW-1 redesign.
- Fixture sites may be deleted — **if the site picker itself is broken for users, that is a product
  bug**; worth five minutes.

## Register
New URL (previous one was deleted): https://claude.ai/code/artifact/8ca35956-54c9-4c95-92f9-108c3705a2cd
Totals: **5 High · 10 Medium · 11 Low/deferred · 11 dropped**. PDF at
`docs/bug-reports/2026-08-26-v136-issue-register.pdf`.

## Lesson
Auto-triage labels are hints, not verdicts — a "Bug" label backed by console errors is worthless on a
page with known ambient 502s, and a "Test/Data" label can be a low-confidence fallback hiding 48
axe-core findings. Classify from the assertion text, then verify live. The single most valuable move
was the **same-run positive control**: AFC_04 passing on the exact field AFC_03 called missing
refuted it outright, with no live testing needed.
