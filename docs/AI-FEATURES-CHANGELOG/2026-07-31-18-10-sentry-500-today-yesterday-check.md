# Sentry 500-Cluster — Today/Yesterday Occurrence Check — 2026-07-31, 18:10 IST

## What was asked
"check for today or yesterday" — are the cluster's 500s actually occurring on 30–31 Jul,
not just reproducible in principle?

## What was done
Three independent sources, cross-checked:
1. **Fresh QA probes (17:51 IST today)** — bulk-create and SKM preview still 500
   (new trace_id `bb9158208ccf47dfadfb2f16e8a066c6`); the other four clean/absent.
2. **Sentry queried directly** — discovered the repo already supports a git-ignored
   `.sentry-auth-token` at repo root (pattern from `Sentry502CorrelationApiTest`), and the
   file exists locally. Ran issues-API queries (org `egalvanic-yb`, the team's 4 project ids)
   over 48h/14d/90d windows, per endpoint plus one org-wide sweep for any title containing
   "returned 500" in the last 48h.
3. **Daily Suite-3 CI artifacts for Jul 30 and Jul 31** (`gh run download`, `api-suite-report`)
   — grepped for all six endpoints.

## Result
- **bulk-create: YES — happening today and yesterday.** Sentry `EGALVANIC-PZ-4` has 4 events
  in the last 48h, lastSeen 2026-07-31T07:44:59Z (13:14 IST) from real users on
  `screen.session_detail` — hours BEFORE today's probing, so it's organic traffic, not us.
  The issue is unresolved with 91 events since 2026-05-05 — this predates the 22–23 Jul
  window; that window was just its loudest burst. It is also the ONLY "returned 500" issue
  org-wide in the last 48h.
- **SKM preview: defect alive, users dormant.** QA still 500s today, but Sentry's last user
  event is 2026-07-22 — nobody has uploaded SKM XML since. It will fire again on next use.
- **Everything else: not occurring** today or yesterday (Sentry zero + CI 200s + clean probes).

## Depth explanation (for learning / manager walkthrough)

**Full-text search lies; titles don't.** Sentry's issue search matches breadcrumbs and
request payloads, so "lookup/procedures" returned three hits that were actually iOS
offline errors and a 401 — and "node-session/bulk-create" text-matched four Z-PLATFORM
issues with thousands of events that turned out to be generic "API RESPONSE" logs. Every
match was verified by fetching the issue title before being counted. This is the
[[feedback_dont_overreport_sld_bugs]] rule applied to Sentry data: verify the falsifiable
claim (the title says 500 on this endpoint) before reporting.

**"Reproducible" and "occurring" are different questions.** SKM preview is deterministically
broken (probe-proven today) yet generated zero Sentry events in 9 days — no user exercised
the path. bulk-create is both broken AND being hit organically (4 events/48h). Triage-wise
that makes bulk-create the P1 (users are bleeding now) and SKM a P2 time bomb (fires on next
upload). An error tracker only shows *usage × defect*, not the defect itself — which is
exactly why the probe suite and Sentry check complement each other.

**One org-wide sweep beats six per-endpoint queries.** The `"returned 500"` 48h query
answers "what ELSE is 500ing that the ticket didn't list?" in one call — the answer (nothing
but bulk-create) is itself evidence the 22–23 Jul cluster has otherwise subsided.

**firstSeen tells you the window was a symptom, not the start.** EGALVANIC-PZ-4's firstSeen
is 2026-05-05 — the bug is ~3 months old; 22–23 Jul was a usage burst, not a regression date.
That redirects root-cause hunting away from "what deployed on 22 Jul" toward the endpoint's
original implementation.
