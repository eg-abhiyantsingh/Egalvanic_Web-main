# "Extract from Photos" filled only core attributes, leaving every readiness-scored engineering field blank — replaced with a Sonnet nameplate agent that writes node.* columns

**QA verdict — #1058 / #1246 / pipeline #68: PARTIAL (shipped + contract confirmed; core not web-observable)**
**Tested:** 2026-09-02 · acme.qa.egalvanic.ai · bundle grep + live API + UI walk.
**Artifact:** https://claude.ai/code/artifact/bd6a24fd-514e-4045-b398-6db7df1771dc

## Env claim WRONG: ticket says "absent from cicd/qa" — it is on QA
QA bundle index-BCC_7hbn.js contains: "nameplate-agent", mode:"nameplate", "Extract from Photos" x3,
"Overwrite existing values" ("If unchecked, only empty fields... will be filled"), "Extract asset name"
("AI will generate a descriptive name"), "Extract asset subtype" ("AI will determine the subtype from...").
POST /api/extraction/nameplate-agent/apply exists (400 "node_id is required", summary "Persist a
NAMEPLATE-flavor config to a node"). So frontend #1246 + apply route ARE deployed to QA.

## Confirmed (presence + contract)
- Button ships; 3-checkbox gate (Overwrite fill-blanks, name gate, subtype gate) all present in bundle.
- Backend apply route registered + validating.
- Target fields exist (asset Engineering tab = the readiness fields that stayed blank).

## NOT verifiable from web (stated, not skipped)
- Sonnet extraction filling node.* from a PHOTO — external asset-agent Lambda + needs a legible nameplate
  photo fixture (acme sandbox has none). THE CORE OF THE TICKET.
- Fill-blanks vs Overwrite runtime effect, name/subtype gate at apply — need a real extraction run.
- Bulk counter/Stop/apply-as-they-land, 500-asset cap — couldn't reach Edit-Core-Attributes bulk surface
  (grid "Bulk Edit" = Export/Import/Template menu, not this; the surface is photo/edit-gated).
- 413 photo-resize — needs fixture, Lambda-internal.
- Tenant-scoping 404 — ATTEMPTED via demo tenant but demo has only 2 SLDs, no nodes → no foreign node id
  constructible. Genuinely blocked.
- ai_usage ledger + flavor split, polling 5xx/4xx, Step Function — infra/logs.

## To finish
Attach a legible nameplate photo to a QA breaker → run Extract → confirm manufacturer/trip/pole/AIC fill +
persist. That one fixture unlocks single-asset + fill-blanks + overwrite + name/subtype. Bulk/ledger/413/
polling/StepFn → pipeline repo + CloudWatch (hand off like ZP-3855). Tenant-404 → needs a populated 2nd tenant.

NOT stamped PASS: this ticket exists because the old tool "could call a page correct without looking";
stamping green on extraction I never observed would repeat that error. No mutations — all probes rejected/read.
