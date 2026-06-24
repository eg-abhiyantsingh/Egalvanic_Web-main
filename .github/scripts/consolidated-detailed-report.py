#!/usr/bin/env python3
"""
Consolidated DETAILED report generator.

The per-module Detailed Reports (ExtentSpark HTML with INLINE base64 screenshots,
`reports/detail-report/Detailed_Report_<Module>_<ts>.html`) are large (tens of MB),
so they are NOT emailed. This bundles every module's detailed report. Two outputs,
both written to <output-dir> (and so both captured by the CI artifact upload):

  1. Consolidated_Detailed_Report.html  — navigable index: left-nav of modules +
     an iframe that loads the selected module (one at a time). Lighter to open.

  2. Consolidated_Detailed_Report_SingleFile.html  — ONE self-contained file with
     EVERY module stacked vertically and FLATTENED (Spark's app-shell chrome and
     internal scrolling removed, every test expanded). Open it and the whole run is
     one long scrollable page — so a single full-page screenshot / "Save as PDF"
     captures everything. Each module is embedded as an auto-resizing srcdoc iframe,
     so modules keep their own (sandboxed) styling and nothing is re-parsed or lost.

Usage:
  python3 consolidated-detailed-report.py <input-dir> <output-dir> [--title "..."]
    input-dir : dir to scan recursively for Detailed_Report_*.html
                (CI: all-reports ; local: reports/detail-report)
    output-dir: where to write the two HTML files (+ modules/ for the nav index)

Dedup model — IMPORTANT for the parallel suites:
  The same module name (e.g. "Asset Management") is produced by MANY parallel CI
  jobs — every asset group AND all 5 engineering suites call createTest("Asset
  Management", ...), each in its own JVM, each emitting its own
  Detailed_Report_Asset_Management_<ts>.html into its own artifact
  (all-reports/reports-s2-<group>/...). Those reports hold DIFFERENT tests, so they
  must NOT collapse into one. We therefore key by (group, module) — group taken from
  the artifact subdir — keeping the newest report per (group, module) (still drops
  genuine same-job re-run dupes). When a module name comes from >1 group, its display
  name is suffixed with the group so every group's tests are kept AND distinguishable.
  Run directly against a flat dir (local single run) → group is "" and behavior is the
  old "newest per module" exactly.
"""

import argparse
import glob
import html
import os
import re
import shutil
import sys

NAME_RE = re.compile(r"Detailed_Report_(.+?)_(\d{8}_\d{6})\.html$")

# Injected into each module's HTML (inside its srcdoc iframe) to FLATTEN the Spark
# SPA into a plain scrollable document.
#
# Ground truth of the Spark "detailed" layout (verified by walking the DOM):
#   .test-wrapper.row          (display:flex — left list + right viewer)
#     .test-list (~395px)
#       .test-list-wrapper.ps-container
#         ul.test-list-item
#           li.test-item              <- ONE per test (always in the DOM)
#             .test-detail            <- the summary row (name / time / status badge)
#             .test-contents.d-none   <- the FULL detail, hidden via Bootstrap d-none
#               .detail-body          <- step table + inline screenshots
#     .test-content (ps-container)    <- right-hand VIEWER; Spark clones the active
#                                        test here on click. Balloons when unset.
# So we (a) HIDE the viewer pane, (b) un-flex the wrapper + widen the list to full
# width, and (c) force every li.test-item's .test-contents/.detail-body visible
# (override d-none). !important beats Spark's classes even after its JS runs on load.
FLATTEN_SHIM = """
<style id="__cons_flatten">
  /* hide the app-shell chrome AND the right-hand viewer pane (the ps-container that
     balloons). All real per-test content lives in the .test-list items below. */
  .header, .navbar, .side-nav, .vheader, .nav-logo, .search-box, .search-input,
  .nav-right, .nav-left, .test-list-tools, .nav-tabs, .back-to-test,
  .test-content, .test-content-tools, .test-content-detail,
  .view:not(.test-view), .dashboard-view, .category-view, .exception-view,
  .fa-fast-forward, [class*="fast-forward"] { display: none !important; }
  /* let the shell + list flow to natural height with no internal scroll, full width */
  html, body, .app, .layout, .vcontainer, .main-content, .content, .test-wrapper,
  .test-list, .test-list-wrapper, .test-list-item, .ps, .ps-container, .ps-content, .scrollable {
    height: auto !important; max-height: none !important; min-height: 0 !important;
    overflow: visible !important; position: static !important; transform: none !important;
    float: none !important; width: auto !important; max-width: none !important;
  }
  /* un-flex the two-column row so the test list spans the full width */
  .test-wrapper { display: block !important; }
  .test-list, .test-list-wrapper, ul.test-list-item, .test-list-item { flex: none !important; }
  body { background: #1b2233 !important; }
  .test-list, .test-list-wrapper { padding: 0 !important; margin: 0 !important; border: 0 !important; }
  ul.test-list-item, .test-list ul, .test-list ol { list-style: none !important; margin: 0 !important; padding: 0 !important; }
  /* every test = a full-width stacked block */
  li.test-item, .test-item { display: block !important; width: auto !important; float: none !important;
    list-style: none !important; border: 0 !important; border-bottom: 1px solid #33405e !important;
    padding: 16px 22px !important; margin: 0 !important; cursor: default !important; }
  /* force the (normally d-none) full detail of every test visible */
  .test-item .test-detail { display: block !important; }
  .test-item .test-contents, .test-item .test-contents.d-none { display: block !important; }
  .test-item .detail-body { display: block !important; height: auto !important;
    max-height: none !important; overflow: visible !important; opacity: 1 !important; }
  /* screenshots: cap width but never clip */
  .r-img, .screen-img, .media-container img, .detail-body img, img {
    max-width: 960px !important; height: auto !important; }
</style>
<script id="__cons_resize">
(function () {
  function expand() {
    try { if (typeof toggleView === 'function') toggleView('test-view'); } catch (e) {}
    // un-collapse every test: mark active + strip the d-none on its detail wrapper
    document.querySelectorAll('.test-item').forEach(function (it) { it.classList.add('active'); });
    document.querySelectorAll('.test-item .test-contents').forEach(function (c) { c.classList.remove('d-none'); });
  }
  function contentHeight() {
    // Measure the real content extent (the test-wrapper that holds the visible list),
    // NOT body.scrollHeight — the latter includes phantom space from Spark's hidden
    // perfect-scrollbar elements and overestimates, leaving big empty gaps.
    var tw = document.querySelector('.test-wrapper');
    var h = tw ? tw.getBoundingClientRect().height : 0;
    if (h < 40 && document.body) h = document.body.scrollHeight;
    return Math.ceil(h);
  }
  function postHeight() {
    var h = contentHeight();
    // (1) set our own iframe height directly — works whenever the parent is
    //     same-origin (HTTP serve, or opening the file directly), so the page is
    //     correctly sized even before the parent's JS runs / if postMessage is
    //     missed. Guarded: cross-origin access throws and we fall back to (2).
    try { if (window.frameElement && h > 40) window.frameElement.style.height = (h + 28) + 'px'; } catch (e) {}
    // (2) post the height so the parent can size us when (1) is blocked (cross-origin).
    try { parent.postMessage({ __consHeight: true, h: h }, '*'); } catch (e) {}
  }
  function init() {
    expand();
    postHeight();
    [120, 400, 900, 1800].forEach(function (ms) { setTimeout(function () { expand(); postHeight(); }, ms); });
    try { new ResizeObserver(postHeight).observe(document.body); } catch (e) {}
    window.addEventListener('load', function () { expand(); postHeight(); });
  }
  if (document.readyState !== 'loading') init();
  else document.addEventListener('DOMContentLoaded', init);
})();
</script>
"""


def group_from_relpath(rel):
    """CI lays each parallel job's artifact in its own subdir:
       all-reports/reports-s2-<group>/reports/detail-report/Detailed_Report_*.html
    Use that first path component to tell apart same-module reports from DIFFERENT
    groups (different tests). Strip the artifact-name prefix to a clean group label.
    Returns "" when the report sits directly in input-dir (local single-run use)."""
    parts = rel.replace("\\", "/").split("/")
    if len(parts) < 2:
        return ""  # file is directly in input-dir → no group context
    top = parts[0]
    for pref in ("reports-s2-", "reports-s1-", "reports-"):
        if top.startswith(pref):
            return top[len(pref):]
    return top


def discover(input_dir):
    """Return a list of entry dicts {module, group, display, path, ts}, keeping the
    newest report per (group, module). Distinct parallel groups that share a module
    name are ALL kept (and their display name disambiguated by group) instead of
    collapsing to a single newest report."""
    chosen = {}  # (group, module) -> (path, ts)
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
        group = group_from_relpath(os.path.relpath(path, input_dir))
        key = (group, module)
        prev = chosen.get(key)
        if prev is None or ts > prev[1]:
            chosen[key] = (path, ts)

    # A module name is "ambiguous" when produced by more than one group — only then
    # do we suffix the display name (keeps single-group modules clean).
    groups_per_module = {}
    for (group, module) in chosen:
        groups_per_module.setdefault(module, set()).add(group)

    entries = []
    for (group, module), (path, ts) in chosen.items():
        ambiguous = len(groups_per_module.get(module, ())) > 1
        display = f"{module} · {group}" if (ambiguous and group) else module
        entries.append({"module": module, "group": group, "display": display,
                        "path": path, "ts": ts})
    # readable, stable order: by module, then group
    entries.sort(key=lambda e: (e["module"].lower(), e["group"].lower()))
    return entries


def safe_filename(module):
    return re.sub(r"[^A-Za-z0-9._-]+", "_", module) + ".html"


def anchor_id(module):
    return "mod-" + re.sub(r"[^A-Za-z0-9]+", "-", module).strip("-").lower()


def inject_shim(module_html):
    """Append the flatten shim just before </body> (case-insensitive) or at the end."""
    idx = module_html.lower().rfind("</body>")
    if idx == -1:
        return module_html + FLATTEN_SHIM
    return module_html[:idx] + FLATTEN_SHIM + module_html[idx:]


def srcdoc_escape(s):
    """Escape an HTML document for use as a double-quoted srcdoc attribute value."""
    return s.replace("&", "&amp;").replace('"', "&quot;")


def build_index(modules, title, timestamp):
    """Navigable index (one module at a time via iframe). modules: (name, rel, size)."""
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
  header .meta {{ font-size:12px; opacity:.85; margin-top:3px; }}
  header .meta a {{ color:#9ec1ff; }}
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
  <div class="meta">{len(modules)} module report(s) &middot; {total_mb:.1f} MB total &middot; generated {html.escape(timestamp)} &middot;
    want everything on one screenshot? open <a href="Consolidated_Detailed_Report_SingleFile.html">the single-file version</a></div>
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


def build_single_file(modules, title, timestamp):
    """ONE self-contained file: every module flattened + stacked vertically.
    modules: list of (display_name, module_html_string, size_bytes)."""
    total_mb = sum(s for _, _, s in modules) / (1024 * 1024)

    jump_links = "\n".join(
        f'<a class="jump" href="#{anchor_id(name)}">{html.escape(name)}'
        f'<span class="sz">{size/1024/1024:.1f} MB</span></a>'
        for name, _h, size in modules
    )

    sections = []
    for name, mod_html, _size in modules:
        srcdoc = srcdoc_escape(inject_shim(mod_html))
        sections.append(
            f"""  <section class="module" id="{anchor_id(name)}">
    <h2 class="modtitle">{html.escape(name)} <a class="top" href="#top">&uarr; top</a></h2>
    <iframe class="modframe" loading="eager" scrolling="no"
            title="{html.escape(name)} detailed report" srcdoc="{srcdoc}"></iframe>
  </section>"""
        )
    sections_html = "\n".join(sections)

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>{html.escape(title)} — single file</title>
<style>
  * {{ box-sizing: border-box; }}
  body {{ margin:0; background:#141a29; color:#e7ebf3;
          font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif; }}
  #top header {{ background:#1f2a44; padding:16px 22px; }}
  #top h1 {{ margin:0; font-size:18px; font-weight:600; }}
  #top .meta {{ font-size:12.5px; opacity:.85; margin-top:4px; }}
  .toc {{ display:flex; flex-wrap:wrap; gap:8px; padding:14px 22px; background:#192136; border-bottom:1px solid #2a3550; }}
  .toc .lbl {{ width:100%; font-size:11px; letter-spacing:.05em; text-transform:uppercase; color:#8a96b3; margin-bottom:2px; }}
  a.jump {{ display:inline-flex; align-items:center; gap:6px; padding:6px 12px; border-radius:6px;
            background:#243152; color:#cfe0ff; text-decoration:none; font-size:13px; border:1px solid #33446e; }}
  a.jump:hover {{ background:#2e3f6a; }}
  a.jump .sz {{ font-size:10.5px; color:#8fa3cf; }}
  section.module {{ border-bottom:3px solid #0c1018; }}
  .modtitle {{ position:sticky; top:0; z-index:5; margin:0; padding:11px 22px; font-size:16px; font-weight:600;
               background:#26324f; color:#fff; border-top:1px solid #3a4a72; box-shadow:0 2px 6px rgba(0,0,0,.35); }}
  .modtitle .top {{ float:right; font-size:11px; font-weight:400; color:#9ec1ff; text-decoration:none; }}
  iframe.modframe {{ display:block; width:100%; min-height:200px; border:0; background:#1b2233; }}
  .foot {{ padding:18px 22px; font-size:12px; color:#8a96b3; }}
  @media print {{ .modtitle {{ position:static; }} a.jump {{ display:none; }} }}
</style>
</head>
<body>
<a id="top"></a>
<div id="top"><header>
  <h1>{html.escape(title)}</h1>
  <div class="meta">{len(modules)} module(s) &middot; {total_mb:.1f} MB total &middot; generated {html.escape(timestamp)} &middot;
    one scrollable page — use your browser's full-page screenshot or &ldquo;Save as PDF&rdquo; to capture everything.</div>
</header></div>
<div class="toc"><span class="lbl">Jump to module</span>
{jump_links}
</div>
{sections_html}
<div class="foot">End of consolidated detailed report. Each module above is its full Detailed Report
  (every test, every step, inline screenshots), flattened into this single file.</div>
<script>
  // Size each embedded module's iframe to its content so the whole page is one
  // continuous scroll (no nested scrollbars) — required for a clean full-page
  // screenshot / print. Two mechanisms: (1) the module posts its height as it
  // renders (good during load); (2) a parent-side reconcile reads each module's
  // true content height directly (srcdoc is same-origin) and is authoritative.
  function sizeFrame(f, h) {{ if (h > 40) f.style.height = (Math.ceil(h) + 28) + 'px'; }}
  window.addEventListener('message', function (e) {{
    var d = e.data;
    if (!d || !d.__consHeight) return;
    var frames = document.querySelectorAll('iframe.modframe');
    for (var i = 0; i < frames.length; i++) {{
      if (frames[i].contentWindow === e.source) {{ sizeFrame(frames[i], d.h); break; }}
    }}
  }});
  function reconcile() {{
    document.querySelectorAll('iframe.modframe').forEach(function (f) {{
      try {{
        var doc = f.contentDocument; if (!doc) return;
        var tw = doc.querySelector('.test-wrapper');
        var h = tw ? tw.getBoundingClientRect().height : (doc.body ? doc.body.scrollHeight : 0);
        sizeFrame(f, h);
      }} catch (err) {{ /* cross-origin or not ready */ }}
    }});
  }}
  [400, 900, 1600, 3000, 5000].forEach(function (ms) {{ setTimeout(reconcile, ms); }});
  window.addEventListener('load', reconcile);
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

    entries = discover(args.input_dir)
    if not entries:
        print(f"[consolidated-detailed] no Detailed_Report_*.html found under {args.input_dir} — nothing to do")
        return 0

    os.makedirs(args.output_dir, exist_ok=True)
    modules_dir = os.path.join(args.output_dir, "modules")
    os.makedirs(modules_dir, exist_ok=True)

    nav_entries = []        # (name, rel, size) for the navigable index
    single_entries = []     # (name, html_string, size) for the single file
    used_fnames = set()
    for e in entries:
        src, name = e["path"], e["display"]
        with open(src, "r", encoding="utf-8", errors="replace") as fh:
            mod_html = fh.read()
        size = len(mod_html.encode("utf-8"))
        # nav index: copy the raw module file into modules/ under a unique name
        fname = safe_filename(name)
        if fname in used_fnames:
            stem, ext = os.path.splitext(fname)
            i = 2
            while f"{stem}-{i}{ext}" in used_fnames:
                i += 1
            fname = f"{stem}-{i}{ext}"
        used_fnames.add(fname)
        shutil.copyfile(src, os.path.join(modules_dir, fname))
        nav_entries.append((name, f"modules/{fname}", size))
        single_entries.append((name, mod_html, size))

    ts = args.timestamp or "this run"

    index = build_index(nav_entries, args.title, ts)
    index_path = os.path.join(args.output_dir, "Consolidated_Detailed_Report.html")
    with open(index_path, "w", encoding="utf-8") as fh:
        fh.write(index)

    single = build_single_file(single_entries, args.title, ts)
    single_path = os.path.join(args.output_dir, "Consolidated_Detailed_Report_SingleFile.html")
    with open(single_path, "w", encoding="utf-8") as fh:
        fh.write(single)

    total_mb = sum(s for _, _, s in nav_entries) / (1024 * 1024)
    print(f"[consolidated-detailed] {len(nav_entries)} module(s), {total_mb:.1f} MB")
    print(f"[consolidated-detailed] wrote {index_path} (navigable)")
    print(f"[consolidated-detailed] wrote {single_path} (single file — everything on one page)")
    for name, rel, size in nav_entries:
        print(f"    - {name}: {size/1024/1024:.1f} MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
