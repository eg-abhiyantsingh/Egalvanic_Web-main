# 2026-07-31 — Sentry 500-cluster: is it coming today or yesterday?

**Prompt:** "check for today or yesterday" (follow-up on the Sentry 500-cluster verification).

## Answer

| Endpoint | Today (31 Jul) | Yesterday (30 Jul) | Source |
|---|---|---|---|
| `POST /mapping/node-session/bulk-create` | **YES — 500s live.** Sentry `EGALVANIC-PZ-4`: 4 events/48h, lastSeen **13:14 IST today** (real users, before our probing); + QA probe 500 at 17:51 IST (trace `bb915820…`) | **YES** (within the 4 events/48h) | Sentry API + live QA probe |
| `POST /skm/import-xml/preview` | Defect **still in code** (QA probe 500 at 17:51 IST) but **no user events** — Sentry lastSeen 2026-07-22 (dormant: nobody uploaded SKM XML since) | No user events | Sentry API + live QA probe |
| `GET /lookup/procedures` | Clean — 200 in CI (332 ms) + probes; no Sentry 500 issue in 48h/90d | Clean — 200 in CI (217 ms) | CI artifacts + Sentry |
| `POST /reporting/quote_preview_html` | Clean — zero Sentry events in 90d; render errors caught per page | Clean | Sentry + probes |
| `GET /bulk-edit/rules-status/{id}` | No Sentry 500s; route absent on QA build | Same | Sentry + probes |
| `POST /onboarding/jobs` | Clean — zero Sentry events | Clean | Sentry + probes |

**Headline:** org-wide, the ONLY "returned 500" Sentry issue that fired in the last 48h is
the bulk-create one — unresolved since 2026-05-05 (91 events / 90d).
Permalink: https://egalvanic-yb.sentry.io/issues/7023335482/

## Method

- Sentry queried directly via issues API (org `egalvanic-yb`, 4 project ids from
  `Sentry502CorrelationApiTest`), token from the git-ignored `.sentry-auth-token`;
  windows 48h / 14d / 90d; false-positive text matches (iOS offline noise, generic
  "callback") excluded by title inspection.
- Fresh QA probes of all 6 endpoints at 17:51 IST (same verdicts as morning).
- Suite-3 daily CI artifacts for Jul 30 + Jul 31 downloaded (`gh run download`) and grepped —
  corroborates lookup/procedures healthy both days; also re-confirms the unrelated known reds
  (`/tasks/{sld_id}` 500 3/3 both days, v2/issues/list array-filter 500).

## Files changed

- `docs/bug-evidence/sentry-500-cluster-jul22-23/EVIDENCE.md` — new "Sentry-side check"
  section with the 48h/90d findings and permalink.
