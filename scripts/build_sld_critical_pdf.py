#!/usr/bin/env python3
"""Build a self-contained HTML (base64 screenshots) for the SLD v3 critical-findings
report, then convert to PDF via headless Chrome. Run:  python3 scripts/build_sld_critical_pdf.py
"""
import base64, os, html

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOT = os.path.join(ROOT, "docs", "bug-hunts")

def img(relpath):
    p = os.path.join(SHOT, relpath)
    if not os.path.exists(p):
        return f'<div class="missing">[missing screenshot: {html.escape(relpath)}]</div>'
    with open(p, "rb") as f:
        b64 = base64.b64encode(f.read()).decode()
    return f'<img src="data:image/png;base64,{b64}" />'

def sev(label, cls):
    return f'<span class="sev {cls}">{label}</span>'

def steps(items):
    lis = "".join(f"<li>{s}</li>" for s in items)
    return f"<ol class='steps'>{lis}</ol>"

# ---- document sections -------------------------------------------------------
SECTIONS = []

SECTIONS.append(("cover", f"""
<div class="cover">
  <h1>eGalvanic SLD v3 — Critical Bug Findings</h1>
  <p class="sub">Release-gate sign-off · deep interactive bug hunt</p>
  <div class="retract">
    <b>⚠️ RETRACTED / RE-TRIAGED after product-owner review (2026-06-10).</b> On re-examination the
    findings below do <b>not</b> hold up as product defects:
    <b>CRIT-1</b> is withdrawn (nodes connect via bus-attachment — <i>Add to… / Box / Source-Target</i> —
    not free edges; <code>allowLink=false</code> is by-design); <b>Export "no-op"</b> is inconclusive
    (automated download capture is unreliable; it likely works manually); the
    <b>S1/S2/S6 "data-integrity" items are test-data quality, not bugs</b> (rule: "empty/0 data is not a
    bug"); the interactive items are by-design/cosmetic. The only result that stands is the
    <b>negative</b> one: stored-XSS does not execute and all SLD read+write endpoints enforce 401 on
    both hosts (no BOLA). This document is kept for the API map + security verification only.
  </div>
  <table class="meta">
    <tr><td>Date</td><td>2026-06-10</td></tr>
    <tr><td>App under test</td><td>https://acme.qa.egalvanic.ai/slds (v1.21, GoJS diagram engine)</td></tr>
    <tr><td>Tester</td><td>Claude Code (Opus), driven by abhiyant.singh@egalvanic.com</td></tr>
    <tr><td>Method</td><td>Live Playwright (non-headless) + GoJS-model introspection + parallel
        unauthenticated API sweep (8 agents) + direct curl auth probes</td></tr>
    <tr><td>Scope</td><td>Create / move / delete / edit / export of SLD diagrams; three-layer
        integrity (UI / browser-local / backend); security (XSS, auth/BOLA)</td></tr>
  </table>
  <div class="verdict">
    <b>Verdict (post re-triage):</b> <b>no major SLD defects confirmed.</b> The headline items were
    withdrawn (see banner). What stands is the <b>negative</b> security verification — stored-XSS does
    not execute and all SLD read+write endpoints enforce 401 on both hosts (no BOLA). Remaining
    observations are test-data quality or by-design, not release blockers.
  </div>
</div>
"""))

# Scoreboard
SECTIONS.append(("scoreboard", f"""
<h2>1 &nbsp; Critical / HIGH scoreboard</h2>
<table class="score">
  <tr><th>ID</th><th>Severity</th><th>Status</th><th>Summary</th></tr>
  <tr><td>CRIT-1</td><td>{sev('HIGH','hi')}</td><td>NEW</td><td>Web "+ Asset" creates orphan nodes; no way to draw edges (live S1 cause)</td></tr>
  <tr><td>SLD-BUG-15</td><td>{sev('HIGH','hi')}</td><td>confirmed</td><td>Export button does nothing (release blocker)</td></tr>
  <tr><td>SLD-BUG-14</td><td>{sev('HIGH','hi')}</td><td>confirmed</td><td>Diagram double-mounted &rarr; 2&times; render + duplicate fetches</td></tr>
  <tr><td>BUG-G S1/S2/S4/S6</td><td>{sev('HIGH','hi')}</td><td>confirmed</td><td>Orphans / default-coord pile-up / unclassified edges / soft-delete leak (87/107 SLDs)</td></tr>
  <tr><td>SLD-BUG-09</td><td>{sev('HIGH','hi')}</td><td>confirmed</td><td>Wild Goose: 490 nodes / 0 rendered edges</td></tr>
  <tr><td>SLD-BUG-17..20</td><td>{sev('MED','me')}</td><td>confirmed</td><td>Delete "cannot be undone" vs Undo; dead Delete-key; drag moves neighbor; a11y focus-trap</td></tr>
  <tr><td>Stored &lt;script&gt; exec</td><td>{sev('SAFE','sa')}</td><td>tested</td><td>Does NOT execute (React-escaped)</td></tr>
  <tr><td>Unauth API / BOLA</td><td>{sev('SAFE','sa')}</td><td>tested</td><td>All SLD read+write endpoints 401 on both hosts</td></tr>
</table>
"""))

# CRIT-1
SECTIONS.append(("crit1", f"""
<h2>2 &nbsp; CRIT-1 {sev('HIGH','hi')} {sev('NEW','new')} — Web "+ Asset" creates structurally-orphan nodes</h2>
<p>The web SLD editor can <b>add</b> and <b>move</b> nodes, but <b>cannot draw edges</b>
(<code>diagram.allowLink === false</code>, and there is no connect / link tool anywhere in the UI).
So <b>every node created in the web is born with 0 connections, and there is no in-web way to ever
connect it.</b> This is the live, current-code root cause of systemic bug <b>S1</b> (orphan nodes,
82 of 107 SLDs) &mdash; not merely a legacy migration artifact.</p>

<h3>Steps to reproduce</h3>
{steps([
  "Open <code>https://acme.qa.egalvanic.ai/slds</code> and pick site <b>gyu</b> in the <i>Site</i> selector.",
  "Click the red <b>Lock Graph</b> FAB (bottom-right) once to <b>unlock</b> editing.",
  "In the top toolbar click <b>+ Asset</b> &rarr; the <b>Select Node Type</b> palette opens (Fig. 2.1).",
  "Click any type, e.g. <b>Fuse</b> &rarr; a banner shows <b>&ldquo;Click to place asset: Fuse&rdquo;</b> (Fig. 2.2).",
  "Click an empty area of the canvas &rarr; a node <b>&ldquo;Fuse-3&rdquo;</b> is created and persisted (<code>POST https://eg-pz.qa.egalvanic.ai/api/node/create</code> &rarr; <b>201</b>).",
  "Inspect the new node&rsquo;s connections &mdash; it has <b>0 links</b> (Fig. 2.3), and there is <b>no tool to connect it</b> to anything.",
])}
<p class="ea"><b>Expected:</b> a newly created device can be wired into the single-line diagram (the whole
point of an SLD). &nbsp; <b>Actual:</b> the node is permanently isolated; the web offers no edge-drawing
affordance, and the on-canvas <b>&ldquo;No issues&rdquo;</b> badge does not flag the disconnected node.</p>
<p class="ea"><b>Impact (HIGH):</b> every web-created node becomes an orphan &rarr; directly produces the
S1 connectivity-loss cluster. A user &ldquo;building&rdquo; an SLD in the web ends up with a pile of
unconnected symbols. <i>(The node does land at the clicked coordinate, not (100,100) &mdash; so this is
NOT the cause of S2&rsquo;s pile-up, which comes from mobile/import. The create also logs an
<code>iOS bridge not available for handler: graphUpdate</code> dead-call.)</i></p>

<figure>{img('critical-2026-06-10/01-after-add-asset-click.png')}<figcaption>Fig. 2.1 — +Asset opens the &ldquo;Select Node Type&rdquo; palette (Impedance / Protection / Source / Load / Bus).</figcaption></figure>
<figure>{img('critical-2026-06-10/02-after-select-fuse-type.png')}<figcaption>Fig. 2.2 — &ldquo;Click to place asset: Fuse&rdquo; placement mode.</figcaption></figure>
<figure>{img('critical-2026-06-10/03-created-orphan-fuse3.png')}<figcaption>Fig. 2.3 — the created node &ldquo;Fuse-3&rdquo; (bottom-left, selected) is fully disconnected: 0 edges, and no way to connect it.</figcaption></figure>
"""))

# CRIT-2 Export
SECTIONS.append(("crit2", f"""
<h2>3 &nbsp; CRIT-2 / SLD-BUG-15 {sev('HIGH','hi')} — Export is a silent no-op (release-gate blocker)</h2>
<h3>Steps to reproduce</h3>
{steps([
  "Open any SLD (e.g. gyu).",
  "Click the blue <b>Export</b> button (bottom-left of the canvas).",
  "Observe the result.",
])}
<p class="ea"><b>Expected:</b> an exported PDF / PNG / JSON of the diagram (explicit v3 release-gate capability).
&nbsp; <b>Actual:</b> <b>nothing happens</b> &mdash; no file download, no format menu, no dialog, no network
request, and no console log or error. Reproduced across 3 attempts.</p>
<p class="ea"><b>Impact:</b> the &ldquo;export diagrams&rdquo; feature is effectively non-functional and gives the
user zero feedback.</p>
"""))

# Live S1 + double mount
SECTIONS.append(("s1", f"""
<h2>4 &nbsp; SLD-BUG-09 / S1 {sev('HIGH','hi')} — 490 nodes render with 0 edges (connectivity lost)</h2>
<p>Switching to <b>(s) Wild Goose Brewery</b> renders <b>490 nodes and 0 edges</b> &mdash; the single-line
diagram shows no connections at all. The backend keeps only 5 &ldquo;active&rdquo; edges and all 5 are orphans
(their endpoints reference deleted nodes), so GoJS draws none. This also exposes <b>SLD-BUG-14</b>: the page
mounts <b>two</b> GoJS diagrams (one in a <code>display:none</code> 0&times;0 container) and <b>both</b> fully
load all 490 nodes &rarr; 2&times; render cost + the duplicate-fetch storm.</p>
<h3>Steps to reproduce</h3>
{steps([
  "Open <code>/slds</code>; in the <i>Site</i> selector choose <b>(s) Wild Goose Brewery</b>.",
  "Wait ~8s for the diagram to load.",
  "Observe: hundreds of device boxes, <b>no connecting lines</b> between them (Fig. 4.1).",
  "Introspect the GoJS model: <code>diagram.nodes.count</code> = 490, <code>diagram.links.count</code> = 0; and <code>G.Diagram.fromDiv</code> returns <b>two</b> diagrams both with 490 nodes.",
])}
<figure>{img('sld-switch-stale-canvas.png')}<figcaption>Fig. 4.1 — Wild Goose: 490 nodes, 0 edges. The right-hand panel lists real electrical issues, but the topology is gone.</figcaption></figure>
"""))

# Overlap / S2 / toolbar
SECTIONS.append(("s2", f"""
<h2>5 &nbsp; SLD-BUG-01/06 / S2+S7 {sev('MED','me')} — Default-coordinate pile-up &amp; label overlap</h2>
<p>On the small <b>gyu</b> SLD, 4 of 6 nodes sit at exactly <code>(100,100)</code> and render stacked on
top of each other; bus-group labels overlap and become unreadable, yet the badge still says
<b>&ldquo;No issues&rdquo;</b> (validation checks electrical data, not layout). With the v3 view toggles on
(MiniMap, Trace Lineage, Edge Labels, Status Badges) the overlap is vivid (Fig. 5.1).</p>
<h3>Steps to reproduce</h3>
{steps([
  "Open <code>/slds</code> &rarr; site <b>gyu</b>.",
  "Enable the toolbar toggles: <b>Show MiniMap</b>, <b>Trace Lineage</b>, <b>Show Edge Labels</b>, <b>Show Status Badges</b>.",
  "Observe the overlapping bus group &ldquo;dosconnect switch test&rdquo; colliding with &ldquo;pole 3&rdquo; / the fuse, while the badge reads &ldquo;No issues&rdquo;.",
])}
<figure>{img('sld-toolbar-features.png')}<figcaption>Fig. 5.1 — v3 view features active; overlapping/garbled bus labels while the badge claims &ldquo;No issues&rdquo;.</figcaption></figure>
"""))

# Edit affordances + delete + XSS-safe
SECTIONS.append(("edit", f"""
<h2>6 &nbsp; Edit affordances, delete semantics, and the stored-XSS check</h2>
<p>Unlocking + selecting a node reveals an editing toolbar (<b>Undo / Redo / Refresh / Copy / Add to&hellip; /
+ Asset / Box / Edit / Delete</b>). Findings:</p>
<ul>
  <li><b>SLD-BUG-17 {sev('MED','me')}</b> — the Delete confirmation says <b>&ldquo;This action cannot be
      undone&rdquo;</b> while the toolbar exposes an <b>Undo</b> button. Delete is a soft-delete
      (<code>POST /api/node/bulk-delete</code> &rarr; 200) whose records keep leaking back through
      <code>/api/sld/{{id}}</code> (= S6) &mdash; contradictory and a data-loss-confusion risk.</li>
  <li><b>SLD-BUG-18 {sev('MED','me')}</b> — the <b>Delete key</b> and <b>right-click</b> do nothing; deletion
      is only possible via the toolbar button.</li>
</ul>
<h3>Stored-XSS test &mdash; {sev('SAFE','sa')} (does NOT execute)</h3>
<p>Wild Goose contains a node literally named <code>&lt;script&gt;alert('XSS')&lt;/script&gt;_1780930307763</code>.
Tested for execution:</p>
{steps([
  "Site selector &rarr; <b>(s) Wild Goose Brewery</b>; wait for load.",
  "Override <code>window.alert</code> and scan the DOM for any live <code>&lt;script&gt;</code> / <code>&lt;img onerror&gt;</code> carrying the payload.",
  "Select the payload node and open <b>Edit</b> &rarr; the <b>Edit Asset</b> dialog renders the name in an <code>&lt;input&gt;</code> (Fig. 6.1).",
  "Result: <code>alert</code> never fires; 0 injected scripts; canvas is plain GoJS text.",
])}
<p class="ea"><b>Verdict:</b> <b>not exploitable</b> as stored XSS (React default escaping + input-value
rendering). Residual is data-hygiene only: the field stores raw <code>&lt;script&gt;</code> (no write-time
sanitization). <b>Do not file as XSS.</b></p>
<figure>{img('critical-2026-06-10/04-xss-node-edit-panel.png')}<figcaption>Fig. 6.1 — the &lt;script&gt; payload sits inertly in the Edit Asset <b>Name input field</b> (escaped, not executed).</figcaption></figure>
"""))

# Security verified
SECTIONS.append(("sec", f"""
<h2>7 &nbsp; Security — VERIFIED SAFE (no unauthenticated data exposure / BOLA)</h2>
<p>A parallel unauthenticated sweep (8-agent workflow) plus direct <code>curl</code> with <b>no token/cookie</b>
against the real SLD endpoints on <b>both</b> API hosts:</p>
<table class="score">
  <tr><th>Endpoint</th><th>Method</th><th>Unauthenticated result</th></tr>
  <tr><td>acme&hellip;/api/sld/{{realId}}</td><td>GET</td><td>{sev('401','sa')} &ldquo;No authorization provided&rdquo;</td></tr>
  <tr><td>acme&hellip;/api/lookup/nodes/{{realId}}</td><td>GET</td><td>{sev('401','sa')}</td></tr>
  <tr><td>acme&hellip;/api/users/{{realId}}/slds</td><td>GET</td><td>{sev('401','sa')}</td></tr>
  <tr><td>acme&hellip;/api/node/update/*, edge/update/*</td><td>PUT</td><td>{sev('401','sa')}</td></tr>
  <tr><td>eg-pz&hellip;/api/node/create, /node/update/*, /node/bulk-delete</td><td>POST/PUT</td><td>{sev('401','sa')} &ldquo;Missing or invalid authorization header&rdquo;</td></tr>
</table>
<p class="ea"><b>Verdict:</b> SLD read <b>and</b> write endpoints correctly reject unauthenticated requests on
both hosts &mdash; <b>no BOLA / data-exposure</b>. The only quirk: unmatched <code>/api/*</code> paths return
<b>200 with the SPA index.html</b> instead of a 404 JSON (the same SPA-fallback routing behind BUG-F&rsquo;s
<code>JSON.parse</code> error storms).</p>
"""))

body = "\n".join(s for _, s in SECTIONS)

HTML = f"""<!DOCTYPE html><html><head><meta charset="utf-8"><style>
@page {{ size: A4; margin: 16mm 14mm; }}
* {{ box-sizing: border-box; }}
body {{ font-family: -apple-system, "Helvetica Neue", Arial, sans-serif; color:#1a1a1a; font-size:11px; line-height:1.5; }}
h1 {{ font-size:26px; margin:0 0 4px; color:#0b3d63; }}
h2 {{ font-size:16px; margin:22px 0 8px; color:#0b3d63; border-bottom:2px solid #0b3d63; padding-bottom:3px; page-break-after:avoid; }}
h3 {{ font-size:12.5px; margin:12px 0 4px; color:#333; }}
.sub {{ color:#666; font-size:13px; margin:0 0 14px; }}
code {{ background:#f1f3f5; padding:1px 4px; border-radius:3px; font-family:"SF Mono",Menlo,monospace; font-size:10px; }}
table {{ border-collapse:collapse; width:100%; margin:8px 0; }}
table.meta td {{ border:1px solid #dde; padding:5px 8px; vertical-align:top; }}
table.meta td:first-child {{ width:130px; font-weight:600; background:#f6f8fa; }}
table.score th {{ background:#0b3d63; color:#fff; text-align:left; padding:5px 8px; font-size:10px; }}
table.score td {{ border:1px solid #dde; padding:5px 8px; }}
.cover {{ page-break-after: always; }}
.verdict {{ margin-top:18px; padding:10px 12px; background:#fff6f6; border-left:4px solid #c0392b; }}
.retract {{ margin:14px 0 4px; padding:11px 13px; background:#fff8e6; border:1.5px solid #e08e0b; border-radius:5px; font-size:10.5px; }}
.sev {{ display:inline-block; font-size:9px; font-weight:700; padding:1px 6px; border-radius:9px; color:#fff; vertical-align:middle; }}
.sev.hi {{ background:#c0392b; }} .sev.me {{ background:#e08e0b; }} .sev.sa {{ background:#1e8449; }} .sev.new {{ background:#0b3d63; }}
ol.steps {{ margin:4px 0 8px 0; padding-left:20px; }}
ol.steps li {{ margin:3px 0; }}
.ea {{ margin:6px 0; }}
figure {{ margin:10px 0 16px; page-break-inside:avoid; }}
figure img {{ width:100%; border:1px solid #cfd6dd; border-radius:4px; }}
figcaption {{ font-size:9.5px; color:#555; margin-top:4px; font-style:italic; }}
.missing {{ color:#c0392b; font-style:italic; }}
ul {{ margin:4px 0 8px; padding-left:18px; }} ul li {{ margin:3px 0; }}
</style></head><body>
{body}
</body></html>"""

out_html = os.path.join(SHOT, "SLD-v3-Critical-Findings.html")
with open(out_html, "w") as f:
    f.write(HTML)
print("wrote", out_html, os.path.getsize(out_html), "bytes")
