#!/usr/bin/env python3
"""Assemble the duplicate-API re-verification findings into a single self-contained
HTML (base64-embedded screenshots), print-ready for Chrome --print-to-pdf.
Re-verification of the 2026-07-17 duplicate-API audit after the dev team reported
the critical planned-workorder-line twin fixed. Live evidence captured 2026-07-23."""
import base64, pathlib

HERE = pathlib.Path(__file__).parent

def img(stem, caption):
    f = HERE / f"{stem}.png"
    if not f.exists():
        return f'<p style="color:#b00020">[missing screenshot: {stem}.png]</p>'
    data = base64.b64encode(f.read_bytes()).decode()
    return f'''
    <figure>
      <img src="data:image/png;base64,{data}" alt="{caption}"/>
      <figcaption>{caption}</figcaption>
    </figure>'''

HTML = f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"/>
<title>Re-verification — duplicate API endpoints (planned-workorder-line twin) NOT fixed</title>
<style>
  @page {{ size: A4; margin: 14mm 12mm; }}
  * {{ box-sizing: border-box; }}
  body {{ font-family: -apple-system, "Segoe UI", Helvetica, Arial, sans-serif; color:#1a1a1a; font-size:11pt; line-height:1.5; }}
  h1 {{ font-size:18pt; margin:0 0 2mm; }}
  h2 {{ font-size:13pt; margin:7mm 0 2mm; border-bottom:2px solid #b00020; padding-bottom:1mm; color:#b00020; }}
  h3 {{ font-size:11.5pt; margin:4mm 0 1mm; }}
  .meta {{ color:#555; font-size:9.5pt; margin-bottom:4mm; }}
  .verdict {{ background:#fdecea; border:1px solid #b00020; border-radius:6px; padding:4mm; margin:3mm 0; }}
  .verdict b {{ color:#8e0000; }}
  .callout {{ background:#fff8e1; border-left:4px solid #f9a825; padding:2.5mm 3.5mm; margin:3mm 0; font-size:10pt; }}
  .good {{ background:#e8f5e9; border-left:4px solid #2e7d32; padding:2.5mm 3.5mm; margin:3mm 0; font-size:10pt; }}
  table {{ border-collapse:collapse; width:100%; margin:2mm 0; font-size:10pt; }}
  th,td {{ border:1px solid #ccc; padding:2mm 3mm; text-align:left; vertical-align:top; }}
  th {{ background:#f2f2f2; }}
  .bad {{ color:#b00020; font-weight:700; }}
  .ok  {{ color:#1b7a1b; font-weight:700; }}
  code, .mono {{ font-family:"SF Mono", Menlo, Consolas, monospace; font-size:9pt; background:#f4f4f4; padding:0 3px; border-radius:3px; }}
  pre {{ font-family:"SF Mono", Menlo, Consolas, monospace; font-size:8.5pt; background:#f4f4f4; border:1px solid #ddd; border-radius:4px; padding:3mm; white-space:pre-wrap; word-break:break-all; }}
  figure {{ margin:3mm 0 5mm; page-break-inside:avoid; }}
  figure img {{ width:100%; border:1px solid #bbb; border-radius:4px; }}
  figcaption {{ font-size:9pt; color:#444; margin-top:1.5mm; }}
  ul,ol {{ margin:1.5mm 0 1.5mm 5mm; padding:0; }}
  li {{ margin:1mm 0; }}
  .pagebreak {{ page-break-before:always; }}
  .foot {{ margin-top:8mm; padding-top:2mm; border-top:1px solid #ccc; color:#777; font-size:8.5pt; }}
</style></head>
<body>

<h1>Re-verification: duplicate API endpoints — <span class="bad">NOT fixed</span></h1>
<div class="meta">QA environment <b>acme.qa.egalvanic.ai</b> · live probes 23&nbsp;July&nbsp;2026 (authenticated admin session) ·
original findings 17&nbsp;July&nbsp;2026 (Parallel Suite&nbsp;3 <span class="mono">DuplicateApiAuditTest</span> + Suite&nbsp;2 <span class="mono">ApiDuplicateCallTestNG</span>) ·
eGalvanic QA Automation</div>

<div class="verdict">
<b>Verdict: the CRITICAL dash/underscore twin is NOT fixed — the broken behaviour reproduces today, unchanged.</b><br/>
The development team reported this fixed. Re-probing live on 23&nbsp;July&nbsp;2026:
<ul>
  <li><span class="bad">Both spellings are still registered</span> in the live <span class="mono">/api/swagger.json</span>:
      <b>6 dash paths + 12 underscore paths</b> (17&nbsp;Jul: 6+13) — write methods (create/update/delete/hard-delete) exist on <i>both</i> sides.</li>
  <li><span class="bad">The underscore list read still ignores pagination and never answers</span>:
      <span class="mono">GET /api/planned_workorder_line/?page=1&amp;per_page=5</span> was aborted after <b>45&nbsp;s with no response</b>
      — the exact 17&nbsp;Jul signature ("TIMEOUT 36&nbsp;s+, unbounded read"). Probed in a simultaneous race where a control
      endpoint answered in <b>0.7&nbsp;s</b>, so this is the endpoint, not the backend.</li>
  <li><span class="bad">The one thing that changed made it worse</span>: the paginated dash list
      <span class="mono">/planned-workorder-lines/</span> <b>still answers live</b> (HTTP&nbsp;200 in <b>1.4&nbsp;s</b> in the same race —
      identical to 17&nbsp;Jul) but has been <b>removed from the spec</b>. The working route is now undocumented while the
      spec advertises only the broken one.</li>
  <li>Bounded reads on the same resource answer (<span class="mono">by-workorder/{{id}}</span> → HTTP&nbsp;200 in 2.6&nbsp;s in the race),
      proving the resource itself is reachable — the defect is specifically the unpaginated list implementation.</li>
</ul>
The runtime (frontend) duplicate — <span class="mono">GET /sites/{{id}}/status</span> fetched twice on one dashboard load
(once with <span class="mono">?narrative=false</span>, once bare) — <b>also still reproduces</b> (screenshot below).
</div>

__SCREENSHOTS__

<h2>Where CI now tracks this — Parallel Suite 3</h2>
<p><span class="mono">DuplicateApiAuditTest</span> (suite <span class="mono">suite-api-health.xml</span>, "API Duplicate-Endpoint Audit") gained two
<b>FIX-CHECK tripwire tests on 23&nbsp;Jul&nbsp;2026</b>. They hard-fail every run until the fix contract genuinely holds — so the suite
itself now answers "is it fixed yet?":</p>
<table>
<tr><th>Test</th><th>Fix contract it enforces</th><th>Status today</th></tr>
<tr><td class="mono">testFixCheckUnderscoreListPaginates</td>
    <td><span class="mono">GET /planned_workorder_line/?page=1&amp;per_page=5</span> answers HTTP&nbsp;200 in &lt;10&nbsp;s honouring pagination</td>
    <td class="bad">RED — no response in 35&nbsp;s (socket timeout)</td></tr>
<tr><td class="mono">testFixCheckSingleSpelling</td>
    <td>the resource exists under ONE canonical route family (not both spellings)</td>
    <td class="bad">RED — 6 dash + 12 underscore paths registered</td></tr>
</table>
<p class="callout">Both tests <b>skip</b> (never false-fail) when a control endpoint shows the QA backend is in one of its ambient
502/504 degradation episodes — during this re-verification one such episode occurred and even <span class="mono">/action-items/counts</span>
returned 502 after 41&nbsp;s, so the guard is not hypothetical.</p>

<h2>Reproduce it yourself (~1 minute)</h2>
<pre>TOKEN=$(curl -sk https://acme.qa.egalvanic.ai/api/auth/login -H 'Content-Type: application/json' \\
  -d '{{"email":"&lt;qa-admin&gt;","password":"&lt;password&gt;","subdomain":"acme"}}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

# 1. THE BUG — hangs (unbounded read, pagination ignored). Ctrl-C when bored:
time curl -sk -H "Authorization: Bearer $TOKEN" \\
  'https://acme.qa.egalvanic.ai/api/planned_workorder_line/?page=1&amp;per_page=5' -o /dev/null

# 2. Control — the SAME resource, bounded, answers fine:
time curl -sk -H "Authorization: Bearer $TOKEN" \\
  'https://acme.qa.egalvanic.ai/api/planned_workorder_line/by-workorder/&lt;any-wo-uuid&gt;' -o /dev/null

# 3. Both spellings still in the spec:
curl -sk https://acme.qa.egalvanic.ai/api/swagger.json | python3 -c \\
  'import sys,json; ps=json.load(sys.stdin)["paths"]; \\
   print(*[p+" "+str(list(ps[p])) for p in ps if "planned" in p and "line" in p], sep="\\n")'</pre>

<h2>Recommended fix (unchanged since 17 Jul — none of it has happened)</h2>
<ol>
  <li><b>One canonical route family</b> (the dash form, matching the rest of the v1.35 API); move ALL methods onto it.</li>
  <li><b>Restore a paginated list read</b> — the removed <span class="mono">/planned-workorder-lines/</span> honoured
      <span class="mono">page/per_page</span> and answered in 1.4&nbsp;s; the surviving underscore list scans unbounded and times out.</li>
  <li><b>Alias or 410 the underscore family</b> so the two registrations cannot drift further (they already have: writes on both sides, different path shapes).</li>
  <li>Frontend quick win: collapse the dashboard's two <span class="mono">/sites/{{id}}/status</span> variants into one call.</li>
</ol>

<div class="foot">Sources: live authenticated browser probes + swagger.json fetches, 23 Jul 2026 · Parallel Suite 3
<span class="mono">DuplicateApiAuditTest</span> (incl. new FIX-CHECK tripwires) · Suite 2 <span class="mono">ApiDuplicateCallTestNG</span> (runtime duplicates)
· machine-readable copy: reports/api-duplicate-endpoints-report.md in the api-health-report CI artifact ·
evidence bundle: docs/bug-repro/duplicate-api-endpoints/</div>
</body></html>"""

SHOTS = [
    ("shot1-swagger-both-spellings", "Screenshot 1 — live /api/swagger.json (rendered for readability, 23 Jul 2026 13:06 UTC): the resource is "
     "registered under BOTH spellings — 6 /planned-workorder-line/* (dash) + 12 /planned_workorder_line/* (underscore) paths, write methods "
     "on both sides. Note the warning: the paginated dash list /planned-workorder-lines/ is no longer in the spec (though it still answers live)."),
    ("shot2-race-pending", "Screenshot 2 — live probe on the authenticated session (12:54:19 UTC), all 4 requests fired SIMULTANEOUSLY: "
     "the dash list answered in 1.4 s, the bounded by-workorder read in 2.6 s, the control in 0.7 s — while THE BUG "
     "(underscore list, pagination requested) is still hanging with no response at 27.1 s."),
    ("shot3-race-timeout", "Screenshot 3 — the same probe at completion: GET /api/planned_workorder_line/?page=1&per_page=5 aborted after "
     "45 s with NO response (unbounded read, pagination ignored) — the exact 17-Jul signature. Every other row answered normally, "
     "proving the backend was healthy and the failure is specific to this endpoint."),
    ("shot4-dashboard-status-dup", "Screenshot 4 — /dashboard load (12:16:14 UTC), entries from the page's own resource-timing log: "
     "GET /sites/{id}/status fired TWICE on one load, 1 ms apart (variant 1 with ?narrative=false, variant 2 bare). "
     "The frontend redundant-fetch finding from 17 Jul is also still present. (/users/{id}/roles fired once on this load.)"),
]

shots_html = "\n".join(img(stem, cap) for stem, cap in SHOTS)
html = HTML.replace("__SCREENSHOTS__", "<h2>Evidence — live screenshots (23 Jul 2026)</h2>" + shots_html)
out = HERE / "Duplicate-API-NotFixed-Reverification.html"
out.write_text(html)
print(f"wrote {out} ({len(html)//1024} KB)")
