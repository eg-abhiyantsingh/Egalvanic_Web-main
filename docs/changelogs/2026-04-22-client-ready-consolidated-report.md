# 2026-04-22 — Client-ready consolidated report: generator + CI wiring

## Prompt
> @bug pdf/parallel2suite.html — see this file in CI/CD. pass/fail is different. I need to send this report to client so optimize it, make it better so that I can send to client.

## Model in use
Claude Opus 4.7 (1M context), xhigh effort, always-thinking on.

## Branch safety
Work on `main` of the **QA automation framework repo**. Production website repo untouched.

## The problem

The existing `Consolidated_Client_Report_Suite2.html` (aka `parallel2suite.html`) is technically correct but client-hostile:
- Test names show developer-speak: `"BUG-004: Invalid asset URL leaks 'Failed to fetch enriched node details: 400'"`
- Known bug-verification failures are mixed in with genuine functional failures — same red "FAIL" badge on both, so client can't tell them apart
- No executive summary in plain language — just raw numbers
- Module names like `"AI Exploratory"`, `"Admin Forms"` aren't client product-area terms
- Pass/fail numbers ARE accurate (85 passed / 24 failed / 109 total — matches CI run 24772098520), but the presentation makes the product look worse than it is

## What I built

### 1. Client-ready HTML transformer — [`.github/scripts/client-report.py`](.github/scripts/client-report.py)

Takes the technical consolidated HTML as input, emits a client-polished HTML. Key transformations:

**(a) Executive summary at the top**
- Pass-rate donut ring (SVG)
- Plain-English paragraph: "Across 109 automated verifications... 85 passed, 24 did not pass. Of the 24 failures, 5 are known issues currently tracked by engineering, and 19 are functional verifications that need follow-up. Overall pass rate: 78.0%."
- 4 key-stat tiles

**(b) Feature Area Coverage table**
- Product-friendly area names (`Authentication` → `User Login & Access`, `Admin Forms` → `Admin Configuration Forms`, `AI Visual Regression` → `Visual Regression`, etc.)
- Per-area pass rate + status chip
- Client sees the whole platform's coverage at a glance

**(c) Functional Issues — Needs Attention**
- Only functional failures here (not known-bug verifications)
- Grouped by area
- Clear message: "genuine regressions or environment issues that require engineering review"

**(d) Known Issues — Status Check (separated section)**
- The 8 Curated Bug Verification tests get their OWN section
- Each bug mapped to a client-friendly label + description (e.g., `BUG-004` → `"Error Message Quality: Invalid asset URLs should show a user-friendly error, not internal details"`)
- Status shown as **Resolved** (green) or **Still Active** (red)
- Clear framing: *"Still Active here is expected until the underlying issue is fixed — it is NOT a new regression."*
- This reframes the 5 "failures" from "product is broken" to "here's the status of issues we're already tracking"

**(e) Verifications Passed summary**
- One line summary + pointer to the coverage table above

**(f) Print-ready CSS**
- `@media print` rules
- Page-break-inside avoid on sections
- Single-column layout with generous whitespace
- Client can open in browser → Print → Save as PDF for email delivery

### 2. Generated preview: [`bug pdf/parallel2suite-client.html`](bug%20pdf/parallel2suite-client.html) (19.9 KB)

Built from the existing 109-test CI run (24772098520) so you can preview the output before the next CI run. Open it in any browser.

### 3. Wired into CI — [.github/workflows/parallel-suite-2.yml](.github/workflows/parallel-suite-2.yml)

Added a new step in the summary job AFTER `consolidated-report.py` runs:
```yaml
- name: Generate client-ready report
  if: always()
  run: |
    python3 .github/scripts/client-report.py \
      reports/client-report/Consolidated_Client_Report_Suite2.html \
      reports/client-report/Client_Report_Polished_Suite2.html
```

The `consolidated-client-report-suite2` artifact now contains BOTH:
- `Consolidated_Client_Report_Suite2.html` — technical, for engineering
- `Client_Report_Polished_Suite2.html` — client-polished, **send this one to the client**

The step summary now flags the polished report explicitly as "SEND TO CLIENT".

## The key design insight — separating known issues from functional failures

The original report treated all failures identically — "FAIL" badge, red chip. That's technically accurate but semantically wrong:

| Test Type | What FAIL means | Client's concern |
|---|---|---|
| Functional smoke (PM login, facility selector) | Regression — a feature that worked is now broken | HIGH — new bug introduced |
| Curated bug verification (BUG-001..008) | Known issue still present — not yet fixed | LOW — already tracked |

The polished report puts these in SEPARATE sections with different framing, so:
- A client reading "5 Known Issues Still Active" understands: these are already on our radar, engineering is working on them.
- A client reading "19 Functional Issues Needs Attention" understands: these are new — look at these carefully.

Before this split, the client would see "24 failed out of 109 — looks like 22% of the product is broken" and panic. After the split: "5 known issues tracking, 19 functional items to review, 78% pass rate overall" — accurate AND fair to the product state.

## Verification done

- `python3 -c "import ast; ast.parse(open('.github/scripts/client-report.py').read())"` → python OK
- `python3 -c "import yaml; yaml.safe_load(...)"` on parallel-suite-2.yml → YAML OK
- Regenerated `bug pdf/parallel2suite-client.html` from the existing 109-test data — renders correctly, matches the CI numbers exactly
- Screenshot captured via Playwright CDP — verified executive summary, coverage table, failures section, known-issues section all render clean

## How to use

### Today (with existing report)
Already done — [`bug pdf/parallel2suite-client.html`](bug%20pdf/parallel2suite-client.html) is ready to send to the client. Open it in a browser, either attach as HTML or Print → Save as PDF for email.

### Future CI runs
Automatic — every time Parallel Suite 2 runs, the `consolidated-client-report-suite2` artifact will include both the technical AND the client-polished HTML. Download the artifact, extract `Client_Report_Polished_Suite2.html`, and send to client.

## In-depth explanation (for learning + manager reporting)

### Why a post-processing step instead of rewriting consolidated-report.py
The consolidated-report.py emits the canonical technical record. Engineering uses it to debug failures, see test durations, drill into error messages. Replacing it with a client-polished version would destroy that debugging value.

A post-processor that transforms technical → client runs in ~50ms, requires no changes to the upstream pipeline, and lets both audiences get what they need. Single-source-of-truth data (consolidated HTML), two presentations (technical + polished).

### Why regex parsing of HTML instead of a proper parser
The source HTML is generated by consolidated-report.py with a predictable structure (module divs, test-row divs, fixed class names). Regex is fragile for arbitrary HTML, but for HTML we control the generator for, it's 20 lines of code vs 200 lines of BeautifulSoup setup. If consolidated-report.py's output structure ever changes, both files will need updates — that's a fine tradeoff for the simplicity.

### Why CLIENT_AREA_NAMES and BUG_DESCRIPTIONS are hardcoded dicts
Internationalization + UI copy usually lives in a separate config. For now, hardcoded is faster and visible. If the client wants Hindi/French versions or different phrasing, lifting these dicts into a JSON config file is a trivial next step.

### Why Known Issues shows DETAILED descriptions but functional failures show RAW test names
Known issues are a curated list — we own the product description. Functional failures come from hundreds of tests with varying name quality. Normalizing ALL test names to client-friendly labels would require per-test metadata that doesn't exist yet. Instead:
- Known issues get polished labels (small set, high value)
- Functional failures show test name as-is + area grouping (tradeoff: some test names are developer-speak, but at least the area grouping gives context)

Future improvement: add a test-name-to-client-label mapping for the functional tests too, once the test catalog stabilizes.

### Print-ready CSS — why it matters for client delivery
The client is likely to save the HTML as PDF and email it. `@media print` with `break-inside: avoid` on sections ensures the PDF doesn't split a table across two pages. The max-width: 1000px means when printed on A4, everything fits without horizontal scrolling.

## Rollback
`git revert <this-commit>` removes the new script + restores the workflow to emit only the technical report. The client-polished HTML at `bug pdf/parallel2suite-client.html` would stay (it's a generated artifact, not tracked at the same path).
