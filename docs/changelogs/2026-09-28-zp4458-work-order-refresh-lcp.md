# ZP-4458 filed: refreshing a work order takes ~5 s (LCP ~4.1 s) — missed in the ZP-4444 check

**Date:** 2026-09-28 · **Env:** acme.stage.egalvanic.ai, build index-COmQBZM2.js · **Ticket:** https://egalvanic.atlassian.net/browse/ZP-4458

The owner showed Chrome DevTools LCP 4.26 s ("poor") after refreshing work order WOSERVICEQ on staging. My ZP-4444
verdict had timed the grid from the work order's own first request (0.5–2.4 s) and left out app start-up.

## Measured (new `ZP4444RefreshLcpTest`, 9 refreshes each, one browser, warm + cold cache)
| | Work order | Dashboard (control) |
|---|---|---|
| HTML arrives | 1.32 s | 1.33 s |
| App menu on screen | 3.74 s | 3.69 s |
| LCP | 4.13 s | 4.01 s |
| Grid on screen | 5.01 s | — |

## Why (from Resource Timing + response headers)
1. Deep-link HTML (`/sessions/<id>`, `/dashboard`) comes through CloudFront's error fallback: `x-cache: Error from
   cloudfront`, `no-cache`, 0.9–1.5 s every time; `/` and `/index.html` are 0.08 s cache hits. Same on QA.
2. `auth/v2/me` starts ~10 ms after `alliance-config` answers in all 18 refreshes → sequential.
3. Route code (SessionDetail + ~50 chunks incl. formio.full.min) loads only after 7 start-up calls return.
4. LCP element = the "Loading work order details…" / "Checking agreements" placeholder, so LCP = start-up time.

## Jira
ZP-4458 created (Bug, Medium, sprint Z-26-09-S3 id 1223, fixVersion Web v2.3, Backlog → To Do) with 2 screenshots
attached (ids 37651, 37652). ZP-4444 stays In QA; its Artifact now has a "After a browser refresh" section.

## Files
- `src/test/java/com/egalvanic/qa/testcase/ZP4444RefreshLcpTest.java` (new)
- `docs/bug-evidence/2026-09-28-wo-refresh-lcp/` (screenshots, raw per-refresh JSON with third-party query strings stripped, medians)
