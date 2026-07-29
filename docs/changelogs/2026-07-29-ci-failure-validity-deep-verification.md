# 2026-07-29 (part 2) — Deep validity check of the failing CI tests: are they real?

**Prompt:** "check in deepth fail test case are valid or not"

I adversarially re-verified each remaining Suite-3 red — trying to *falsify* my own earlier verdicts,
not confirm them. One of my own tests was proven INVALID (over-claim) and corrected.

## Verdicts

| Failing test | Verdict | Evidence |
|---|---|---|
| `MutationSemanticsApiTest.testAsyncWriteEventuallyConverges` | **VALID defect** | `GET /tasks/{sld}` → 500 on **all 8 SLDs tested** + a garbage UUID → GLOBAL backend regression, not our sandbox data |
| `MutationSemanticsApiTest.testDeleteIdempotency` | **VALID defect** | same global 500 |
| `CrudLifecycleApiTest.testTaskCrudLifecycle` | **VALID defect** | same global 500; write path + `GET /task/{id}` work, so data is intact |
| `DuplicateApiAuditTest.testFixCheckUnderscoreListPaginates` | **VALID defect, WRONG framing → reframed** | `GET /planned_workorder_line/` times out 45s with `?page/per_page`, `?limit=1`, AND bare — real unbounded-read/availability defect; but "must paginate / dash-underscore twin" framing was wrong |
| `DuplicateApiAuditTest.testFixCheckSingleSpelling` | **INVALID test → RETRACTED** | dash `/planned-workorder-line/*` = quote lines (`quote_api`); underscore `/planned_workorder_line/*` = planned-WO lines. DIFFERENT resources, not one split in two |
| `IrFlirContractApiTest.testFlirIndEndpointContract` | **test was invalid, already fixed; hardened further** | no genuine FLIR-IND endpoint exists; also removed a latent false-negative in my own Jul-29-morning fix |
| `SecurityHeadersApiTest.testContentTypeContract` | **VALID (same root cause as the list timeout)** | excluded the one known endpoint, correct |

## The over-claim I corrected (honesty note)

The 2026-07-23 "CRITICAL dash/underscore twin" finding — including the PDF shared with a developer —
was **wrong**. Proof from live swagger operationIds + summaries:
- `/planned-workorder-line/create` → `quote_api.create_quote_line`, summary "Create a new **quote line**"
- `/planned_workorder_line/` (POST) → `planned_workorder_line_api.create_planned_workorder_line`,
  summary "Create **Planned workorder line**"

Two distinct resources (Sales quote-lines vs Ops planned-WO-lines) with confusingly-similar paths —
not one resource registered twice. "Collapse to one route" would have been the wrong fix.

### Changes
- `DuplicateApiAuditTest`: removed `testFixCheckSingleSpelling` (false contract); renamed
  `testFixCheckUnderscoreListPaginates` → `testFixCheckUnderscoreListResponds` (asserts only the real
  timeout defect); downgraded `testDashUnderscoreTwins` from `critical` to `info` with corrected
  wording; corrected the generated report preamble.
- `IrFlirContractApiTest`: dropped the fragile blanket `/ir_photo/*` skip (would false-negative a real
  FLIR-IND endpoint shipped under `/ir_photo/`); the 3-param acceptance signature already excludes the
  existing pipeline (verified no current path carries all of ir_photo_key + visual_photo_key + platform).
- `docs/bug-repro/duplicate-api-endpoints/CORRECTION-2026-07-29.md`: written retraction for the shared
  evidence folder.

## Net result

Two genuinely-VALID product defects remain red on purpose (both need dev tickets):
1. **`GET /tasks/{sld_id}` → 500 globally** (tasks LIST broken since ~Jul-28; write path fine).
2. **`GET /planned_workorder_line/` unbounded-read timeout** (45s, never returns; live since Jul-09).

Everything else red was either an intentional-and-correct guard or a test bug — the one over-claim
(`single-spelling twin`) is retracted, so CI no longer asserts a defect that isn't one.
