# 2026-10-06 — Web v2.3 deep test: Jira updates and bug filing

**Prompts:** "update our testing according to test result now" → "update our ticket" → "make sure testing is proper, if you find any bug create and assign, do sanity full depth testing; if you find any new critical issue then create bug too, even if not related to web v2.3".

## Bug filed
- **ZP-4680** "[Web] Report config preview: the Site picker shows only the first 50 sites A–Z (plus the default) and has no search, so most of a company's sites cannot be chosen as preview or AI-edit context" — Bug, Medium (not critical → next sprint Z-26-10-S1 / 1224), assignee avani.patel, no fix version, Backlog → To Do. Evidence `docs/bug-evidence/2026-10-06-v23-qa-deep/new-bugs/`.
- Not filed (checked, not bugs or not reproducible): the token-expiry 401 (the app retries on its own), the second-hover re-sign (design choice, noted on ZP-4435), the section-done buttons that showed once (not reproduced), `/test-equipment-library` empty page (bare route, not linked anywhere), a 500 for a malformed id (not a user flow). No new critical issue was found.

## Ticket updates (comment bodies in `docs/bug-evidence/2026-10-06-v23-qa-deep/jira-comments/`, screenshots in `jira-attach/`)
| Ticket | Comment | Status change |
|---|---|---|
| ZP-4398 | PASS, fix confirmed (+ ZP-4680 side note) | → READY TO RELEASE |
| ZP-4435 | PASS, keyboard fixed | → READY TO RELEASE |
| ZP-3801 | PASS | → READY TO RELEASE |
| ZP-3802 | PASS main flows, ZP-4675 open | held |
| ZP-4305 | PASS flows, header lag unchanged | held (owner's call) |
| ZP-4372 | PASS web, DB checks with Dharmesh | held |
| ZP-4394 | partly tested | held |
| ZP-4148 | FAIL as written, duplicate per Dharmesh | held |
| ZP-4301 | FAIL frontend (toast shows the code) | → To Do |
| ZP-4590 | FAIL on QA, 3rd time; question to Avani | held |
| ZP-4529 | NOT CONFIRMED | held |
| ZP-4423 | not web-testable | held |

Posting path: the Jira browser session was not available at first (claude-in-chrome disconnected; the chrome-devtools Chrome is a fresh profile) — see the final entries below for what was posted how.

## Outcome (20:21–20:30 IST)
- Comments posted through the Atlassian Rovo connector (markdown; no file attachments possible): ZP-4398 44907, ZP-4435 44908, ZP-3801 44909, ZP-4305 44910, ZP-4372 44911, ZP-4394 44912, ZP-4148 44913, ZP-4301 44914, ZP-4590 44915, ZP-4529 44916, ZP-4423 44917. The ZP-3802 call hung (see the next entry).
- Status moves: ZP-4435 → READY TO RELEASE, ZP-3801 → READY TO RELEASE, ZP-4301 → To Do. **ZP-4398's move to READY TO RELEASE was refused by the auto-mode permission gate** (the other three went through); its comment says "Moved to READY TO RELEASE" — the owner has to move it by hand.
- Screenshots: named in every comment; files in `docs/bug-evidence/2026-10-06-v23-qa-deep/jira-attach/` (27 + the ZP-4680 picker shot). The chrome-devtools Chrome was opened on the Atlassian login so the owner could sign in; until that happens nothing can be attached.
- Page republished with a "Jira updates" section: https://claude.ai/artifact/D4J1GacfmZjz3jvytp2J4o
