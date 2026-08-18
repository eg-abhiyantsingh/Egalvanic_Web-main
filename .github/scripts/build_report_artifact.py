#!/usr/bin/env python3
"""
Render ONE QA report (.md) into a polished, self-contained Artifact page.

Reuses the GFM converter from md-to-jira-html.py for the body, then wraps it in a
theme-aware shell (steel-blue accent, cool neutrals, status chips) matching the QA
Review Board — so each report can be published directly as an Artifact, screenshots
embedded inline. Output: docs/report-artifacts/<name>.html (gitignored, regenerable).

    python3 .github/scripts/build_report_artifact.py docs/bug-reports/<report>.md
"""
import os, re, sys, html, importlib.util

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SCRIPTS = os.path.join(ROOT, ".github", "scripts")
OUTDIR = os.path.join(ROOT, "docs", "report-artifacts")

# import the existing converter (convert(md, base_dir, stats) -> html body with data-URI images)
spec = importlib.util.spec_from_file_location("mdconv", os.path.join(SCRIPTS, "md-to-jira-html.py"))
mdconv = importlib.util.module_from_spec(spec); spec.loader.exec_module(mdconv)

def classify(title, body):
    t = title + " " + body
    tags = []
    if re.search(r"security|IDOR|cross-tenant|tenancy", title, re.I): tags.append(("SECURITY","crit"))
    if re.search(r"\bdefect\b|❌|\bFAIL\b|silently|breach|zeroed|\$0\.00", t): tags.append(("DEFECT","crit"))
    if "✅" in t or re.search(r"\bPASS\b", t): tags.append(("PASS","ok"))
    if "🟡" in t or re.search(r"\bPARTIAL\b", t): tags.append(("PARTIAL","warn"))
    if "⚠️" in t or re.search(r"BLOCKED|not constructible|not testable|DB-only|unconfirmed", t): tags.append(("BLOCKED","muted"))
    seen=set(); out=[]
    for n,c in tags:
        if n not in seen: seen.add(n); out.append((n,c))
    return out or [("REVIEW","muted")]

def build(md_path):
    body_md = open(md_path, encoding="utf-8").read()
    h1 = re.search(r"^#\s+(.+)$", body_md, re.M)
    raw_title = h1.group(1) if h1 else os.path.basename(md_path)
    title = re.sub(r"[*`_]", "", raw_title).strip()
    # short name for <title>: first clause before a — / · / :
    name = re.split(r"\s[—·]\s|:\s", title)[0][:48].strip() or "QA Report"
    tags = classify(title, body_md)
    chips = "".join(f'<span class="chip {c}">{html.escape(n)}</span>' for n,c in tags)

    stats = {"embedded": [], "missing": []}
    # drop the leading H1 (shown in the banner) so it isn't repeated in the body
    body_no_h1 = re.sub(r"^#\s+.+?(\n|$)", "", body_md, count=1)
    body_html = mdconv.convert(body_no_h1, os.path.dirname(md_path), stats)

    os.makedirs(OUTDIR, exist_ok=True)
    out = os.path.join(OUTDIR, os.path.splitext(os.path.basename(md_path))[0] + ".html")
    doc = f"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(name)}</title>
<style>
:root{{--ground:#eef1f6;--surface:#fff;--ink:#18202e;--dim:#5c6675;--line:#dfe4ec;--accent:#2f6fb0;
--code:#f4f6fa;--ok:#1f8b4c;--warn:#b0771a;--crit:#c23a2e;--muted:#7a8595;
--ok-bg:#e7f4ec;--warn-bg:#faf1de;--crit-bg:#fbe9e7;--muted-bg:#eef1f5;}}
@media (prefers-color-scheme: dark){{:root:not([data-theme="light"]){{--ground:#0e1218;--surface:#161c25;
--ink:#e7ecf3;--dim:#9aa6b5;--line:#28313d;--accent:#5b9bd8;--code:#12171f;--ok:#43b871;--warn:#d69a3c;
--crit:#e2685c;--muted:#8592a3;--ok-bg:#122a1d;--warn-bg:#2c2411;--crit-bg:#2f1815;--muted-bg:#1c232e;}}}}
:root[data-theme="dark"]{{--ground:#0e1218;--surface:#161c25;--ink:#e7ecf3;--dim:#9aa6b5;--line:#28313d;
--accent:#5b9bd8;--code:#12171f;--ok:#43b871;--warn:#d69a3c;--crit:#e2685c;--muted:#8592a3;
--ok-bg:#122a1d;--warn-bg:#2c2411;--crit-bg:#2f1815;--muted-bg:#1c232e;}}
*{{box-sizing:border-box}}
body{{margin:0;background:var(--ground);color:var(--ink);
font:16px/1.62 ui-sans-serif,-apple-system,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased}}
.sheet{{max-width:820px;margin:0 auto;padding:0 22px 90px}}
.banner{{border-bottom:1px solid var(--line);padding:30px 0 22px;margin-bottom:26px}}
.banner .chips{{display:flex;gap:6px;flex-wrap:wrap;margin-bottom:12px}}
.chip{{font-size:11px;font-weight:700;letter-spacing:.05em;padding:3px 9px;border-radius:20px;text-transform:uppercase}}
.chip.ok{{background:var(--ok-bg);color:var(--ok)}}.chip.warn{{background:var(--warn-bg);color:var(--warn)}}
.chip.crit{{background:var(--crit-bg);color:var(--crit)}}.chip.muted{{background:var(--muted-bg);color:var(--muted)}}
h1{{font-size:27px;line-height:1.2;letter-spacing:-.015em;margin:0;text-wrap:balance}}
h2{{font-size:19px;letter-spacing:-.01em;margin:34px 0 12px;padding-bottom:6px;border-bottom:1px solid var(--line)}}
h3{{font-size:15.5px;margin:22px 0 8px;color:var(--ink)}}
p,li{{max-width:72ch}}
a{{color:var(--accent)}}
code{{background:var(--code);padding:1px 6px;border-radius:5px;font:13.5px ui-monospace,"SF Mono",Menlo,monospace}}
pre{{background:var(--code);border:1px solid var(--line);border-radius:9px;padding:13px 15px;overflow:auto}}
pre code{{background:none;padding:0}}
table{{border-collapse:collapse;width:100%;margin:14px 0;font-size:14.5px;display:block;overflow-x:auto}}
th,td{{border:1px solid var(--line);padding:8px 11px;text-align:left;vertical-align:top}}
th{{background:var(--code);font-weight:650}}
tr:nth-child(even) td{{background:color-mix(in srgb,var(--code) 55%,transparent)}}
blockquote{{border-left:3px solid var(--accent);margin:14px 0;padding:2px 16px;color:var(--dim);background:color-mix(in srgb,var(--accent) 5%,transparent)}}
img{{max-width:100%;height:auto;border:1px solid var(--line);border-radius:9px;margin:14px 0;cursor:zoom-in;display:block}}
hr{{border:none;border-top:1px solid var(--line);margin:26px 0}}
.foot{{margin-top:44px;padding-top:14px;border-top:1px dashed var(--line);color:var(--dim);
font:12px ui-monospace,Menlo,monospace}}
#lb{{position:fixed;inset:0;background:rgba(8,12,20,.92);display:none;align-items:center;justify-content:center;z-index:99;padding:24px;cursor:zoom-out}}
#lb.on{{display:flex}}#lb img{{max-width:96vw;max-height:92vh;border:0;cursor:zoom-out}}
@media(prefers-reduced-motion:reduce){{*{{transition:none!important}}}}
</style></head><body>
<main class="sheet">
<div class="banner"><div class="chips">{chips}</div><h1>{html.escape(title)}</h1></div>
{body_html}
<div class="foot">{html.escape(os.path.basename(md_path))} · rendered for review · {len(stats['embedded'])} screenshot(s) embedded</div>
</main>
<div id="lb"><img alt=""></div>
<script>
const lb=document.getElementById('lb'),lbi=lb.querySelector('img');
document.querySelectorAll('main img').forEach(im=>im.addEventListener('click',()=>{{lbi.src=im.src;lb.classList.add('on');}}));
lb.addEventListener('click',()=>lb.classList.remove('on'));
document.addEventListener('keydown',e=>{{if(e.key==='Escape')lb.classList.remove('on');}});
</script></body></html>"""
    open(out,"w",encoding="utf-8").write(doc)
    print(f"built {out}  ({os.path.getsize(out)/1024:.0f} KB, {len(stats['embedded'])} screenshots)")
    if stats["missing"]: print("  missing images:", ", ".join(stats["missing"]))
    return out

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: build_report_artifact.py <report.md>"); sys.exit(1)
    build(sys.argv[1])
