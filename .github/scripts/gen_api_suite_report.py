#!/usr/bin/env python3
"""
Consolidated eGalvanic API Test Suite report.

The per-area styled reports (api-health-report.html = ~50-endpoint health check,
api-catalog-report.html = ~1077-op spec-driven probe) each cover ONE area only. This
builds a SINGLE landing page for the whole of Parallel Suite 3 — pagination (curated +
catalog-wide), the agent-token contract, error/transport contracts, CRUD/input-validation/
mutation semantics, etc. — so opening the artifact shows every API test area at once, not
just performance.

Usage:
    gen_api_suite_report.py <testng-results.xml> <reports_dir> <out.html>

It parses testng-results.xml for authoritative per-class pass/fail/skip counts and inlines
each area's markdown findings report. Missing inputs degrade gracefully (section omitted).
"""
import sys, os, re, html
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
    ("ApiFullCatalogHealthApiTest",  "Full Catalog (spec-driven)",    "api-catalog-report.md",
     "Every operation in /api/swagger.json (~1077 ops) probed for 5xx / SPA-fallback / auth-gate."),
    ("ListApiContractApiTest",       "List Pagination — Curated",     "list-api-contract-report.md",
     "7 known-good list endpoints: paginated + bounded default + max-limit + filter + fast."),
    ("ListApiCatalogAuditTest",      "List Pagination — Catalog-wide","list-api-catalog-report.md",
     "SAME pagination contract applied to EVERY collection the live spec exposes (spec-driven)."),
    ("PaginationBehaviorApiTest",    "Pagination Behavior",           "pagination-behavior-report.md",
     "Disjoint pages, stable ordering, beyond-end, abusive params, total consistency."),
    ("AgentsContractApiTest",        "Agents Contract (agent-token)", None,
     "Agent-token privilege boundary on 91 /agents/* ops + /agent/health SDK/service contract."),
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
]

# ── parse testng-results.xml → per-class pass/fail/skip (config methods excluded) ──
counts = {}
suite_name = ""
if os.path.exists(RESULTS):
    try:
        root = ET.parse(RESULTS).getroot()
        s = root.find("suite")
        if s is not None:
            suite_name = s.get("name", "")
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
            if set("".join(cells)) <= set("-: "):   # separator row
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

# ── totals ──
tp = sum(c["pass"] for c in counts.values())
tf = sum(c["fail"] for c in counts.values())
ts = sum(c["skip"] for c in counts.values())
ttot = tp + tf + ts
now = datetime.datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
env = "QA"

# ── build summary rows + sections ──
sum_rows, sections = [], []
covered = set()
for simple, label, mdfile, desc in AREAS:
    covered.add(simple)
    c = counts.get(simple)
    if c is None and not (mdfile and os.path.exists(os.path.join(REPORTS, mdfile))):
        continue  # neither ran nor has a report → omit
    p = c["pass"] if c else 0; f = c["fail"] if c else 0; k = c["skip"] if c else 0
    verdict = "FAIL" if f else ("PASS" if p else "SKIP")
    vcls = verdict.lower()
    anchor = re.sub(r"[^a-z0-9]+", "-", label.lower()).strip("-")
    sum_rows.append(
        f'<tr class="v-{vcls}"><td><a href="#{anchor}">{html.escape(label)}</a></td>'
        f'<td>{p+f+k}</td><td class="p">{p}</td><td class="f">{f}</td><td class="s">{k}</td>'
        f'<td><span class="badge b-{vcls}">{verdict}</span></td>'
        f'<td class="desc">{html.escape(desc)}</td></tr>')
    body = ""
    if mdfile:
        fp = os.path.join(REPORTS, mdfile)
        if os.path.exists(fp):
            body = md_to_html(open(fp, encoding="utf-8", errors="replace").read())
        else:
            body = "<p class='muted'>No markdown report produced for this area in this run.</p>"
    else:
        body = ("<p class='muted'>Assertion-only area (no markdown findings file). "
                "Per-test detail is in the ExtentReports detail-report/ HTML for this class.</p>")
    sections.append(
        f'<section id="{anchor}"><h2>{html.escape(label)} '
        f'<span class="mini badge b-{vcls}">{verdict}</span></h2>'
        f'<p class="areadesc">{html.escape(desc)}</p>'
        f'<p class="counts">{p} passed · {f} failed · {k} skipped</p>{body}</section>')

# classes that ran but aren't in our map (future-proofing)
for simple, c in counts.items():
    if simple in covered: continue
    if c["pass"] + c["fail"] + c["skip"] == 0: continue
    verdict = "FAIL" if c["fail"] else ("PASS" if c["pass"] else "SKIP")
    sum_rows.append(
        f'<tr class="v-{verdict.lower()}"><td>{html.escape(simple)}</td>'
        f'<td>{c["pass"]+c["fail"]+c["skip"]}</td><td class="p">{c["pass"]}</td>'
        f'<td class="f">{c["fail"]}</td><td class="s">{c["skip"]}</td>'
        f'<td><span class="badge b-{verdict.lower()}">{verdict}</span></td><td class="desc">—</td></tr>')

overall = "FAIL" if tf else "PASS"
CSS = """
:root{--bg:#f6f8fb;--card:#fff;--ink:#1a2233;--muted:#5b6b82;--line:#e3e9f2;--accent:#2557d6;
--pass:#137a4b;--passbg:#e6f5ee;--fail:#b3261e;--failbg:#fdecea;--skip:#8a6d00;--skipbg:#fdf6e3;}
@media (prefers-color-scheme:dark){:root{--bg:#0f141b;--card:#161d27;--ink:#e7edf6;--muted:#9fb0c6;
--line:#26303d;--accent:#6aa0ff;--passbg:#0f2e20;--failbg:#3a1512;--skipbg:#33290a;}}
*{box-sizing:border-box}body{margin:0;font:15px/1.55 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;background:var(--bg);color:var(--ink)}
.wrap{max-width:1120px;margin:0 auto;padding:28px 20px 60px}
header.top{display:flex;flex-wrap:wrap;align-items:center;gap:12px;justify-content:space-between;margin-bottom:8px}
h1{font-size:24px;margin:0}.sub{color:var(--muted);font-size:13px;margin:2px 0 20px}
.kpis{display:flex;flex-wrap:wrap;gap:12px;margin:16px 0 26px}
.kpi{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:14px 18px;min-width:120px}
.kpi .n{font-size:26px;font-weight:700}.kpi .l{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.04em}
.badge{display:inline-block;padding:2px 9px;border-radius:999px;font-size:12px;font-weight:700}
.mini{font-size:11px;vertical-align:middle}
.b-pass{background:var(--passbg);color:var(--pass)}.b-fail{background:var(--failbg);color:var(--fail)}.b-skip{background:var(--skipbg);color:var(--skip)}
.tblwrap{overflow-x:auto;border:1px solid var(--line);border-radius:12px;margin:10px 0}
table{border-collapse:collapse;width:100%;background:var(--card)}
th,td{padding:9px 12px;text-align:left;border-bottom:1px solid var(--line);font-size:13.5px;vertical-align:top}
th{background:rgba(127,127,127,.06);font-size:12px;text-transform:uppercase;letter-spacing:.03em;color:var(--muted)}
td.p{color:var(--pass);font-weight:700}td.f{color:var(--fail);font-weight:700}td.s{color:var(--skip);font-weight:700}
td.desc{color:var(--muted);font-size:12.5px}
a{color:var(--accent);text-decoration:none}a:hover{text-decoration:underline}
section{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:18px 20px;margin:16px 0}
section h2{font-size:18px;margin:0 0 4px}.areadesc{color:var(--muted);font-size:13px;margin:0 0 4px}
.counts{font-size:12.5px;color:var(--muted);margin:0 0 10px}
section code{background:rgba(127,127,127,.14);padding:1px 5px;border-radius:5px;font-size:12.5px}
.muted{color:var(--muted)}hr{border:0;border-top:1px solid var(--line);margin:14px 0}
.overall{font-size:13px;font-weight:700}
"""
kpi = lambda n,l,cls="": f'<div class="kpi"><div class="n {cls}">{n}</div><div class="l">{l}</div></div>'
htmlout = f"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>eGalvanic API Test Suite Report</title><style>{CSS}</style></head><body><div class="wrap">
<header class="top"><h1>eGalvanic API Test Suite — Consolidated Report</h1>
<span class="badge b-{overall.lower()} overall">{overall}</span></header>
<div class="sub">Parallel Suite 3 · environment {env} · generated {now}{(' · ' + html.escape(suite_name)) if suite_name else ''}</div>
<div class="kpis">{kpi(len([r for r in sum_rows]),'Test areas')}{kpi(ttot,'Test cases')}
{kpi(tp,'Passed','p')}{kpi(tf,'Failed','f')}{kpi(ts,'Skipped','s')}</div>
<div class="tblwrap"><table><thead><tr><th>Area</th><th>Tests</th><th>Pass</th><th>Fail</th><th>Skip</th><th>Verdict</th><th>What it checks</th></tr></thead>
<tbody>{''.join(sum_rows)}</tbody></table></div>
{''.join(sections)}
<p class="sub">Per-test detail: <code>detail-report/</code> (ExtentReports per class). Area findings above are inlined from each report's markdown. Report-mode areas stay green by default; set the matching <code>STRICT_*</code> flag to gate on violations.</p>
</div></body></html>"""

os.makedirs(os.path.dirname(OUT) or ".", exist_ok=True)
open(OUT, "w", encoding="utf-8").write(htmlout)
print("suite-report: wrote %s (%d areas, %d cases: %d pass / %d fail / %d skip)"
      % (OUT, len(sum_rows), ttot, tp, tf, ts))
