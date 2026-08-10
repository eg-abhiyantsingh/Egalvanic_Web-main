# [Report Builder] Live preview times out on session (Work Order) report configs

**Env:** QA V1.36 · https://acme.qa.egalvanic.ai · **Found:** 2026-08-10
**Ticket:** Report Builder & AI Config Editor · **Severity:** High · **Priority:** High
**Screenshot:** `docs/bug-evidence/report-builder/EVIDENCE-report-builder-preview-hangs.png`

## Summary

The split-pane **live preview** — the headline feature of this ticket — fails for `session`
(Work Order) report configs. `POST /api/reporting/configs/{id}/preview-html` runs past the
60-second gateway limit and returns **504**; in the editor UI the preview pane simply **never
settles**, with no error, no timeout message and no spinner text. `quote_emp` configs preview
normally, so this is specific to session reports.

## Evidence — `POST /reporting/configs/{id}/preview-html`, acting as internal Admin

| Config | Type | `template_format` | Result | Elapsed |
|---|---|---|---|---|
| Eaton EMP Quote | quote_emp | — | **200** (5068 b HTML) | fast |
| EMP Quote (global) | quote_emp | — | **200** (1545 b HTML) | fast |
| Work Order Issue Report | session | html | 200 (487 b) | **30.5 s** |
| **criticoregroup** | session | html | **504 Gateway Timeout** | **60.0 s** |
| **abhiyant page** | session | html | **504 Gateway Timeout** | **60.0 s** |
| **IR Thermography Report (Frontloaded)** (global) | session | — | **504** | 60.4 s |
| **IR Thermography Report (Nested)** (global) | session | — | **504** | 60.5 s |

**4 of 5** session configs time out, all at exactly the 60 s gateway ceiling. The one that
succeeded still took **30.5 seconds** to render 487 bytes — not "live" by any reading.

## User-facing behaviour (the part that matters)

Opening **criticoregroup** — badged **"Ready to Use"**, Session + HTML, all pages green — at
`/reporting/config/d8e98d2c-6897-45c3-87e2-43dbf1cb8cbc` as internal Admin:

- The preview pane renders an empty page frame reading only *"New Template / Cover Page"*.
- After **84 seconds** it had **still not settled**: no rendered report, no error, no
  "Preview unavailable", no loading text.
- A silent indefinite hang is worse than a visible failure — the user cannot tell whether the
  config is broken, the data is missing, or the page is still working.

## Steps to reproduce

1. Log in and activate the internal **Admin** role.
2. Open `/reporting/config/d8e98d2c-6897-45c3-87e2-43dbf1cb8cbc` (`criticoregroup`).
3. Watch the left preview pane for 90 seconds.
4. **Actual:** placeholder frame only; never resolves; no error shown.
   **Expected:** the report renders, or a clear failure message appears well inside the 60 s
   gateway limit.

## Notes / scope

- Global IR Thermography configs whose pages use `.docx` templates degrade *gracefully* in the
  UI ("Template 'default_foreword.docx' is not HTML; preview unavailable"), so the hang is
  specific to **HTML** session configs where the render is actually attempted.
- A near-empty stub config returns a fast, honest **500** *"Data preparation failed: … No data
  available to generate this report."* — that path is fine, though 500 is arguably the wrong
  status for "your config has no data" (it will pollute error monitoring).
- Relevant to QA item 6 ("split-pane preview draggable and updates live") and item 11
  ("page accounting and self-check surfacing report honestly rather than optimistically").
