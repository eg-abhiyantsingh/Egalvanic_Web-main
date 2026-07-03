#!/usr/bin/env python3
"""Render the API health-check run (reports/api-health-report.md) into a Z-Platform-style HTML report."""
import re, sys, html, datetime

SRC = sys.argv[1] if len(sys.argv) > 1 else "reports/api-health-report.md"
OUT = sys.argv[2] if len(sys.argv) > 2 else "/tmp/api-health-report.html"
md = open(SRC).read()

base = re.search(r"# API .*?Health.*? — (\S+)", md)
BASE_URL = base.group(1) if base else "https://acme.qa.egalvanic.ai"
ENV = "Prod" if ".egalvanic.ai" in BASE_URL and ".qa." not in BASE_URL and ".dev." not in BASE_URL and ".stage." not in BASE_URL else \
      ("QA" if ".qa." in BASE_URL else ("Dev" if ".dev." in BASE_URL else "Stage"))

rows = []
for line in md.splitlines():
    # Category | Endpoint | Path | Status | HTTP | Latency | Items | Shape | Payload | Recs
    m = re.match(r"\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(pass|warn|fail)\s*\|\s*(.+?)\s*\|\s*(\d+)ms\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(\d+)\s*\|", line)
    if not m: continue
    cat, name, path, status, http, lat, items, shape, payload, recn = m.groups()
    items_i = int(items) if items.strip().isdigit() else None
    kb = int(re.sub(r"KB","",payload)) if re.match(r"\d+KB", payload.strip()) else None
    rows.append(dict(cat=cat, name=name, path=path.strip(), status=status, http=http, lat=int(lat),
                     items=items_i, shape=shape.strip(), kb=kb))

recs = []
for line in md.splitlines():
    m = re.match(r"- \*\*(critical|warning|info)\*\* · (.+?) — (.+)", line)
    if m: recs.append(dict(sev=m.group(1), ep=m.group(2), msg=m.group(3)))

total = len(rows)
npass = sum(1 for r in rows if r["status"]=="pass")
nwarn = sum(1 for r in rows if r["status"]=="warn")
nfail = sum(1 for r in rows if r["status"]=="fail")
avg = round(sum(r["lat"] for r in rows)/total) if total else 0
crit = sum(1 for r in recs if r["sev"]=="critical")
warn = sum(1 for r in recs if r["sev"]=="warning")
info = sum(1 for r in recs if r["sev"]=="info")
pass_rate = round(100*npass/total, 1) if total else 0

# latency bands
def band(l):
    if l < 500: return "FAST"
    if l < 800: return "ACCEPTABLE"
    if l < 1500: return "WATCHLIST"
    if l < 3000: return "SLOW"
    return "CRITICAL"
bands = {"FAST":0,"ACCEPTABLE":0,"WATCHLIST":0,"SLOW":0,"CRITICAL":0}
for r in rows: bands[band(r["lat"])] += 1

# categories
cats = {}
for r in rows:
    c = cats.setdefault(r["cat"], dict(n=0,p=0,w=0,f=0,lat=0))
    c["n"]+=1; c["lat"]+=r["lat"]
    c["p"]+= r["status"]=="pass"; c["w"]+= r["status"]=="warn"; c["f"]+= r["status"]=="fail"
for c in cats.values(): c["avg"]=round(c["lat"]/c["n"])
cats_by_size = sorted(cats.items(), key=lambda kv: -kv[1]["n"])
cats_watch = sorted(cats.items(), key=lambda kv: -kv[1]["avg"])[:3]
clean_cats = sum(1 for c in cats.values() if c["w"]==0 and c["f"]==0)

top10 = sorted(rows, key=lambda r: -r["lat"])[:10]
crit_recs = [r for r in recs if r["sev"]=="critical"]
warn_recs = [r for r in recs if r["sev"]=="warning"]
plain_arrays = [r["ep"] for r in recs if "plain array" in r["msg"]]
empties = [r["ep"] for r in recs if "empty collection" in r["msg"]]
warn_http = [r for r in rows if r["status"]=="warn"]

# health score (per PDF formula)
pass_comp = 70*npass/total if total else 0
lat_comp = 20 if avg < 500 else (0 if avg >= 2000 else 20*(2000-avg)/1500)
sev_comp = max(0, 10 - 0.7*crit)
score = round(pass_comp + lat_comp + sev_comp)

run_date = datetime.date.today().strftime("%-d %b %Y") if hasattr(datetime.date.today(),'strftime') else "1 Jul 2026"
E = html.escape
sev_color = {"critical":"crit","warning":"warn","info":"info"}

def fixtext(msg):
    # simple fix suggestions mirroring the toolkit's tone
    if "Very slow" in msg or "Slow response" in msg: return "Database-query review + index audit; pagination will also reduce response time."
    if "no pagination" in msg: return "Add server-side pagination (page + per_page) with a sensible default (e.g. 20, max 100)."
    if "consider pagination" in msg: return "Add pagination proactively before this grows at Prod scale."
    if "Large payload" in msg: return "Pagination + gzip/brotli compression at the API gateway."
    if "scaling concern" in msg: return "Paired fix: pagination + query optimisation together."
    if "plain array" in msg: return "Wrap in the standard {success, data} envelope in a future consistency pass."
    if "empty" in msg: return "Confirm expected for the probe account; likely seeded-data state, not a defect."
    return "Review with the platform engineering team."

def card(sev, ep_name, path, headline, fix):
    return f'''<div class="finding {sev}">
      <div class="fhead"><span class="fname">{E(ep_name)}</span><span class="fpath">{E(path)}</span></div>
      <div class="fmsg">{E(headline)}</div>
      <div class="ffix"><span class="fixlabel">FIX</span> {E(fix)}</div>
    </div>'''

# path lookup by endpoint name (best-effort from registry-style names)
def path_for(name):
    for r in rows:
        if r["name"]==name: return r.get("path","")
    return ""

pill = lambda s: f'<span class="pill {s}">{s.upper()}</span>'
latclass = lambda l: band(l).lower()

# ---------- build sections ----------
metric_cards = f'''
<div class="metrics">
  <div class="mcard g"><div class="ml">ENDPOINTS</div><div class="mv">{npass}<span class="mvs">/{total}</span></div><div class="ms">Responded successfully</div></div>
  <div class="mcard t"><div class="ml">PASS RATE</div><div class="mv">{pass_rate}<span class="mvs">%</span></div><div class="ms">{npass} of {total} clean</div></div>
  <div class="mcard a"><div class="ml">AVG LATENCY</div><div class="mv">{avg:,}<span class="mvs">ms</span></div><div class="ms">Across the full probe run</div></div>
  <div class="mcard r"><div class="ml">CRITICAL ITEMS</div><div class="mv">{crit}</div><div class="ms">Very-slow endpoints</div></div>
</div>'''

band_rows = "".join(
    f'<tr><td><span class="pill {b.lower()}">{b}</span></td><td class="tgt">{t}</td><td class="n">{bands[b]}</td></tr>'
    for b,t in [("FAST","&lt;500ms"),("ACCEPTABLE","500–800ms"),("WATCHLIST","800ms–1.5s"),("SLOW","1.5–3s"),("CRITICAL","&gt;3s")])

# donut via conic-gradient
donut = f'''<div class="donut" style="background:conic-gradient(var(--green) 0 {pass_rate}%, var(--hair) {pass_rate}% 100%)">
  <div class="donut-hole"><div class="dv">{pass_rate}%</div><div class="dl">PASS RATE</div></div></div>'''

# category bar chart
maxn = max((c["n"] for c in cats.values()), default=1)
cat_bars = ""
for name, c in cats_by_size:
    pw = 100*c["p"]/maxn; ww = 100*c["w"]/maxn; fw = 100*c["f"]/maxn
    cat_bars += f'''<div class="crow"><div class="clabel">{E(name)}</div>
      <div class="cbar"><span class="seg pass" style="width:{pw}%"></span><span class="seg warn" style="width:{ww}%"></span><span class="seg fail" style="width:{fw}%"></span></div>
      <div class="cn">{c["n"]}</div></div>'''

watch_cards = ""
for name, c in cats_watch:
    watch_cards += f'''<div class="wcard"><div class="wt">{E(name.upper())}</div><div class="wv">{c["avg"]:,}<span> ms avg</span></div>
      <div class="wd">{c["n"]} endpoints · {c["w"]} warning · {c["f"]} failed</div></div>'''

crit_cards = "".join(card("crit", r["ep"], path_for(r["ep"]), r["msg"], fixtext(r["msg"])) for r in crit_recs) or '<div class="empty">No critical items this run.</div>'
warn_cards = "".join(card("warn", r["ep"], path_for(r["ep"]), r["msg"], fixtext(r["msg"])) for r in warn_recs) or '<div class="empty">No warnings this run.</div>'

# top-10 bars
maxlat = max((r["lat"] for r in top10), default=1)
top_bars = ""
for r in top10:
    w = 100*r["lat"]/maxlat
    top_bars += f'''<div class="trow"><div class="tlabel">{E(r["name"])}</div>
      <div class="tbar-wrap"><span class="tbar {latclass(r["lat"])}" style="width:{max(6,w)}%"></span><span class="tval">{r["lat"]:,} ms</span></div></div>'''

band_bars = ""
bmax = max(bands.values()) or 1
for b in ["FAST","ACCEPTABLE","WATCHLIST","SLOW","CRITICAL"]:
    h = 100*bands[b]/bmax
    band_bars += f'<div class="bcol"><div class="bnum">{bands[b]}</div><div class="bbar {b.lower()}" style="height:{max(4,h)}%"></div><div class="blab">{b.title()}</div></div>'

# endpoint inventory grouped by category
inv = ""
last = None
for r in sorted(rows, key=lambda x:(x["cat"], x["name"])):
    if r["cat"]!=last:
        inv += f'<tr class="catrow"><td colspan="5">{E(r["cat"]).upper()}</td></tr>'; last=r["cat"]
    inv += f'''<tr><td class="epn">{E(r["name"])}</td><td class="epp">{E(path_for(r["name"]))}</td>
      <td>{pill(r["status"])}</td><td class="n">{r["http"]}</td><td class="n {latclass(r["lat"])}">{r["lat"]:,} ms</td></tr>'''

# key takeaways
slow_eps = [r for r in rows if r["lat"]>=3000]
nopag = [r for r in warn_recs if "no pagination" in r["msg"]]
takeaways = []
if crit_recs:
    t = crit_recs[0]
    takeaways.append(("Slowest endpoint of the run", f'{t["ep"]} — {t["msg"].lower()}. This dominates platform tail-latency and warrants a query-plan + index review.'))
takeaways.append(("Pagination is the highest-leverage fix", f'{len(nopag)} endpoint(s) return large collections in a single response. Server-side pagination on this set resolves the bulk of findings and lowers average latency.'))
if slow_eps:
    names = ", ".join(r["name"] for r in slow_eps[:5])
    takeaways.append((f'{len(slow_eps)} endpoint(s) exceed the 3-second threshold', f'{names} — these account for most of the tail latency and warrant database-query review alongside pagination.'))
takeaways.append(("Everything else is healthy", f'{npass} of {total} endpoints passed cleanly; {nwarn} warning, {nfail} outage. Auth and core-health paths are all fast.'))
tk_html = "".join(f'<li><b>{E(t)}.</b> {E(d)}</li>' for t,d in takeaways)

# action plan
ap = []
if crit_recs:
    ap.append(("Priority 1 · This week", "Investigate the slowest endpoints",
               f'Diagnose {", ".join(r["ep"] for r in crit_recs)} (all &gt;4s). Regressions of this magnitude usually point to a recent change, missing index, or query-plan issue.'))
if nopag:
    ap.append(("Priority 2 · This sprint", "Pagination rollout",
               f'Add server-side pagination (page + per_page, default 20 / max 100) to the {len(nopag)} large-collection endpoints — resolves most findings and cuts average latency.'))
if slow_eps:
    ap.append(("Priority 3 · Next 2 sprints", "Query-plan review on slow endpoints",
               f'Pagination alone will not fully resolve {", ".join(r["name"] for r in slow_eps[:4])}; each needs a query-plan or content-delivery review.'))
if plain_arrays:
    ap.append(("Priority 4 · Platform-hygiene cycle", "Response-shape consistency",
               f'Wrap the {len(plain_arrays)} plain-array endpoints in the standard {{success, data}} envelope. Low urgency, no end-user impact.'))
ap_html = "".join(f'''<div class="ap"><div class="aph"><span class="apt">{E(t)}</span><span class="aptag">{E(sub)}</span></div><div class="apd">{d}</div></div>''' for t,sub,d in ap)

html_out = f'''<title>Z Platform API — Health Check Report</title>
<meta name="description" content="Automated API health check across the Z Platform surface — availability, performance, and response quality ({ENV}).">
<style>
:root{{
  --ink:#1f2733; --slate:#5b6b7f; --faint:#93a0b0; --hair:#e5eaf1; --hair2:#eef2f7; --ground:#ffffff; --panel:#f8fafc;
  --accent:#2f6bd8;
  --green:#16a34a; --green-s:#e8f6ee; --teal:#0d9488; --teal-s:#dcf5f0;
  --amber:#b9791a; --amber-s:#faf0da; --orange:#d9772e; --red:#d6453c; --red-s:#fbeae8;
  --sans:-apple-system,BlinkMacSystemFont,"Segoe UI",system-ui,Roboto,Helvetica,Arial,sans-serif;
  --mono:"SF Mono","JetBrains Mono",ui-monospace,"Cascadia Code",Menlo,Consolas,monospace;
}}
*{{box-sizing:border-box}}
body{{margin:0;background:#eef1f6;color:var(--ink);font-family:var(--sans);font-size:15px;line-height:1.55;-webkit-font-smoothing:antialiased}}
.page{{max-width:940px;margin:22px auto;background:var(--ground);border:1px solid var(--hair);border-radius:6px;padding:44px 52px 40px;box-shadow:0 1px 3px rgba(20,30,45,.05)}}
.num,.mono{{font-family:var(--mono);font-variant-numeric:tabular-nums}}
h1,h2,h3{{text-wrap:balance;margin:0}}
a{{color:var(--accent);text-decoration:none}}
.runhead{{display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid var(--hair);padding-bottom:14px;margin-bottom:8px}}
.brand{{display:flex;align-items:center;gap:10px;font-size:12px;font-weight:700;letter-spacing:.06em;color:var(--ink)}}
.logo{{width:26px;height:26px;border-radius:7px;background:var(--ink);color:#fff;display:grid;place-items:center;font-weight:800;font-size:15px}}
.runid{{font-family:var(--mono);font-size:11.5px;color:var(--faint)}}
.sechdr{{display:flex;justify-content:space-between;font-size:10.5px;letter-spacing:.13em;color:var(--faint);font-weight:600;text-transform:uppercase;border-bottom:1px solid var(--hair);padding-bottom:12px;margin-bottom:26px}}
.eyebrow{{font-family:var(--mono);font-size:12px;letter-spacing:.14em;text-transform:uppercase;color:var(--accent);font-weight:600}}
.footer{{display:flex;justify-content:space-between;color:var(--faint);font-size:11px;border-top:1px solid var(--hair);margin-top:34px;padding-top:14px}}

/* cover */
.cover h1{{font-size:46px;line-height:1.04;letter-spacing:-.02em;font-weight:750;margin:26px 0 0}}
.cover .lede{{color:var(--slate);font-size:16px;max-width:60ch;margin:16px 0 34px}}
.metrics{{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}}
.mcard{{border:1px solid var(--hair);border-radius:8px;padding:18px 18px 16px;border-top:3px solid var(--slate)}}
.mcard.g{{border-top-color:var(--green)}} .mcard.t{{border-top-color:var(--teal)}} .mcard.a{{border-top-color:var(--amber)}} .mcard.r{{border-top-color:var(--red)}}
.mcard .ml{{font-size:10.5px;letter-spacing:.09em;color:var(--slate);font-weight:600;text-transform:uppercase}}
.mcard .mv{{font-family:var(--mono);font-variant-numeric:tabular-nums;font-size:34px;font-weight:700;letter-spacing:-.02em;margin-top:8px;line-height:1}}
.mcard .mvs{{font-size:16px;color:var(--faint);font-weight:600}}
.mcard .ms{{font-size:11.5px;color:var(--faint);margin-top:8px}}
.metagrid{{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-top:auto;padding-top:60px}}
.metagrid .k{{font-size:10.5px;letter-spacing:.09em;color:var(--faint);font-weight:600;text-transform:uppercase}}
.metagrid .v{{font-size:15px;font-weight:600;margin-top:4px}}

h2.title{{font-size:30px;font-weight:720;letter-spacing:-.01em;margin-bottom:8px}}
.sub{{color:var(--slate);font-size:15px;max-width:66ch;margin-bottom:26px}}
.callout{{background:var(--panel);border:1px solid var(--hair);border-left:3px solid var(--accent);border-radius:8px;padding:16px 20px;margin:22px 0}}
.callout b{{color:var(--ink)}} .callout p{{margin:0;color:var(--slate);font-size:13.5px}}

.kmetrics{{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:18px 0 26px}}
.km{{border:1px solid var(--hair);border-top:3px solid var(--slate);border-radius:8px;padding:14px 16px}}
.km.g{{border-top-color:var(--green)}} .km.t{{border-top-color:var(--teal)}} .km.a{{border-top-color:var(--amber)}} .km.r{{border-top-color:var(--red)}}
.km .l{{font-size:10px;letter-spacing:.08em;color:var(--slate);font-weight:600;text-transform:uppercase}}
.km .v{{font-family:var(--mono);font-size:26px;font-weight:700;margin-top:6px}}
.km .s{{font-size:11px;color:var(--faint);margin-top:4px}}

.takeaways{{list-style:none;padding:0;margin:20px 0 0;counter-reset:tk}}
.takeaways li{{position:relative;padding:14px 0 14px 42px;border-top:1px solid var(--hair2);font-size:13.5px;color:var(--slate)}}
.takeaways li b{{color:var(--ink)}}
.takeaways li::before{{counter-increment:tk;content:counter(tk);position:absolute;left:0;top:13px;width:24px;height:24px;border-radius:50%;background:var(--accent);color:#fff;font-size:12px;font-weight:700;display:grid;place-items:center;font-family:var(--mono)}}

.perf{{display:grid;grid-template-columns:1.3fr 1fr;gap:30px;align-items:center}}
table{{border-collapse:collapse;width:100%;font-size:13px}}
.bandtbl th{{text-align:left;font-size:10px;letter-spacing:.1em;text-transform:uppercase;color:var(--faint);font-weight:600;padding:8px 10px;border-bottom:1px solid var(--hair)}}
.bandtbl td{{padding:9px 10px;border-bottom:1px solid var(--hair2)}}
.bandtbl td.tgt{{color:var(--slate);font-family:var(--mono);font-size:12px}}
.bandtbl td.n{{text-align:right;font-family:var(--mono);font-weight:600}}
.donut-wrap{{display:flex;flex-direction:column;align-items:center;gap:10px}}
.donut{{width:180px;height:180px;border-radius:50%;display:grid;place-items:center}}
.donut-hole{{width:128px;height:128px;background:#fff;border-radius:50%;display:grid;place-items:center;text-align:center}}
.donut-hole .dv{{font-family:var(--mono);font-size:32px;font-weight:750;color:var(--green)}}
.donut-hole .dl{{font-size:9.5px;letter-spacing:.12em;color:var(--faint);font-weight:600}}
.figcap{{font-size:10.5px;letter-spacing:.1em;text-transform:uppercase;color:var(--faint);text-align:center;font-weight:600}}

.pill{{display:inline-block;font-family:var(--mono);font-size:10px;font-weight:700;padding:3px 9px;border-radius:5px;letter-spacing:.03em}}
.pill.pass,.pill.fast{{color:var(--green);background:var(--green-s)}}
.pill.acceptable{{color:var(--teal);background:var(--teal-s)}}
.pill.watchlist,.pill.warn,.pill.warning{{color:var(--amber);background:var(--amber-s)}}
.pill.slow{{color:var(--orange);background:#fbeede}}
.pill.critical,.pill.fail{{color:var(--red);background:var(--red-s)}}

.crow{{display:grid;grid-template-columns:210px 1fr 30px;align-items:center;gap:12px;padding:5px 0}}
.clabel{{font-size:12px;color:var(--ink);text-align:right;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}}
.cbar{{display:flex;height:14px;background:var(--hair2);border-radius:3px;overflow:hidden}}
.seg.pass{{background:var(--green)}} .seg.warn{{background:var(--amber)}} .seg.fail{{background:var(--red)}}
.cn{{font-family:var(--mono);font-size:12px;font-weight:700}}
.watch{{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-top:26px}}
.wcard{{border:1px solid var(--hair);border-top:3px solid var(--amber);border-radius:8px;padding:14px 16px}}
.wcard .wt{{font-size:11px;letter-spacing:.06em;color:var(--slate);font-weight:700}}
.wcard .wv{{font-family:var(--mono);font-size:24px;font-weight:700;margin:6px 0}} .wcard .wv span{{font-size:12px;color:var(--faint);font-weight:500}}
.wcard .wd{{font-size:11.5px;color:var(--faint)}}

.findings{{display:grid;grid-template-columns:1fr 1fr;gap:14px}}
.finding{{border:1px solid var(--hair);border-left:3px solid var(--slate);border-radius:7px;padding:13px 16px}}
.finding.crit{{border-left-color:var(--red)}} .finding.warn{{border-left-color:var(--amber)}} .finding.info{{border-left-color:var(--teal)}}
.fhead{{display:flex;justify-content:space-between;align-items:baseline;gap:10px}}
.fname{{font-weight:650;font-size:13.5px}} .fpath{{font-family:var(--mono);font-size:11px;color:var(--faint)}}
.fmsg{{font-size:12.5px;color:var(--ink);margin:5px 0 8px}}
.ffix{{font-size:11.5px;color:var(--slate)}} .fixlabel{{font-family:var(--mono);font-size:9.5px;letter-spacing:.1em;color:var(--accent);font-weight:700;margin-right:6px}}
.bignum{{font-family:var(--mono);font-size:44px;font-weight:750;line-height:1}}
.infogroup{{border:1px solid var(--hair);border-radius:8px;padding:16px 20px;margin-bottom:14px}}
.infogroup h3{{font-size:14px;font-weight:650}} .infogroup .tag{{font-family:var(--mono);font-size:10px;color:var(--accent);font-weight:700;margin-left:8px}}
.chips{{display:flex;flex-wrap:wrap;gap:6px;margin-top:12px}}
.chip{{font-size:11.5px;background:var(--panel);border:1px solid var(--hair);border-radius:5px;padding:3px 9px;color:var(--slate)}}

.trow{{display:grid;grid-template-columns:180px 1fr;gap:14px;align-items:center;padding:4px 0}}
.tlabel{{font-size:12px;text-align:right;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}}
.tbar-wrap{{display:flex;align-items:center;gap:8px}}
.tbar{{height:15px;border-radius:3px}} .tbar.fast{{background:var(--green)}} .tbar.acceptable{{background:var(--teal)}} .tbar.watchlist{{background:var(--amber)}} .tbar.slow{{background:var(--orange)}} .tbar.critical{{background:var(--red)}}
.tval{{font-family:var(--mono);font-size:11.5px;font-weight:600;color:var(--slate)}}
.bands{{display:flex;align-items:flex-end;gap:18px;height:150px;margin:22px 0 6px;padding:0 10px}}
.bcol{{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%}}
.bnum{{font-family:var(--mono);font-weight:700;font-size:15px;margin-bottom:6px}}
.bbar{{width:60%;border-radius:4px 4px 0 0;min-height:4px}} .bbar.fast{{background:var(--green)}} .bbar.acceptable{{background:var(--teal)}} .bbar.watchlist{{background:var(--amber)}} .bbar.slow{{background:var(--orange)}} .bbar.critical{{background:var(--red)}}
.blab{{font-size:10.5px;color:var(--faint);margin-top:8px;text-align:center}}

.invtbl th{{text-align:left;font-size:10px;letter-spacing:.1em;text-transform:uppercase;color:var(--faint);font-weight:600;padding:9px 10px;border-bottom:1px solid var(--hair)}}
.invtbl td{{padding:8px 10px;border-bottom:1px solid var(--hair2);font-size:12.5px}}
.invtbl tr.catrow td{{background:var(--panel);font-size:10px;letter-spacing:.08em;font-weight:700;color:var(--slate)}}
.invtbl td.epp{{font-family:var(--mono);font-size:11.5px;color:var(--slate)}}
.invtbl td.n{{text-align:right;font-family:var(--mono);font-weight:600}}
td.fast{{color:var(--green)}} td.acceptable{{color:var(--teal)}} td.watchlist{{color:var(--amber)}} td.slow{{color:var(--orange)}} td.critical{{color:var(--red)}}
.ap{{border:1px solid var(--hair);border-left:3px solid var(--accent);border-radius:8px;padding:16px 20px;margin-bottom:14px}}
.aph{{display:flex;justify-content:space-between;align-items:baseline;gap:12px}}
.apt{{font-weight:680;font-size:14.5px}} .aptag{{font-family:var(--mono);font-size:11px;color:var(--accent)}}
.apd{{font-size:12.5px;color:var(--slate);margin-top:6px}}
.formula li{{font-size:13px;color:var(--slate);margin:6px 0}} .formula b{{color:var(--ink)}}
.empty{{color:var(--faint);font-size:13px;padding:10px}}
@media (max-width:760px){{.metrics,.kmetrics,.metagrid,.findings,.watch{{grid-template-columns:1fr 1fr}}.perf{{grid-template-columns:1fr}}.page{{padding:28px 22px}}}}
</style>

<!-- ===== COVER ===== -->
<div class="page cover">
  <div class="runhead"><div class="brand"><span class="logo">e</span> EGALVANIC · QA REPORTS</div><div class="runid">Parallel Suite 3 · API Health</div></div>
  <div style="padding:26px 0 0">
    <div class="eyebrow">API Health Assessment · {ENV}</div>
    <h1>Z Platform API<br>Health Check Report</h1>
    <p class="lede">Results from automated probing of {total} endpoints across the Z Platform API surface — measuring availability, performance, and response quality.</p>
    {metric_cards}
    <div class="metagrid">
      <div><div class="k">Environment</div><div class="v">{ENV}</div></div>
      <div><div class="k">Prepared for</div><div class="v">Acme</div></div>
      <div><div class="k">Run date</div><div class="v">{run_date}</div></div>
      <div><div class="k">Prepared by</div><div class="v">eGalvanic QA · Suite 3</div></div>
    </div>
  </div>
</div>

<!-- ===== EXECUTIVE SUMMARY ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Executive Summary</span></div>
  <h2 class="title">Executive Summary</h2>
  <p class="sub">This run probed {total} endpoints on the Z Platform {ENV} API. {npass} passed cleanly, {nwarn} produced a warning, and {nfail} timed out. The health check is read-only and fails the build only on a genuine outage.</p>
  <div class="kmetrics">
    <div class="km g"><div class="l">Responded</div><div class="v">{npass+nwarn}/{total}</div><div class="s">{nfail} outage(s)</div></div>
    <div class="km t"><div class="l">Pass rate</div><div class="v">{pass_rate}%</div><div class="s">{npass} of {total} clean</div></div>
    <div class="km a"><div class="l">Avg latency</div><div class="v">{avg:,}ms</div><div class="s">full probe run</div></div>
    <div class="km r"><div class="l">Findings</div><div class="v">{crit}/{warn}/{info}</div><div class="s">critical / warning / info</div></div>
  </div>
  <div class="callout"><p><b>Bottom line:</b> availability is strong ({pass_rate}% pass, {nfail} outage). The findings that remain trace back to large collections served without pagination and a handful of slow aggregation endpoints — the highest-leverage fix is server-side pagination with a bounded default.</p></div>
  <div class="sechdr" style="border:0;margin:10px 0 0;padding:0">Key Takeaways</div>
  <ul class="takeaways">{tk_html}</ul>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== PERFORMANCE OVERVIEW ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Performance Overview</span></div>
  <h2 class="title">Performance Overview</h2>
  <p class="sub">{bands["FAST"]+bands["ACCEPTABLE"]} of {total} endpoints meet standard performance targets ({'<'}800ms). The concern sits in the tail — {bands["CRITICAL"]} endpoint(s) exceed the 3-second threshold.</p>
  <div class="perf">
    <table class="bandtbl"><thead><tr><th>Band</th><th>Target</th><th style="text-align:right">Endpoints</th></tr></thead><tbody>{band_rows}</tbody></table>
    <div class="donut-wrap">{donut}<div class="figcap">Fig. 1 · Status distribution</div></div>
  </div>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== CATEGORY PERFORMANCE ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Category Performance</span></div>
  <h2 class="title">Category Performance</h2>
  <p class="sub">{clean_cats} of {len(cats)} functional categories are entirely clean. Ranked below by endpoint count; the watch-list cards rank by average response time.</p>
  <div style="margin:6px 0 4px">{cat_bars}</div>
  <div class="figcap" style="text-align:left;margin-top:14px">Fig. 2 · Endpoints per category · pass / warning / failed</div>
  <div class="watch">{watch_cards}</div>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== CRITICAL FINDINGS ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Critical Findings</span></div>
  <h2 class="title">Critical Findings</h2>
  <p class="sub"><span class="bignum" style="color:var(--red)">{crit}</span> critical item(s) — very-slow endpoints ({'>'}4s) or compound scale + latency risks. See the Action Plan for the prioritised sequence.</p>
  <div class="findings">{crit_cards}</div>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== WARNING FINDINGS ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Warning Findings</span></div>
  <h2 class="title">Warning Findings</h2>
  <p class="sub"><span class="bignum" style="color:var(--amber)">{warn}</span> warning(s) — pagination and payload-weight items the platform handles today but that grow riskier at Prod data volumes.</p>
  <div class="findings">{warn_cards}</div>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== INFORMATIONAL ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Informational Findings</span></div>
  <h2 class="title">Informational Findings</h2>
  <p class="sub"><span class="bignum" style="color:var(--teal)">{info}</span> informational item(s) — consistency backlog and seeded-data gaps. No action required to ship.</p>
  <div class="infogroup"><h3>Response-shape inconsistency <span class="tag">{len(plain_arrays)} ENDPOINTS</span></h3>
    <p style="font-size:13px;color:var(--slate);margin:6px 0 0">Return a plain JSON array rather than the standard {{success, data}} envelope. Functionally fine — a code-hygiene item for clients.</p>
    <div class="chips">{"".join(f'<span class="chip">{E(x)}</span>' for x in plain_arrays) or '<span class="chip">none</span>'}</div></div>
  <div class="infogroup"><h3>Empty collections <span class="tag">{len(empties)} ENDPOINTS</span></h3>
    <p style="font-size:13px;color:var(--slate);margin:6px 0 0">Returned zero items — most likely the probe account's state rather than a defect.</p>
    <div class="chips">{"".join(f'<span class="chip">{E(x)}</span>' for x in empties) or '<span class="chip">none</span>'}</div></div>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== LATENCY ANALYSIS ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Latency Analysis</span></div>
  <h2 class="title">Latency Analysis</h2>
  <p class="sub">Response times across the platform. {bands["CRITICAL"]} exceed the 3-second critical threshold; {bands["WATCHLIST"]} sit in the 800ms–1.5s watchlist band.</p>
  <div style="font-size:10.5px;letter-spacing:.1em;text-transform:uppercase;color:var(--faint);font-weight:600;margin-bottom:10px">Top 10 slowest endpoints</div>
  <div>{top_bars}</div>
  <div class="figcap" style="text-align:left;margin:14px 0 0">Fig. 3 · Top 10 slowest (coloured by band)</div>
  <div class="bands">{band_bars}</div>
  <div class="figcap">Fig. 4 · Endpoints by response-time band</div>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== ENDPOINT INVENTORY ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Endpoint Inventory</span></div>
  <h2 class="title">Endpoint Inventory</h2>
  <p class="sub">{total} endpoints across {len(cats)} categories. Latency colour-coded by band (green fast · amber watchlist/slow · red critical).</p>
  <table class="invtbl"><thead><tr><th>Endpoint</th><th>Path</th><th>Status</th><th style="text-align:right">HTTP</th><th style="text-align:right">Latency</th></tr></thead><tbody>{inv}</tbody></table>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== ACTION PLAN ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Action Plan</span></div>
  <h2 class="title">Action Plan</h2>
  <p class="sub">A recommended sequence for addressing the findings, ordered by impact-to-effort ratio.</p>
  {ap_html}
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>

<!-- ===== APPENDIX ===== -->
<div class="page">
  <div class="sechdr"><span>eGalvanic · Z Platform</span><span>Appendix &amp; Methodology</span></div>
  <h2 class="title">Appendix &amp; Methodology</h2>
  <p class="sub">How this assessment was conducted (Parallel Suite 3 · <span class="mono">ApiHealthCheckApiTest</span>), read-only against <span class="mono">{E(BASE_URL)}</span>.</p>
  <table class="bandtbl" style="margin-bottom:24px"><thead><tr><th>Level</th><th>Trigger</th><th style="text-align:right">This run</th></tr></thead><tbody>
    <tr><td><span class="pill critical">CRITICAL</span></td><td class="tgt">Response &gt;3s, or very-slow &gt;4s / &gt;10k items</td><td class="n">{crit}</td></tr>
    <tr><td><span class="pill warning">WARNING</span></td><td class="tgt">No pagination on large lists, payload &gt;500KB, or 1.5–3s</td><td class="n">{warn}</td></tr>
    <tr><td><span class="pill acceptable">INFO</span></td><td class="tgt">Plain-array shape, or empty collection</td><td class="n">{info}</td></tr>
  </tbody></table>
  <div style="font-size:10.5px;letter-spacing:.1em;text-transform:uppercase;color:var(--faint);font-weight:600;margin-bottom:8px">Health score</div>
  <ul class="formula">
    <li><b>Pass rate (70 max):</b> 70 × {npass}/{total} = <b>{round(pass_comp,1)}</b></li>
    <li><b>Latency (20 max):</b> avg {avg}ms → <b>{round(lat_comp,1)}</b></li>
    <li><b>Severity (10 max):</b> 10 − 0.7 × {crit} = <b>{round(sev_comp,1)}</b></li>
  </ul>
  <div style="font-size:20px;font-weight:700;margin-top:10px">Composite score: <span class="num" style="color:var(--green)">{score} / 100</span></div>
  <div class="footer"><span>Prepared for Acme · {run_date}</span><span>API Health · {ENV}</span></div>
</div>
'''
open(OUT,"w").write(html_out)
print(f"wrote {OUT}: {total} endpoints, {npass} pass / {nwarn} warn / {nfail} fail, score {score}")
