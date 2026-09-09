# ZP-3863 — sample-entity picker for report authoring · QA verdict: **works, but still fails for IR — the ticket's own scenario**

**Ticket:** [ZP-3863](https://egalvanic.atlassian.net/browse/ZP-3863) — *"[Web] Report page authoring
sampled the newest session, so IR pages rendered empty and looked like broken templates"*
Task · Medium · labels `api`, `backend` · status **Ready for QA** · reporter Dharmesh Avaiya · assignee Eric Ehlert
**Artifact:** https://claude.ai/code/artifact/4406f190-0c4c-4183-abf3-eb50225d13a9
**Tested:** 2026-09-09 · **Env:** `acme.qa.egalvanic.ai` · V1.36 · tenant acme
**Endpoint:** `GET /api/reporting/sample-entities`
**Seat:** PM (`+project@`, 114 mapped sites, holds `reports.view`); cross-checked on Technician, Account
Manager, Client Portal, FM and Electrical Engineer — all on QA. The staff `list_sample_entities` tool was
also run, but it is pointed at **production**, so it is reported separately below and never used as QA
evidence.
**Evidence:** `docs/bug-evidence/zp-3863-sample-entities/`

---

## Verdict

The endpoint is live and does the main thing the ticket asked for: it ranks candidate work orders by how
much data the target query will actually find, so an author no longer lands on the newest-but-empty
session by default. **Six of the eight review steps pass.** Two fail, and one of them fails in exactly
the case the ticket is named after.

| # | Review step | Verdict |
|---|---|---|
| 1 | `entity_type=session` + `name_contains` returns candidates with work type + per-entity signal | ✅ PASS |
| 1b | `name_contains` is case-insensitive | ✅ PASS |
| 2 | `work_type` nudges the sort without hard-filtering | ❌ **FAIL** — see Defect A |
| 3 | `query_name` changes the reported signal | ❌ **FAIL** — see Defect B |
| 4 | a zero-signal candidate is called out rather than hidden | ✅ PASS |
| 5 | site scoping — no entity from an unmapped site appears | ⚠️ not runnable as written; substitute checks pass |
| 6 | negatives fail cleanly | ✅ PASS |
| 8 | `tests/test_company_scope.py` on dev | ⛔ blocked — no backend repo access |

---

## Defect A — asking for IR samples returns no IR work orders, and the endpoint says none exist

**This is the ticket's own headline case.** The ticket exists because IR report pages rendered empty.
The fix lets an author say "give me IR work orders". At the documented default limit, that request comes
back with **zero IR work orders** plus a note telling the author to stop trying.

**Repro — one request, no setup:**

```
GET /api/reporting/sample-entities?entity_type=session&work_type=IR
```

**Actual** (`count=10`, `work_type_preference="IR"`):

```
IR work orders in the response: 0        (all ten rows are [no work type])
note: "None carry work type 'IR' — most entities have no work type set at all.
       Judge from the names instead, or re-query with name_contains."
```

**Expected:** the IR work orders the tenant has are preferred to the top.

**The note is false.** The same request with `limit=25` — same seat, same data, seconds apart — returns
**4 IR work orders lifted to the top**:

| id | name | signal |
|---|---|---|
| `66e7e7b1-7dc8-4998-81ee-301f39ad940f` | test with ab 26 | 0 |
| `6b70f713-b2b5-44d8-a8ca-f84dc3ee2e58` | test wo 26 aug | 0 |
| `bb9c945a-3e2c-4ecd-bf58-15d84e2c0b4b` | QA-WT08 Infrared Thermography | 0 |
| `2b217cd3-a05d-447f-979a-45f069331510` | IR_WO_QA_2026-07-21 | 0 |

Stable across 5 consecutive runs, and identical via `config_type=session`. The product UI agrees the data
exists: the Work Orders grid (**Closed** tab) returns **1–9 of 9** matches for "Infrared", and
`QA-WT08 Infrared Thermography` opens as a real closed work order with an IR Photos tab — screenshots in
the evidence folder.

### The behaviour is a function of `limit`, and the default is in the broken range

Number of rows of the requested work type actually returned:

| `limit` | IR | IR Checklist | PM Forms | note emitted |
|---|---|---|---|---|
| 8 | 0 | 0 | 8 | yes (false) |
| **10 (default)** | **0** | **0** | 9 | **yes (false)** |
| 12 | 0 | 0 | 12 | yes (false) |
| 13 | 0 | 1 | 13 | yes (false) |
| 15 | 0 | 6 | 15 | yes (false) |
| 16 | 2 | 6 | 15 | — |
| 20 | 3 | 9 | 16 | — |
| 25 | 4 | 9 | 23 | — |

The match count **grows monotonically with `limit`**, which is the tell: the preference is not "lift every
matching entity to the top", it is "lift whichever matches happen to fall inside a signal-ranked pool
whose depth scales with `limit`". Every IR work order on this tenant has `signal` 0 or 1, so a shallow
pool never reaches them — and when the pool contains no match, the endpoint concludes none exist.

**Not IR-specific.** `COM` returns 1 match at `limit=10` and 6 at `limit=25`. IR and IR Checklist are just
the cases where the shortfall reaches zero at the default and triggers the false note. Work types with at
least one high-signal member (`PM Forms`, `AF`) mask the bug entirely.

**It reproduces on PRODUCTION too.** The staff `list_sample_entities` tool (pointed at prod) run against
`acme` with `work_type=IR` returns **10 candidates, none of them IR**, at its default limit of 10 — and
**6 IR work orders** at `limit=25` (*IR Scan*, *Infrared Thermography (i.R)*, *IR Scan* ×3,
*Demo-WO-IRScan*, *IR Thermography*). Different tenant data, same defect, so this is not a QA-data
artefact.

**Why it matters:** an author following the tool's own documented usage asks for IR, is told IR work orders
don't exist, falls back to "judge from the names", and picks a non-IR session — which renders an empty IR
page that looks like a broken template. That is verbatim the failure ZP-3863 was filed to remove.

**Root cause is a lead, not a confirmed diagnosis** — the pool-depth explanation fits every observation
above, but I could not read the backend source (see step 8). What would confirm it: the pool size used for
the work-type preference pass, and whether `signal` ordering is applied before that pass.

**Suggested fix:** resolve `work_type` matches independently of the signal-ranked pool, then merge, so a
zero-signal match still outranks a non-match. Only emit the "none carry work type X" note after
confirming zero matches **tenant-wide**, not zero matches in the current window. A signal of 0 is already
documented as legitimate ("0 means an empty render"), so zero-signal entities must not be unreachable.

## Defect B — four of the seven real session queries silently ignore `query_name`

Review step 3 asks that the reported signal track the bound query — issue count, task count, distinct
nodes, form instances. Three queries do this. Four accept the name and silently fall back to the default
`tasks+issues` ranking, returning **byte-identical signals to a query name I invented**:

| `query_name` (all real, all v2, per `list_report_queries`) | `ranked_by` | signals (limit 5) | |
|---|---|---|---|
| `eg_get_session_ids_with_issues` | `issue` | 15, 3, 3, 2, 1 | ✅ |
| `eg_get_session_ids_with_tasks` | `task` | 6, 6, 5, 4, 2 | ✅ |
| `eg_get_session_ids_with_eg_forms` | `eg_form` | 980, 0, 0, 0, 0 | ✅ |
| `eg_get_session_ids_with_nodes` | `tasks+issues` | 15, 9, 7, 6, 5 | ❌ |
| `eg_get_session_ids_with_forms` | `tasks+issues` | 15, 9, 7, 6, 5 | ❌ |
| `eg_get_session_ids_with_neta_forms` | `tasks+issues` | 15, 9, 7, 6, 5 | ❌ |
| `eg_get_session_ids_with_neta_forms_for_class` | `tasks+issues` | 15, 9, 7, 6, 5 | ❌ |
| `totally_made_up_query_name_xyz` *(control)* | `tasks+issues` | 15, 9, 7, 6, 5 | — |

`eg_get_session_ids_with_nodes` is described by the engine itself as *"Work orders with at least one
asset"* — the asset-page case named in the ticket's step 3. An author binding an asset page is ranked by
issues and tasks instead of by assets, with nothing in the response saying so.

**Suggested fix:** map the remaining four queries to their own signal, and reject an unrecognised
`query_name` rather than silently substituting the default — right now a typo and a real query are
indistinguishable in the response.

## Step 5 — why it could not be run as written, and what I checked instead

The step asks for a seat restricted to a subset of sites (`mapping_user_sld`). **No QA seat is both
site-restricted and able to call this endpoint:**

| seat | mapped sites | `reports.view` | endpoint |
|---|---|---|---|
| FM (`+fm@`) | 11 | no | **422** `permission_denied`, `required_permission: reports.view` |
| Electrical Engineer (`+electric@`) | 4 | no | **422** `permission_denied` |
| PM (`+project@`) | 114 | yes | 200 |
| Technician (`+tec@`) | 113 | yes | 200 |
| Account Manager (`+accountm@`) | 113 | yes | 200 |
| Client Portal (`+clientportal@`) | 234 | yes | 200 |

The two restricted seats are refused cleanly with the correct required permission — correct behaviour, but
it leaves the scoping assertion unexercised. Substitute checks, all passing:

- **Every candidate resolves to a site the seat is mapped to.** For each of the four seats that can call
  the endpoint, all 25 returned candidates were resolved via `GET /ir_session/{id}` and their site checked
  against that seat's own `accessible_sld_ids`: **0 out-of-scope entities**, 0 unresolved.
- **Scope is not overridable by parameter.** `company_id`, `company`, `subdomain`, `sld_id` and
  `mapping_user_sld` (foreign UUID / `demo`) are all ignored — the returned id list is *identical* to the
  unparameterised baseline, not merely the same length. No foreign tenant was touched.
- **A caller with wider reach is not enough to test scoping.** I originally cited the staff
  `list_sample_entities` tool as a per-caller scope comparison. **That was wrong and is retracted:** the
  staff MCP is pointed at **production** (its `acme` is `0a61e613-7887-4c10-99bf-59cedc4460f2`, branding in
  `eg-pz-prod-s3-branding-ohio`; QA's `acme` is `d59d449b-09d8-45d6-8f0a-ef70024b1293`), so its 6 IR work
  orders are different-environment data, not a differently-scoped view of the same tenant. It is not
  evidence for or against scoping. The two checks above stand on QA-only evidence.

**To close step 5 properly:** a seat with `reports.view` and a narrow `mapping_user_sld` (2–3 sites). I did
not create or modify any user to manufacture one.

## Steps that pass, with the evidence

**Step 1 —** `entity_type=session&name_contains=IR&limit=10` → 10 candidates, 3 distinct work types
(`None`×8, `IR Checklist`, `IR`), per-entity signals `16, 9, 5, 2, 1, 0, 0, 0, 0, 0`. Response carries
`count`, `entity_kind` (`ir_session_id`), `ranked_by`, `signal_meaning`, `name_contains`, and
`work_type_preference` when `work_type` is set. Baseline ordering is strictly signal-descending.

**Step 1b —** `name_contains=ir` and `name_contains=IR` return identical id sets.

**Step 2, the half that works —** `work_type` is correctly a *soft* preference, not a filter:
non-matching and no-work-type entities remain in the response. That part of the contract holds; only the
matching is unreliable (Defect A).

**Step 4 —** a zero-signal candidate is reported as an explicit numeric `signal: 0`, never omitted or
null — 5 of 10 rows in the `name_contains=IR` sample — and `signal_meaning` spells out the consequence:
*"rows of 'tasks+issues' this entity has for the query to read; 0 means an empty render"*. This is the
step that most directly addresses the original complaint, and it is solid.

**Step 6 —** negatives are clean:

| input | result |
|---|---|
| `entity_type=banana` | **400** `bad_entity_type`, message names the valid values |
| `config_type=not_a_type` | **400** `bad_entity_type`, same helpful message |
| `name_contains=zzz-no-such-entity-zzz` | **200**, `count=0`, empty list — not an error |
| only `entity_type=session` | **200**, 10 candidates |
| `config_type=session` / both params | **200**, 10 candidates |

No unscoped list is ever returned for a bad type. `limit` is capped at 25 (values above are clamped, not
rejected).

## Adjacent observation, not a ZP-3863 defect

The **Client Portal** seat reports **234 mapped sites — more than PM's 114 — and holds `reports.view`**,
so it can call this authoring endpoint. That may be correct for how portal users are mapped on this
tenant, and it is outside this ticket's scope, so I am recording it as a lead rather than a finding. Worth
a look by whoever owns portal scoping.

## Test data

- IR work orders invisible at the default limit:
  - https://acme.qa.egalvanic.ai/sessions/66e7e7b1-7dc8-4998-81ee-301f39ad940f — *test with ab 26*
  - https://acme.qa.egalvanic.ai/sessions/6b70f713-b2b5-44d8-a8ca-f84dc3ee2e58 — *test wo 26 aug*
  - https://acme.qa.egalvanic.ai/sessions/bb9c945a-3e2c-4ecd-bf58-15d84e2c0b4b — *QA-WT08 Infrared Thermography*
  - https://acme.qa.egalvanic.ai/sessions/2b217cd3-a05d-447f-979a-45f069331510 — *IR_WO_QA_2026-07-21*
- Work Orders grid, Closed tab, search "Infrared" → 1–9 of 9 — https://acme.qa.egalvanic.ai/sessions
- Raw responses for every step — `docs/bug-evidence/zp-3863-sample-entities/sample-entities-responses.json`
- Screenshots — `01-ir-work-order-exists.png`, `02-infrared-work-orders-grid.png`

**Footprint:** read-only. Every call was a `GET`; no work order, user, site or report config was created,
modified or deleted, and no foreign tenant was contacted. The Work Orders grid was filtered in the UI only.
