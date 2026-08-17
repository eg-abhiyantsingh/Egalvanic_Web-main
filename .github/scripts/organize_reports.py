#!/usr/bin/env python3
"""
Organize docs/jira-export into one folder PER DAY, so review is tidy.

Layout produced:
    docs/jira-export/
        00_INDEX.md            <- top of the folder; newest day first, TODAY starred
        2026-08-17/            <- one folder per day
            ILLUSTRATED-site-walk-quote-zeroed.pdf
            JIRA-TICKET-...pdf
        2026-08-14/
            ...
        _html/                 <- loose .html exports tucked out of the way

- Each PDF's day = the date in its source filename if present, else its modified date.
- File names inside a day-folder are kept clean (no date prefix — the folder carries the date).
- Idempotent; a regenerated PDF overwrites the old copy (no duplicates).

Runs automatically at the end of md-to-jira-html.py --pdf. Run standalone to refresh:
    python3 .github/scripts/organize_reports.py
"""
import os, re, datetime, glob, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
EXPORT = os.path.join(ROOT, "docs", "jira-export")
REPORTS = os.path.join(ROOT, "docs", "bug-reports")
DATE_RE = re.compile(r"(\d{4}-\d{2}-\d{2})")
DAYDIR_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
MONTHS = ["January","February","March","April","May","June","July","August",
          "September","October","November","December"]

def human(iso):
    y,m,d = map(int, iso.split("-"))
    return f"{d} {MONTHS[m-1]} {y}"

def clean_name(name):
    """Strip a leading 'YYYY-MM-DD__' or 'YYYY-MM-DD-' the old flat scheme may have added."""
    name = re.sub(r"^\d{4}-\d{2}-\d{2}__", "", name)
    return name

def day_for(path, name):
    m = DATE_RE.search(name)
    if m:
        return m.group(1)
    return datetime.date.fromtimestamp(os.path.getmtime(path)).isoformat()

def title_for(stem):
    """First H1 of the matching source .md in docs/bug-reports/, else a de-slugged name."""
    for md in glob.glob(os.path.join(REPORTS, "*.md")):
        base = os.path.splitext(os.path.basename(md))[0]
        if base == stem or base.endswith(stem) or stem.endswith(base):
            try:
                for line in open(md, encoding="utf-8"):
                    if line.startswith("# "):
                        return line[2:].strip()
            except Exception:
                pass
    return stem.replace("-", " ")

def organize():
    if not os.path.isdir(EXPORT):
        print("no jira-export folder yet"); return
    today = datetime.date.today().isoformat()

    # 0) sweep clutter so the root shows only day-folders + index
    for entry in os.listdir(EXPORT):
        p = os.path.join(EXPORT, entry)
        if entry.startswith(".") and os.path.isdir(p):      # chrome user-data-dir & friends
            shutil.rmtree(p, ignore_errors=True)
        elif os.path.isdir(p) and entry.endswith("-images"):  # --paste-kit image folders
            dest = os.path.join(EXPORT, "_paste-kit"); os.makedirs(dest, exist_ok=True)
            shutil.move(p, os.path.join(dest, entry))

    # 1) loose HTML -> _html/
    html = glob.glob(os.path.join(EXPORT, "*.html"))
    if html:
        os.makedirs(os.path.join(EXPORT, "_html"), exist_ok=True)
        for h in html:
            shutil.move(h, os.path.join(EXPORT, "_html", os.path.basename(h)))

    # 2) any PDF sitting at the folder ROOT -> its day-folder (clean name)
    for pdf in glob.glob(os.path.join(EXPORT, "*.pdf")):
        name = os.path.basename(pdf)
        d = day_for(pdf, name)
        dest_dir = os.path.join(EXPORT, d)
        os.makedirs(dest_dir, exist_ok=True)
        shutil.move(pdf, os.path.join(dest_dir, clean_name(name)))  # overwrite older copy

    # 2b) any remaining loose file at root (paste-kit .jira.md / .txt, etc.) -> _paste-kit/
    for entry in os.listdir(EXPORT):
        p = os.path.join(EXPORT, entry)
        if os.path.isfile(p) and entry != "00_INDEX.md":
            dest = os.path.join(EXPORT, "_paste-kit"); os.makedirs(dest, exist_ok=True)
            shutil.move(p, os.path.join(dest, entry))

    # 3) build the index from the day-folders
    days = {}
    for entry in os.listdir(EXPORT):
        p = os.path.join(EXPORT, entry)
        if os.path.isdir(p) and DAYDIR_RE.match(entry):
            pdfs = sorted(os.path.basename(x) for x in glob.glob(os.path.join(p, "*.pdf")))
            if pdfs:
                days[entry] = pdfs

    total = sum(len(v) for v in days.values())
    L = []
    L.append("# QA reports — review index")
    L.append("")
    L.append(f"**As of {human(today)}.** One folder per day, newest first. "
             "Each PDF is self-contained (text + screenshots) — drag straight onto a Jira ticket.")
    L.append("")
    L.append(f"_{total} reports across {len(days)} days. "
             "Auto-refreshed on every PDF; or run `python3 .github/scripts/organize_reports.py`._")
    L.append("")
    if today not in days:
        L.append(f"## {human(today)} ★ TODAY")
        L.append("")
        L.append("_(no reports yet today)_")
        L.append("")
    for d in sorted(days, reverse=True):
        star = " ★ TODAY" if d == today else ""
        L.append(f"## {human(d)}{star}   ·   `{d}/`")
        L.append("")
        for name in days[d]:
            stem = re.sub(r"\.pdf$", "", name)
            L.append(f"- **[{name}]({d}/{name})** — {title_for(stem)}")
        L.append("")
    open(os.path.join(EXPORT, "00_INDEX.md"), "w", encoding="utf-8").write("\n".join(L))
    print(f"organized {total} PDFs into {len(days)} day-folders; today={human(today)}")
    print(f"index -> {os.path.join(EXPORT, '00_INDEX.md')}")

if __name__ == "__main__":
    organize()
