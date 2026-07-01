# `egalvanic-api-quality` plugin + git hook — distribute the API check to every dev

- **Title:** Packaged the `api-contract-review` skill as an installable Claude Code **plugin** (with a
  repo-root **marketplace**) and added a **git hook** that runs the live probe before push/PR. Devs now
  get the pre-PR API check with one `/plugin install`, and can gate pushes automatically.
- **Date:** 2026-07-01
- **Time:** 17:00
- **Prompt:** "yes" — to the offered follow-ups (package as installable plugin + marketplace; add a
  ready-to-use pre-commit hook).

## What changed
**Plugin + marketplace (distribution):**
- `plugins/egalvanic-api-quality/.claude-plugin/plugin.json` — plugin manifest (name/version/description/
  author/keywords).
- `plugins/egalvanic-api-quality/skills/api-contract-review/` — the skill bundled in the plugin (mirrors
  `.claude/skills/api-contract-review/`, verified identical).
- `.claude-plugin/marketplace.json` (repo root) — marketplace `egalvanic-qa` listing the plugin with
  `source: ./plugins/egalvanic-api-quality`.
- Devs install with:
  `/plugin marketplace add eg-abhiyantsingh/Egalvanic_Web-main` → `/plugin install egalvanic-api-quality@egalvanic-qa`.

**Git hook (automated gate):**
- `.claude/skills/api-contract-review/git-hooks/pre-commit` — the hook body (works as pre-push or
  pre-commit). OPT-IN: exits 0 with a reminder until configured, so it never blocks an unconfigured dev.
  When configured (`EG_API_BASE` + `EG_TOKEN` + `EG_API_PATHS`/`.api-contract-paths`), it runs
  `probe_endpoint.py` on each touched endpoint and **blocks with exit 1 if any FAILs** (override with
  `--no-verify`). Locates the probe in the skill folder, `~/.claude/skills`, or the plugin.
- `.claude/skills/api-contract-review/git-hooks/install.sh` — installs it (default pre-push; `pre-commit`
  arg supported); backs up an existing hook.
- README updated with concrete install steps for both plugin and hook, and the `.api-contract.env` /
  `.api-contract-paths` config (token file to be gitignored).

## Distribution model (shift-left, three tiers)
1. **Plugin** (recommended) → one command per dev, versioned, works across their repos.
2. **Per-repo** `.claude/skills/` copy → auto-discovered, no install.
3. **Git hook** → automated pre-push/PR gate for teams that want enforcement, using the same contract as
   QA's Parallel Suite 3. Dev editor (skill) + push gate (hook) + scheduled CI (Suite 3) = the same one
   contract enforced at three points.

## Notes
- Secure by default: probe verifies TLS (`--insecure` only for self-signed dev/QA; refuses prod), read-only
  GETs, tokens never committed (`.api-contract.env` is gitignored).
- Validated the plugin/marketplace structure before shipping.
