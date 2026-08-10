# Upload Anything / onboarding jobs

**Verified:** 2026-08-10 against QA **V1.36** (`https://acme.qa.egalvanic.ai`).

Upload Anything lets a user drop documents (thermography reports, one-lines, panel schedules,
equipment lists, photos) onto an empty site; an AI extraction job turns them into assets.

## The active-job endpoint

```
GET /api/onboarding/jobs/active?sld_id=<site-uuid>
→ 200 { "success": true, "job": { … } }        // a job exists for that site
→ 200 { "success": true }                       // NO job  (note: `job` key simply absent)
```

**Trap:** "no active job" is a **200 with the `job` key missing**, not a 404 and not
`job: null`. Code (and tests) that check `res.ok` or `'job' in res` without checking the value
will misread an empty site as having a job.

### Job object (real sample, status `imported`)

| Field | Notes |
|---|---|
| `id`, `company_id`, `sld_id` | job identity + the site it belongs to |
| `status` | lifecycle — `pending` / `running` gate the invite; `imported` seen on a finished job |
| `execution_arn` | AWS Step Functions execution (`eg-pz-qa-ai-doc-extraction-sfn-ohio`) — extraction is a state machine, so jobs are genuinely long-running |
| `source_files[]` | `{filename, bytes, s3_key}` under `onboarding-jobs/<id>/source/` |
| `result_key` | `onboarding-jobs/<id>/output/onboarding_template.xlsx` |
| `summary_key` | `onboarding-jobs/<id>/output/summary.json` |
| `counts` | `total_assets`, `assets_with_location`, `by_class{}`, `connections`, `conductor_assets`, `node_attribute_values`, `documents[]` |
| `counts.warnings[]` | **rich bug-hunting surface** — free-text AI caveats |
| `error` | null on success |

### `counts.warnings` is worth mining

Real warnings from one QA job:

- an asset classed as `Transformer` whose nameplate suggests a fused main switch — *"needs human review of asset class"*
- manufacturer stated as `Other` → **not retained, treated as unknown**
- voltage / manufacturer / `phase_configuration` supplied but **dropped** because they are not
  eligible attributes on the Transformer class
- **"Document pages 7-9 failed to render (template errors) — possible additional assets not recoverable"**
- no feed/connection statements found → no connections submitted

Each of these is a testable claim about silent data loss: the extraction *admits* it dropped
fields and failed pages. Good hunting ground for "imported successfully but data is missing".

## The empty-site invite ("Let's get your assets in")

Copy: *"Let's get your assets in — This site doesn't have any assets yet. Upload anything you
have — thermography reports, one-line diagrams, panel schedules, equipment lists, or just a
fo[lder]…"*

### Conditions (verified by controlled experiment, PR #1127)

| Condition | Invite on `/assets` |
|---|---|
| Site empty, no active job | **shown** ← the control |
| Site empty, job `running` | hidden |
| Site empty, job `pending` | hidden |
| Site empty, `getActiveJob` rejects (network failure) | **shown** (fails open) |
| Site empty, `getActiveJob` returns HTTP 500 | **shown** (fails open) |
| Check still in flight | **held** — neither shown nor flashed, until the answer lands |

### Dashboard's copy of the invite is unreachable — IMPORTANT

`/dashboard` **does** call `getActiveJob` (observed), but its invite never renders in normal
navigation: dashboard is itself a FORCED_ALL page, so `sldId` is `'all'`, and the invite bails
on `'all'`. Verified 2026-08-10: on an empty site with **no** job, the Dashboard invite is
absent — the same result as with a running job.

**Consequence for testing:** any Dashboard assertion of the form "invite is hidden while a job
runs" is **vacuous** — it passes because the invite is never there. Only `/assets` can prove
the gate. This is exactly the trap that makes a green suite meaningless; see
[browser-testing-techniques.md](browser-testing-techniques.md) on control-first testing.

## Related

- Assets list (used to decide "site is empty"): `GET /api/lookup/v2/nodes/{sld_id}?page=1&page_size=25`
- Empty sites to test against: [qa-env-test-data.md](qa-env-test-data.md)
- Scope rules for `/jobs`: [forced-all-pages-and-site-scoping.md](forced-all-pages-and-site-scoping.md)
