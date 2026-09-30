# Web v2.2.2 (Jira version 14272): release check on stage

**Date:** 2026-09-30, 14:05–14:25 IST
**Prompt:** "https://egalvanic.atlassian.net/projects/ZP/versions/14272/tab/release-report-all-issues test all this ticket"
**Release page:** https://claude.ai/artifact/W2LRm5JVfjhxMSM6UZiGqB · ZP-4464 test report v7: https://claude.ai/artifact/EhE9HsXzVG2W3QpTDbSNbx

## What changed on stage before this check
- New frontend build `index-BN2GRUnL.js` (page chunk `Subscription-CK8W-fzs.js`) and the backend branch `bugfix/ZP-4486-subscription-qa-fixes` deployed ~14:00 IST. The API now carries `fees.list_cents`, `fees.ended`, `module_count`, `site_count`, `renewal_cents`, `subscription.ended_at` / `next_item_end_at` / `term_end_at`, `renewal_rate_card`, `modules[].counted`.
- Avani posted the acceptance spec on ZP-4464 (30 Sep 02:45–02:52 CDT): states A1–A8, B1–B6, C1–C5, D1–D2, E1, F status codes, G banner, H activity, each with a SQL setup block. Saved locally at `/tmp/zp4464stage/avani-testcases.md` (not committed: it holds SQL against stage and prices).

## Verdicts (6 tickets)
| Ticket | Jira | QA on stage | Action |
|---|---|---|---|
| ZP-4483 marker | Resolved | **PASS** on real data: white marker, 8.19:1 (was 2.42:1), label "Today · 80 days of full access left" | Transitioned to READY TO RELEASE (id 7) |
| ZP-4479 totals | Resolved | Foundation half PASS ("List price $65,800 · Included in your Foundation order"); site half needs C1–C4 | Blocked on data |
| ZP-4486 expired | Resolved | Fix deployed; needs A5/A7/C5/D2 (+E1) | Blocked on data |
| ZP-4485 ✕ | Ready for QA | Still shows with `dismissible:false`; developer: "expected, discussed with Dharmesh" | Owner decision; not moved |
| ZP-4464 story | Ready for QA | A1 PASS; 21 states waiting for SQL blocks | Blocked on data |
| ZP-4463 Extract from Photos | To Do | Not in the build | — |

Also seen in the new build: "10 of 3 sites" now has a red bar and an explanation (first-day open item 2 done). "Grac" still clipped.

## Tooling
- `verify-state.sh <label>` (in /tmp): after the developer runs one SQL block, runs the Admin + Account Manager seat checks and keeps the evidence in `test-output/zp4464-release/<label>/`.
- `TIMELINE_JS` found the new white marker without changes (it looks for the thin childless element; the label is a sibling above the bar).

## Evidence
`test-output/zp4464-build4-A1` (new build, state A1, Admin + AM), `zp4464-full-1349` (old build, same data, for the before/after).
