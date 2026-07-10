# SLD V3 load-performance & payload benchmark (QA coverage)

**Date:** 2026-07-10 UTC · **Branch:** main

## Ask
Cover the "SLD V3 Payload Optimization" ticket (follow-up to ZP-2985): reduce DB queries + large-payload
serialization on SLD/view loads; target small view ~1.9s→~0.8s, all-view queries 15-18→3, address
node_terminals (~8.6MB on 8k+ nodes); document before/after for small/medium/large; no API-structure regression.

## Framing
The optimization is a BACKEND change (DB + serialization) in the read-only product repo — not something this
QA repo can implement. What QA delivers is the **black-box benchmark + verification harness** that measures and
proves the observable acceptance criteria and guards against regression.

## What
New `SldV3PayloadBenchmarkApiTest` (Suite 3). Per SLD (sampled or `-Dsld.benchmark.ids=`), measures
`GET /sld/v3/{id}`: load latency, total payload bytes, per-node `node_terminals` total (the 8.6MB bottleneck),
the dominant top-level keys, and a `/sld/v2/{id}` before/after delta; classifies small/medium/large by node
count. HARD asserts 200 + JSON + the response keeps its top-level structure (id/nodes/edges/mappings —
regression guard). Report-mode; `-DSTRICT_SLD_PERF=true` gates latency/size-target misses. Writes
`reports/sld-v3-payload-benchmark.md`; wired into the consolidated report (own section + feeds Performance
findings) and the Suite 3 artifact.

## Live findings (immediate value — 4-SLD sample)
- **Every SLD load is 2.7–4.3s and ships `eg_forms` ≈ 1.4 MB regardless of SLD size** — even a 3-node SLD is a
  3.0 MB / 3.6s payload. A company-wide forms blob is serialized into every SLD load; this is the universal
  bottleneck (distinct from, and on top of, node_terminals).
- **`node_terminals` scales with node count** (14KB@15 nodes, 102KB@107 nodes) → ~8 MB at 8k nodes (matches ticket).
- **v3 ≈ v2** in latency/size on these SLDs — confirms "still not up to the mark."

## AC coverage
- ✅ load latency (v3 vs v2 before/after) · ✅ node_terminals total · ✅ payload size + dominant keys ·
  ✅ no API-structure regression (hard).
- ⚠️ **DB query count (15-18→3)** is NOT black-box observable — needs backend instrumentation/APM (e.g. a
  request-scoped `X-DB-Query-Count` header or a SQLAlchemy query-count assertion in a backend test). Documented
  in the report as the one AC QA can't verify over the API; flagged for the backend team.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/SldV3PayloadBenchmarkApiTest.java` (new)
- `suite-api-health.xml`, `.github/scripts/gen_api_suite_report.py`, `.github/workflows/parallel-suite-3.yml`
