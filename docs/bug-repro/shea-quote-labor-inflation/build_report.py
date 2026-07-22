#!/usr/bin/env python3
"""Assemble the Shea quote labor-hours findings into a single self-contained HTML
(base64-embedded screenshots), print-ready for Chrome --print-to-pdf.
Revised 2026-07-22 to the HONEST verdict after full verification."""
import base64, pathlib

HERE = pathlib.Path(__file__).parent

def img(stem, caption):
    data = base64.b64encode((HERE / f"{stem}.png").read_bytes()).decode()
    return f'''
    <figure>
      <img src="data:image/png;base64,{data}" alt="{caption}"/>
      <figcaption>{caption}</figcaption>
    </figure>'''

HTML = f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"/>
<title>Findings — Quote labor-hour calculation defects</title>
<style>
  @page {{ size: A4; margin: 14mm 12mm; }}
  * {{ box-sizing: border-box; }}
  body {{ font-family: -apple-system, "Segoe UI", Helvetica, Arial, sans-serif; color:#1a1a1a; font-size:11pt; line-height:1.5; }}
  h1 {{ font-size:19pt; margin:0 0 2mm; }}
  h2 {{ font-size:13pt; margin:7mm 0 2mm; border-bottom:2px solid #1565c0; padding-bottom:1mm; color:#1565c0; }}
  h3 {{ font-size:11.5pt; margin:4mm 0 1mm; }}
  .meta {{ color:#555; font-size:9.5pt; margin-bottom:4mm; }}
  .verdict {{ background:#e8f0fe; border:1px solid #1565c0; border-radius:6px; padding:4mm; margin:3mm 0; }}
  .verdict b {{ color:#0d47a1; }}
  .callout {{ background:#fff8e1; border-left:4px solid #f9a825; padding:2.5mm 3.5mm; margin:3mm 0; font-size:10pt; }}
  .good {{ background:#e8f5e9; border-left:4px solid #2e7d32; padding:2.5mm 3.5mm; margin:3mm 0; font-size:10pt; }}
  table {{ border-collapse:collapse; width:100%; margin:2mm 0; font-size:10pt; }}
  th,td {{ border:1px solid #ccc; padding:2mm 3mm; text-align:left; vertical-align:top; }}
  th {{ background:#f2f2f2; }}
  td.num {{ text-align:right; font-variant-numeric:tabular-nums; }}
  .bad {{ color:#b00020; font-weight:700; }}
  .ok  {{ color:#1b7a1b; font-weight:700; }}
  code, .mono {{ font-family:"SF Mono", Menlo, Consolas, monospace; font-size:9.5pt; background:#f4f4f4; padding:0 3px; border-radius:3px; }}
  figure {{ margin:3mm 0 5mm; page-break-inside:avoid; }}
  figure img {{ width:100%; border:1px solid #bbb; border-radius:4px; }}
  figcaption {{ font-size:9pt; color:#444; margin-top:1.5mm; }}
  ul,ol {{ margin:1.5mm 0 1.5mm 5mm; padding:0; }}
  li {{ margin:1mm 0; }}
  .url {{ word-break:break-all; }}
  .pagebreak {{ page-break-before:always; }}
  .foot {{ margin-top:8mm; padding-top:2mm; border-top:1px solid #ccc; color:#777; font-size:8.5pt; }}
</style></head>
<body>

<h1>Findings — Quote labor-hour calculation defects</h1>
<div class="meta">
  <b>Context:</b> follow-up to the Shea Electric (Andrew Borgardt, Desmond Vincent) report from the 2026-07-21 call —
  Cancer Center quote showing ~219 h vs an ~80 h estimate.<br/>
  <b>Investigated:</b> 2026-07-22 on the QA environment, read-only. <b>Shea's production data was never opened.</b><br/>
  <b>Quote inspected on QA:</b>
  <span class="url mono">https://acme.qa.egalvanic.ai/quotes/d7cc59cf-3f2f-4467-ae57-18bd0a70e814</span>
</div>

<div class="verdict">
  <b>Verdict (after full verification):</b> Shea's <b>inflated total was NOT reproduced on QA</b> — every QA quote
  with data has a correct customer-facing total. Verification <i>did</i> surface a <b>real, narrower defect</b>:
  a quote's internal labor-hour fields contradict each other (16&nbsp;h vs 8&nbsp;h, and one line whose breakdown
  computes to 98&nbsp;h), and one line's hours are arithmetically mis-derived. These do not corrupt the price on the
  quotes tested, but they are genuine calculation bugs and the most likely root of Shea's issue on their own data.
</div>

<div class="callout">
  <b>Correction from an earlier draft.</b> An earlier version claimed the inflation was "reproduced on QA" and that a
  5-minute task billed as 4&nbsp;hours was inflation. Both were wrong on re-check: the 4&nbsp;hours is a normal
  <b>4-hour trip minimum</b> (standard field-service billing), and no QA quote shows an inflated <i>total</i>.
  This report states only what the evidence supports.
</div>

<h2>What was checked, and what it showed</h2>
<p>I scanned <b>every quote in the QA company</b> (81 quotes; only 6 contain line-item data) and compared, for each,
the honest work (sum of per-line minutes) against the number the customer is actually billed. The billed total was
<b>never</b> higher than the honest work — including on the large multi-procedure quotes:</p>
<table>
  <tr><th>Quote</th><th class="num">Line items</th><th class="num">Honest hours (Σ minutes)</th><th class="num">Billed hours (pricing)</th><th>Total inflated?</th></tr>
  <tr><td>"The proposed Electrical…"</td><td class="num">845</td><td class="num">542.6</td><td class="num">542.48</td><td class="ok">No (matches)</td></tr>
  <tr><td>"The proposed Electrical…"</td><td class="num">854</td><td class="num">550.7</td><td class="num">550.56</td><td class="ok">No (matches)</td></tr>
  <tr><td>testtt (inspected below)</td><td class="num">17</td><td class="num">16.0</td><td class="num">8.25</td><td class="ok">No (billed is lower)</td></tr>
  <tr><td>test desc</td><td class="num">4</td><td class="num">8.8</td><td class="num">0 (unpriced)</td><td class="ok">No</td></tr>
</table>
<div class="good">
  <b>Conclusion on reproduction:</b> the QA environment does not contain a quote that reproduces Shea's inflated
  total. The two big multi-procedure quotes (the closest analogue to a large job like the Cancer Center) reconcile
  to within 0.1 h. So Shea's 219-vs-80 is almost certainly specific to <b>their</b> quote's data — most likely the
  duplicate line items and blank rows they were deleting on the call.
</div>

<h2>The real defect that WAS found (worth a ticket)</h2>
<p>On the inspected quote, the same labor is described by three numbers that cannot all be right:</p>
<table>
  <tr><th>Field / surface</th><th>Value</th><th>Source</th></tr>
  <tr><td>Line Items tab — 16 lines × 60&nbsp;min</td><td class="num">16.0 h</td><td class="mono">/api/quote/&#123;id&#125;/lines</td></tr>
  <tr><td>Labor line "Clean, Tighten, Torque" — <b>est_hours</b> field</td><td class="num">8.0 h</td><td class="mono">/api/workorder/&#123;id&#125;/labor-lines</td></tr>
  <tr><td>The <b>same</b> labor line's breakdown object: <span class="mono">count 14 × 420 min</span></td><td class="num bad">98.0 h</td><td>same line's <span class="mono">procedure_breakdown</span></td></tr>
</table>
<div class="callout">
  <b>Two concrete arithmetic errors:</b><br/>
  1. That breakdown reads <span class="mono">14 × 420 min = 5,880 min = 98 h</span>. There are only <b>16</b> assets at
  <b>60&nbsp;min</b> each (= 16 h). 420 = 14 × 30, so the per-asset minutes appear to have been <b>multiplied by the
  asset count and then stored as the per-asset value</b> — a count-times-count (squaring) error. This is the kind of
  mistake that produces 2–3× inflation on larger scopes.<br/>
  2. A second line's <span class="mono">est_hours</span> is impossible: its own breakdown (10&nbsp;min + 120&nbsp;min =
  130&nbsp;min = <b>2.17 h</b>) is stored as <b>0.17 h</b> — the leading digit is dropped.<br/>
  3. A phantom blank line exists — auto-generated asset <span class="mono">_UC3a_1779882698559</span>, 0 minutes — the
  "unspecified / blank row" Andrew described.
</div>

<div class="pagebreak"></div>
<h2>Screenshots (QA, read-only)</h2>

<h3>1 — Quote Overview: total is correct</h3>
<p>Headline reads <b>Total Hours 8.3</b>, <b>Line Items 17</b>, <b>Assets 16</b>. This total is consistent with the
$3,588 price — the customer-facing number is right on this quote.</p>
{img("step1-overview-8.3h-17lines", "Quote Overview — Total Hours 8.3, Line Items 17 (consistent with the $3,588 price).")}

<h3>2 — Labor tab: internal hour fields disagree</h3>
<p>The Planned-WO chip reads <b>9 h</b> while the header reads <b>8.3</b>, and a labor line shows Est. Hours 0.08.
These are the internal inconsistencies — not (on this quote) a wrong total.</p>
{img("step2-labor-tab-9h-chip-vs-8.3h-header", "Labor tab — WO chip '9 h' vs header '8.3'; internal hour fields do not agree.")}

<h3>3 — Line Items: the blank/auto-generated row</h3>
<p>First line's asset is <span class="mono">_UC3a_1779882698559</span> (auto-generated placeholder, not a real
asset). "Bulk Add Procedures" / "Add Line Item" are how duplicate lines get created — there is no duplicate check for labor.</p>
{img("step3-lineitems-autogen-node-bulk-add", "Line Items — auto-generated placeholder asset + bulk-add controls (duplication vectors).")}

<h2>Likely root cause &amp; related tickets</h2>
<ul>
  <li>A deprecated per-line "minutes" value is still used as a fallback for hours (<b>ZP-1092</b>); when it diverges
      from the newer hours field, different screens show different totals.</li>
  <li>No duplicate detection for labor lines; bulk-add and bulk recalc can add the same asset twice
      (<b>ZP-783</b>: "bulk recalc creates duplicate work orders").</li>
  <li>A per-asset time discount for multi-procedure assets was built but not shipped (<b>ZP-1407</b>).</li>
  <li>The <span class="mono">14 × 420</span> squaring pattern above is the concrete new evidence to attach.</li>
</ul>

<h2>Recommended next steps</h2>
<ol>
  <li>File a backend ticket for the labor-hour field inconsistency, with the <span class="mono">14 × 420 = 98 h</span>
      breakdown and the <span class="mono">2.17 → 0.17</span> line as evidence. This is app-engineering (pricing/quote
      service), not a QA-side fix.</li>
  <li>To diagnose Shea's specific 219-vs-80: inspect <b>their</b> Cancer Center quote data read-only, or replay the
      exact steps that generated their duplicate/blank rows. The QA environment cannot reproduce it as-is.</li>
</ol>

<div class="foot">
  Investigated read-only on QA, 2026-07-22. Quote inspected:
  https://acme.qa.egalvanic.ai/quotes/d7cc59cf-3f2f-4467-ae57-18bd0a70e814 &middot; Shea production data was not accessed.
  A re-runnable reproduction script (QuoteLaborInflationRepro) accompanies this report.
</div>

</body></html>"""

out = HERE / "Shea-Quote-Labor-Findings.html"
out.write_text(HTML, encoding="utf-8")
print("wrote", out)
