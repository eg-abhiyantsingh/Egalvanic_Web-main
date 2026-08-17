#!/usr/bin/env python3
"""
Render a QA markdown report to a SELF-CONTAINED HTML file for pasting into Jira.

Why this exists
---------------
Copying a .md file's text into a Jira comment loses the screenshots, because the
images are relative repo paths (../bug-evidence/foo.png) — plain text that Jira
cannot resolve. This renders the markdown to HTML and inlines every referenced
image as a base64 data: URI. Open the result in a browser, Select All, Copy, and
paste into the Jira comment: the clipboard then carries real image data, which
Jira uploads as attachments automatically.

No third-party dependencies (no markdown lib, no pandoc) — the converter below
handles the subset of GFM these reports actually use.

If the HTML paste loses the images
----------------------------------
Jira Cloud's editor (ADF) sanitises `data:` URIs out of pasted HTML, so the
screenshots vanish while the text survives. That is a limitation of Jira, not of
the HTML. Use --paste-kit instead, which is bulletproof on both Cloud and Server:

  python3 .github/scripts/md-to-jira-html.py REPORT.md --paste-kit

It writes a text version with numbered [SCREENSHOT n] markers plus a flat folder
of images named in document order, then tells you the command to put each image
on the clipboard as real PNG data (which every Jira editor accepts):

  python3 .github/scripts/md-to-jira-html.py REPORT.md --copy 1

Usage
-----
  python3 .github/scripts/md-to-jira-html.py docs/bug-reports/REPORT.md [more.md ...]
  python3 .github/scripts/md-to-jira-html.py --all          # every dated report
  python3 .github/scripts/md-to-jira-html.py REPORT.md --open
  python3 .github/scripts/md-to-jira-html.py REPORT.md --paste-kit
  python3 .github/scripts/md-to-jira-html.py REPORT.md --copy 2

Output goes to docs/jira-export/<name>.html and is git-ignored by convention.
"""

import argparse
import base64
import glob
import html
import mimetypes
import os
import re
import subprocess
import time
import sys

# ---------------------------------------------------------------- image embed

def data_uri(path):
    """Read an image and return a base64 data: URI, or None if unreadable."""
    try:
        with open(path, "rb") as fh:
            raw = fh.read()
    except OSError:
        return None
    mime = mimetypes.guess_type(path)[0] or "image/png"
    return "data:%s;base64,%s" % (mime, base64.b64encode(raw).decode("ascii"))


# ------------------------------------------------------------ inline markdown

def inline(text, base_dir, stats):
    """Convert inline markdown. Text arrives raw; we escape then re-inject tags."""
    out = html.escape(text, quote=False)

    # images first (they look like links with a leading !)
    def img_sub(m):
        alt, src = m.group(1), m.group(2).strip()
        if not re.match(r"^[a-z]+:", src):
            resolved = os.path.normpath(os.path.join(base_dir, src))
            uri = data_uri(resolved)
            if uri:
                stats["embedded"].append(os.path.basename(resolved))
                return ('<img alt="%s" src="%s">' % (html.escape(alt, quote=True), uri))
            stats["missing"].append(src)
            return '<em>[missing image: %s]</em>' % html.escape(src)
        return '<img alt="%s" src="%s">' % (html.escape(alt, quote=True), html.escape(src, quote=True))

    out = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", img_sub, out)

    # links — drop repo-relative ones to plain text, they mean nothing in Jira
    def link_sub(m):
        label, href = m.group(1), m.group(2).strip()
        if re.match(r"^[a-z]+:", href) or href.startswith("#"):
            return '<a href="%s">%s</a>' % (html.escape(href, quote=True), label)
        return "<strong>%s</strong>" % label

    out = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", link_sub, out)

    out = re.sub(r"`([^`]+)`", r"<code>\1</code>", out)
    out = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", out)
    out = re.sub(r"(?<![\w*])\*([^*\n]+)\*(?![\w*])", r"<em>\1</em>", out)
    return out


# ------------------------------------------------------------- block markdown

def convert(md, base_dir, stats):
    lines = md.split("\n")
    out, i = [], 0
    n = len(lines)

    while i < n:
        line = lines[i]

        # fenced code block
        m = re.match(r"^```(\w*)\s*$", line)
        if m:
            lang = m.group(1)
            i += 1
            buf = []
            while i < n and not re.match(r"^```\s*$", lines[i]):
                buf.append(lines[i]); i += 1
            i += 1
            out.append('<pre class="code" data-lang="%s"><code>%s</code></pre>'
                       % (lang, html.escape("\n".join(buf), quote=False)))
            continue

        # GFM table: header row, separator row, then body
        if "|" in line and i + 1 < n and re.match(r"^\s*\|?[\s:|-]+\|[\s:|-]*$", lines[i + 1]):
            def cells(row):
                row = row.strip()
                if row.startswith("|"): row = row[1:]
                if row.endswith("|"): row = row[:-1]
                return [c.strip() for c in row.split("|")]

            head = cells(line)
            i += 2
            body = []
            while i < n and "|" in lines[i] and lines[i].strip():
                body.append(cells(lines[i])); i += 1
            t = ["<table><thead><tr>"]
            t += ["<th>%s</th>" % inline(c, base_dir, stats) for c in head]
            t.append("</tr></thead><tbody>")
            for row in body:
                t.append("<tr>" + "".join(
                    "<td>%s</td>" % inline(c, base_dir, stats) for c in row) + "</tr>")
            t.append("</tbody></table>")
            out.append("".join(t))
            continue

        # heading
        m = re.match(r"^(#{1,6})\s+(.*)$", line)
        if m:
            lvl = len(m.group(1))
            out.append("<h%d>%s</h%d>" % (lvl, inline(m.group(2), base_dir, stats), lvl))
            i += 1
            continue

        # horizontal rule
        if re.match(r"^\s*(---|\*\*\*|___)\s*$", line):
            out.append("<hr>")
            i += 1
            continue

        # blockquote (consecutive > lines)
        if re.match(r"^\s*>", line):
            buf = []
            while i < n and re.match(r"^\s*>", lines[i]):
                buf.append(re.sub(r"^\s*>\s?", "", lines[i])); i += 1
            out.append("<blockquote>%s</blockquote>" % convert("\n".join(buf), base_dir, stats))
            continue

        # lists
        if re.match(r"^\s*[-*+]\s+", line) or re.match(r"^\s*\d+\.\s+", line):
            ordered = bool(re.match(r"^\s*\d+\.\s+", line))
            items = []
            while i < n and (re.match(r"^\s*[-*+]\s+", lines[i]) or re.match(r"^\s*\d+\.\s+", lines[i])):
                txt = re.sub(r"^\s*(?:[-*+]|\d+\.)\s+", "", lines[i])
                i += 1
                # continuation lines (indented, not a new item)
                while i < n and lines[i].strip() and not re.match(r"^\s*(?:[-*+]|\d+\.)\s+", lines[i]) \
                        and not re.match(r"^(#{1,6})\s", lines[i]) and "|" not in lines[i]:
                    txt += " " + lines[i].strip(); i += 1
                items.append("<li>%s</li>" % inline(txt, base_dir, stats))
            tag = "ol" if ordered else "ul"
            out.append("<%s>%s</%s>" % (tag, "".join(items), tag))
            continue

        # blank
        if not line.strip():
            i += 1
            continue

        # paragraph (gather until blank / block start)
        buf = [line]
        i += 1
        while i < n and lines[i].strip() \
                and not re.match(r"^(#{1,6})\s", lines[i]) \
                and not re.match(r"^```", lines[i]) \
                and not re.match(r"^\s*>", lines[i]) \
                and not re.match(r"^\s*(?:[-*+]|\d+\.)\s+", lines[i]) \
                and not re.match(r"^\s*(---|\*\*\*|___)\s*$", lines[i]) \
                and "|" not in lines[i]:
            buf.append(lines[i]); i += 1
        out.append("<p>%s</p>" % inline(" ".join(buf), base_dir, stats))

    return "\n".join(out)


CSS = """
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;
line-height:1.55;color:#172b4d;max-width:900px;margin:24px auto;padding:0 20px}
h1{font-size:1.7em;border-bottom:2px solid #dfe1e6;padding-bottom:.3em}
h2{font-size:1.35em;border-bottom:1px solid #dfe1e6;padding-bottom:.25em;margin-top:1.6em}
h3{font-size:1.12em;margin-top:1.4em}
table{border-collapse:collapse;width:100%;margin:1em 0}
th,td{border:1px solid #dfe1e6;padding:7px 10px;text-align:left;vertical-align:top;font-size:.93em}
th{background:#f4f5f7;font-weight:600}
code{background:#f4f5f7;padding:1px 5px;border-radius:3px;
font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:.9em}
pre.code{background:#f4f5f7;padding:12px;border-radius:4px;overflow-x:auto;border:1px solid #dfe1e6}
pre.code code{background:none;padding:0}
img{max-width:100%;height:auto;border:1px solid #dfe1e6;border-radius:4px;margin:.6em 0;display:block}
blockquote{border-left:3px solid #dfe1e6;margin:1em 0;padding:.1em 1em;color:#5e6c84;background:#fafbfc}
hr{border:0;border-top:1px solid #dfe1e6;margin:1.8em 0}
.banner{background:#deebff;border:1px solid #b3d4ff;border-radius:4px;padding:10px 14px;
margin-bottom:20px;font-size:.9em;color:#0747a6}
"""

BANNER = ("<div class=\"banner\"><strong>To paste into Jira:</strong> click anywhere in this page, "
          "press <strong>&#8984;A</strong> then <strong>&#8984;C</strong>, and paste into the Jira "
          "comment box. The screenshots are embedded in this file, so they travel with the text and "
          "Jira will upload them as attachments.</div>")

# Print styling — keeps screenshots and table rows from being sliced across pages.
PRINT_CSS = """
@page{margin:14mm 12mm}
body{max-width:none;margin:0;font-size:10.5pt}
h1,h2,h3{page-break-after:avoid;break-after:avoid}
img{page-break-inside:avoid;break-inside:avoid;max-width:100%;
border:1px solid #c1c7d0;box-shadow:none}
table,blockquote,pre.code{page-break-inside:avoid;break-inside:avoid}
tr{page-break-inside:avoid;break-inside:avoid}
a{color:#0052cc;text-decoration:none}
"""


def render(md_path, out_dir, for_print=False):
    base_dir = os.path.dirname(os.path.abspath(md_path))
    with open(md_path, "r", encoding="utf-8") as fh:
        md = fh.read()
    stats = {"embedded": [], "missing": []}
    body = convert(md, base_dir, stats)
    title = os.path.splitext(os.path.basename(md_path))[0]
    m = re.search(r"^#\s+(.*)$", md, re.M)
    if m:
        title = re.sub(r"[*`]", "", m.group(1))
    css = CSS + (PRINT_CSS if for_print else "")
    banner = "" if for_print else BANNER          # irrelevant once it is a PDF
    doc = ("<!doctype html><html><head><meta charset=\"utf-8\">"
           "<title>%s</title><style>%s</style></head><body>%s%s</body></html>"
           % (html.escape(title), css, banner, body))
    os.makedirs(out_dir, exist_ok=True)
    suffix = ".print.html" if for_print else ".html"
    out_path = os.path.join(out_dir, os.path.splitext(os.path.basename(md_path))[0] + suffix)
    with open(out_path, "w", encoding="utf-8") as fh:
        fh.write(doc)
    return out_path, stats


# ------------------------------------------------------------------------ pdf

CHROME_CANDIDATES = [
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
    "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser",
]


def find_chrome():
    env = os.environ.get("CHROME_BINARY")
    if env and os.path.exists(env):
        return env
    for p in CHROME_CANDIDATES:
        if os.path.exists(p):
            return p
    return None


def to_pdf(md_path, out_dir):
    """Render to a print-styled HTML, then let Chrome print it to a single PDF.

    Chrome is used headlessly here purely as a PDF renderer — this is not a test
    run, so it does not conflict with the no-headless rule for Selenium suites.
    It also runs in its own temp profile, so it will not disturb an open browser.
    """
    chrome = find_chrome()
    if not chrome:
        print("  no Chrome/Chromium/Edge found — set CHROME_BINARY to a browser binary")
        return 1

    html_path, stats = render(md_path, out_dir, for_print=True)
    stem = os.path.splitext(os.path.basename(md_path))[0]
    pdf_path = os.path.abspath(os.path.join(out_dir, stem + ".pdf"))
    profile = os.path.join(out_dir, ".chrome-profile")

    # --headless=new is required from Chrome ~112 on; the legacy --headless hangs
    # indefinitely here (verified on Chrome 151), as does pairing it with
    # --run-all-compositor-stages-before-draw. Keep this invocation minimal.
    cmd = [chrome, "--headless=new", "--disable-gpu", "--no-sandbox",
           "--no-pdf-header-footer",
           "--user-data-dir=%s" % os.path.abspath(profile),
           "--print-to-pdf=%s" % pdf_path,
           "file://%s" % os.path.abspath(html_path)]
    # Two Chrome behaviours to work around, both verified on Chrome 151:
    #  1. capture_output() blocks for the whole timeout — Chrome forks updater and
    #     crash-handler children that inherit the stderr pipe and never close it,
    #     long after the PDF has been written. So the streams go to a log file.
    #  2. Chrome then lingers instead of exiting, which wasted 120s per document.
    #     So rather than waiting on the process, poll for the PDF to be complete
    #     (a finished PDF ends with %%EOF) and terminate Chrome once it is.
    log_path = os.path.join(out_dir, ".chrome-print.log")
    if os.path.exists(pdf_path):
        os.remove(pdf_path)

    def complete():
        try:
            if os.path.getsize(pdf_path) < 2048:
                return False
            with open(pdf_path, "rb") as fh:
                fh.seek(-1024, os.SEEK_END)
                return b"%%EOF" in fh.read()
        except OSError:
            return False

    with open(log_path, "wb") as log:
        proc = subprocess.Popen(cmd, stdout=log, stderr=log)
        waited = 0.0
        while waited < 120:
            if proc.poll() is not None or complete():
                break
            time.sleep(0.4)
            waited += 0.4
        if proc.poll() is None:
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()

    if not os.path.exists(pdf_path) or os.path.getsize(pdf_path) < 2048:
        print("  PDF FAILED for %s" % md_path)
        try:
            with open(log_path, "r", errors="replace") as fh:
                print("  chrome log tail: %s" % fh.read()[-400:])
        except OSError:
            pass
        return 1

    # page count straight from the PDF, so we report what was actually produced
    pages = 0
    try:
        with open(pdf_path, "rb") as fh:
            blob = fh.read()
        pages = len(re.findall(rb"/Type\s*/Page[^s]", blob))
    except OSError:
        pass

    print("\n%s" % os.path.basename(md_path))
    print("  -> %s" % pdf_path)
    print("     %.0f KB, %s page(s), %d screenshot(s) embedded"
          % (os.path.getsize(pdf_path) / 1024.0, pages or "?", len(stats["embedded"])))
    if stats["missing"]:
        print("     !! MISSING: %s" % ", ".join(stats["missing"]))
        return 1
    try:
        os.remove(html_path)                      # intermediate, not worth keeping
    except OSError:
        pass
    return 0


# ------------------------------------------------------------------ paste kit

def find_images(md, base_dir):
    """Return [(alt, abs_path)] for every image reference, in document order."""
    found = []
    for m in re.finditer(r"!\[([^\]]*)\]\(([^)]+)\)", md):
        alt, src = m.group(1), m.group(2).strip()
        if re.match(r"^[a-z]+:", src):
            continue
        found.append((alt, os.path.normpath(os.path.join(base_dir, src))))
    return found


def strip_for_jira(md, images):
    """Replace image refs with numbered markers; drop repo-relative link targets."""
    idx = {"n": 0}

    def marker(m):
        alt, src = m.group(1), m.group(2).strip()
        if re.match(r"^[a-z]+:", src):
            return m.group(0)
        idx["n"] += 1
        cap = alt.strip() or os.path.basename(src)
        return ("\n>>>>>> SCREENSHOT %d — paste here <<<<<<\n_%s_\n" % (idx["n"], cap))

    out = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", marker, md)
    # repo-relative links mean nothing in Jira -> keep the label only
    out = re.sub(r"\[([^\]]+)\]\((?![a-z]+:|#)[^)]+\)", r"\1", out)
    return out


def copy_to_clipboard(path):
    """Put a PNG on the macOS clipboard as real image data (not a file path)."""
    if sys.platform != "darwin":
        print("  --copy is macOS-only; drag the file in instead: %s" % path)
        return 1
    script = ('set the clipboard to (read (POSIX file "%s") as %s)'
              % (os.path.abspath(path), "«class PNGf»"))
    r = subprocess.run(["osascript", "-e", script], capture_output=True, text=True)
    if r.returncode != 0:
        print("  clipboard copy FAILED: %s" % (r.stderr or "").strip())
        return 1
    return 0


def paste_kit(md_path, out_dir):
    base_dir = os.path.dirname(os.path.abspath(md_path))
    stem = os.path.splitext(os.path.basename(md_path))[0]
    with open(md_path, "r", encoding="utf-8") as fh:
        md = fh.read()
    images = find_images(md, base_dir)

    os.makedirs(out_dir, exist_ok=True)
    txt_path = os.path.join(out_dir, stem + ".jira.md")
    with open(txt_path, "w", encoding="utf-8") as fh:
        fh.write(strip_for_jira(md, images))

    img_dir = os.path.join(out_dir, stem + "-images")
    os.makedirs(img_dir, exist_ok=True)
    for old in glob.glob(os.path.join(img_dir, "*")):
        os.remove(old)
    copied = []
    for i, (alt, src) in enumerate(images, 1):
        if not os.path.exists(src):
            print("  !! missing image %d: %s" % (i, src)); continue
        dest = os.path.join(img_dir, "%02d-%s" % (i, os.path.basename(src)))
        with open(src, "rb") as a, open(dest, "wb") as b:
            b.write(a.read())
        copied.append((i, alt, dest))

    print("\n%s" % os.path.basename(md_path))
    print("  text  -> %s" % txt_path)
    print("  images-> %s/  (%d, numbered in document order)" % (img_dir, len(copied)))
    print("\n  HOW TO POST THIS TO JIRA")
    print("  1. Open %s, select all, copy, paste into the Jira comment." % os.path.basename(txt_path))
    print("     Jira converts the markdown (headings, tables, bold) as you paste.")
    print("  2. For each >>>>>> SCREENSHOT n <<<<<< marker, run the matching command,")
    print("     click the marker line in Jira and hit Cmd+V. Then delete the marker line.")
    for i, alt, _ in copied:
        print("        python3 .github/scripts/md-to-jira-html.py %s --copy %d   # %s"
              % (md_path, i, (alt or "")[:52]))
    print("\n  Alternative: drag everything in %s/ onto the issue at once to attach"
          % os.path.basename(img_dir))
    print("  them, then reference by filename. Numbering matches the markers.")
    return 0


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("files", nargs="*", help="markdown file(s) to render")
    ap.add_argument("--all", action="store_true", help="render every dated report in docs/bug-reports/")
    ap.add_argument("--out", default="docs/jira-export", help="output directory")
    ap.add_argument("--open", dest="do_open", action="store_true", help="open the result in a browser")
    ap.add_argument("--pdf", action="store_true",
                    help="produce a single PDF with the screenshots embedded — attach it to Jira")
    ap.add_argument("--paste-kit", action="store_true",
                    help="text + numbered images for Jira (use when HTML paste loses the pictures)")
    ap.add_argument("--copy", type=int, metavar="N",
                    help="put the Nth image of the report on the clipboard as real PNG data")
    args = ap.parse_args()

    targets = list(args.files)
    if args.all:
        targets += sorted(glob.glob("docs/bug-reports/20*.md"))
    if not targets:
        ap.error("give a markdown file, or --all")

    # --copy: clipboard one image, then stop
    if args.copy is not None:
        md_path = targets[0]
        base_dir = os.path.dirname(os.path.abspath(md_path))
        with open(md_path, "r", encoding="utf-8") as fh:
            images = find_images(fh.read(), base_dir)
        if not 1 <= args.copy <= len(images):
            print("  image %d out of range — this report has %d" % (args.copy, len(images)))
            return 1
        alt, src = images[args.copy - 1]
        rc = copy_to_clipboard(src)
        if rc == 0:
            print("  copied image %d/%d to the clipboard: %s"
                  % (args.copy, len(images), os.path.basename(src)))
            print("  caption: %s" % (alt or "(none)"))
            print("  -> click the matching marker line in Jira and press Cmd+V")
        return rc

    if args.pdf:
        rc = 0
        made = []
        for md_path in targets:
            if not os.path.exists(md_path):
                print("  SKIP (not found): %s" % md_path); rc = 1; continue
            rc |= to_pdf(md_path, args.out)
            made.append(os.path.join(args.out,
                        os.path.splitext(os.path.basename(md_path))[0] + ".pdf"))
        if rc == 0:
            print("\n  Drag the PDF straight onto the Jira ticket — text and screenshots in one file.")
        # Auto-organize: date-stamp the new PDF(s) and refresh the review index so the
        # export folder always sorts by date with today at the top.
        try:
            sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
            import organize_reports
            organize_reports.organize()
        except Exception as e:
            print("  (index refresh skipped: %s)" % e)
        if args.do_open and sys.platform == "darwin" and made:
            subprocess.run(["open"] + [m for m in made if os.path.exists(m)], check=False)
        return rc

    if args.paste_kit:
        rc = 0
        for md_path in targets:
            if not os.path.exists(md_path):
                print("  SKIP (not found): %s" % md_path); rc = 1; continue
            rc |= paste_kit(md_path, args.out)
        return rc

    rc = 0
    for md_path in targets:
        if not os.path.exists(md_path):
            print("  SKIP (not found): %s" % md_path); rc = 1; continue
        out_path, stats = render(md_path, args.out)
        size_kb = os.path.getsize(out_path) / 1024.0
        print("\n%s" % os.path.basename(md_path))
        print("  -> %s  (%.0f KB)" % (out_path, size_kb))
        print("     %d image(s) embedded%s" % (
            len(stats["embedded"]),
            (": " + ", ".join(stats["embedded"])) if stats["embedded"] else ""))
        if stats["missing"]:
            print("     !! %d MISSING: %s" % (len(stats["missing"]), ", ".join(stats["missing"])))
            rc = 1
        if args.do_open and sys.platform == "darwin":
            subprocess.run(["open", out_path], check=False)
    return rc


if __name__ == "__main__":
    sys.exit(main())
