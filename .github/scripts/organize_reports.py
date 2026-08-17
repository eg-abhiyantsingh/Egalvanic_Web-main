#!/usr/bin/env python3
"""
Organize docs/jira-export for easy review.

- Every PDF gets an ISO date prefix  ->  YYYY-MM-DD__<name>.pdf  (one flat folder, sorts by date)
    date = the date in the source filename if present, else the file's modified date.
- Loose .html exports are tucked into _html/ so the folder is PDF-only.
- A single 00_INDEX.md is (re)written at the very top of the folder, newest date first,
  with TODAY called out at the top. Each entry shows a one-line description pulled from the
  matching report's title in docs/bug-reports/.

Idempotent — safe to run repeatedly. Run standalone:
    python3 .github/scripts/organize_reports.py
It is also called automatically at the end of md-to-jira-html.py --pdf, so new PDFs land
date-stamped and the index refreshes on their own from now on.
"""
import os, re, datetime, glob, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
EXPORT = os.path.join(ROOT, "docs", "jira-export")
REPORTS = os.path.join(ROOT, "docs", "bug-reports")
DATE_RE = re.compile(r"(\d{4}-\d{2}-\d{2})")
PREFIXED = re.compile(r"^\d{4}-\d{2}-\d{2}__")
MONTHS = ["January","February","March","April","May","June","July","August",
          "September","October","November","December"]

def human(iso):
    y,m,d = map(int, iso.split("-"))
    return f"{d} {MONTHS[m-1]} {y}"

def file_date(path, name):
    m = DATE_RE.search(name)
    if m:
        return m.group(1)
    return datetime.date.fromtimestamp(os.path.getmtime(path)).isoformat()

def title_for(stem):
    """First H1 of the matching source .md in docs/bug-reports/, else a de-slugged name."""
    # stem is the name WITHOUT the date-prefix and .pdf
    candidates = [stem]
    m = DATE_RE.search(stem)
    for md in glob.glob(os.path.join(REPORTS, "*.md")):
        base = os.path.splitext(os.path.basename(md))[0]
        if base == stem or base.endswith(stem) or stem.endswith(base):
            try:
                for line in open(md, encoding="utf-8"):
                    if line.startswith("# "):
                        return line[2:].strip()
            except Exception:
                pass
    return stem.replace("__", " · ").replace("-", " ")

def organize():
    if not os.path.isdir(EXPORT):
        print("no jira-export folder yet"); return
    today = datetime.date.today().isoformat()

    # 1) tuck away loose HTML
    html = glob.glob(os.path.join(EXPORT, "*.html"))
    if html:
        os.makedirs(os.path.join(EXPORT, "_html"), exist_ok=True)
        for h in html:
            shutil.move(h, os.path.join(EXPORT, "_html", os.path.basename(h)))

    # 2) date-prefix every PDF (idempotent)
    for pdf in glob.glob(os.path.join(EXPORT, "*.pdf")):
        name = os.path.basename(pdf)
        if PREFIXED.match(name):
            continue
        d = file_date(pdf, name)
        rest = DATE_RE.sub("", name).lstrip("-_ ").strip()  # drop an inline date if any
        rest = re.sub(r"^[-_ ]+", "", rest) or name
        target = os.path.join(EXPORT, f"{d}__{rest}")
        if os.path.abspath(target) != os.path.abspath(pdf):
            # a freshly (re)generated un-prefixed PDF replaces any older date-stamped copy
            shutil.move(pdf, target)

    # 3) group by date, newest first
    groups = {}
    for pdf in glob.glob(os.path.join(EXPORT, "*.pdf")):
        name = os.path.basename(pdf)
        d = name[:10] if PREFIXED.match(name) else file_date(pdf, name)
        groups.setdefault(d, []).append(name)

    lines = []
    lines.append(f"# QA reports — review index")
    lines.append("")
    lines.append(f"**As of {human(today)}.** Newest first. Each PDF is self-contained (text + screenshots) — drag straight onto a Jira ticket.")
    lines.append("")
    lines.append(f"_{sum(len(v) for v in groups.values())} reports across {len(groups)} days. Regenerated automatically; run `python3 .github/scripts/organize_reports.py` to refresh._")
    lines.append("")
    for d in sorted(groups, reverse=True):
        star = " ★ TODAY" if d == today else ""
        lines.append(f"## {human(d)}{star}")
        lines.append("")
        for name in sorted(groups[d]):
            stem = re.sub(r"\.pdf$", "", PREFIXED.sub("", name))
            lines.append(f"- **[{name}]({name})** — {title_for(stem)}")
        lines.append("")
    open(os.path.join(EXPORT, "00_INDEX.md"), "w", encoding="utf-8").write("\n".join(lines))
    print(f"organized {sum(len(v) for v in groups.values())} PDFs across {len(groups)} days; today={human(today)}")
    print(f"index -> {os.path.join(EXPORT,'00_INDEX.md')}")

if __name__ == "__main__":
    organize()
