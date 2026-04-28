# Claude.ai Routine Runs — setup prompts (3 ready-to-paste routines)

**Date / Time:** 2026-04-28, 12:30 IST
**Branch:** `main`
**Context:** User showed Team-plan usage with `0 / 25` routine runs available today, asked if I could connect to it from code.

## TL;DR

I (Claude Code, the CLI) **cannot** programmatically invoke the Routine Runs feature in claude.ai — that's a UI feature in a different Claude product (consumer/Team), not the Claude API. But the framework I built earlier today writes test reports to raw GitHub URLs, which is exactly what Routine Runs needs as input.

**You don't need to share an API key with me.** Configure the 3 routines below in claude.ai (Settings → Routines → Create), each pointing at a raw GitHub URL from our repo, and they'll run automatically without any code I write knowing about your account.

## Where to set them up

In the claude.ai UI:

1. Click your name in the bottom-left → **Settings**
2. Scroll to **Routines**
3. Click **+ New Routine** for each of the 3 below

## Routine 1 — Daily Test Failure Analyzer

**Frequency:** Daily, after CI typically settles (e.g., 09:00 IST = 03:30 UTC)
**Name:** `eGalvanic — Daily test failure summary`

**Prompt:**

```
Fetch this URL and produce an executive summary suitable for a 5-minute QA standup:

https://raw.githubusercontent.com/eg-abhiyantsingh/Egalvanic_Web-main/main/daily-summary/<TODAY>.md

(Replace <TODAY> with today's YYYY-MM-DD. If the file 404s, also try yesterday's date.)

Then:
1. Summarize the top 3 failures by impact (real product bug > test-side issue)
2. For each, name the most likely root cause in one sentence
3. Recommend ONE failure to triage first today, with a one-line reason
4. End with a 1-sentence trend note vs yesterday (failure count up/down/same)

Be concise. 8 sentences max for the whole reply. No headers. No filler.
```

**Why this works:** The file at that URL is auto-generated and committed by `.github/workflows/daily-ai-analysis.yml` every day at 04:00 UTC and after every Parallel Suite 2 run. The Routine doesn't need credentials — `daily-summary/*.md` is in a public-readable path.

## Routine 2 — Daily API Security Scanner

**Frequency:** Daily at 09:30 IST (or whatever fits your AppSec review cadence)
**Name:** `eGalvanic — Daily API security triage`

**Prompt:**

```
Fetch this URL:

https://raw.githubusercontent.com/eg-abhiyantsingh/Egalvanic_Web-main/main/daily-summary/security-<TODAY>.md

(Replace <TODAY> with today's date. Fall back to yesterday if 404.)

Then:
1. State the security posture in one sentence (GREEN / N findings).
2. If any findings: identify the single MOST IMPORTANT one to investigate today, with a 1-sentence rationale.
3. Suggest the concrete next step (manual curl, request a tenant-2 account for full IDOR test, or "no action — all clear").

Plain prose, no headers, 6 sentences max. Output should be readable by an AppSec engineer in under 60 seconds.
```

## Routine 3 — On-Demand Bug Report Generator

**Frequency:** On-demand only (don't schedule — invoke when needed)
**Name:** `eGalvanic — Convert ready-bug to Jira ticket`

**Prompt template (you'll paste a specific URL each time):**

```
Fetch this URL (a single failure's ready-bug markdown):

[PASTE: https://raw.githubusercontent.com/eg-abhiyantsingh/Egalvanic_Web-main/main/ready-bug/<filename>.md]

Then transform into a Jira-ticket-ready format:

**Title:** [a 6-12 word summary, action-oriented, e.g., "Issue Class blank-submit accepts invalid form"]
**Severity:** [HIGH / MEDIUM / LOW]
**Steps to Reproduce:**
1. ...
2. ...
3. ...
**Expected Result:** [one sentence]
**Actual Result:** [one sentence + relevant exception line]
**Screenshot:** [reference the attached file]
**Environment:** acme.qa.egalvanic.ai
**Detected by:** Automated test [test name] on [date]

Keep total length under 250 words. If the original .md has an "AI-enhanced" root cause, include 1 sentence from it under "Notes for dev:".
```

## Why these 3 specifically

| Routine | Cost (your quota) | Saves you per day |
|---|---|---|
| Daily failure summary | 1 of 25 | 15-30 min of report-reading |
| Daily security triage | 1 of 25 | 10-15 min of OWASP scan review |
| On-demand bug → Jira | 1 per use | 5-10 min per Jira ticket |

3 daily + ~5 on-demand = ~8/25 routine runs/day. You have 17 left for other use cases.

## Setup verification

After creating each routine:

1. Click **"Run now"** on the routine to test
2. Check the output appears in your claude.ai sidebar history
3. If 404 (no daily-summary file yet for today): wait until our 04:00 UTC cron runs, OR trigger Parallel Suite 2 manually to populate `test-output/bug-detection-report.json` first

## What this changes vs. the API-key path

The Routine Runs setup gives you the same outcome as setting `CLAUDE_API_KEY` in GitHub Actions, but:

| | Routine Runs (this approach) | API Key in CI |
|---|---|---|
| Cost | $0 (your Team plan quota) | ~$0.40/month at our usage |
| Where output goes | Your claude.ai conversation history | `daily-summary/*.md` in main (already committed) |
| Read by who | You, anyone with claude.ai access | Anyone with repo access |
| Trigger | Scheduled in claude.ai UI | Scheduled in GitHub Actions |
| Setup complexity | Click 3 buttons in claude.ai | Add 1 GitHub Actions secret |

You can do **both** — no conflict. The CI-side AI enrichment writes prose into the .md files, then your Routine Runs fetches those same .md files and produces a derivative summary. Net cost stays under $1/month.

## Why I can't do this from code

The Routine Runs API surface (claude.ai's `/api/routines/*`) is gated behind your personal session cookies, not by an API key. There's no public REST endpoint for "create a routine on user X's behalf". Anthropic deliberately scopes Routines to UI-driven setup so users own and audit them.

If/when Anthropic ships a Routines API, the integration becomes one-shot. Until then, the 3-minute manual setup above is the right path.

---

_Per memory rule: this changelog file is for learning + manager review. The 3 prompt templates can be copy-pasted directly into claude.ai Settings → Routines._
