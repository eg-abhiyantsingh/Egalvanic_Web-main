# LaunchDarkly Onboarding Log (working file)

Living log for the in-progress LaunchDarkly onboarding. A new agent session can
resume from **Next step** below. This file is deleted and replaced by
`LAUNCHDARKLY.md` when onboarding completes.

## Checklist

- [x] Step 0 — Onboarding log created (`docs/LAUNCHDARKLY_ONBOARDING.md`)
- [x] Step 1 — Explore project: **done**
- [x] Step 2 — Detect agent: **done** (`claude-code`)
- [x] Step 3 — Companion skills installed: **done**
- [ ] Step 4 — MCP server: **in progress**
- [ ] Step 5 — SDK install (detect → plan → apply): not started
- [ ] Step 6 — First feature flag: not started
- [ ] Follow-through — `LAUNCHDARKLY.md` + editor rules: not started

## Context

- **Agent:** claude-code
- **Stack:** Java 11, Maven (`qa-automation-suite`). Selenium 4.29 + TestNG 7.8 +
  REST Assured QA automation framework (not a deployed service — test harness
  with `main()` entry points and TestNG suites).
- **Existing LaunchDarkly usage:** none found (searched `launchdarkly`,
  `ldclient`, `LDClient` across `src/` and `pom.xml`).
- **Environment type:** server-side Java → **Java server SDK** is the likely fit;
  target app/module confirmation pending at SDK-detect step (repo is a test
  framework, so the "runnable app" question needs user input).
- **Project key / environment key:** not yet known.

## MCP

- Configured: **config written, pending user OAuth + session reload.**
- Hosted server entry added to `.mcp.json` (project root):
  `https://mcp.launchdarkly.com/mcp/launchdarkly` (type: http, OAuth — no API key).
- Probe result (this session): MCP tools not yet visible → user must approve the
  server and authenticate (run `/mcp` in Claude Code, or restart the session),
  then say "continue LaunchDarkly onboarding".

## Commands run

- `npx skills add launchdarkly/agent-skills --skill onboarding -y`
- `npx skills add launchdarkly/ai-tooling --skill launchdarkly-flag-create launchdarkly-flag-discovery launchdarkly-flag-targeting launchdarkly-flag-cleanup -y --agent claude-code`

## Blockers / errors

- None so far.

## Next step

**Step 4 (verify): Probe LaunchDarkly MCP tools after the user completes OAuth /
reloads the session.** If MCP still unavailable, fall back to ldcli/REST and
continue to **Step 5: SDK install** (target-app confirmation needed — this repo
is a QA test framework, so ask the user what to integrate: this framework, the
app under test's repo, or a demo app).
