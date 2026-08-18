#!/usr/bin/env python3
"""
Build ONE self-contained visual QA review page from docs/bug-reports/*.md.

Why: a folder of 30+ PDFs + a buried 00_INDEX.md is unscannable. This renders every
report as a card — newest day first, a status chip + severity stripe, the one-line
verdict, and its screenshots inline (click to enlarge) — in a single HTML file that
opens in any browser. Screenshots are downscaled (sips) and embedded as data URIs so
the file is fully self-contained (no server, works offline).

Output: docs/qa-review-board.html  (also the source for the published Artifact)
Run:    python3 .github/scripts/build_review_dashboard.py
"""
import os, re, glob, base64, subprocess, tempfile, html, datetime

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REPORTS = os.path.join(ROOT, "docs", "bug-reports")
OUT = os.path.join(ROOT, "docs", "qa-review-board.html")
DATE_RE = re.compile(r"(\d{4}-\d{2}-\d{2})")
MONTHS = ["January","February","March","April","May","June","July","August",
          "September","October","November","December"]

def human(iso):
    y,m,d = map(int, iso.split("-")); return f"{d} {MONTHS[m-1]} {y}"

def strip_md(s):
    s = re.sub(r"`([^`]*)`", r"\1", s)
    s = re.sub(r"\*\*([^*]*)\*\*", r"\1", s)
    s = re.sub(r"\*([^*]*)\*", r"\1", s)
    s = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", s)
    s = re.sub(r"[#>|]", "", s)
    return s.strip()

def data_uri(png_path):
    """Downscale a PNG to a JPEG data URI via macOS sips (keeps the file small)."""
    try:
        tmp = tempfile.NamedTemporaryFile(suffix=".jpg", delete=False).name
        r = subprocess.run(["sips","-Z","1200","-s","format","jpeg","-s","formatOptions","72",
                            png_path,"--out",tmp], capture_output=True, timeout=30)
        src = tmp if (r.returncode==0 and os.path.getsize(tmp)>0) else png_path
        with open(src,"rb") as fh: b = fh.read()
        mime = "image/jpeg" if src==tmp else "image/png"
        if src==tmp: os.unlink(tmp)
        return f"data:{mime};base64,{base64.b64encode(b).decode()}"
    except Exception:
        with open(png_path,"rb") as fh:
            return "data:image/png;base64,"+base64.b64encode(fh.read()).decode()

def classify(title, body):
    t = (title+" "+body)
    tags = []
    if re.search(r"security|IDOR|cross-tenant|tenancy", title, re.I): tags.append(("SECURITY","crit"))
    # a filed defect / failure
    if re.search(r"\bdefect\b|❌|\bFAIL\b|silently|500|leak|breach|zeroed|\$0\.00", t): tags.append(("DEFECT","crit"))
    if "✅" in t or re.search(r"\bPASS\b", t): tags.append(("PASS","ok"))
    if "🟡" in t or re.search(r"\bPARTIAL\b", t): tags.append(("PARTIAL","warn"))
    if "⚠️" in t or re.search(r"BLOCKED|not constructible|not testable|DB-only|unconfirmed", t): tags.append(("BLOCKED","muted"))
    # dedupe preserving order
    seen=set(); out=[]
    for name,cls in tags:
        if name not in seen: seen.add(name); out.append((name,cls))
    if not out: out=[("REVIEW","muted")]
    # primary severity for the stripe
    order=["crit","warn","ok","muted"]
    prim = sorted([c for _,c in out], key=lambda c: order.index(c))[0]
    return out, prim

def verdict(body):
    # text under a "Verdict" heading, else first real paragraph
    m = re.search(r"^#{1,3}\s*Verdict[^\n]*\n(.*?)(?=\n#{1,3}\s|\Z)", body, re.S|re.M|re.I)
    chunk = m.group(1) if m else body
    for line in chunk.split("\n"):
        s = strip_md(line)
        if len(s) > 25 and not s.startswith(("|","---")) and "**Tested" not in line:
            return s[:280]
    return ""

def report_screens(md_path, body):
    shots=[]
    def add(rel):
        for base in (os.path.dirname(md_path), ROOT):
            p=os.path.normpath(os.path.join(base, rel))
            if os.path.exists(p) and p.lower().endswith((".png",".jpg",".jpeg")):
                if p not in shots: shots.append(p)
                return
    for m in re.findall(r"!\[[^\]]*\]\(([^)]+)\)", body): add(m)      # ![](img)
    for m in re.findall(r"\]\(([^)]+\.png)\)", body): add(m)          # ](link.png)
    # any bug-evidence path mentioned in code/prose (reports often cite paths, not images)
    for m in re.findall(r"(?:\.\./|docs/)?bug-evidence/[A-Za-z0-9_./-]+\.png", body):
        add(m if m.startswith(("../","docs/")) else m)
        add("docs/"+m if not m.startswith(("docs/","../")) else m)
    return shots[:8]

def build():
    today = datetime.date.today().isoformat()
    reports=[]
    for md in glob.glob(os.path.join(REPORTS,"*.md")):
        name=os.path.basename(md)
        dm=DATE_RE.search(name)
        if not dm: continue                      # only dated QA reports
        body=open(md,encoding="utf-8").read()
        h1=re.search(r"^#\s+(.+)$", body, re.M)
        title=strip_md(h1.group(1)) if h1 else name
        tags,prim=classify(title,body)
        reports.append({"date":dm.group(1),"title":title,"verdict":verdict(body),
                        "tags":tags,"prim":prim,"shots":report_screens(md,body),"file":name})
    # group by date desc
    days={}
    for r in reports: days.setdefault(r["date"],[]).append(r)

    total=len(reports)
    n_sec=sum(1 for r in reports if any(t[0]=="SECURITY" for t in r["tags"]))
    n_def=sum(1 for r in reports if any(t[0]=="DEFECT" for t in r["tags"]))
    n_pass=sum(1 for r in reports if any(t[0]=="PASS" for t in r["tags"]))
    n_block=sum(1 for r in reports if any(t[0] in ("BLOCKED","PARTIAL") for t in r["tags"]))
    n_shots=sum(len(r["shots"]) for r in reports)

    STRIPE={"crit":"var(--crit)","warn":"var(--warn)","ok":"var(--ok)","muted":"var(--muted)"}
    CHIP={"crit":"crit","warn":"warn","ok":"ok","muted":"muted"}

    def card(r):
        chips="".join(f'<span class="chip {CHIP[c]}">{html.escape(n)}</span>' for n,c in r["tags"])
        shots=""
        if r["shots"]:
            imgs="".join(f'<img loading="lazy" src="{data_uri(p)}" alt="{html.escape(os.path.basename(p))}" '
                         f'data-full="1" tabindex="0">' for p in r["shots"])
            shots=f'<div class="shots">{imgs}</div>'
        else:
            shots='<div class="noshot">No screenshots (API / bundle verification)</div>'
        v=f'<p class="verdict">{html.escape(r["verdict"])}</p>' if r["verdict"] else ""
        return (f'<article class="card" style="--stripe:{STRIPE[r["prim"]]}">'
                f'<div class="chead"><h3>{html.escape(r["title"])}</h3><div class="chips">{chips}</div></div>'
                f'{v}{shots}'
                f'<div class="cfile">{html.escape(r["file"])}</div></article>')

    sections=""
    for d in sorted(days, reverse=True):
        star=' <span class="today">TODAY</span>' if d==today else ""
        cards="".join(card(r) for r in sorted(days[d], key=lambda r:r["title"]))
        sections+=(f'<section class="day"><h2>{human(d)}{star}<span class="count">{len(days[d])}</span></h2>'
                   f'<div class="grid">{cards}</div></section>')

    HTMLDOC=f"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>QA Review Board</title>
<style>
:root{{
  --ground:#eef1f6; --surface:#ffffff; --surface2:#f6f8fb; --ink:#18202e; --dim:#5c6675;
  --line:#dfe4ec; --accent:#2f6fb0;
  --ok:#1f8b4c; --warn:#b0771a; --crit:#c23a2e; --muted:#7a8595;
  --ok-bg:#e7f4ec; --warn-bg:#faf1de; --crit-bg:#fbe9e7; --muted-bg:#eef1f5;
}}
@media (prefers-color-scheme: dark){{ :root:not([data-theme="light"]){{
  --ground:#0e1218; --surface:#161c25; --surface2:#1b222c; --ink:#e7ecf3; --dim:#9aa6b5;
  --line:#28313d; --accent:#5b9bd8;
  --ok:#43b871; --warn:#d69a3c; --crit:#e2685c; --muted:#8592a3;
  --ok-bg:#122a1d; --warn-bg:#2c2411; --crit-bg:#2f1815; --muted-bg:#1c232e;
}}}}
:root[data-theme="dark"]{{
  --ground:#0e1218; --surface:#161c25; --surface2:#1b222c; --ink:#e7ecf3; --dim:#9aa6b5;
  --line:#28313d; --accent:#5b9bd8;
  --ok:#43b871; --warn:#d69a3c; --crit:#e2685c; --muted:#8592a3;
  --ok-bg:#122a1d; --warn-bg:#2c2411; --crit-bg:#2f1815; --muted-bg:#1c232e;
}}
*{{box-sizing:border-box}}
body{{margin:0;background:var(--ground);color:var(--ink);
  font:15px/1.55 ui-sans-serif,-apple-system,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;
  -webkit-font-smoothing:antialiased}}
.wrap{{max-width:1160px;margin:0 auto;padding:0 20px 80px}}
header{{position:sticky;top:0;z-index:20;background:color-mix(in srgb,var(--ground) 88%,transparent);
  backdrop-filter:blur(8px);border-bottom:1px solid var(--line);padding:16px 0 14px;margin-bottom:26px}}
.hrow{{max-width:1160px;margin:0 auto;padding:0 20px;display:flex;flex-wrap:wrap;align-items:baseline;gap:12px}}
h1{{font-size:20px;font-weight:700;letter-spacing:-.01em;margin:0}}
.sub{{color:var(--dim);font-size:13px}}
.tiles{{max-width:1160px;margin:12px auto 0;padding:0 20px;display:grid;
  grid-template-columns:repeat(5,1fr);gap:10px}}
.tile{{background:var(--surface);border:1px solid var(--line);border-radius:10px;padding:11px 13px}}
.tile .n{{font-size:24px;font-weight:700;font-variant-numeric:tabular-nums;letter-spacing:-.02em}}
.tile .l{{font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:var(--dim);margin-top:2px}}
.tile.crit .n{{color:var(--crit)}} .tile.ok .n{{color:var(--ok)}} .tile.warn .n{{color:var(--warn)}}
.day{{margin-top:30px}}
.day>h2{{display:flex;align-items:center;gap:10px;font-size:14px;text-transform:uppercase;
  letter-spacing:.07em;color:var(--dim);font-weight:700;margin:0 0 12px;
  border-bottom:1px solid var(--line);padding-bottom:8px}}
.today{{background:var(--accent);color:#fff;font-size:10px;letter-spacing:.08em;
  padding:2px 7px;border-radius:20px}}
.count{{margin-left:auto;color:var(--dim);font-weight:600}}
.grid{{display:grid;grid-template-columns:1fr;gap:14px}}
.card{{background:var(--surface);border:1px solid var(--line);border-left:4px solid var(--stripe);
  border-radius:12px;padding:15px 17px;box-shadow:0 1px 2px rgba(20,30,50,.04)}}
.chead{{display:flex;flex-wrap:wrap;align-items:baseline;gap:10px;justify-content:space-between}}
.chead h3{{font-size:16px;font-weight:650;margin:0;letter-spacing:-.01em;text-wrap:balance;flex:1 1 60%}}
.chips{{display:flex;gap:6px;flex-wrap:wrap}}
.chip{{font-size:10.5px;font-weight:700;letter-spacing:.05em;padding:3px 8px;border-radius:20px;
  text-transform:uppercase}}
.chip.ok{{background:var(--ok-bg);color:var(--ok)}} .chip.warn{{background:var(--warn-bg);color:var(--warn)}}
.chip.crit{{background:var(--crit-bg);color:var(--crit)}} .chip.muted{{background:var(--muted-bg);color:var(--muted)}}
.verdict{{color:var(--ink);font-size:13.5px;margin:10px 0 0;max-width:80ch}}
.shots{{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}}
.shots img{{height:96px;width:auto;max-width:200px;object-fit:cover;border:1px solid var(--line);
  border-radius:7px;cursor:zoom-in;background:var(--surface2);transition:transform .12s}}
.shots img:hover,.shots img:focus{{transform:scale(1.03);outline:2px solid var(--accent);outline-offset:2px}}
.noshot{{margin-top:10px;font-size:12px;color:var(--muted);font-style:italic}}
.cfile{{margin-top:11px;font:11.5px ui-monospace,"SF Mono",Menlo,monospace;color:var(--dim);
  border-top:1px dashed var(--line);padding-top:8px}}
/* lightbox */
#lb{{position:fixed;inset:0;background:rgba(8,12,20,.92);display:none;align-items:center;
  justify-content:center;z-index:100;padding:24px;cursor:zoom-out}}
#lb.on{{display:flex}} #lb img{{max-width:96vw;max-height:92vh;border-radius:8px;box-shadow:0 20px 60px rgba(0,0,0,.5)}}
@media(max-width:720px){{.tiles{{grid-template-columns:repeat(2,1fr)}} .chead h3{{flex-basis:100%}}}}
@media(prefers-reduced-motion:reduce){{*{{transition:none!important}}}}
</style></head>
<body>
<header>
  <div class="hrow"><h1>QA Review Board</h1>
    <span class="sub">eGalvanic Web · manual ticket QA · as of {human(today)} · newest first</span></div>
  <div class="tiles">
    <div class="tile"><div class="n">{total}</div><div class="l">Reports</div></div>
    <div class="tile ok"><div class="n">{n_pass}</div><div class="l">With passes</div></div>
    <div class="tile crit"><div class="n">{n_def}</div><div class="l">Defects</div></div>
    <div class="tile warn"><div class="n">{n_block}</div><div class="l">Partial / blocked</div></div>
    <div class="tile crit"><div class="n">{n_sec}</div><div class="l">Security</div></div>
  </div>
</header>
<main class="wrap">
{sections}
</main>
<div id="lb"><img alt=""></div>
<script>
const lb=document.getElementById('lb'), lbi=lb.querySelector('img');
document.querySelectorAll('.shots img').forEach(im=>{{
  const open=()=>{{lbi.src=im.src;lb.classList.add('on');}};
  im.addEventListener('click',open);
  im.addEventListener('keydown',e=>{{if(e.key==='Enter')open();}});
}});
lb.addEventListener('click',()=>lb.classList.remove('on'));
document.addEventListener('keydown',e=>{{if(e.key==='Escape')lb.classList.remove('on');}});
</script>
</body></html>"""
    open(OUT,"w",encoding="utf-8").write(HTMLDOC)
    kb=os.path.getsize(OUT)/1024
    print(f"built {OUT}  ({kb:.0f} KB, {total} reports, {n_shots} screenshots embedded)")

if __name__=="__main__":
    build()
