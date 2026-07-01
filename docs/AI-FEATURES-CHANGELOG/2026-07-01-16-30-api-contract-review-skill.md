# `api-contract-review` — a Claude skill for developers to self-check APIs before PRs

- **Title:** New Claude Code skill (`.claude/skills/api-contract-review/`) that lets any developer check an
  API endpoint against the eGalvanic non-functional contract (pagination / filters / max-limit 50-100 /
  default 10-20 / expected records / response time) + baseline security + performance — before opening a
  PR. Includes a zero-dependency live probe validated against QA.
- **Date:** 2026-07-01
- **Time:** 16:30
- **Prompt:** Mukul Panchal — "generate a Claude skill that every developer can use to check [the API
  non-functional requirements + security + performance] when they develop, before they generate PRs" (+
  "cover all modules, all APIs, while creating/editing").

## What changed
`.claude/skills/api-contract-review/`:
- **SKILL.md** — triggers on "check my API / API review before PR / pagination check / API security". Two
  modes: static (review the branch diff's handler against the contract — no server needed, works in CI) and
  live (probe a dev server). Emits a PR-ready pass/warn/fail checklist; FAIL blocks the PR.
- **reference.md** — the full contract + thresholds (pagination page/per_page honored, default ≤20, max
  ≤100, latency bands, payload, and security: auth-required, IDOR, input-validation→400-not-500, injection,
  no data leakage, token-in-header). Mirrors what QA's Parallel Suite 3 enforces, so dev-side == CI.
- **pr-checklist.md** — copy-paste checklist for the PR description.
- **probe_endpoint.py** — stdlib-only live probe; checks records/shape, pagination-honored, max-limit cap,
  default size, filter safety, response time, and no-token→401. TLS verified by default (`--insecure`
  opt-in for self-signed dev/QA); refuses a prod-looking base URL; read-only GETs; **exits non-zero on any
  FAIL** so it doubles as a pre-commit / CI gate.
- **README.md** — how every dev adopts it (per-repo `.claude/skills/`, personal `~/.claude/skills/`, or
  package as a plugin for the team marketplace).

## Validated live (QA)
- `probe_endpoint.py … /node_classes` → correctly reported **3 FAIL** (pagination ignored, unbounded
  default 554, max-limit not capped) + WARN (3034ms) + PASS (records, filter-safety, auth 401). Exit 1.
- `… /edge_classes` → 1 FAIL (pagination) + WARN (51 > 20) + PASS. Exit 1.
- Auth check confirmed no-token → HTTP 401 on both.

## The shift-left story (answers "done by the devs developing/merging/reviewing")
This pushes the API non-functional check to the point it's cheapest to fix — the developer's editor, before
the PR — instead of catching it later in QA or prod. It's the same contract QA's **Parallel Suite 3**
runs on a schedule, so dev self-check and CI enforcement agree. "Cover all modules / all APIs while
creating/editing" is then an incremental, per-module effort the devs drive with this skill (list + create +
edit + delete endpoints), with Suite 3 as the backstop — matching the team's stated plan to take the
non-functional requirement module-by-module in parallel.

## Depth explanation (for learning + manager review)
A skill is the right tool here (not another CI suite): CI catches issues *after* the code is pushed; a skill
catches them *while the developer writes the code*, and it's portable to the backend/frontend repos where
the APIs actually live (the QA repo can't test code that isn't in it). Keeping the thresholds identical to
Suite 3 means there's one contract, enforced in two places (left = skill, right = CI). The probe is
stdlib-only and secure-by-default so any developer can run it with zero setup and no MITM exposure.
