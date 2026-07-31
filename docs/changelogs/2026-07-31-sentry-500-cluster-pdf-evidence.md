# 2026-07-31 — Sentry 500-Cluster: full-sheet PDF with screenshot proof

**Prompt:** "share me full sheet pdf with proof of screenshot" (follow-up to the same-day
Sentry 500-cluster re-verification, commit `3c033b3`).

## Deliverable

**`docs/bug-evidence/sentry-500-cluster-jul22-23/Sentry-500-Cluster-QA-Reverification-2026-07-31.pdf`**
— 4-page A4 sheet: metadata + verdict box, all-6-endpoints summary table, one screenshot card
per probe (8 cards), copy-paste curl repros for the two live bugs.

## How the proof was captured (not mocked)

- All 8 probes were **re-executed live at capture time** (~17:36 IST) via same-origin `fetch`
  from `https://acme.qa.egalvanic.ai` in a Playwright-driven browser; each card shows the real
  HTTP status, latency, byte size, capture timestamp, and raw response body.
- Verdicts unchanged from the morning curl run — bulk-create 500 reproduced a **4th time**
  (fresh trace_id `cc97cac484224141ab77ee4ea0f7de84`), SKM 500s reproduced for both triggers,
  SKM control (with `encoding="UTF-8"`) passed again.
- Screenshots: Playwright element screenshots of each evidence card + full-page sheet
  (`sentry500-full-sheet.png`, `sentry500-summary.png`, `card-1…8.png`).
- PDF: `report.html` printed via headless Chrome (`--print-to-pdf`, no header/footer).

## Files added

- `Sentry-500-Cluster-QA-Reverification-2026-07-31.pdf` (the shareable sheet)
- `report.html` (source of the PDF, images referenced locally)
- `sentry500-full-sheet.png`, `sentry500-summary.png`, `card-1…card-8.png` (10 screenshots)
