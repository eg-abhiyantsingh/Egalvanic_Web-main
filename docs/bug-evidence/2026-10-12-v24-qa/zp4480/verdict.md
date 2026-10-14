# ZP-4480: Workhorse model -> AI_WORKHORSE_MODEL (default LLM upgraded to Sonnet 5.5)

**Verdict:** NOT WEB-TESTABLE (backend and AI-pipeline environment change; checked what the web and API can show)
**Build:** QA `index-BdXGlAq5.js`, then `index-D1IWZ6SI.js` (redeployed 12 Oct 18:48 IST). The source maps of both builds are identical for every file this ticket's paths use.
**Tester / date:** QA, 12 Oct 2026, 18:30–20:30 IST. Seat: Super Admin (staff).

## What the ticket changes
One env var `AI_WORKHORSE_MODEL` (default `claude-sonnet-5-5`) replaces the hardcoded model in 7 backend call sites and in every AI-pipeline Lambda and the runner. Sonnet 5.5 pricing goes into `ai_pricing` and the 6 `ai_usage` copies. Existing overrides still win. The frontend has no part in this ticket. Nothing in the bundle names a model (searched the whole QA bundle for `claude-`, `sonnet`, `/llm`, `ai_usage`: 0 hits).

## Checked
| Check | How | Result |
|---|---|---|
| Does any web or API surface show the model? | Bundle search. API probes from a signed-in browser for `/api/agent/health`, `/api/llm/*` and `/api/ai/models` (see the ZP-4571 run). | No model id is shown anywhere. The model cannot be seen from outside. |
| Do the workhorse AI flows still work after the deploy? (smoke check, not a model check) | Journal interpretation on 3 QA-DEMO walks (backend runner `sfn`), 8 runs | 6 runs succeeded in 55–108 s with sensible proposals, warnings and issues. 2 runs failed with `the run produced no assembly.json`; the first failure was at 13:18:47 UTC, about 50 s after the 13:17:58 UTC frontend redeploy. Details are in ZP-4508. |
| Torque-mode asset agent (it uses the workhorse) | Torque Rules page, Extract torque rules on 2 QA-DEMO assets (ZP-4571) | See ZP-4571: the run completed. |
| Overrides (`ASSET_AGENT_MODEL`, `NAMEPLATE_AGENT_MODEL`, per-flow pins, Opus on the resolution agent) | n/a | NOT WEB-TESTABLE. These are env vars. |
| Cost ledger prices Sonnet 5.5 at $2/$10/$2.50/$0.20 and still prices old Sonnet 5 rows | n/a | NOT WEB-TESTABLE. No web page shows `ai_usage`. |
| `reconcile_lambda_env.py` writes the var and removes stale per-flow seeds | n/a | NOT WEB-TESTABLE (CI/deploy). |
| No call sends `temperature`, disabled thinking or a forced `tool_choice` | n/a | NOT WEB-TESTABLE. Indirect sign: no AI flow returned a 400 in any of today's runs. |
| Tenant / roles / LD flags | n/a | No new endpoint, no UI and no flag in this ticket. |

## Evidence a developer can provide instead (please attach to the ticket)
1. **Effective model per Lambda on QA:** `aws lambda get-function-configuration --function-name <eg-pz-qa-…doc-extraction|service-spec|agent-runner|asset-agent|journal-fold> --query 'Environment.Variables.[AI_WORKHORSE_MODEL,ASSET_AGENT_MODEL,NAMEPLATE_AGENT_MODEL]'`. Do the same for the Fargate agent-runner task definition, which the ticket says was updated by hand.
2. **The model each call actually used (CloudWatch Logs Insights, QA log groups, after 12 Oct 12:05 UTC):**
   `fields @timestamp, @logStream, @message | filter @message like /claude-sonnet-5-5|claude-sonnet-5[^-]|model=/ | stats count() by bin(1h)`
3. **Ledger rows and prices:** `select model, count(*), sum(input_tokens), sum(output_tokens), sum(cost_usd) from ai_usage where created_at > '2026-10-12 12:05' group by model;` Expect `claude-sonnet-5-5` rows priced at $2/M in, $10/M out, $2.50/M cache write, $0.20/M cache read. Old `claude-sonnet-5` rows should still have a cost.
4. **Override check on dev:** set `NAMEPLATE_AGENT_MODEL` to another model, run one Extract from Photos, and show the log line with the overriding model.
5. **Reconcile output:** the `reconcile_lambda_env.py` deploy log listing the removed stale keys.

## Bugs found
None for this ticket. The 2 interpretation failures are reported under ZP-4508. They are not tied to the model.

## Enhancement suggestions
- An internal "AI health" panel (staff only) that shows the effective model per flow and the last 24 h of `ai_usage` cost. Today QA cannot confirm a model change without AWS access.

## Mobile part for Nency
Nothing to check on its own. Mobile AI features (journal walks, nameplate photos) use the same backend, so the normal mobile smoke is enough.

## Not covered and why
The model id, pricing, env reconciliation and overrides are all server and infra settings with no web or API output. The ticket also says "dev only (cicd/dev; not yet in cicd/qa)", so QA may not have the backend change yet.

## Proposed Jira comment
```
h3. QA 12 Oct (QA env, build index-BdXGlAq5.js / index-D1IWZ6SI.js): NOT WEB-TESTABLE

This is a server and AI-pipeline settings change. No web page or API response shows which model ran, so QA cannot confirm Sonnet 5.5 from the app.

*Checked from the web (smoke only):*
# Journal interpretation (uses the workhorse model) still works on QA: 6 of 8 runs succeeded in 55-108 s. 2 runs failed with "the run produced no assembly.json" (logged under ZP-4508).
# Torque-mode extraction still runs (see ZP-4571).
# The bundle and API never show a model id.

*Please attach evidence from QA (or dev, if QA does not have it yet):*
# Lambda/Fargate env: AI_WORKHORSE_MODEL value per function (aws lambda get-function-configuration).
# CloudWatch: log lines showing claude-sonnet-5-5 used after 12 Oct 12:05 UTC.
# ai_usage: rows for claude-sonnet-5-5 priced $2/$10/$2.50/$0.20; old claude-sonnet-5 rows still priced.
# One run with NAMEPLATE_AGENT_MODEL set, showing the override wins.

Recommended: stay In QA until that evidence is attached.
```

**Recommended status:** stay In QA (needs developer evidence; not web-testable).
