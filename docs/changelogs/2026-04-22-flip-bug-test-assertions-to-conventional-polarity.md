# 2026-04-22 — Flip all 8 bug-verification assertions to conventional polarity (PASS=works, FAIL=bug)

## Prompt
> ✅ BUG001_No404Page, ✅ BUG005_NoMaxLengthOnIssueTitle, ✅ BUG008_NoRateLimiting — this 3 test case should fail but they are passing

## Model in use
Claude Opus 4.7 (1M context), xhigh effort, always-thinking on.

## Branch safety
Work on `main` of the **QA automation framework repo**. Production website repo untouched.

## The problem you caught

The 8 curated regression tests were written with an "inverted assertion" pattern:
- `assertTrue(bugPresent)` — test PASSES when bug is present, FAILS when fixed
- Design intent: document bugs + force dev to flip when they fix them

**Why this is bad for client reports:**
- CI dashboard shows **8/8 PASSED** — reads as "all is well"
- But actually means "all 8 bugs confirmed present" (bad)
- Client sees green ticks and thinks the product is healthy
- New team members / managers reading the report are misled

**What you expect (and what's correct):**
- `assertFalse(bugPresent)` — test PASSES when feature works, FAILS when bug present
- CI dashboard shows red for bugs → signal is truthful
- This is the conventional QA polarity used by every other test in the codebase

## What changed — all 8 assertions flipped

### [DeepBugVerificationTestNG.java](src/test/java/com/egalvanic/qa/testcase/DeepBugVerificationTestNG.java)

| Test | Before | After |
|---|---|---|
| BUG-001 (No 404) | `assertTrue(bugPresent)` | `assertFalse(bugPresent, "Invalid URL renders blank...")` |
| BUG-002 (CSP Beamer) | `assertTrue(count >= 1)` (ambiguous) | `assertEquals(blockedCount, 0, "X Beamer fonts CSP-blocked")` (tightened logic: counts transferSize=0 entries) |
| BUG-003 (Issue Class) | `assertTrue(dialogClosed)` | `assertFalse(dialogClosed, "form accepted blank Issue Class")` |
| BUG-004 (Raw API leak) | `assertTrue(leakPresent)` | `assertFalse(leakPresent, "raw backend error exposed in UI")` |
| BUG-005 (No maxLength) | `assertTrue(bugPresent)` | `assertFalse(bugPresent, "input accepted 2000+ chars")` |
| BUG-006 (Slow pages) | `assertTrue(avg >= 6000)` | `assertTrue(avg < 6000, "avg load X ms exceeds 6s threshold")` |

### [SecurityAuditTestNG.java](src/test/java/com/egalvanic/qa/testcase/SecurityAuditTestNG.java)

| Test | Before | After |
|---|---|---|
| BUG-007 (SameSite=None) | `assertTrue(bugPresent)` | `assertFalse(bugPresent, "auth cookie SameSite=None — CSRF surface")` |
| BUG-008 (No rate limit) | `assertTrue(bugPresent)` | `assertFalse(bugPresent, "6 failed logins, 0 rate-limit signals")` |

### BUG-002 specifically — also tightened the logic

The original test was ambiguous: `beamerFontCount >= 1` counted ALL Beamer font resource entries, both blocked and successful. So if CSP were fixed AND fonts loaded successfully, count would still be ≥ 1 and the test would still pass as "bug confirmed" — wrong either way.

New logic: count ONLY entries where `transferSize === 0 && duration < 10` — this is the signature of a CSP-blocked resource (the request never completed, nothing transferred). Other failure modes (DNS, network) give different timing profiles.

Then `assertEquals(blocked, 0)`:
- PASS if `blocked == 0` — no CSP blocking detected
- FAIL if `blocked > 0` — one or more fonts CSP-blocked, with count in the message

### Assertion messages rewritten

Every flipped assertion now has a failure message that:
1. **Names the bug** (`"BUG-004: Invalid asset URL exposes raw backend error..."`)
2. **Describes the defect** (`"Fix: catch the 400 in the frontend, show a generic 'Asset not found' message instead"`)
3. **Gives dev a concrete action** (not "remove this test")

This makes CI failure reports self-documenting — a dev reading the failure knows exactly what broke and how to start the fix.

### Class Javadocs updated

Both files now declare the contract explicitly:
```
Assertion contract (updated 2026-04-22):
  PASS  = feature works correctly (bug NOT detected in this run)
  FAIL  = bug detected — the assertion message describes the defect and fix
```

## What happens on the next CI run

Previously: 8/8 passed (misleading green).

Now, against current production, you should expect:
- BUG-001: **FAIL** (bug still present — there's no 404 page)
- BUG-002: **FAIL or PASS** depending on whether Beamer fonts currently blocked
- BUG-003: **FAIL** (bug still present — form accepts blank Issue Class)
- BUG-004: **FAIL** (bug still present — raw API error leaks)
- BUG-005: **FAIL** (bug still present — no maxLength)
- BUG-006: **FAIL or PASS** depending on current page load times
- BUG-007: **FAIL** (bug still present — SameSite=None)
- BUG-008: **FAIL** (bug still present — no rate limiting)

**This is good.** The RED in CI now correctly signals active bugs to the dev team. When dev fixes one, the corresponding test turns green. Client-facing reports show an accurate product health status.

## Compile
`mvn clean test-compile` → BUILD SUCCESS, 56 test sources.

## In-depth explanation (for learning + manager reporting)

### Inverted vs normal assertions — when each is appropriate

**Inverted (`assertTrue(bugPresent)`):**
- Good for: documenting a bug in a test that shouldn't run in normal CI (quarantine / disabled by default).
- Bad for: any test in your main CI pipeline. The false-green problem dominates.

**Normal (`assertFalse(bugPresent)`):**
- Good for: everyday tests, CI gates, regression suites, client reports.
- This is the default for a reason — people read "green" as "healthy" and "red" as "broken". Fighting that intuition creates communication debt.

I picked the inverted pattern initially because I was treating the test file as a "living bug report". That was wrong — the client doesn't care about the historical story, they care about "what's broken right now". Your intuition was correct.

### Why "Fix: X" in the assertion message matters

A CI failure message in TestNG shows up in the ExtentReport + JUnit XML + GitHub Actions UI. When a dev sees:

> `BUG-004: Invalid asset URL exposes raw backend error 'Failed to fetch enriched node details: 400' (internal API method name) in the UI. Fix: catch the 400 in the frontend, show a generic 'Asset not found' message instead.`

…they can start on the fix immediately without reading the test code or looking up the bug report. Short failure messages like "bugPresent=true" force them to dig, which wastes time. Long failure messages that explain the defect + suggest a fix are a force multiplier.

### How to interpret the new test outputs

| CI shows | What it means |
|---|---|
| All 8 green | Product is healthy — every curated bug is currently fixed / not reproducible |
| Red on BUG-007 only | SameSite=None regression — someone loosened the cookie setter. Alert security/backend team. |
| Red on BUG-008 only | Rate limiting broke — check AWS WAF rules or Cognito config |
| Red on BUG-001 | 404 page regressed or was never added |
| Red on BUG-006 | Page load perf regressed — run a lighthouse trace |
| etc. |

Each red test gives a specific signal. Before the flip, "green" was a meaningless signal (could mean bugs present OR bugs absent, you couldn't tell).

### The BUG-002 logic tightening — a real improvement

Before: `beamerFontCount >= 1` — passes if Beamer made any font requests. Doesn't distinguish blocked vs successful.

After: `blockedCount = Beamer fonts with transferSize=0 && duration<10`. Assert that this is 0.

Why `transferSize=0 && duration<10`: a CSP-blocked resource appears in `performance.getEntriesByType('resource')` with 0 transferSize (nothing came across the wire) and very short duration (blocked at the browser level before network). Successfully loaded fonts have real transferSize and duration. DNS failures have 0 transferSize but longer duration. Timeouts have 0 transferSize and 30s+ duration. The combined predicate uniquely identifies CSP blocks.

This is a real QA improvement — the original test could have given false positives (counting successful Beamer requests as "bug present") or false negatives (if Beamer didn't load at all, count would be 0 = "bug fixed" incorrectly).

## Rollback
`git revert <this-commit>` restores the inverted pattern. Not recommended — the inverted pattern was the source of the problem.

## What comes next

Run the curated suite in CI:
```bash
mvn clean test -DsuiteXmlFile=deep-bug-verification-testng.xml
```

You'll now see red ticks for bugs still in prod. Share the failure report with your dev team — each failure message names the bug and suggests the fix.

Once dev fixes a bug, the corresponding test turns green automatically. No test-file edit needed. When all 8 are green, your client report can legitimately say "all validated bugs resolved."
