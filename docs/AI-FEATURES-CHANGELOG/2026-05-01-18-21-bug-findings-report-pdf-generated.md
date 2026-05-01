# Bug Findings Report PDF — 17 Confirmed Bugs with Screenshots & Repro Steps

**Date / Time:** 2026-05-01, 18:21 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Output files:**
- `docs/bug-reports/Bug_Findings_Report_2026_05_01.pdf` (2.2 MB, 19 pages)
- `docs/bug-reports/Bug_Findings_Report_2026_05_01.html` (1.0 MB, self-contained)

## What you asked for

> "any finding or bugs in full automation run if yes then create a pdf which screenshot and proper step"

Built a comprehensive 19-page PDF report cataloguing all **17 confirmed
real product bugs** surfaced by automation, with embedded screenshots,
detailed reproduction steps, and severity classification.

## The 8-part plan I followed

| Part | Status | What |
|---|---|---|
| 1-4 | ✓ | Bug inventory (17 confirmed bugs) + structured HTML with embedded screenshots as data URIs |
| 5 | ✓ | PDF conversion via Chrome headless `--print-to-pdf` |
| 6 | ✓ | Verified PDF renders: cover page + 17 detail pages with screenshots + index |
| 7 | ✓ | This changelog |
| 8 | ✓ | Commit + push to `main` (NEVER `developer`) |

## What's in the PDF

### Page 1 — Cover + Summary

- Title: "eGalvanic Web — Bug Findings Report"
- Subtitle: "Automated Test Suite Run · 1 May 2026 · 17 confirmed product issues"
- Stat cards: Total 17 / High 7 / Medium 10
- Full index table linking to each bug

### Pages 2-19 — One bug per page

Each bug card has:
- **Bug ID** (BUG-01 through BUG-17)
- **Severity badge** (HIGH red / MEDIUM yellow)
- **Title** — concise one-liner
- **Surface** — where the bug is reproducible
- **Caught by** — the test class+method that detects it
- **Description** — full context
- **Steps to reproduce** — numbered, manually verifiable
- **Expected behavior** — green-bordered block
- **Actual behavior** — red-bordered block
- **Impact** — yellow-bordered block (business/user-visible consequence)
- **Screenshot** — embedded as data URI (where available)

## The 17 bugs by severity

### HIGH severity (7)

| # | Bug | Where |
|---|---|---|
| BUG-01 | Kebab menu doesn't close on ESC, outside-click, or toggle | `/assets/<uuid>` |
| BUG-02 | `Uncaught TypeError: r?.hasAttribute is not a function` (5x/load) | `/assets` console |
| BUG-03 | Dashboard 1953 vs grid 8429 (asset count mismatch) | Dashboard ↔ /assets |
| BUG-04 | Dashboard 147 vs Tasks 0 | Dashboard ↔ /tasks |
| BUG-05 | Dashboard 266 vs Issues 0 | Dashboard ↔ /issues |
| BUG-06 | Asset grid empty after refresh | `/assets` F5 |
| BUG-07 | Beamer URL leaks user PII (role/company/name) | Beamer widget |

### MEDIUM severity (10)

| # | Bug | Where |
|---|---|---|
| BUG-08 | `/api/auth/v2/me` → 401 every page load | Auth bootstrap |
| BUG-09 | `/api/auth/v2/refresh` → 400 every page load | Auth refresh |
| BUG-10 | PLuG init "Error initializing with authentication" 3x | PLuG widget |
| BUG-11 | `[SLD] No sldId` warning logged 9x per page | useEffect re-render |
| BUG-12 | `/api/sld/<uuid>` 7.6s response time | SLD fetch |
| BUG-13 | `/api/node_classes/user/<uuid>` 6.5s | Class taxonomy |
| BUG-14 | `/api/lookup/nodes/<uuid>` 7.1s | Node lookup |
| BUG-15 | Roles API called 2x per page (duplicate fetch) | `/connections` |
| BUG-16 | Sign-In button enabled with empty form | `/login` |
| BUG-17 | aria-hidden on element with retained focus | Accessibility |

## How the PDF was built (technical detail for learning)

1. **Python script** (`/tmp/build_bug_report.py`) reads each bug from a structured catalog.
2. For each bug, the script:
   - Loads its screenshot from `test-output/screenshots/`
   - Base64-encodes the PNG bytes
   - Embeds as `data:image/png;base64,...` in the HTML (no external file dependencies — fully self-contained)
3. Renders all bugs into a single HTML file with print-friendly CSS:
   - `@page { size: A4; margin: 16mm 14mm; }` — A4 page setup
   - `page-break-inside: avoid` on each bug section — keeps each bug on one page when possible
   - Color-coded severity badges
   - Green/red/yellow blocks for expected/actual/impact
4. **Chrome headless** converts HTML → PDF:
   ```
   "Google Chrome" --headless --disable-gpu --no-sandbox \
     --print-to-pdf="...pdf" \
     --print-to-pdf-no-header \
     --virtual-time-budget=10000 \
     "file://...html"
   ```
5. Result: 2.2 MB PDF, 19 pages, fully self-contained.

## Why this approach (vs alternatives)

| Option | Pro | Con | Chose? |
|---|---|---|---|
| Chrome headless `--print-to-pdf` | Already installed; renders modern CSS perfectly; fast | Extra command-line invocation | ✓ Yes |
| `wkhtmltopdf` | Portable, no Chrome needed | Not installed; older WebKit; CSS quirks | No |
| Python `weasyprint` | Pure Python | Not installed; install requires libs | No |
| Python `reportlab` | Programmatic; deterministic | More code; harder to style; no HTML reuse | No |
| Pandoc | Markdown → PDF | LaTeX dependency for high quality | No |

Chrome headless was the right pick — it was already installed and renders
the same CSS the user will see if they open the HTML in a browser.

## Files changed

| File | Type | Size |
|---|---|---|
| `docs/bug-reports/Bug_Findings_Report_2026_05_01.pdf` | NEW — final PDF deliverable | 2.2 MB |
| `docs/bug-reports/Bug_Findings_Report_2026_05_01.html` | NEW — self-contained HTML source | 1.0 MB |
| `docs/AI-FEATURES-CHANGELOG/2026-05-01-18-21-...md` | NEW — this changelog | — |

The Python build script (`/tmp/build_bug_report.py`) is intentionally NOT
committed — it's a one-shot generator. If you need to regenerate (after
new bugs are added), I can recreate it.

## What you can do now

### Send to client / file ZP tickets

The PDF is ready to attach to email or upload to JIRA. Each bug has:
- A unique BUG-NN ID for tracking
- Reproduction steps a manual tester or developer can follow
- Screenshot showing the actual buggy state
- Severity for prioritization

### Recommended ZP tickets to file (in priority order)

**Block-on-ship priority (HIGH):**
1. BUG-01 (kebab focus trap) — accessibility violation
2. BUG-02 (TypeError 5x/load) — JS runtime error indicating broken DOM-traversal
3. BUG-03/04/05 (dashboard count mismatches) — data integrity issues, customer-visible
4. BUG-06 (asset grid empty after refresh) — F5 broken
5. BUG-07 (Beamer PII leak) — privacy/GDPR concern

**Should-fix-this-sprint (MEDIUM):**
6. BUG-12/13/14 (3 perf endpoints over 5s) — UX degradation
7. BUG-08/09 (auth bootstrap 401/400) — console pollution + retry waste
8. BUG-15 (Roles 2x duplicate) — wasted backend cycles
9. BUG-16 (Sign-In disabled state) — UX + backend load
10. BUG-17 (aria-hidden focus) — likely linked to BUG-01

**Lower priority:**
11. BUG-10 (PLuG init) — third-party widget; depends on DevRev SDK
12. BUG-11 (SLD log noise) — easy fix (remove log + tighten useEffect)

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` per memory rule.

---

_Per memory rule: this changelog is for learning + manager review.
The PDF is the deliverable; this doc explains what's in it, how it
was generated, and which bugs to file as ZP tickets first._
