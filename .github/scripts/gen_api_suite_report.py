#!/usr/bin/env python3
"""
Consolidated, client-grade eGalvanic API Test Suite report (Parallel Suite 3).

One styled, print-to-PDF-ready document covering EVERY API test area: health check, full-catalog
probe, pagination (curated + catalog-wide + behavior), agent-token contract, duplicate-endpoint
audit, IR/FLIR pipeline, error/transport, security headers, filter/search consistency, CRUD,
input validation, mutation semantics — plus an executive summary, latency analysis, and a full
per-operation endpoint inventory appendix (the complete evidence table; 1000+ rows).

Usage:
    gen_api_suite_report.py <testng-results.xml> <reports_dir> <out.html>

Parses testng-results.xml for authoritative per-class pass/fail/skip counts and inlines each
area's markdown findings. Missing inputs degrade gracefully (section omitted / noted).
Print with the browser's "Save as PDF" — @page CSS paginates with a cover page and per-section
page breaks (the inventory appendix alone spans 200+ pages).
"""
import sys, os, re, html, json, base64, glob
import datetime
# Prefer defusedxml (immune to XXE / billion-laughs); fall back to stdlib for the
# trusted, build-generated testng-results.xml when defusedxml isn't on the runner.
try:
    import defusedxml.ElementTree as ET   # type: ignore
except ImportError:
    import xml.etree.ElementTree as ET

RESULTS = sys.argv[1] if len(sys.argv) > 1 else "target/surefire-reports/testng-results.xml"
REPORTS = sys.argv[2] if len(sys.argv) > 2 else "reports"
OUT     = sys.argv[3] if len(sys.argv) > 3 else "reports/api-suite-report.html"

# Class simple-name → (area label, markdown report filename or None, one-line "what it checks").
AREAS = [
    ("ApiHealthCheckApiTest",        "API Health Check",              "api-health-report.md",
     "~50-endpoint Z-Platform registry: status, latency, payload, pagination advisories."),
    ("ApiFullCatalogHealthApiTest",  "Full Catalog (spec-driven)",    None,   # inventory appendix instead
     "Every operation in /api/swagger.json (1000+ ops) probed for 5xx / SPA-fallback / auth-gate. Full table in Appendix A."),
    ("ListApiContractApiTest",       "List Pagination — Curated",     "list-api-contract-report.md",
     "7 known-good list endpoints: paginated + bounded default + max-limit + filter + fast."),
    ("ListApiCatalogAuditTest",      "List Pagination — Catalog-wide","list-api-catalog-report.md",
     "SAME pagination contract applied to EVERY collection the live spec exposes (spec-driven)."),
    ("PaginationBehaviorApiTest",    "Pagination Behavior",           "pagination-behavior-report.md",
     "Disjoint pages, stable ordering, beyond-end, abusive params, total consistency."),
    ("AgentsContractApiTest",        "Agents Contract (agent-token)", None,
     "Agent-token privilege boundary on 91 /agents/* ops + /agent/health SDK/service contract."),
    ("DuplicateApiAuditTest",        "Duplicate-Endpoint Audit",      "api-duplicate-endpoints-report.md",
     "Spec-level duplicates: dash/underscore twins, trailing-slash twins, singular/plural roots, v1/v2 overlaps."),
    ("IrFlirContractApiTest",        "IR / FLIR Photo Pipeline",      None,
     "Auth-gates on every ir_photo/ir_session mutation, ir_session read models, + FLIR-IND (ZP-3112) validation contract watch."),
    ("ErrorContractApiTest",         "Error / Transport Contract",    "error-contract-report.md",
     "Unknown-resource 404s, malformed-input never-5xx, unauth 401 gate, 502 detector."),
    ("SecurityHeadersApiTest",       "Security Headers / CORS",       "security-headers-report.md",
     "Content-Type vs SPA-shell, nosniff/HSTS/no-store, evil-Origin CORS, version leakage."),
    ("FilterSearchConsistencyApiTest","Filter / Search Consistency",  "filter-search-consistency-report.md",
     "Filter purity, search correctness, pagination disjoint, list==detail on /v2/issues/list."),
    ("CrudLifecycleApiTest",         "CRUD Lifecycle",                "crud-lifecycle-report.md",
     "Authenticated create→read→update→delete round-trips for task / issue / building."),
    ("InputValidationApiTest",       "Input Validation",              "input-validation-report.md",
     "Malformed/negative bodies must yield 4xx (never 5xx) and not leak DB/stack internals."),
    ("MutationSemanticsApiTest",     "Mutation Semantics",            "mutation-semantics-report.md",
     "Async queue convergence vs x-direct-write, delete idempotency, DELETE media-type."),
    ("SldV3PayloadBenchmarkApiTest",  "SLD V3 Payload Benchmark",     "sld-v3-payload-benchmark.md",
     "Per-SLD v3 load latency, total payload, node_terminals total, dominant keys, and v2 before/after delta."),
    ("AllPagesStabilityApiTest",     "All-Pages 502 Sweep",           "all-pages-502-report.md",
     "Every app page's live-captured backing endpoints swept repeatedly for 502/5xx/timeout, per page."),
    ("Page502ScreenshotSweepTestNG", "Page 502 Screenshot Sweep",     "page-502-screenshot-report.md",
     "Real logged-in browser walks every sidebar page; any 5xx/502 its API calls return is screenshotted as evidence."),
    ("Page502ScreenshotSweepTestNG", "Per-Page Network & Console",    "page-network-console-report.md",
     "DevTools-style per-page evidence: the app /api calls each page fired (method/status/timing, duplicates flagged) + console errors."),
    ("Sentry502CorrelationApiTest",  "Sentry 502 Correlation",        "sentry-502-report.md",
     "Unresolved 502 issues (last 24h) pulled from Sentry — the inside view pairing with the outside sweeps."),
    ("ApiDuplicateCallTestNG",       "Runtime Duplicate Calls (Suite 2)", "api-duplicate-calls-report.md",
     "Browser-driven: same endpoint refetched 3–4× on one page load (runs in Suite 2's api toggle)."),
]

# ── parse testng-results.xml → per-class pass/fail/skip (config methods excluded) ──
counts = {}
if os.path.exists(RESULTS):
    try:
        root = ET.parse(RESULTS).getroot()
        for cls in root.iter("class"):
            simple = cls.get("name", "").split(".")[-1]
            c = counts.setdefault(simple, {"pass": 0, "fail": 0, "skip": 0})
            for m in cls.findall("test-method"):
                if m.get("is-config") == "true":
                    continue
                st = (m.get("status") or "").upper()
                if st == "PASS": c["pass"] += 1
                elif st == "FAIL": c["fail"] += 1
                elif st == "SKIP": c["skip"] += 1
    except Exception as e:
        sys.stderr.write("suite-report: could not parse %s: %s\n" % (RESULTS, e))

# ── tiny markdown → HTML (headings, tables, bold, lists, hr, code) ──
def md_to_html(md):
    out, in_tbl, in_ul = [], False, False
    def close_tbl():
        nonlocal in_tbl
        if in_tbl: out.append("</tbody></table></div>"); in_tbl = False
    def close_ul():
        nonlocal in_ul
        if in_ul: out.append("</ul>"); in_ul = False
    def inline(t):
        t = html.escape(t)
        t = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", t)
        t = re.sub(r"`([^`]+)`", r"<code>\1</code>", t)
        return t
    lines = md.splitlines()
    for i, ln in enumerate(lines):
        if re.match(r"^\|.*\|\s*$", ln):
            cells = [c.strip() for c in ln.strip().strip("|").split("|")]
            if set("".join(cells)) <= set("-: "):
                continue
            nxt = lines[i+1].strip() if i+1 < len(lines) else ""
            header = bool(re.match(r"^\|[\s:-]+\|\s*$", nxt))
            if not in_tbl:
                close_ul(); out.append('<div class="tblwrap"><table>')
                if header:
                    out.append("<thead><tr>" + "".join("<th>%s</th>" % inline(c) for c in cells) + "</tr></thead><tbody>")
                    in_tbl = True; continue
                out.append("<tbody>"); in_tbl = True
            out.append("<tr>" + "".join("<td>%s</td>" % inline(c) for c in cells) + "</tr>")
            continue
        close_tbl()
        if ln.startswith("### "): close_ul(); out.append("<h4>%s</h4>" % inline(ln[4:]))
        elif ln.startswith("## "): close_ul(); out.append("<h3>%s</h3>" % inline(ln[3:]))
        elif ln.startswith("# "):  close_ul(); out.append("<h2>%s</h2>" % inline(ln[2:]))
        elif ln.strip() == "---":  close_ul(); out.append("<hr>")
        elif ln.startswith("- ") or ln.startswith("* "):
            if not in_ul: out.append("<ul>"); in_ul = True
            out.append("<li>%s</li>" % inline(ln[2:]))
        elif ln.strip() == "": close_ul()
        else: close_ul(); out.append("<p>%s</p>" % inline(ln))
    close_tbl(); close_ul()
    return "\n".join(out)

# ── full-catalog rows → inventory appendix + latency analysis + evidence stats ──
CAT_ROWS = []
cat_md = os.path.join(REPORTS, "api-catalog-report.md")
if os.path.exists(cat_md):
    for line in open(cat_md, encoding="utf-8", errors="replace"):
        m = re.match(r"\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(pass|warn|fail)\s*\|\s*(.+?)\s*\|\s*(\d+)ms\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(\d+)\s*\|", line)
        if m:
            cat, name, path, status, http, lat, items, shape, payload, recn = m.groups()
            CAT_ROWS.append(dict(cat=cat, op=name, status=status, http=http.strip(),
                                 lat=int(lat), shape=shape.strip(), payload=payload.strip()))

# ── evidence captures (reports/api-evidence.json) → visual HTTP capture cards ──
EVIDENCE = []
ev_path = os.path.join(REPORTS, "api-evidence.json")
if os.path.exists(ev_path):
    try:
        EVIDENCE = json.load(open(ev_path, encoding="utf-8"))
    except Exception as e:
        sys.stderr.write("suite-report: could not parse api-evidence.json: %s\n" % e)

SEV_ORDER = {"critical": 0, "high": 1, "warning": 2, "info": 3}
def sev_cls(s): return {"critical": "fail", "high": "fail", "warning": "skip", "info": "info"}.get(s, "info")

def evidence_card(ev):
    sev = ev.get("severity", "info"); vcls = sev_cls(sev)
    status = ev.get("status", 0)
    scls = "fail" if (isinstance(status, int) and (status >= 500 or status == 0)) else \
           ("skip" if isinstance(status, int) and 400 <= status < 500 else "pass")
    req_body = ev.get("requestBody") or ""
    method_line = html.escape(ev.get("method", "")) + " " + html.escape(ev.get("url", ""))
    auth_line = html.escape(ev.get("auth", "(none)"))
    body_line = '<div class="ev-req">' + method_line + '</div>' \
                + '<div class="ev-h">Authorization: ' + auth_line + '</div>' \
                + (('<div class="ev-h">Body: <code>' + html.escape(req_body) + '</code></div>') if req_body else "")
    # Only flag a leak on an ERROR response (>=400): a 200 whose legit body merely contains a keyword
    # (e.g. the /mutations ledger) is not an information-disclosure leak.
    leak_badge = ' <span class="badge b-fail mini">INTERNALS LEAK</span>' \
        if (ev.get("leak") and isinstance(status, int) and status >= 400) else ""
    meta = html.escape(str(ev.get("shape", ""))) + " · " + str(ev.get("latencyMs", "?")) + "ms · " \
           + html.escape(str(ev.get("contentType", "")))
    snippet = html.escape(str(ev.get("respSnippet", "")))
    return ('<div class="evcard">'
            '<div class="ev-top"><span class="badge b-' + vcls + '">' + html.escape(sev.upper()) + '</span>'
            '<span class="ev-note">' + html.escape(ev.get("note", "")) + '</span></div>'
            + body_line
            + '<div class="ev-resp"><span class="badge b-' + scls + ' mini">HTTP ' + str(status) + '</span> '
            '<span class="muted">' + meta + '</span>' + leak_badge + '</div>'
            '<pre class="ev-body">' + snippet + '</pre></div>')

# group evidence by category, most-severe first
ev_by_cat = {}
for ev in EVIDENCE:
    ev_by_cat.setdefault(ev.get("category", "other"), []).append(ev)
for lst in ev_by_cat.values():
    lst.sort(key=lambda e: SEV_ORDER.get(e.get("severity", "info"), 9))
n_leaks = sum(1 for e in EVIDENCE if e.get("leak") and isinstance(e.get("status"), int) and e.get("status") >= 400)
n_5xx = sum(1 for e in EVIDENCE if isinstance(e.get("status"), int) and e.get("status", 0) >= 500)

# ── embedded PNG screenshots: fresh run captures (reports/evidence/) + committed assets
#    (.github/report-assets/, since reports/ is gitignored so committed samples live there) ──
SHOTS, _seen = [], set()
for pat in (os.path.join(REPORTS, "evidence", "*.png"), ".github/report-assets/*.png"):
    for png in sorted(glob.glob(pat)):
        base = os.path.basename(png)
        if base in _seen:
            continue
        _seen.add(base)
        try:
            b64 = base64.b64encode(open(png, "rb").read()).decode("ascii")
            name = base.rsplit(".", 1)[0].replace("evidence-", "").replace("-", " ")
            SHOTS.append((name, b64))
        except Exception:
            pass

# ── totals ──
tp = sum(c["pass"] for c in counts.values())
tf = sum(c["fail"] for c in counts.values())
ts = sum(c["skip"] for c in counts.values())
ttot = tp + tf + ts
now = datetime.datetime.utcnow().strftime("%d %B %Y, %H:%M UTC")
overall = "FAIL" if tf else "PASS"

# ── summary rows + sections ──
sum_rows, sections, toc = [], [], []
areas_present = 0
for simple, label, mdfile, desc in AREAS:
    c = counts.get(simple)
    has_md = mdfile and os.path.exists(os.path.join(REPORTS, mdfile))
    if c is None and not has_md and not (simple == "ApiFullCatalogHealthApiTest" and CAT_ROWS):
        continue
    areas_present += 1
    p = c["pass"] if c else 0; f = c["fail"] if c else 0; k = c["skip"] if c else 0
    ran = (p + f + k) > 0
    verdict = "FAIL" if f else ("PASS" if p else ("INFO" if has_md and not ran else "SKIP"))
    vcls = verdict.lower()
    anchor = re.sub(r"[^a-z0-9]+", "-", label.lower()).strip("-")
    toc.append(f'<li><a href="#{anchor}">{html.escape(label)}</a> — <span class="badge b-{vcls} mini">{verdict}</span></li>')
    sum_rows.append(
        f'<tr class="v-{vcls}"><td><a href="#{anchor}">{html.escape(label)}</a></td>'
        f'<td>{p+f+k}</td><td class="p">{p}</td><td class="f">{f}</td><td class="s">{k}</td>'
        f'<td><span class="badge b-{vcls}">{verdict}</span></td>'
        f'<td class="desc">{html.escape(desc)}</td></tr>')
    if has_md:
        body = md_to_html(open(os.path.join(REPORTS, mdfile), encoding="utf-8", errors="replace").read())
    elif simple == "ApiFullCatalogHealthApiTest" and CAT_ROWS:
        body = ("<p>Every spec operation probed this run: <strong>%d operations</strong>. "
                "Per-operation status/latency/shape evidence is in <a href='#appendix-inventory'>Appendix A — Full Endpoint Inventory</a>; "
                "latency analysis in the <a href='#latency-analysis'>Performance section</a>.</p>" % len(CAT_ROWS))
    else:
        body = ("<p class='muted'>Assertion-only area (no markdown findings file). Per-test logs with request/response "
                "evidence are in the ExtentReports detail HTML for this class (artifact <code>detail-report/</code>).</p>")
    sections.append(
        f'<section id="{anchor}" class="pagebreak"><h2>{html.escape(label)} '
        f'<span class="mini badge b-{vcls}">{verdict}</span></h2>'
        f'<p class="areadesc">{html.escape(desc)}</p>'
        f'<p class="counts">{p} passed · {f} failed · {k} skipped</p>{body}</section>')

# unmapped classes that ran (future-proofing)
mapped = {a[0] for a in AREAS}
for simple, c in counts.items():
    if simple in mapped or (c["pass"] + c["fail"] + c["skip"]) == 0: continue
    verdict = "FAIL" if c["fail"] else ("PASS" if c["pass"] else "SKIP")
    sum_rows.append(
        f'<tr class="v-{verdict.lower()}"><td>{html.escape(simple)}</td>'
        f'<td>{c["pass"]+c["fail"]+c["skip"]}</td><td class="p">{c["pass"]}</td>'
        f'<td class="f">{c["fail"]}</td><td class="s">{c["skip"]}</td>'
        f'<td><span class="badge b-{verdict.lower()}">{verdict}</span></td><td class="desc">—</td></tr>')

# ── evidence section (grouped capture cards) + screenshots section ──
CAT_LABELS = {
    "invalid-request": "Invalid-Request Handling &amp; Security (malformed input)",
    "availability": "Availability &amp; 502 / 5xx Watch",
    "performance": "Performance — Heaviest Endpoints",
    "pagination": "Pagination — Uncapped Collections",
    "spa-fallback": "SPA Fallback (API path → app shell)",
    "auth-gate": "Authentication Gates",
    "agent-token": "Agent-Token Boundary",
}
evidence_section = ""
if EVIDENCE:
    blocks = []
    banner = ""
    if n_leaks or n_5xx:
        banner = (f'<p class="banner-fail"><strong>{n_leaks} response(s) leaked server internals (SQL/stack)</strong> '
                  f'and {n_5xx} returned 5xx during capture — see the Invalid-Request &amp; Security cards below. '
                  f'These are the highest-severity, client-reportable findings with full request/response evidence.</p>')
    order = ["invalid-request", "availability", "performance", "pagination", "spa-fallback", "auth-gate", "agent-token"]
    for cat in order + [c for c in ev_by_cat if c not in order]:
        if cat not in ev_by_cat: continue
        cards = "".join(evidence_card(e) for e in ev_by_cat[cat])
        blocks.append(f'<h3>{CAT_LABELS.get(cat, html.escape(cat))} <span class="muted">({len(ev_by_cat[cat])})</span></h3>{cards}')
    evidence_section = (f'<section id="evidence" class="pagebreak"><h2>Issue Evidence — Live HTTP Captures</h2>'
                        f'<p class="areadesc">Each card is a real request/response captured live this run — the REST-API '
                        f'equivalent of a screenshot: method, URL, redacted auth, status, latency and the actual response body. '
                        f'{len(EVIDENCE)} captures across {len(ev_by_cat)} issue classes.</p>{banner}{"".join(blocks)}</section>')

screenshots_section = ""
if SHOTS:
    imgs = "".join(f'<figure class="shot"><img src="data:image/png;base64,{b64}" alt="{html.escape(name)}"/>'
                   f'<figcaption>{html.escape(name.title())}</figcaption></figure>' for name, b64 in SHOTS)
    screenshots_section = (f'<section id="screenshots" class="pagebreak"><h2>Visual Evidence — Screenshots</h2>'
                           f'<p class="areadesc">Rendered captures of representative issues and the API surface under test.</p>{imgs}</section>')

# ── latency analysis + inventory appendix from catalog rows ──
lat_section = inventory_section = ""
if CAT_ROWS:
    bands = [("&lt; 500 ms (fast)", lambda l: l < 500), ("500–1500 ms", lambda l: 500 <= l < 1500),
             ("1.5–4 s (slow)", lambda l: 1500 <= l < 4000), ("4–8 s (very slow)", lambda l: 4000 <= l < 8000),
             ("&ge; 8 s (critical)", lambda l: l >= 8000)]
    total = len(CAT_ROWS)
    bars = []
    for name, pred in bands:
        n = sum(1 for r in CAT_ROWS if pred(r["lat"]))
        pct = round(100 * n / total, 1) if total else 0
        bars.append(f'<div class="latrow"><div class="latlab">{name}</div>'
                    f'<div class="latbar"><div class="latfill" style="width:{max(pct,0.5)}%"></div></div>'
                    f'<div class="latn">{n} ops · {pct}%</div></div>')
    slow = sorted(CAT_ROWS, key=lambda r: -r["lat"])[:20]
    slow_rows = "".join(f"<tr><td>{html.escape(r['op'])}</td><td>{r['lat']}ms</td><td>{r['http']}</td>"
                        f"<td>{r['shape']}</td><td>{r['payload']}</td></tr>" for r in slow)
    lat_section = f"""
<section id="latency-analysis" class="pagebreak"><h2>API Performance — Latency Analysis</h2>
<p class="areadesc">Latency distribution across all {total} probed operations (full-catalog run), plus the 20 slowest endpoints.</p>
{''.join(bars)}
<h3>Slowest 20 operations</h3>
<div class="tblwrap"><table><thead><tr><th>Operation</th><th>Latency</th><th>HTTP</th><th>Shape</th><th>Payload</th></tr></thead>
<tbody>{slow_rows}</tbody></table></div>
<p class="muted">≥ 30 s entries are read-timeouts — the endpoint did not answer within the probe window (e.g. <code>/planned_workorder_line/</code>, flagged for pagination).</p>
</section>"""
    inv_rows = "".join(f"<tr class='v-{r['status']}'><td>{html.escape(r['cat'])}</td><td>{html.escape(r['op'])}</td>"
                       f"<td><span class='badge b-{'pass' if r['status']=='pass' else ('fail' if r['status']=='fail' else 'skip')} mini'>{r['status']}</span></td>"
                       f"<td>{r['http']}</td><td>{r['lat']}ms</td><td>{r['shape']}</td><td>{r['payload']}</td></tr>"
                       for r in CAT_ROWS)
    inventory_section = f"""
<section id="appendix-inventory" class="pagebreak"><h2>Appendix A — Full Endpoint Inventory ({len(CAT_ROWS)} operations)</h2>
<p class="areadesc">Per-operation evidence from the spec-driven full-catalog probe: every operation in the live
<code>/api/swagger.json</code> with its observed status, HTTP code, latency, body shape and payload size.
This is the raw capture behind the health verdicts — the API-testing equivalent of a screenshot per endpoint.</p>
<div class="tblwrap"><table><thead><tr><th>Family</th><th>Operation</th><th>Verdict</th><th>HTTP</th><th>Latency</th><th>Shape</th><th>Payload</th></tr></thead>
<tbody>{inv_rows}</tbody></table></div></section>"""

# ══════════════════════════════════════════════════════════════════════════════
# CRITICAL FINDINGS DASHBOARD — aggregate every real issue across all sources so
# the FIRST pages show the problems (the suite is report-mode/green-by-default, so
# the area verdicts alone would hide the findings a client actually cares about).
# ══════════════════════════════════════════════════════════════════════════════
def _ep_from_url(u):
    return u.split("/api", 1)[1] if "/api" in u else u

def _read(name):
    p = os.path.join(REPORTS, name)
    return open(p, encoding="utf-8", errors="replace").read() if os.path.exists(p) else ""

SEV_RANK = {"critical": 0, "high": 1, "medium": 2}
GROUPS = ["Security & Data Exposure", "Availability & Errors (5xx / 502 / timeout)",
          "Performance — Slow Endpoints", "Pagination — Unbounded Collections", "API Hygiene"]

def collect_findings():
    F = []
    def add(sev, group, endpoint, issue, ms=None, ev=False):
        F.append(dict(severity=sev, group=group, endpoint=endpoint.strip(), issue=issue.strip(), ms=ms, ev=ev))

    # 1) live evidence captures
    for e in EVIDENCE:
        status = e.get("status"); ep = e.get("method", "") + " " + _ep_from_url(e.get("url", ""))
        note = e.get("note", "")
        if e.get("leak") and isinstance(status, int) and status >= 400:
            add("critical", GROUPS[0], ep, f"HTTP {status}: response leaks server internals (SQL/stack trace) — {note}", ev=True)
        elif isinstance(status, int) and status >= 500:
            add("critical", GROUPS[1], ep, f"HTTP {status}: {note}", ev=True)
        elif status == 0:
            add("critical", GROUPS[1], ep, f"Unresponsive / transport failure — {note}", ev=True)
        elif e.get("category") == "pagination" and e.get("severity") in ("critical", "high"):
            add("high", GROUPS[3], ep, note, ev=True)

    # 2) full-catalog Recommendations  ("- **critical** · GET /x — message")
    for m in re.finditer(r"- \*\*(critical|warning)\*\* · (.+?) — (.+)", _read("api-catalog-report.md")):
        raw, ep, msg = m.group(1), m.group(2).strip(), m.group(3).strip()
        low = msg.lower()
        mm = re.search(r"(\d+)\s*ms", msg); ms = int(mm.group(1)) if mm else None
        if "timed out" in low or "unresponsive" in low:
            add("critical", GROUPS[2], ep, "Unresponsive (read timed out)", ms=30000)
        elif "very slow" in low or "slow response" in low:
            add("critical" if (ms or 0) >= 8000 else "high", GROUPS[2], ep, f"Slow response: {ms}ms", ms=ms)
        elif "5xx" in low:
            add("critical", GROUPS[1], ep, msg)
        elif "unauthenticated" in low or "auth gate" in low:
            add("critical", GROUPS[0], ep, msg)
        elif "html" in low or "spa fallback" in low:
            add("high", GROUPS[4], ep, "Fixed API path serves the SPA shell instead of JSON")

    # 3) list-API pagination VIOLATIONs (catalog-wide + curated)
    for fn in ("list-api-catalog-report.md", "list-api-contract-report.md"):
        for line in _read(fn).splitlines():
            if not line.strip().endswith("| VIOLATION |"):
                continue
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            if len(cells) < 9:
                continue
            path, total, default, paginated, maxlimit, filt, resp = cells[1], cells[2], cells[3], cells[4], cells[5], cells[6], cells[7]
            issues = []
            try: tot = int(re.sub(r"[^0-9]", "", total) or 0)
            except Exception: tot = 0
            if "UNBOUNDED" in default or (paginated == "NO" and tot > 100):
                issues.append(f"not paginated — returns all {total} uncapped")
            if maxlimit.startswith("NO"):
                issues.append("no max-limit")
            if issues:
                add("high" if tot <= 500 else "critical", GROUPS[3], "GET " + path, "; ".join(issues))
            sm = re.search(r"(\d{4,})ms", resp)
            if sm:
                add("critical" if int(sm.group(1)) >= 8000 else "high", GROUPS[2], "GET " + path, f"Slow list response: {sm.group(1)}ms", ms=int(sm.group(1)))

    # 3b) SLD V3 payload benchmark — rows whose Findings column is not "OK" (slow load / heavy node_terminals)
    for line in _read("sld-v3-payload-benchmark.md").splitlines():
        if not line.startswith("| ") or "| OK |" in line or "Findings" in line or "---" in line:
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 9 and cells[8] and cells[8] != "OK":
            low = cells[8].lower()
            sev = "critical" if ("hard ceiling" in low or "bottleneck" in low) else "high"
            add(sev, GROUPS[2], "GET /sld/v3/" + cells[1], "SLD load (" + cells[0] + ", " + cells[2] + " nodes): " + cells[8])

    # 3c) all-pages 502 sweep — rows are "| Page | Incidents | Detail |"; any 502/5xx is critical
    for line in _read("all-pages-502-report.md").splitlines():
        if not line.startswith("| ") or "Incidents" in line or "---" in line:
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 3 and cells[0] and cells[0] != "Page":
            detail = cells[2]
            low = detail.lower()
            if "502" in low or "5xx" in low or "exception" in low:
                add("critical", GROUPS[1], "page:" + cells[0],
                    "Page-load 502/5xx (" + cells[1] + " incident(s)): " + detail)
            elif "timeout" in low:
                add("high", GROUPS[2], "page:" + cells[0],
                    "Page-load timeout (" + cells[1] + " incident(s)): " + detail)

    # 3d) browser page 502 screenshot sweep — rows "| Page | Incidents | Screenshot | Detail |"
    for line in _read("page-502-screenshot-report.md").splitlines():
        if not line.startswith("| ") or "Incidents" in line or "---" in line:
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 4 and cells[0] and cells[0] != "Page":
            add("critical", GROUPS[1], "page:" + cells[0],
                "BROWSER-VISIBLE 5xx (" + cells[1] + " incident(s), screenshot: " + cells[2] + "): " + cells[3], ev=True)

    # 3e) Sentry 502 correlation — rows "| Sentry ID | Title | Events | Users | Last seen | Project | Link |"
    for line in _read("sentry-502-report.md").splitlines():
        if not line.startswith("| ") or "Sentry ID" in line or "---" in line:
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 7 and cells[0] and cells[0] != "Sentry ID":
            add("critical", GROUPS[1], "sentry:" + cells[5],
                "Unresolved 502 in Sentry (24h): " + cells[1] + " — " + cells[2] + " events, " + cells[3]
                + ", last " + cells[4] + " — " + cells[6], ev=True)

    # 4) duplicate-endpoint criticals
    for line in _read("api-duplicate-endpoints-report.md").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 4 and cells[1] == "critical":
            add("high", GROUPS[4], cells[2], f"Duplicate endpoint definition ({cells[0]}) — also {cells[3]}")

    # 5) error/transport contract probes (stability, malformed-input 5xx, unauth-exposed, unknown-resource)
    for line in _read("error-contract-report.md").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) < 3 or cells[0] in ("Probe", ""):
            continue
        probe, result, detail = cells[0], cells[1], cells[2]
        if probe == "stability-probe" and result in ("FAIL", "WARN"):
            add("critical" if result == "FAIL" else "high", GROUPS[1], "critical-path panel",
                "Backend instability across the critical-path panel — " + detail)
        elif probe == "malformed-path":
            m = re.search(r"5xx=(\d+)", detail)
            if m and int(m.group(1)) > 0:
                add("critical", GROUPS[0], "malformed path params",
                    m.group(1) + " malformed-input requests returned 5xx (server error / SQL leak on bad input) — " + detail)
        elif probe == "unauthenticated-get":
            m = re.search(r"exposed-200=(\d+)", detail)
            if m and int(m.group(1)) > 0:
                add("critical", GROUPS[0], "unauthenticated GET",
                    m.group(1) + " endpoint(s) return data to an UNAUTHENTICATED request — " + detail)
        elif probe == "unknown-resource":
            m = re.search(r"5xx=(\d+)", detail)
            if m and int(m.group(1)) > 0:
                add("high", GROUPS[1], "unknown-resource GET",
                    m.group(1) + " unknown-resource request(s) returned 5xx instead of 404 — " + detail)

    # dedupe by (endpoint, group), keep the most severe / highest-latency
    best = {}
    for f in F:
        k = (f["endpoint"], f["group"])
        cur = best.get(k)
        if cur is None or SEV_RANK[f["severity"]] < SEV_RANK[cur["severity"]] or (f["ms"] or 0) > (cur["ms"] or 0):
            best[k] = f
    out = list(best.values())
    out.sort(key=lambda f: (SEV_RANK[f["severity"]], -(f["ms"] or 0), f["group"]))
    return out

FINDINGS = collect_findings()
nf_crit = sum(1 for f in FINDINGS if f["severity"] == "critical")
nf_high = sum(1 for f in FINDINGS if f["severity"] == "high")

def _finding_row(f):
    sev = f["severity"]; vcls = "fail" if sev in ("critical", "high") else "skip"
    ev = ' <span class="badge b-info mini">evidence</span>' if f["ev"] else ""
    return ('<tr><td><span class="badge b-' + vcls + ' mini">' + sev.upper() + '</span></td>'
            '<td class="mono">' + html.escape(f["endpoint"]) + '</td>'
            '<td>' + html.escape(f["issue"]) + ev + '</td></tr>')

findings_section = ""
if FINDINGS:
    by_group = {}
    for f in FINDINGS:
        by_group.setdefault(f["group"], []).append(f)
    blocks = []
    for g in GROUPS:
        items = by_group.get(g, [])
        if not items:
            continue
        shown = items[:25]
        more = ('<p class="muted">+ ' + str(len(items) - 25) + ' more in this category (see the area sections &amp; Appendix A).</p>') if len(items) > 25 else ""
        rows = "".join(_finding_row(f) for f in shown)
        gc = sum(1 for f in items if f["severity"] == "critical"); gh = len(items) - gc
        blocks.append('<h3>' + html.escape(g) + ' <span class="muted">(' + str(gc) + ' critical, ' + str(gh) + ' high)</span></h3>'
                      '<div class="tblwrap"><table><thead><tr><th>Severity</th><th>Endpoint</th><th>Issue</th></tr></thead>'
                      '<tbody>' + rows + '</tbody></table></div>' + more)
    findings_section = (
        '<section id="findings" class="pagebreak"><h2>Critical &amp; High-Priority Findings</h2>'
        '<p class="areadesc">Every real issue surfaced across all test areas, aggregated and ranked — '
        'the suite runs report-mode (green-by-default monitor), so this is where the actionable problems live.</p>'
        '<div class="findbanner"><span class="badge b-fail">' + str(nf_crit) + ' CRITICAL</span> '
        '<span class="badge b-skip">' + str(nf_high) + ' HIGH</span> '
        '<span class="muted">across ' + str(len(by_group)) + ' categories · full request/response evidence in the Issue Evidence section</span></div>'
        + "".join(blocks) + '</section>')

CSS = """
:root{--bg:#f6f8fb;--card:#fff;--ink:#1a2233;--muted:#5b6b82;--line:#e3e9f2;--accent:#2557d6;
--pass:#137a4b;--passbg:#e6f5ee;--fail:#b3261e;--failbg:#fdecea;--skip:#8a6d00;--skipbg:#fdf6e3;
--info:#245a8f;--infobg:#e8f1fb;}
@media (prefers-color-scheme:dark){:root{--bg:#0f141b;--card:#161d27;--ink:#e7edf6;--muted:#9fb0c6;
--line:#26303d;--accent:#6aa0ff;--passbg:#0f2e20;--failbg:#3a1512;--skipbg:#33290a;--infobg:#122a40;}}
*{box-sizing:border-box}body{margin:0;font:15px/1.55 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;background:var(--bg);color:var(--ink)}
.wrap{max-width:1120px;margin:0 auto;padding:28px 20px 60px}
.cover{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:60px 40px;text-align:center;margin-bottom:24px}
.cover h1{font-size:32px;margin:0 0 6px}.cover .stamp{font-size:20px;margin:18px 0}
.cover .meta{color:var(--muted);font-size:14px;line-height:2}
.kpis{display:flex;flex-wrap:wrap;gap:12px;margin:16px 0 26px;justify-content:center}
.kpi{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:14px 18px;min-width:120px}
.kpi .n{font-size:26px;font-weight:700}.kpi .l{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.04em}
.badge{display:inline-block;padding:2px 9px;border-radius:999px;font-size:12px;font-weight:700}
.mini{font-size:11px;vertical-align:middle}
.b-pass{background:var(--passbg);color:var(--pass)}.b-fail{background:var(--failbg);color:var(--fail)}
.b-skip{background:var(--skipbg);color:var(--skip)}.b-info{background:var(--infobg);color:var(--info)}
.tblwrap{overflow-x:auto;border:1px solid var(--line);border-radius:12px;margin:10px 0}
table{border-collapse:collapse;width:100%;background:var(--card)}
th,td{padding:8px 11px;text-align:left;border-bottom:1px solid var(--line);font-size:13px;vertical-align:top}
th{background:rgba(127,127,127,.06);font-size:11.5px;text-transform:uppercase;letter-spacing:.03em;color:var(--muted)}
td.p{color:var(--pass);font-weight:700}td.f{color:var(--fail);font-weight:700}td.s{color:var(--skip);font-weight:700}
td.desc{color:var(--muted);font-size:12.5px}
a{color:var(--accent);text-decoration:none}a:hover{text-decoration:underline}
section{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:18px 20px;margin:16px 0}
section h2{font-size:19px;margin:0 0 4px}.areadesc{color:var(--muted);font-size:13px;margin:0 0 6px}
.counts{font-size:12.5px;color:var(--muted);margin:0 0 10px}
section code{background:rgba(127,127,127,.14);padding:1px 5px;border-radius:5px;font-size:12.5px}
.muted{color:var(--muted)}hr{border:0;border-top:1px solid var(--line);margin:14px 0}
.mono{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:12px}
.findbanner{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin:6px 0 14px}
.findbanner .badge{font-size:14px;padding:4px 12px}
#findings h3{font-size:15px;margin:16px 0 4px}
.kpi .n.f{color:var(--fail)}
.toc ol{columns:2;font-size:14px}.toc li{margin:4px 0}
.latrow{display:flex;align-items:center;gap:10px;margin:6px 0}
.latlab{width:170px;font-size:12.5px;color:var(--muted)}
.latbar{flex:1;background:rgba(127,127,127,.12);border-radius:6px;height:16px;overflow:hidden}
.latfill{height:100%;background:var(--accent);border-radius:6px}
.latn{width:120px;font-size:12px;color:var(--muted);text-align:right}
.banner-fail{background:var(--failbg);color:var(--fail);border:1px solid var(--fail);border-radius:10px;padding:10px 14px;font-size:13.5px}
.evcard{border:1px solid var(--line);border-radius:10px;margin:10px 0;overflow:hidden}
.ev-top{display:flex;align-items:center;gap:10px;padding:8px 12px;background:rgba(127,127,127,.06);border-bottom:1px solid var(--line)}
.ev-note{font-size:13px}
.ev-req{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:12.5px;padding:8px 12px 2px;font-weight:700}
.ev-h{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:11.5px;color:var(--muted);padding:0 12px}
.ev-resp{padding:8px 12px 4px;font-size:12.5px}
.ev-body{margin:0;padding:10px 12px;background:#0d1117;color:#e6edf3;font-family:ui-monospace,Menlo,Consolas,monospace;
font-size:11.5px;white-space:pre-wrap;word-break:break-word;overflow-x:auto;max-height:340px}
.shot{margin:0 0 18px;border:1px solid var(--line);border-radius:10px;overflow:hidden;background:var(--card)}
.shot img{display:block;max-width:100%;height:auto}
.shot figcaption{padding:8px 12px;font-size:12.5px;color:var(--muted);border-top:1px solid var(--line)}
@media print{
  @page{size:A4;margin:14mm}
  body{background:#fff;color:#111}
  .wrap{max-width:none;padding:0}
  section,.cover,.kpi,.tblwrap{border-color:#ccc;box-shadow:none;background:#fff}
  .pagebreak{break-before:page}
  .cover{break-after:page;min-height:70vh;display:flex;flex-direction:column;justify-content:center}
  a{color:#111}
  thead{display:table-header-group}
  tr{break-inside:avoid}
}
"""
kpi = lambda n, l, cls="": f'<div class="kpi"><div class="n {cls}">{n}</div><div class="l">{l}</div></div>'

htmlout = f"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>eGalvanic API Test Suite Report</title><style>{CSS}</style></head><body><div class="wrap">

<div class="cover">
  <h1>eGalvanic Platform — API Test Suite Report</h1>
  <div class="areadesc">Parallel Suite 3 · Comprehensive API Quality Assessment</div>
  <div class="stamp">{('<a href="#findings" style="text-decoration:none"><span class="badge b-fail" style="font-size:18px;padding:6px 22px">' + str(nf_crit) + ' CRITICAL · ' + str(nf_high) + ' HIGH FINDINGS</span></a>') if nf_crit or nf_high else ('<span class="badge b-' + overall.lower() + '" style="font-size:18px;padding:6px 22px">' + overall + '</span>')}
  <div class="muted" style="font-size:12px;margin-top:8px">Test suite: {overall} (report-mode monitor) · findings below are advisory unless a STRICT_* gate is enabled</div></div>
  <div class="kpis">{kpi(areas_present,'Test areas')}{kpi(ttot,'Test cases executed')}
  {kpi(tp,'Passed','p')}{kpi(tf,'Failed','f')}{kpi(ts,'Skipped','s')}{kpi(len(CAT_ROWS) or '—','Endpoints probed')}
  {kpi('<a href="#findings" style="color:inherit">'+str(nf_crit)+'</a>','Critical findings','f')}{kpi(nf_high,'High findings','f')}
  {kpi(len(EVIDENCE) or '—','Evidence captures')}{kpi(n_leaks,'Internals leaks','f')}</div>
  <div class="meta">Environment: QA (acme.qa.egalvanic.ai) &nbsp;·&nbsp; Generated: {now}<br>
  Coverage: health · full catalog · pagination (curated + catalog-wide + behavior) · agent-token security ·
  duplicate endpoints · IR/FLIR pipeline · error &amp; transport contracts · security headers/CORS ·
  filter/search consistency · CRUD lifecycle · input validation · mutation semantics</div>
</div>

{findings_section}

<section class="toc"><h2>Contents</h2><ol>
{'<li><a href="#findings">Critical &amp; High-Priority Findings</a></li>' if FINDINGS else ''}
{''.join(toc)}
{'<li><a href="#evidence">Issue Evidence — Live HTTP Captures</a></li>' if EVIDENCE else ''}
{'<li><a href="#screenshots">Visual Evidence — Screenshots</a></li>' if SHOTS else ''}
<li><a href="#latency-analysis">API Performance — Latency Analysis</a></li>
<li><a href="#appendix-inventory">Appendix A — Full Endpoint Inventory</a></li></ol></section>

<section class="pagebreak"><h2>Executive Summary</h2>
<p>This report consolidates <strong>{areas_present} independent API test areas</strong> executed against the QA
environment, totalling <strong>{ttot} test cases</strong> ({tp} passed, {tf} failed, {ts} skipped) and probing
<strong>{len(CAT_ROWS) or 'n/a'} live API operations</strong> enumerated at run time from the platform's own
OpenAPI specification — new endpoints are covered automatically with no test-code change. Skips are deliberate
and each carries an actionable reason (non-collection endpoints in discovery audits, known QA-fixture
contamination, features not yet deployed).</p>
<div class="tblwrap"><table><thead><tr><th>Area</th><th>Tests</th><th>Pass</th><th>Fail</th><th>Skip</th><th>Verdict</th><th>What it checks</th></tr></thead>
<tbody>{''.join(sum_rows)}</tbody></table></div>
<p class="muted">Report-mode areas stay green by default and log findings; the matching <code>STRICT_*</code> flag
gates each on violations. Per-test request/response logs: <code>detail-report/</code> (ExtentReports, one HTML per area).</p>
</section>

{''.join(sections)}
{evidence_section}
{screenshots_section}
{lat_section}
{inventory_section}

<p class="muted" style="margin-top:18px">eGalvanic QA Automation · Parallel Suite 3 · spec-driven, self-updating coverage ·
print this page (Save as PDF) for the full paginated document.</p>
</div></body></html>"""

os.makedirs(os.path.dirname(OUT) or ".", exist_ok=True)
open(OUT, "w", encoding="utf-8").write(htmlout)
print("suite-report: wrote %s (%d areas, %d cases: %d pass / %d fail / %d skip; inventory rows: %d)"
      % (OUT, areas_present, ttot, tp, tf, ts, len(CAT_ROWS)))
