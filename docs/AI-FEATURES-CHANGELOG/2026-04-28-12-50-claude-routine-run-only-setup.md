# claude.ai Routine Runs — only-via-routines setup (no Java analyzers needed)

**Date / Time:** 2026-04-28, 12:50 IST
**Branch:** `main`

## TL;DR

User asked for the simplest possible setup: only claude.ai Routine Runs, no extra analyzer layers. **Done.** Now there's exactly ONE moving part on the Anthropic side (your routine) and ONE on the GitHub side (the auto-committed JSON). The Java analyzers I built earlier still exist but are now optional — you can ignore them entirely.

## What changed in this commit

### 1. `.gitignore` — surgically allowlisted `bug-detection-report.json`

Old: `test-output/` was wholly ignored, so claude.ai routines couldn't reach the JSON via raw URL.

New: keeps everything in `test-output/` ignored EXCEPT `bug-detection-report.json` (the small machine-readable file the routine reads). Screenshots, visual baselines, healed-locators logs, etc. stay out of the repo.

```gitignore
test-output/
test-output/screenshots/
test-output/visual-baselines/
test-output/visual-diffs/
test-output/healed-locators.json
test-output/*.png
test-output/*.html
test-output/*.txt
!test-output/bug-detection-report.json
```

### 2. `.github/workflows/daily-ai-analysis.yml` — auto-commits fresh JSON after each CI run

Two new steps inserted before the analyzers run:

- **Download latest artifacts** from the most recent Parallel Suite 2 run (uses `dawidd6/action-download-artifact@v3`)
- **Locate the bug-detection-report.json** in those artifacts (picks the largest = most failure data) and copies it to the canonical `test-output/` path

The existing **commit-back-to-main** step is updated to also `git add test-output/bug-detection-report.json` so the freshly-downloaded JSON gets pushed to the raw URL for claude.ai routines to fetch.

Net effect: every time CI runs, the JSON at this URL refreshes:

```
https://raw.githubusercontent.com/eg-abhiyantsingh/Egalvanic_Web-main/main/test-output/bug-detection-report.json
```

### 3. Committed the current `bug-detection-report.json`

So you can test the Routine Run RIGHT NOW without waiting for the next CI cycle. The current file holds 3 entries from yesterday's CopyToCopyFromTestNG runs (Copy From / Copy To entry-not-found findings) — perfect realistic test data.

## The ONE Routine Run prompt you need

Configure exactly ONE Routine in claude.ai (Settings → Routines → + New Routine):

**Name:** `eGalvanic — Daily QA brief`
**Frequency:** Daily, 09:00 IST (or any time after the 04:00 UTC cron settles)

**Prompt:**

```
Fetch this URL (raw JSON of test failures from the most recent CI run):

https://raw.githubusercontent.com/eg-abhiyantsingh/Egalvanic_Web-main/main/test-output/bug-detection-report.json

It's an array of failure objects. Each has: testName, classification (REAL_BUG / FLAKY_TEST / LOCATOR_CHANGE / ENVIRONMENT_ISSUE), confidence (0-100), riskLevel, exceptionType, exceptionMessage, rootCause, suggestedFix, pageUrl, timestamp.

Produce a 5-minute QA standup brief with:

1. **Headline** — one sentence: "N failures: M real bugs, F flakes, L locator-changes" or "All green ✓"

2. **Top 3 to triage today** — ranked by impact (REAL_BUG with high confidence > LOCATOR_CHANGE > FLAKY_TEST). For each:
   - Short test name (just ClassName.methodName, no full FQN)
   - 1-line root cause (use the rootCause field, or paraphrase exceptionMessage if rootCause is generic)
   - Severity (riskLevel field)

3. **One concrete action** — name the SINGLE failure to fix first today, with a 1-line "why this one".

4. **Security flag** — if any failure's testName contains "Owasp" or "Security" or "APISecurityTest", call it out separately as "⚠️ Security check needed: <test name>".

Plain prose. No headers beyond the 4 numbered items above. 12 sentences max for the whole reply.
```

That's it. One routine, one URL, one daily 5-minute brief. Total cost: 1 of your 25 daily routine runs (4%).

## What you can ignore now

The Java analyzers I built earlier today (`DailyFailureAnalyzer`, `APISecurityScanner`) and their pre-rendered output (`daily-summary/<date>.md`) are now **optional**. They produce a richer pre-formatted summary that's still committed by the workflow, but if you only use the Routine Run, you can:

- Ignore the `daily-summary/` folder entirely (claude.ai produces its own analysis)
- Treat the Java analyzers as "free, deterministic backup" that runs anyway

Or if you want to fully simplify:

```bash
# Optional cleanup (NOT done in this commit — keeping options open)
rm -rf src/main/java/com/egalvanic/qa/utils/ai/DailyFailureAnalyzer.java
rm -rf src/main/java/com/egalvanic/qa/utils/ai/APISecurityScanner.java
rm -rf daily-summary/
# Then edit .github/workflows/daily-ai-analysis.yml to remove the two `mvn exec:java` steps
```

I'm leaving them in place because: (a) they cost nothing to keep, (b) they provide a fallback if claude.ai ever changes, (c) the auto-committed `daily-summary/<date>.md` is itself a useful artifact even without the routine.

## Live verification

```
$ ls -la test-output/bug-detection-report.json
-rw-r--r-- 1 abhiyantsingh staff 4.0K Apr 27 19:32 test-output/bug-detection-report.json

$ git check-ignore -v test-output/bug-detection-report.json
.gitignore:17:!test-output/bug-detection-report.json   test-output/bug-detection-report.json
# (the negation rule matches → file is ALLOWED in git)

$ git check-ignore -v test-output/screenshots/foo.png
.gitignore:9:test-output/screenshots/   test-output/screenshots/foo.png
# (folder rule matches → screenshots stay ignored)
```

After commit + push:

```
https://raw.githubusercontent.com/eg-abhiyantsingh/Egalvanic_Web-main/main/test-output/bug-detection-report.json
```

becomes a stable URL claude.ai can fetch without auth.

## Why this is the cleanest possible setup

| | Before this commit | After this commit |
|---|---|---|
| Where the routine fetches data from | A pre-rendered Markdown summary I wrote | Raw JSON straight from the test framework |
| Layers of analysis | 2 (Java analyzer + claude.ai routine) | 1 (claude.ai routine only) |
| Things to debug if output is wrong | 3 (test, Java analyzer, routine prompt) | 2 (test, routine prompt) |
| Output format | Pre-formatted by my analyzer | Whatever you ask in the prompt |
| Cost (your quota) | 1 of 25/day | 1 of 25/day |
| Setup time | Configure 3 routines | Configure 1 routine |

The simplification removes a layer that was doing work the routine could do anyway. Your routine prompt now has full control over formatting (rank/group/highlight however you want) without me hardcoding it in Java.

---

_Per memory rule: this changelog file is for learning + manager review. The single routine prompt above is what you copy-paste into claude.ai right now._
