#!/usr/bin/env python3
"""
Consolidated DETAILED report generator.

The per-module Detailed Reports (ExtentSpark HTML with INLINE base64 screenshots,
`reports/detail-report/Detailed_Report_<Module>_<ts>.html`) are large (tens of MB),
so they are NOT emailed. This bundles every module's detailed report into ONE
navigable artifact: an index page with a left-nav of modules + an iframe that loads
the selected module's full detailed report (screenshots and all). Intended to be
uploaded as a CI artifact and downloaded — never attached to email.

Usage:
  python3 consolidated-detailed-report.py <input-dir> <output-dir> [--title "..."]
    input-dir : dir to scan recursively for Detailed_Report_*.html
                (CI: all-reports ; local: reports/detail-report)
    output-dir: where to write Consolidated_Detailed_Report.html + modules/

For each module only the NEWEST timestamped report is kept (drops re-run dupes).
"""

import argparse
import glob
import html
import os
import re
import shutil
import sys

NAME_RE = re.compile(r"Detailed_Report_(.+?)_(\d{8}_\d{6})\.html$")


def discover(input_dir):
    """Return {module_display_name: (path, timestamp)} keeping the newest per module."""
    chosen = {}
    for path in glob.glob(os.path.join(input_dir, "**", "Detailed_Report_*.html"), recursive=True):
        base = os.path.basename(path)
        m = NAME_RE.search(base)
        if not m:
            continue
        module_raw, ts = m.group(1), m.group(2)
        # skip obviously-empty/corrupt reports (e.g. a 502 page captured as html)
        try:
            if os.path.getsize(path) < 2048:
                continue
        except OSError:
            continue
        module = module_raw.replace("_", " ").strip()
        prev = chosen.get(module)
        if prev is None or ts > prev[1]:
            chosen[module] = (path, ts)
    return chosen


def safe_filename(module):
    return re.sub(r"[^A-Za-z0-9._-]+", "_", module) + ".html"


def build_index(modules, title, timestamp):
    """modules: list of (display_name, relative_file, size_bytes)."""
    nav_items = []
    for i, (name, rel, size) in enumerate(modules):
        mb = size / (1024 * 1024)
        nav_items.append(
            f'<li><a href="#" data-src="{html.escape(rel)}" '
            f'class="navlink{" active" if i == 0 else ""}" '
            f'onclick="loadModule(this);return false;">{html.escape(name)}'
            f'<span class="sz">{mb:.1f} MB</span></a></li>'
        )
    first_src = html.escape(modules[0][1]) if modules else ""
    total_mb = sum(s for _, _, s in modules) / (1024 * 1024)
    nav_html = "\n".join(nav_items)
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>{html.escape(title)}</title>
<style>
  * {{ box-sizing: border-box; }}
  body {{ margin:0; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif; height:100vh; display:flex; flex-direction:column; }}
  header {{ background:#1f2a44; color:#fff; padding:12px 18px; flex:0 0 auto; }}
  header h1 {{ margin:0; font-size:16px; font-weight:600; }}
  header .meta {{ font-size:12px; opacity:.8; margin-top:3px; }}
  .wrap {{ display:flex; flex:1 1 auto; min-height:0; }}
  nav {{ flex:0 0 280px; background:#f4f5f7; border-right:1px solid #d9dce1; overflow-y:auto; }}
  nav ul {{ list-style:none; margin:0; padding:8px 0; }}
  nav li a {{ display:flex; justify-content:space-between; align-items:center; gap:8px;
              padding:9px 16px; color:#1f2a44; text-decoration:none; font-size:13px; border-left:3px solid transparent; }}
  nav li a:hover {{ background:#e7eaf0; }}
  nav li a.active {{ background:#e0e6f5; border-left-color:#3355cc; font-weight:600; }}
  nav .sz {{ font-size:11px; color:#8a909c; font-weight:400; }}
  main {{ flex:1 1 auto; min-width:0; }}
  iframe {{ width:100%; height:100%; border:0; }}
  .hint {{ padding:6px 16px; font-size:11px; color:#8a909c; border-top:1px solid #d9dce1; }}
</style>
</head>
<body>
<header>
  <h1>{html.escape(title)}</h1>
  <div class="meta">{len(modules)} module report(s) &middot; {total_mb:.1f} MB total &middot; generated {html.escape(timestamp)} &middot; artifact only (not emailed)</div>
</header>
<div class="wrap">
  <nav>
    <ul>
{nav_html}
    </ul>
    <div class="hint">Each module is its full Detailed Report (steps + inline screenshots).</div>
  </nav>
  <main><iframe id="viewer" src="{first_src}" title="module detailed report"></iframe></main>
</div>
<script>
  function loadModule(el) {{
    document.querySelectorAll('.navlink').forEach(function(a){{ a.classList.remove('active'); }});
    el.classList.add('active');
    document.getElementById('viewer').src = el.getAttribute('data-src');
  }}
</script>
</body>
</html>
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input_dir")
    ap.add_argument("output_dir")
    ap.add_argument("--title", default="Consolidated Detailed Report")
    ap.add_argument("--timestamp", default="")
    args = ap.parse_args()

    chosen = discover(args.input_dir)
    if not chosen:
        print(f"[consolidated-detailed] no Detailed_Report_*.html found under {args.input_dir} — nothing to do")
        return 0

    modules_dir = os.path.join(args.output_dir, "modules")
    os.makedirs(modules_dir, exist_ok=True)

    entries = []
    for module in sorted(chosen):
        src, _ts = chosen[module]
        fname = safe_filename(module)
        dst = os.path.join(modules_dir, fname)
        shutil.copyfile(src, dst)
        entries.append((module, f"modules/{fname}", os.path.getsize(dst)))

    ts = args.timestamp or "this run"
    index = build_index(entries, args.title, ts)
    index_path = os.path.join(args.output_dir, "Consolidated_Detailed_Report.html")
    with open(index_path, "w", encoding="utf-8") as fh:
        fh.write(index)

    total_mb = sum(s for _, _, s in entries) / (1024 * 1024)
    print(f"[consolidated-detailed] {len(entries)} module(s), {total_mb:.1f} MB")
    print(f"[consolidated-detailed] wrote {index_path}")
    for name, rel, size in entries:
        print(f"    - {name}: {rel} ({size/1024/1024:.1f} MB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
