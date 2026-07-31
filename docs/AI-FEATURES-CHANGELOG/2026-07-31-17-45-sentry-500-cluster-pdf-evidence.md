# Sentry 500-Cluster — Full-Sheet PDF with Screenshot Proof — 2026-07-31, 17:45 IST

## What was asked
"Share me full sheet pdf with proof of screenshot" — a shareable PDF version of the morning's
Sentry 500-cluster re-verification, with screenshots as evidence (same format as the earlier
WO-Details-pagination evidence pack).

## What was done
1. **Re-ran all 8 probes live in a real browser** rather than pasting the morning's terminal
   output: navigated Playwright to `https://acme.qa.egalvanic.ai`, logged in via
   `POST /api/auth/login` from the page itself, and executed every probe with same-origin
   `fetch()` — including the multipart XML uploads, built in-browser with `FormData` + `Blob`.
2. Rendered the results as styled evidence cards **inside the live page DOM** (status badge,
   request payload, raw response body, latency, byte size, ISO capture timestamp per card),
   then took Playwright **element screenshots** of each card plus the full sheet.
3. Assembled `report.html` (A4-print CSS, `break-inside: avoid` per card) embedding the 10
   PNGs, and printed it to PDF with headless Chrome `--print-to-pdf --no-pdf-header-footer`.
4. Committed the PDF + screenshots + HTML source into
   `docs/bug-evidence/sentry-500-cluster-jul22-23/` and pushed to `main`.

## Result
4-page PDF: `Sentry-500-Cluster-QA-Reverification-2026-07-31.pdf`
- Page 1: metadata, red verdict box, summary table screenshot (all 6 endpoints), bulk-create
  500 card — **4th deterministic repro**, fresh trace_id `cc97cac484224141ab77ee4ea0f7de84`.
- Page 2: both SKM 500 triggers (malformed XML; spec-legal declaration without `encoding`)
  plus the passing `encoding="UTF-8"` control that isolates trigger B.
- Page 3: lookup/procedures 200 (unbounded 2 MB noted) and quote_preview_html 200 with
  per-page `render_error` (the "appears fixed" evidence).
- Page 4: rules-status SPA-shell (route absent on QA), onboarding clean 400, curl repros.

## Depth explanation (for learning / manager walkthrough)

**Why re-execute instead of screenshotting old output?** A screenshot is only proof if it
shows the system answering *at capture time*. Re-running the probes in-browser puts the live
timestamp, status, and body in the same pixel frame — nothing is transplanted from an earlier
run. It also incidentally strengthened the finding: bulk-create has now 500'd on 4/4 attempts
across two transports (curl and browser fetch), with 4 distinct trace_ids to hand the backend
team for Sentry correlation.

**Why same-origin fetch from the app's page?** The QA host's CSP restricts `connect-src` to
'self'; running `fetch()` from the app's own origin means no CORS/CSP interference and proves
the responses come from exactly the host the frontend uses — the same path real users hit.
The multipart uploads were rebuilt with browser-native `FormData`/`Blob`, mirroring how the
SPA itself would upload an SKM file.

**Why element screenshots instead of one full-page capture?** Each card lands in the PDF at
consistent width and readable font size, and page-break CSS (`break-inside: avoid`) keeps a
card from being sliced across pages. The full-page PNG is still included in the directory for
anyone who wants the single-image view.

**Pipeline reusability:** page → evidence-card DOM → element screenshots → `report.html` →
headless-Chrome PDF is the same pattern as the WO-Details pagination pack; any future API
verification can reuse it by swapping the probe list.
