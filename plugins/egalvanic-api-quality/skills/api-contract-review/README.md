# api-contract-review — for every developer

A Claude Code skill that checks an API endpoint against eGalvanic's non-functional contract **before you
open a PR** — pagination, filters/search, max limit (≤50/100), default page size (10–20), returns expected
records, responds within a few seconds, plus baseline **security** and **performance**. It answers the
QA-team ask (Dharmesh/Mukul) at the point where it's cheapest to fix: in the developer's editor.

## What's in here
| File | Purpose |
|---|---|
| `SKILL.md` | The skill Claude runs (when-to-use, workflow, output). |
| `reference.md` | Full contract spec + thresholds + security/performance checks. |
| `pr-checklist.md` | Copy-paste checklist for the PR description. |
| `probe_endpoint.py` | Zero-dependency live probe (stdlib only); exits non-zero on FAIL. |

## How a developer uses it
1. **In Claude Code**, while building/reviewing an endpoint, say: *"review my API before I open the PR"* or
   `/api-contract-review`. Claude scopes the changed endpoints from your git diff, statically reviews the
   handler against the contract, optionally live-probes a dev server, and outputs the filled checklist.
2. **Or run the probe directly** against a dev server:
   ```
   EG_TOKEN=<your dev token> python3 probe_endpoint.py \
     --base https://acme.dev.egalvanic.ai/api --path /your-endpoint --insecure
   ```

## Getting it into every dev's Claude Code
Pick one:

**A. Install as a plugin (recommended — one command per dev).** This repo ships a marketplace
(`.claude-plugin/marketplace.json`) with the `egalvanic-api-quality` plugin. In Claude Code:
```
/plugin marketplace add eg-abhiyantsingh/Egalvanic_Web-main
/plugin install egalvanic-api-quality@egalvanic-qa
```
That installs the skill + probe + git hook for the developer, versioned; `/plugin update` pulls new versions.

**B. Per repo (no install):** copy this `api-contract-review/` folder into the repo's `.claude/skills/` and
commit it — Claude Code auto-discovers it for everyone on that repo.

**C. Personal (all your repos):** copy it into `~/.claude/skills/`.

## Install the git hook (optional pre-PR gate)
Run the probe automatically before you push / open a PR:
```
.claude/skills/api-contract-review/git-hooks/install.sh          # installs as pre-push (recommended)
.claude/skills/api-contract-review/git-hooks/install.sh pre-commit
```
Then configure it (opt-in — the hook does nothing until these are set, so it never blocks an unconfigured dev):
```
# .api-contract.env  (add this file to .gitignore — it holds your token)
EG_API_BASE=https://acme.dev.egalvanic.ai/api
EG_TOKEN=<your dev token>
EG_API_INSECURE=1            # self-signed dev/QA cert only
```
```
# .api-contract-paths  (endpoints your change touches, one per line)
/node_classes
/company/{id}/opportunities
```
On push, the hook probes each path and **blocks with a non-zero exit if any endpoint FAILs the contract**
(override in a pinch with `git push --no-verify`). Same contract QA's Parallel Suite 3 enforces on a schedule.

## As a PR gate (optional)
Because `probe_endpoint.py` exits non-zero on any FAIL, you can wire it into a pre-commit hook or a CI
step for the endpoints a change touches — the same contract the QA **Parallel Suite 3** enforces on a
schedule. Dev-side skill = shift-left; Suite 3 = safety net.

> Tokens are personal — pass them via `EG_TOKEN`/`--token`, never commit them. `--insecure` is only for
> self-signed dev/QA certs; never use it against production (and the probe refuses a prod-looking base URL).
