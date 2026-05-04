# Bug Findings Report v2 — 32 bugs (added 12 from CI/CD analysis)

**Date / Time:** 2026-05-04, 15:11 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)

## What you asked for

> "check latest ci cd fail test case why they are failling" → "yes" (generate v2 PDF with new findings)

After analyzing the latest CI runs (Parallel Full Suite + Parallel Suite 2, May 1), I identified **15 categories** of failures, of which **12 are new product bugs** not in v1 of the report. Built a v2 PDF that combines the original 17 bugs + 12 new from CI = **32 total**.

## The 9-part plan I followed

| Part | Status | What |
|---|---|---|
| 1 | ✓ | Identify 12 new bugs from CI analysis (BUG-21 onwards) |
| 2 | ✓ | Locate screenshots in local + CI artifacts (most CI bugs lack local screenshots — text-rich entries) |
| 3 | ✓ | Write detailed repro steps for each new bug |
| 4 | ✓ | Extend Python generator to handle all 32 bugs |
| 5 | ✓ | Generate v2 HTML (1.0 MB self-contained) |
| 6 | ✓ | Convert to PDF via Chrome headless (3.3 MB, 34 pages) |
| 7 | ✓ | Verify PDF — cover + index + 32 bug pages |
| 8 | ✓ | This changelog |
| 9 | ✓ | Commit + push to `main` (NEVER `developer`) |

## What's new in v2 vs v1

### v1 (May 1) — 17 bugs

Mostly from local automation runs + Phase1BugHunter rounds:
- BUG-01 to BUG-17 in original PDF
- 7 HIGH severity, 10 MEDIUM
- Located at `docs/bug-reports/Bug_Findings_Report_2026_05_01.pdf` (2.2 MB, 19 pages)

### v2 (May 4) — 32 bugs (+12 net)

Added 12 new bugs surfaced by CI analysis (Parallel Full Suite run `25204867049` + Parallel Suite 2 run `25204870235`):

| New BUG | Title | Severity | Source |
|---|---|---|---|
| **BUG-08** | Auth cookies SameSite=None (CSRF) [SECURITY] | **HIGH** | SecurityAuditTestNG |
| **BUG-09** | No rate limiting on login [SECURITY] | **HIGH** | SecurityAuditTestNG |
| BUG-10 | KPI cards missing on dashboard (0 cards rendered) | HIGH | CriticalPathTestNG |
| BUG-21 | Dashboard work order count vs Work Orders module | MEDIUM | CriticalPathTestNG |
| BUG-22 | Equipment-At-Risk KPI value formatting | MEDIUM | CriticalPathTestNG |
| BUG-23 | Opportunities-Value KPI value formatting | MEDIUM | CriticalPathTestNG |
| BUG-24 | Asset detail page tabs do not load content | MEDIUM | CriticalPathTestNG |
| BUG-25 | Work Order Planning page does not load | MEDIUM | CriticalPathTestNG |
| BUG-26 | Session lost after navigating 5 pages | MEDIUM | CriticalPathTestNG |
| BUG-27 | Company info missing on detail page | MEDIUM | BugHuntDashboardTestNG |
| BUG-28 | SLDs page has duplicate dropdown entries | MEDIUM | BugHuntPagesTestNG |
| BUG-29 | No proper 404 page for invalid routes | MEDIUM | DeepBugVerificationTestNG |
| BUG-30 | CSP rules block Beamer fonts | MEDIUM | DeepBugVerificationTestNG |
| BUG-31 | Connection search/edit-cancel cluster (4 tests) | MEDIUM | ConnectionTestNG |
| BUG-32 | Smoke tests cluster (5 modules) | MEDIUM | Multiple Smoke classes |

### Also re-numbered some entries to consolidate

BUG-15 (Roles 2x) and BUG-16 (Sign-In disabled) from v1 became BUG-18 and BUG-19 in v2.
BUG-17 (aria-hidden) became BUG-20 in v2. This was done to keep HIGH-severity bugs grouped at the top (BUG-01 to BUG-10) and MEDIUM grouped after.

## v2 PDF structure

| Page | Content |
|---|---|
| 1 | Cover with "Version 2 — adds CI findings" red badge + 32/10 HIGH/22 MEDIUM stat cards + "What's new in v2" callout + start of index |
| 2 | Rest of index (BUG-15 through BUG-32) |
| 3-34 | One detailed page per bug |

Each bug page has the same structure as v1: ID badge, severity badge, title, surface, caught-by-test, description, numbered repro steps, expected (green block), actual (red block), impact (yellow block), and screenshot or "no screenshot" note.

## Top priority NEW findings (file ZP tickets immediately)

### P0 — Security (block-on-ship)

1. **BUG-08** — Auth cookies use `SameSite=None`. CSRF risk. Modern web should use `SameSite=Lax` or `Strict`.
2. **BUG-09** — Login endpoint has no rate limiting. Credential-stuffing / brute-force risk.

### P1 — Customer-visible regressions

3. **BUG-10** — Dashboard 0 KPI cards rendered. Homepage broken.
4. **BUG-21** — Work order count mismatch (extends BUG-03/04/05 family).
5. **BUG-22 / BUG-23** — KPI formatting regressions on Equipment-at-Risk and Opportunities-Value.
6. **BUG-24** — Asset detail tabs don't load.
7. **BUG-25** — Work Order Planning page broken.
8. **BUG-26** — Session lost after 5 navigations.

### P2 — Module-specific bugs

9. **BUG-27** — Company info missing on detail page.
10. **BUG-28** — SLDs duplicate dropdown.
11. **BUG-29** — No proper 404 page.
12. **BUG-30** — CSP blocks Beamer fonts.
13. **BUG-31** — Connection search/edit cluster (4 tests).
14. **BUG-32** — Smoke test cluster (5 tests).

## What's NOT in v2 (and why)

The CI run also showed 29 LocationPart2 failures from the "Location + Task" job — but these are **not real bugs**. Investigation (commit `bfa6d65`, May 1) confirmed:
- Single LocationPart2 test in isolation: PASS
- Full LocationPart2 class in isolation: 42/42 PASS
- The CI failures are caused by suite-level state pollution (110 minutes accumulated browser state)

Already fixed via strengthened `BeforeMethod` (4 defensive layers). When the next CI run uses the new test class, those 29 LocationPart2 failures should disappear.

## Files in this commit

| File | What |
|---|---|
| `docs/bug-reports/Bug_Findings_Report_v2_2026_05_04.pdf` | NEW — v2 deliverable (3.3 MB, 34 pages) |
| `docs/bug-reports/Bug_Findings_Report_v2_2026_05_04.html` | NEW — self-contained HTML source (1.0 MB) |
| `docs/AI-FEATURES-CHANGELOG/2026-05-04-15-11-...md` | this changelog |

The Python build script (`/tmp/build_bug_report_v2.py`) is intentionally NOT committed — it's a one-shot generator. If you need a v3 with new bugs, I can extend it.

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` per memory rule.

## How to use the v2 PDF

1. **Open it now:** `open "/Users/abhiyantsingh/Downloads/Egalvanic_Web-main/docs/bug-reports/Bug_Findings_Report_v2_2026_05_04.pdf"`
2. **Send to client:** attach to email — 3.3 MB is fine
3. **Slack/Teams share:** drag-and-drop directly
4. **GitHub URL:** `https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/blob/main/docs/bug-reports/Bug_Findings_Report_v2_2026_05_04.pdf`
5. **File ZP tickets:** each bug has its BUG-NN ID for tracking; use the steps from the PDF in the JIRA description

---

_Per memory rule: this changelog is for learning + manager review. The PDF is the deliverable; this doc explains what changed from v1 and which new bugs to file as ZP tickets first (priority order: P0 security, then P1 customer-visible regressions)._
