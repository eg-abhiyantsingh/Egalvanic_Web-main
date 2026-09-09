# Artifact gap audit — and the two ticket pages that were missing

**Date:** 2026-09-09
**Time:** 11:35 – 12:25 IST
**Prompt:** “done testing all the ticket that i have shared?” → “let me know once you done testing
all the ticket with artificate”

---

## Answer to the question

**Yes for the batch you shared.** All **16** merge-monitor tickets tested on 2026-09-08 are complete
on every axis: verdict file, evidence directory, Jira key, per-prompt changelog, published Artifact
page. Counted, not assumed:

```
ls docs/bug-reports | grep -c 2026-09-08     # 16 verdicts
ls docs/changelogs  | grep -c 2026-09-08     # 16 changelogs
grep -l '^\*\*Artifact' docs/bug-reports/2026-09-08-*  | wc -l   # 16
```

**But the audit found older gaps**, and this prompt closed the recent ones.

## What was actually missing

| Verdict | State before | Action |
|---|---|---|
| `2026-09-02-QA-ghost-form-instances-wo-delete-VERIFIED.md` (ZP-3888) | tested, no Artifact page at all | **page built + published** |
| `2026-09-02-PROD-customers-accounts-tab-missing-multirole.md` (first-role gate) | tested, no Artifact page at all | **page built + published** |
| `2026-09-01-QA-staff-write-surface-fork-helper-regression.md` | page existed and was published, but the verdict never linked it | **cross-ref line added** |

## Published

- **ZP-3888 · Ghost Instance Sweep** — https://claude.ai/code/artifact/2c1a8a42-3843-4ded-92b7-06312f156b46
- **First-role gate · The First Role Wins** — https://claude.ai/code/artifact/4f22e133-5d2a-4671-8d35-3e39fa493f7b
- Existing page now linked from its verdict: https://claude.ai/code/artifact/73b405c3-3eb4-4f20-adaf-28eab71ae675
  (identified by reading the live page — its `<title>` is “Fork Isolation Verdict”, which is why a
  filename search never matched it)

## Jira keys resolved read-only

The offline `testcase/Jira.csv` tops out at ZP-1851, so both tickets were resolved through the Jira
MCP by title:

- **ZP-3888** — “[Web] Deleting a work order left its task-chain EG form instances alive, and reports
  re-attached them as ghost inspection pages to unrelated work orders” (READY TO RELEASE)
- The prod Customers/Accounts defect has **no ticket** — it is a new finding in the
  **ZP-4033** (union-of-roles permissions) / **ZP-4036** (#1351, 11 RoleGate uses deferred) family.
  No issue was created: Jira stays read-only without the owner's say-so.

## New screenshots (QA, live, this session)

The ZP-3888 verdict had **zero** screenshots — its fixtures were deleted *by* the test. Two fresh
captures on QA V1.36 supply the standing “2 real frontend screenshots” rule without inventing
evidence:

1. `zp3888-delete-dialog.png` — Work Orders list → row action → **Delete Work Order** (“This action
   cannot be undone”). Dialog opened on `QA-DEMO PM Forms wheel check-offs - delete me`, then
   **cancelled** — nothing was deleted for the screenshot.
2. `zp3888-wo-forms-tab.png` — the **Forms** tab of WO `b2c2657a`, 9 instances grouped by form type:
   exactly the object class the delete has to sweep.

## Remaining backlog, stated plainly

39 files under `docs/bug-reports/` have neither an Artifact URL nor a local page. Breaking that down:

- **~25 verdicts dated 2026-08-10 → 2026-08-18** predate the artifact-per-ticket rule (the first
  published pages are 2026-08-18). Retro-building them is a real job (~25 pages) and is the owner's
  call, not something to do silently.
- **11 `JIRA-TICKET-*.md` / `JIRA-REPORT-*.md` files** are dev-facing ticket drafts, not per-ticket
  verdicts — they are the ticket text itself and were never meant to have a page.
- **19 August verdicts** *do* have published pages but the markdown never got the cross-reference
  line. Fixing those needs a title→file mapping (published titles are editorial, e.g. “Env cleanup”,
  “Every Cable Was #12”), so it is a mapping exercise rather than a build.

## Depth explanation — what was learned about the process

**1. Two different failures look identical from the outside.** “No `**Artifact:**` line in the
verdict” can mean *the page was never built* or *the page exists and nobody linked it*. The
distinguishing test is not a filename search — published page titles are editorial names, not file
names — but reading the live page and matching its content (the fork page names `#1073 · #1074`,
`ZP-3874 follow-on`, which is exactly what the 2026-09-01 verdict covers). Guessing from a date
match alone would have attached the wrong URL to a verdict, which is worse than leaving it blank.

**2. `sips -c H W` pads, it does not only crop.** The first build embedded both new screenshots with
thick black bands: `sips -c 1610 2133` on a 928-tall image *centres and pads* to the requested box.
The render check caught it. The fix was to compute the real content bounds instead of guessing them —
find the first and last row/column that is not pure white, crop to that, then resize:

```python
nonwhite = (np.asarray(im).sum(axis=2) < 255*3 - 6)
cols = np.where(nonwhite.any(axis=0))[0]; rows = np.where(nonwhite.any(axis=1))[0]
im.crop((cols.min(), rows.min(), cols.max()+1, rows.max()+1))
```

That is worth keeping for every future artifact: browser screenshots of an SPA usually carry a white
gutter, and auto-detecting it beats hand-tuned crop numbers.

**3. Mojibake in a local preview is not a bug in the page.** `python3 -m http.server` serves
`text/html` with no charset, so a browser falls back to windows-1252 and every em-dash renders as
`â€"`. The Artifact wrapper supplies `<meta charset=utf8>`, so the published page is fine. Worth
knowing before “fixing” a file that was never broken — the preview server needs the charset, not the
HTML:

```python
def guess_type(self, path):
    t = super().guess_type(path)
    return t + "; charset=utf-8" if t == "text/html" else t
```

**4. The defect page's design device came from the defect itself.** The bug is that
`roles.find(g => g)` reads index 0, so the page draws the roles array as indexed slots with the read
pointer on `[0]` and the qualifying-but-ignored roles marked. Two seats, same set, different order,
opposite outcome — the mechanism is the visual, not decoration on top of it.

## Files

- `docs/report-artifacts/2026-09-02-QA-ZP-3888-ghost-form-instances-wo-delete.html` (new, 177 KB)
- `docs/report-artifacts/2026-09-02-PROD-first-role-gate-customers-accounts-tab.html` (new, 221 KB)
- `docs/bug-reports/2026-09-02-QA-ghost-form-instances-wo-delete-VERIFIED.md` (ticket + artifact lines)
- `docs/bug-reports/2026-09-02-PROD-customers-accounts-tab-missing-multirole.md` (family + artifact lines)
- `docs/bug-reports/2026-09-01-QA-staff-write-surface-fork-helper-regression.md` (artifact line)
- `docs/bug-evidence/zp-3888-ghost-instances/` (2 new QA screenshots)
- `docs/qa-review-board.html` rebuilt — 86 reports, 67 screenshots, 7,396 KB
