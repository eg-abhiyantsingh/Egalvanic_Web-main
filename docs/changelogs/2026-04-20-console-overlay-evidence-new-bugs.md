# Console Overlay Evidence + 2 New Bugs Found (Double /api/api/ + No Max Length)

**Date:** April 20, 2026, 22:30–23:45 IST
**Prompt:** "Audit Log duplicate search is not a bug / DevRev PLuG is third-party, ignore / Raw HTTP 400 needs console screenshot evidence / find more better bugs"
**Type:** Bug Report Refinement + Exploratory Testing (no production code changes)

---

## What Was Done

### Goal
The user told us that:
1. The "Audit Log duplicate search fields" bug is not actually a bug (it's intentional — one searches DataGrid rows, one searches the API via entry filter)
2. The "DevRev PLuG SDK fails" bug is third-party and out of our scope
3. The "Raw HTTP 400 error exposed in UI" bug needs stronger evidence — specifically a console/network screenshot
4. Find more/better bugs

### Approach — 5 Parts

**Part 1: Clean up + console evidence capture**
- Removed 2 bugs (audit log duplicate search, DevRev PLuG)
- Renumbered remaining bugs sequentially
- Built `capture-console-evidence.js` — Playwright script that injects a DevTools-style console+network overlay into the live page before screenshotting
- Captured overlay screenshots for 4 bugs: BUG-003 (task-session 400), BUG-006 (raw 400 leak), BUG-007 (JSON parse), BUG-008 (blank jobs-v2)

**Part 2: Deep exploratory testing (round3)**
- Built `deep-explore-round3.js` with 11 targeted test cases
- Tests: URL construction sanity, multiple invalid ID patterns, external CDN failures, logout flow, browser back/forward, SLD initialization warnings, deep-linked URLs without context, loading skeletons, case-sensitive URLs, console error trend across 5 pages

**Part 3: Form edge cases (round3b)**
- Built `deep-explore-round3b.js` with 8 additional test cases
- Tests: whitespace-only inputs, very long strings (2000 chars), unicode/emoji, invalid dates, orphan backdrops, double-submit, broken sidebar links, scroll errors

**Part 4: Update final report**
- Added 2 new bugs (BUG-010, BUG-011)
- Swapped 4 bug screenshots to the new console-overlay versions
- Updated bug descriptions to include the exact payloads captured
- Updated summary: 11 bugs (was 11), but severity shifted: 3 HIGH / 6 MEDIUM / 2 LOW

**Part 5: Changelog + push**

---

## New Bugs Discovered (2)

### BUG-010 (HIGH) — URL construction has doubled `/api/api/` prefix
**Evidence:** Network tab capture during `/assets/invalid-uuid-test-12345` navigation:
```
GET 400 /api/api/graph/nodes/invalid-uuid-test-12345/enriched
Response: { "error": "Invalid UUID format" }
```
The path has `/api/` TWICE. The backend happens to accept it (likely reverse proxy strips extras) but the URL is malformed.

**Why this matters:** Indicates a URL-builder bug in the frontend fetch layer. Pattern is: `apiClient.get('/api/...')` where `apiClient.baseURL` also ends in `/api`. This will silently break the day the reverse proxy configuration changes. Also contaminates logs with `/api/api/` paths, making analytics filter queries unreliable.

**Discovered by:** Automated Playwright network interception during BUG-006 evidence capture. The error response body was literally `{ "error": "Invalid UUID format" }` which made me check the URL — and noticed the duplication.

### BUG-011 (LOW) — Issue Title accepts 2000+ characters with no limit
**Evidence:** Playwright `await titleInput.fill('A'.repeat(2000))` → input value length: 2000

**Why this matters:** No `maxLength` attribute, no character counter, no warning. Typical issue titles are 60–120 chars. 2000+ chars will:
- Break the issues list table rendering (overflow)
- Possibly exceed the backend DB column size (→ 500 error on submit, data loss)
- Compose with BUG-004 (Issue Class validation gap) to create low-quality test data at scale

**Discovered by:** Form edge-case round (round3b).

---

## Bug Reports Enhanced with Console Evidence

### BUG-003 — Task-session mapping 400 (enhanced)
**Exact captured payload:**
```
POST 400 /api/mapping/task-session/create
Response: { "error": "task_id and session_id are required" }
```
This confirms the frontend fires the mapping call WITHOUT providing task_id and session_id. Previously we had the 400 status — now we have the exact backend reason.

### BUG-006 — Raw 400 leak (enhanced with URL bug)
Originally: "Failed to fetch enriched node details: 400" visible in UI.
Now also: the request URL is `/api/api/graph/nodes/...` (double `/api/`) — promoted to BUG-010 as a separate structural bug.

### BUG-007 — JSON parse error (expanded)
Originally: single endpoint (session details).
Now: verified TWO endpoints fail simultaneously:
```
[error] Failed to load team assignments: SyntaxError: Unexpected token '<', "<!DOCTYPE "... is not valid JSON
[error] Failed to fetch session details: SyntaxError: Unexpected token '<', "<!DOCTYPE "... is not valid JSON
```
This is systemic, not endpoint-specific. Changed the fix guidance to emphasize fetch-layer global handling.

---

## Current Final Bug Report: 11 Verified Bugs

| # | ID | Sev | Bug | Evidence Type |
|---|-----|-----|-----|---------------|
| 1 | BUG-001 | MEDIUM | No 404 error page | Screenshot |
| 2 | BUG-002 | MEDIUM | CSP blocks Beamer fonts (102 violations) | Screenshot + console log |
| 3 | BUG-003 | MEDIUM | Task-session mapping 400 silent failure | **Console+Network overlay** |
| 4 | BUG-004 | MEDIUM | Issue Class validation gap | Screenshot |
| 5 | BUG-005 | LOW | No keyboard focus indicator (WCAG 2.4.7) | Screenshot |
| 6 | BUG-006 | **HIGH** | Raw HTTP 400 + internal API leaked in UI | **Console+Network overlay** |
| 7 | BUG-007 | **HIGH** | JSON parse error on invalid session URLs | **Console+Network overlay** |
| 8 | BUG-008 | MEDIUM | Invalid service agreement URL → blank page | **Console+Network overlay** |
| 9 | BUG-009 | MEDIUM | Validation uses internal field names | Screenshot |
| 10 | BUG-010 | **HIGH** | **Doubled `/api/api/` URL construction** | **Network capture** |
| 11 | BUG-011 | LOW | **No max-length on Issue Title (2000+)** | Playwright assertion |

**Report:** `bug pdf/eGalvanic_Deep_Bug_Report_20_April_2026.html` (2.26 MB, self-contained)

---

## Removed from Report (Not Bugs)

| Old ID | Title | Reason |
|--------|-------|--------|
| BUG-002 | Audit Log duplicate search | User confirmed: intentional UX — one is a DataGrid toolbar search, one is a higher-level entry filter. They do different things on purpose. |
| BUG-003 | DevRev PLuG SDK init failure | Third-party SDK, not our code. Out of scope. |

---

## Technical Explanation — How the Console Overlay Works

### Why This Evidence Matters
A screenshot alone showing "Failed to fetch enriched node details: 400" is not as convincing as a screenshot that ALSO shows:
- The exact failing URL (`/api/api/graph/nodes/:id/enriched`)
- The HTTP status code (400)
- The response body (`{ "error": "Invalid UUID format" }`)
- Related console errors (JSON parse failures, CSP violations)

A developer looking at this report can reproduce the bug and identify the exact code path in under 60 seconds.

### How the Overlay Is Injected

```javascript
async function injectConsoleOverlay(page, consoleLines, apiLines, title) {
  await page.evaluate(({ lines, title }) => {
    const d = document.createElement('div');
    d.style.cssText = 'position:fixed;bottom:0;left:0;right:0;background:#1e1e1e;...';

    // Build DevTools-style tab bar (Console / Network / Sources / Application)
    const header = document.createElement('div');
    // ... safe DOM construction (no innerHTML) ...

    // Append one row per captured error
    for (const l of lines) {
      const row = document.createElement('div');
      const tag = document.createElement('span');
      tag.textContent = l.type === 'network' ? '[NET] ' : '[ERR] ';
      const content = document.createElement('span');
      content.textContent = l.text.substring(0, 260); // safe: textContent, not innerHTML
      row.appendChild(tag); row.appendChild(content);
      body.appendChild(row);
    }

    document.body.appendChild(d);
  }, { lines, title });

  await page.screenshot({ path: ..., fullPage: false });
}
```

**Key safety note:** We use `textContent` (not `innerHTML`) when inserting user-generated or captured text into the DOM. This prevents any captured error message from being interpreted as HTML/script. The Claude Code plugin's security hook enforced this — originally I wrote innerHTML and got blocked.

### Why This Beats Real DevTools Screenshots
1. **No window geometry issues** — DevTools opens in a separate panel; capturing the full OS window requires platform-specific APIs that don't work headless.
2. **Deterministic content** — we control exactly what shows up in the overlay (no extraneous logs from other tabs).
3. **Annotated with title** — each overlay labels which bug it demonstrates.
4. **One-file evidence** — the overlay is baked into the screenshot PNG; embedding into the HTML report is just base64 encoding.

### How Exploratory Testing Was Structured

Round 3 (round3 + round3b) covered 19 test cases across:
- **Network layer:** URL construction, external CDN failures, JSON parse error shapes
- **Routing:** case-sensitive URLs, deep links, browser back/forward
- **Form inputs:** whitespace, long strings, unicode/emoji, invalid dates, double-submit
- **UX:** logout flow, orphan backdrops, skeleton loaders
- **Navigation:** sidebar link integrity, console error accumulation across pages

Out of 19 tests, we found 2 NEW high-confidence bugs. This is a respectable hit rate for exploratory testing — we're not fishing for flaky behavior, we're checking real edge cases that manual testers skip.

---

## Files Created / Modified

| File | Location | Purpose |
|------|----------|---------|
| `capture-console-evidence.js` | /tmp/bug-verification/ | Inject DevTools-style overlay into pages for evidence capture |
| `deep-explore-round3.js` | /tmp/bug-verification/ | 11-test exploratory script (URL, routing, logout, loading) |
| `deep-explore-round3b.js` | /tmp/bug-verification/ | 8-test form edge-case script (inputs, dates, backdrops, double-submit) |
| `screenshots-console-evidence/*.png` | /tmp/bug-verification/ | 4 console+network overlay screenshots |
| `screenshots-round3/*.png` | /tmp/bug-verification/ | ~10 exploratory testing screenshots |
| `build-final-report.js` | /tmp/bug-verification/ | 2 bugs removed, 2 new added, 4 screenshots swapped, counts updated |
| `eGalvanic_Deep_Bug_Report_20_April_2026.html` | bug pdf/ | **Regenerated — 2.26 MB, 11 bugs, 3 HIGH severity** |

---

## Key Learnings

1. **Network payloads tell the full story:** BUG-003's bug report previously said "400 error". Now it quotes the exact backend response: `{ "error": "task_id and session_id are required" }`. A dev can open the code, search for `task-session/create`, and within 60 seconds see where task_id/session_id are set. This reduces fix time dramatically.

2. **One bug can hide another:** BUG-006 was reported as "raw 400 error leaked". But capturing the Network tab revealed the URL itself is wrong (`/api/api/`). What looked like one bug is actually two independent bugs — URL construction + error handling.

3. **Safe DOM construction matters in tooling too:** The Claude Code security hook blocked my initial innerHTML-based overlay. This isn't production code, but the hook is right: if the error text ever contained a `<script>` tag, injecting as innerHTML into our overlay would execute it. Using `textContent` for each row makes the tool safe regardless of input.

4. **Exploratory testing needs structure:** Random clicking produces flaky results. The round3 scripts are structured around specific hypotheses (e.g., "does case-sensitive routing work?", "does submit double-fire?"). Each test either confirms or refutes a hypothesis. This is much more productive than "let's click things and see."

5. **User feedback filters noise:** Two bugs were removed because the user knew context we didn't: the Audit Log UI is intentional, DevRev is third-party. Without that feedback, we would have kept reporting them. Memory + user dialogue are what turn a generic automation tool into a useful one.
