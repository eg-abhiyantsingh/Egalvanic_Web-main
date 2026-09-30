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

## 14:12 update: ZP-4486 verified on real data (dev.stage)
dev.stage's Foundation row EG-FND-003 was set to expired mid-term (Avani's A7). Old backend at 13:53: "Couldn't load", no banner.
New build at 14:12: Expired pill, "Ended Sep 30, 2026", red banner (also after refresh), Ops Core Included, advanced Locked,
list-price footer, no error — the fix works. Two wording defects on the ended page (reported on the ticket, status left as is):
"Full access left · Ended · Since Dec 30, 2026" (a future date) and "366 days left" on locked rows of an ended plan.
Release page v2. Evidence: `test-output/zp4464-dev-expired-1412`.

## 14:24–14:55: deep sanity on the newest build (index-BG9CyzoX.js; page/banner code identical to 14:05)
- Prompts: "v2.2.2 test in depth", "start testing in depth", "only … stage", "do deep sanity testing", two "create a bug" requests from the ended plan.
- Run: 8 logins (state A1), then Avani set acme expired at 14:40 (A7) → later checks on the ended plan. Real-data checks extended: F1 on real data (✕ → 6 in-app pages hidden → refresh back), E3 probes (unknown / missing X-Subdomain → 401, not the documented 404/200-null; never 400), acme token vs dev tenant → 401, whole-page axe (4 serious: unnamed progress bars, aria-label on a div, "Expired" chip 4.13:1, main scroll area not focusable), page timing 410 ms / DCL 1.3 s, flag off, Chicago view of dev.stage's real row (Sep 30–Sep 30 vs Oct 1–Oct 1: N1 on real data).
- New bugs (owner's request): **ZP-4491** banner should say "ended 10 days ago"; **ZP-4492** term bar still "Today · 80 days of full access left" on an ended plan. Both Avani, Medium, To Do, sprint 1223, Web v2.2.2, screenshots.
- Test code: `realDataChecks` now does F1 on real data, the E3/dev probes, and a whole-page axe + timing step. Runner traps: `eval` re-parses passwords with glob characters (Client Portal seat) → `run-one.sh` (no eval); macOS has no `setsid` → watcher started with `nohup … &!`.
- `watch-states.sh` (in /tmp) re-runs the Admin check every 4 min for an hour and keeps evidence per new state under `test-output/zp4464-release/`.
- Release page v3. Evidence: `zp4464-acme-expired-1444`, `zp4464-deep` (real checks + dev-chicago), `zp4464-stage` (8 seats).
